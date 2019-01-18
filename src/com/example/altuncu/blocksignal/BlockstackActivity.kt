package com.example.altuncu.blocksignal

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.blockstack_activity.*
import org.blockstack.android.sdk.*


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

        val config = "https://flamboyant-darwin-d11c17.netlify.com"
                .toBlockstackConfig(kotlin.arrayOf(Scope.StoreWrite))

        _blockstackSession = BlockstackSession(this@BlockstackActivity, config)
        signInButton.isEnabled = true

        signInButton.setOnClickListener { _: View ->
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

        _blockstackSession?.putFile("blockstack/app.key", userData.decentralizedID, PutFileOptions(true),
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
}