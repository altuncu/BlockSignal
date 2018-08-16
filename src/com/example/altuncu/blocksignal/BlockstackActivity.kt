package com.example.altuncu.blocksignal

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

import android.content.Intent
import android.view.View
import android.widget.Toast
import android.util.Log
import kotlinx.android.synthetic.main.blockstack_activity.*
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.BlockstackConfig
import org.blockstack.android.sdk.Scope
import org.blockstack.android.sdk.UserData
import java.net.URI
// TODO => Add next intent to ConversationListActivity & Update XML & Handle Blockstack sign-in
class BlockstackActivity : AppCompatActivity() {

    private var _blockstackSession: BlockstackSession? = null
    private val TAG = BlockstackActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.blockstack_activity)

        signInButton.isEnabled = false

        val config = java.net.URI("https://flamboyant-darwin-d11c17.netlify.com").run {
            org.blockstack.android.sdk.BlockstackConfig(
                    this,
                    java.net.URI("${this}/redirect"),
                    java.net.URI("${this}/manifest.json"),
                    kotlin.arrayOf(org.blockstack.android.sdk.Scope.StoreWrite))
        }

        _blockstackSession = BlockstackSession(this, config,
                onLoadedCallback = {
                    // Wait until this callback fires before using any of the
                    // BlockstackSession API methods

                    signInButton.isEnabled = true
                })

        signInButton.setOnClickListener { _: View ->
            blockstackSession().redirectUserToSignIn { userDataResult ->
                if (userDataResult.hasValue) {
                    Log.d(TAG, "signed in!")
                    runOnUiThread {
                        onSignIn(userDataResult.value!!)
                    }
                } else {
                    Toast.makeText(this, "error: " + userDataResult.error, Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (intent?.action == Intent.ACTION_VIEW) {
            handleAuthResponse(intent)
        }
    }

    private fun onSignIn(userData: UserData) {
        userDataTextView.text = "Signed in as ${userData.decentralizedID}"
        signInButton.isEnabled = false
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent")

        if (intent?.action == Intent.ACTION_MAIN) {
            blockstackSession().loadUserData { userData ->
                if (userData != null) {
                    runOnUiThread {
                        onSignIn(userData)
                    }
                } else {
                    Toast.makeText(this, "no user data", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (intent?.action == Intent.ACTION_VIEW) {
            handleAuthResponse(intent)
        }
       // startActivity(Intent(this, ConversationListActivity::class.java))
    }

    private fun handleAuthResponse(intent: Intent) {
        val response = intent.dataString
        Log.d(TAG, "response ${response}")
        if (response != null) {
            val authResponseTokens = response.split(':')

            if (authResponseTokens.size > 1) {
                val authResponse = authResponseTokens[1]
                Log.d(TAG, "authResponse: ${authResponse}")
                blockstackSession().handlePendingSignIn(authResponse, { userDataResult ->
                    if (userDataResult.hasValue) {
                        val userData = userDataResult.value!!
                        Log.d(TAG, "signed in!")
                        runOnUiThread {
                            onSignIn(userData)
                        }
                    } else {
                        Toast.makeText(this, "error: " + userDataResult.error, Toast.LENGTH_SHORT).show()
                    }
                })
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


}