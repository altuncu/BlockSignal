package com.example.altuncu.blocksignal

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.example.altuncu.blocksignal.crypto.IdentityKeyUtil
import kotlinx.android.synthetic.main.blockstack_activity.*
import org.blockstack.android.sdk.*
import org.blockstack.android.sdk.model.GetFileOptions
import org.blockstack.android.sdk.model.PutFileOptions
import org.blockstack.android.sdk.model.UserData
import org.blockstack.android.sdk.model.toBlockstackConfig
import org.whispersystems.libsignal.ecc.Curve.calculateSignature


class BlockstackActivity : AppCompatActivity() {
    private val TAG = BlockstackActivity::class.java.simpleName

    private var _blockstackSession: BlockstackSession? = null

    companion object {
        @JvmStatic
        var _username: String? = null
        @JvmStatic
        var _avatar: String? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.blockstack_activity)

        signInButton.isEnabled = false

        val config = "https://altuncu.github.io/BlockSignal/redirect"
                .toBlockstackConfig(arrayOf(Scope.StoreWrite))

        _blockstackSession = BlockstackSession(this, config)

        signInButton.isEnabled = true

        signInButton.setOnClickListener {
            blockstackSession().redirectUserToSignIn { errorResult ->
                if (errorResult.hasErrors) {
                    Toast.makeText(this, "error: " + errorResult.error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onSignIn(userData: UserData) {
        var nextIntent: Intent? = intent.getParcelableExtra("next_intent")

        _username = userData.json.getString("username")
        _avatar = userData.profile?.avatarImage

        val keyPair = IdentityKeyUtil.getIdentityKeyPair(this@BlockstackActivity)
        var sign = calculateSignature(keyPair.privateKey, userData.decentralizedID.toByteArray())

        _blockstackSession?.putFile("blockstack/app.key", sign, PutFileOptions(true),
                { readURLResult ->
                    if (readURLResult.hasValue) {
                        val readURL = readURLResult.value!!
                        Log.d(TAG, "File stored at: ${readURL}")
                    } else {
                        Toast.makeText(this, "error: " + readURLResult.error, Toast.LENGTH_SHORT).show()
                    }
                })

        if (nextIntent == null) {
            nextIntent = Intent(this@BlockstackActivity, ConversationListActivity::class.java)
        }

        startActivity(nextIntent)
        finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent")

        if (intent?.action == Intent.ACTION_MAIN) {
            val userData = blockstackSession().loadUserData()
            if (userData != null) {
                runOnUiThread {
                    onSignIn(userData)
                }
            } else {
                Toast.makeText(this, "no user data", Toast.LENGTH_SHORT).show()
            }
        } else if (intent?.action == Intent.ACTION_VIEW) {
            handleAuthResponse(intent)
        }
    }

    private fun handleAuthResponse(intent: Intent?) {
        val response = intent?.dataString
        Log.d(TAG, "response ${response}")
        if (response != null) {
            val authResponseTokens = response.split(':')

            if (authResponseTokens.size > 1) {
                val authResponse = authResponseTokens[1]
                Log.d(TAG, "authResponse: ${authResponse}")
                blockstackSession().handlePendingSignIn(authResponse) { userDataResult: Result<UserData> ->

                    if (userDataResult.hasValue) {
                        val userData = userDataResult.value!!
                        Log.d(TAG, "signed in!")
                        runOnUiThread {
                            onSignIn(userData)
                        }
                    } else {
                        Toast.makeText(this, "error: " + userDataResult.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun blockstackSession(): BlockstackSession {
        val session = _blockstackSession
        if (session != null) {
            return session
        } else {
            throw IllegalStateException("No session.")
        }
    }

    fun storePhoneNumber(number: String) {
        blockstackSession().putFile("blockstack/phone.number", number, PutFileOptions(true),
                { readURLResult ->
                    if (readURLResult.hasValue) {
                        val readURL = readURLResult.value!!
                        Log.d(TAG, "File stored at: ${readURL}")
                    } else {
                        Toast.makeText(this, "error: " + readURLResult.error, Toast.LENGTH_SHORT).show()
                    }
                })
    }

    fun getPhoneNumber(): String {
        var result = "NULL"
        val options = GetFileOptions(decrypt = true)
        blockstackSession().getFile("blockstack/phone.number", options, { contentResult ->
            if (contentResult.hasValue) {
                result = contentResult.value.toString()
                Log.d(TAG, "File contents: ${result}")
            } else {
                Log.d(TAG, contentResult.error)
            }
        })
        return result
    }
}