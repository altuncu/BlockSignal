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


