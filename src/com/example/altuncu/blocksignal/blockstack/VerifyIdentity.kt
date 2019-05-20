package com.example.altuncu.blocksignal.blockstack

import android.content.Context
import android.util.Log
import com.example.altuncu.blocksignal.recipients.Recipient
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.model.GetFileOptions
import com.example.altuncu.blocksignal.BlockstackActivity
import org.whispersystems.libsignal.ecc.Curve.decodePoint
import org.whispersystems.libsignal.ecc.Curve.verifySignature


class VerifyIdentity {

    private val TAG = VerifyIdentity::class.java.simpleName

    private var _blockstackSession: BlockstackSession = BlockstackActivity().blockstackSession()
    //private var userData: UserData? = null

    fun verifyKeys(recipient: Recipient, context: Context): Boolean {
       /* val config = "https://flamboyant-darwin-d11c17.netlify.com"
                .toBlockstackConfig(arrayOf(org.blockstack.android.sdk.Scope.StoreWrite))

        _blockstackSession = BlockstackSession(context, config)
        checkLogin()*/

        var appKey = getAppKey(recipient.profileName)

        return verifySignature(decodePoint(recipient.profileKey, 0), recipient.profileName.toByteArray(),  appKey.toByteArray())
    }
/*
    private fun checkLogin() {
        val signedIn = _blockstackSession.isUserSignedIn()
        if (signedIn) {
            userData = _blockstackSession.loadUserData()
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
*/
    fun getAppKey(username: String): String {
        var result = "NULL"
        val options = GetFileOptions(decrypt = true, username = username)
        _blockstackSession.getFile("blockstack/app.key", options, { contentResult ->
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


