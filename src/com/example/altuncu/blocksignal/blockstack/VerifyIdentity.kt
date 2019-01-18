package com.example.altuncu.blocksignal.blockstack

import android.content.Context
import android.util.Log
import android.app.Activity
import com.example.altuncu.blocksignal.recipients.Recipient
import org.blockstack.android.sdk.*


class VerifyIdentity {

    private val TAG = VerifyIdentity::class.java.simpleName

    private var _blockstackSession: BlockstackSession? = null
    private var userData: UserData? = null

    fun verifyKeys(recipient: Recipient, context: Context): Boolean? {
        val runjs = RunJavaScript()
        val config = "https://flamboyant-darwin-d11c17.netlify.com"
                .toBlockstackConfig(kotlin.arrayOf(org.blockstack.android.sdk.Scope.StoreWrite))

        _blockstackSession = BlockstackSession(context, config)
        checkLogin()

        var appKey = getAppKey("app.key")
        var appOrigin = "https://flamboyant-darwin-d11c17.netlify.com"

        return runjs.fetchProfileValidateAppAddress(recipient.profileName, appKey, appOrigin)
    }

    private fun checkLogin() {
        val signedIn = blockstackSession().isUserSignedIn()
        if (signedIn) {
            userData = blockstackSession().loadUserData()
            if (userData != null) {
                //runOnUiThread {
                    onSignIn(userData!!)
                //}
            }
        }
    }

    private fun onSignIn(userData: UserData) {
        this.userData = userData
    }

    fun blockstackSession(): BlockstackSession {
        val session = _blockstackSession
        if (session != null) {
            return session
        } else {
            throw IllegalStateException("No session.")
        }
    }

    fun getAppKey(path: String): String {
        var result: String = "NULL"
        val options = GetFileOptions()
        blockstackSession().getFile(path, options, { contentResult ->
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


