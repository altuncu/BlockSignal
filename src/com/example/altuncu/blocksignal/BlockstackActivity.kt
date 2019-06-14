/**
 * Copyright (c) 2019 Enes Altuncu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.example.altuncu.blocksignal

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.example.altuncu.blocksignal.crypto.IdentityKeyUtil
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.blockstack.android.sdk.*
import org.blockstack.android.sdk.model.GetFileOptions
import org.blockstack.android.sdk.model.PutFileOptions
import org.blockstack.android.sdk.model.UserData
import org.blockstack.android.sdk.model.toBlockstackConfig
import org.whispersystems.libsignal.ecc.Curve.calculateSignature
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class BlockstackActivity : AppCompatActivity() {
    private val TAG = BlockstackActivity::class.java.simpleName

    companion object {
        @JvmStatic
        var username: String? = null
        @JvmStatic
        var avatar: String? = null
        @JvmStatic
        var blockstackSession: BlockstackSession? = null
        @JvmStatic
        var phoneNumber: String = "NULL"
    }

    override fun onCreate(savedInstanceState: Bundle?) = runBlocking {
        super.onCreate(savedInstanceState)

        var nextIntent: Intent? = intent.getParcelableExtra("next_intent")

        val newSession = async { establishSession() }
        newSession.await()

        if (intent?.action == Intent.ACTION_VIEW) {
            handleAuthResponse(intent)
        }

        val signedIn = blockstackSession().isUserSignedIn()
        if (signedIn) {
            if (nextIntent == null) {
                nextIntent = Intent(this@BlockstackActivity, RegistrationActivity::class.java)
            }
            startActivity(nextIntent)
            finish()
        } else {
            blockstackSession().redirectUserToSignIn { errorResult ->
                if (errorResult.hasErrors) {
                    Toast.makeText(this@BlockstackActivity, "error: " + errorResult.error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun establishSession() {
        val config = "https://blocksignal.netlify.com"
                .toBlockstackConfig(arrayOf(Scope.StoreWrite))

        blockstackSession = BlockstackSession(this@BlockstackActivity, config)
    }

    private fun onSignIn() {
        var nextIntent: Intent? = intent.getParcelableExtra("next_intent")

        var userData: UserData? = blockstackSession().loadUserData()

        username = userData?.json?.getString("username")
        avatar = userData?.profile?.avatarImage

        val keyPair = IdentityKeyUtil.getIdentityKeyPair(this@BlockstackActivity)
        var sign = calculateSignature(keyPair.privateKey, userData?.decentralizedID?.toByteArray())

        blockstackSession?.putFile("blockstack/app.key", sign, PutFileOptions(true),
                { readURLResult ->
                    if (readURLResult.hasValue) {
                        val readURL = readURLResult.value!!
                        Log.d(TAG, "File stored at: ${readURL}")
                    } else {
                        Toast.makeText(this, "error: " + readURLResult.error, Toast.LENGTH_SHORT).show()
                    }
                })

        if (nextIntent == null) {
            nextIntent = Intent(this, RegistrationActivity::class.java)
        }

        startActivity(nextIntent)
        finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent")

       /* val userData = blockstackSession().loadUserData()
        if (userData != null) {
            runOnUiThread {
                onSignIn(userData)
            }
        } else {
            Toast.makeText(this, "no user data", Toast.LENGTH_SHORT).show()
        }*/
        if (intent?.action == Intent.ACTION_VIEW) {
            handleAuthResponse(intent)
        }
    }

    private fun handleAuthResponse(intent: Intent?) {
        val response = intent?.data?.query
        Log.d(TAG, "response ${response}")
        if (response != null) {
            val authResponseTokens = response.split('=')

            if (authResponseTokens.size > 1) {
                val authResponse = authResponseTokens[1]
                Log.d(TAG, "authResponse: ${authResponse}")
                blockstackSession().handlePendingSignIn(authResponse, {
                    if (it.hasErrors) {
                        Toast.makeText(this, it.error, Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d(TAG, "signed in!")
                        runOnUiThread {
                            onSignIn()
                        }
                    }
                })
            }
        }
    }

    fun blockstackSession(): BlockstackSession {
        val session = blockstackSession
        if (session != null) {
            return session
        } else {
            throw IllegalStateException("No session.")
        }
    }

    fun proveOwnership(number: String) {
        blockstackSession().putFile("blockstack/phone.number", number, PutFileOptions(false), { readURLResult ->
            if (readURLResult.hasValue) {
                val readURL = readURLResult.value!!
                Log.d(TAG, "File stored at: ${readURL}")
            } else {
                Toast.makeText(this@BlockstackActivity, "error: " + readURLResult.error, Toast.LENGTH_SHORT).show()
            }
        })

        val options = GetFileOptions(decrypt = false)
        blockstackSession().getFile("blockstack/phone.number", options, {
            if (it.hasValue) {
                phoneNumber = it.value.toString()
                Log.d(TAG, "File contents: ${it.value.toString()}")
            } else {
                Log.d(TAG, it.error)
            }
        })
    }
}