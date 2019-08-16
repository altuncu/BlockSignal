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
import android.widget.Toast
import com.example.altuncu.blocksignal.recipients.Recipient
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.model.GetFileOptions
import com.example.altuncu.blocksignal.BlockstackActivity
import org.blockstack.android.sdk.model.PutFileOptions


class VerifyIdentity {

    private val TAG = VerifyIdentity::class.java.simpleName

    var phoneNumber: String = "NULL"

    private var _blockstackSession: BlockstackSession = BlockstackActivity().blockstackSession()

    fun verifyKeys(recipient: Recipient): Boolean {
        var appKey = "NULL"
        val options = GetFileOptions(decrypt = false, username = recipient.profileName, verify = true)
        _blockstackSession.getFile("blockstack/app.key", options, { contentResult ->
            if (contentResult.hasValue) {
                appKey = contentResult.value.toString()
                Log.d(TAG, "File contents: ${appKey}")
            } else {
                Log.d(TAG, contentResult.error)
            }
        })

        var number = "NULL"
        _blockstackSession.getFile("blockstack/phone.number",
                                    GetFileOptions(decrypt = false, username = recipient.profileName), {
            if (it.hasValue) {
                number = it.value.toString()
                Log.d(TAG, "File contents: ${number}")
            } else {
                Log.d(TAG, it.error)
            }
        })

        return (appKey != "NULL") && (number == recipient.address.toPhoneString())
    }

    fun storeNumber(number: String) {
        _blockstackSession.putFile("blockstack/phone.number", number, PutFileOptions(false), { readURLResult ->
            if (readURLResult.hasValue) {
                val readURL = readURLResult.value!!
                Log.d(TAG, "File stored at: ${readURL}")
            } else {
                Log.e(TAG, "error: " + readURLResult.error)
            }
        })
    }
}


