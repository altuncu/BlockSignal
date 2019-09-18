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
import android.content.DialogInterface
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.altuncu.blocksignal.recipients.Recipient
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.model.GetFileOptions
import com.example.altuncu.blocksignal.BlockstackActivity
import com.example.altuncu.blocksignal.ConversationListActivity
import com.example.altuncu.blocksignal.R
import com.example.altuncu.blocksignal.database.DatabaseFactory
import com.example.altuncu.blocksignal.database.IdentityDatabase
import org.blockstack.android.sdk.model.PutFileOptions
import java.net.URL


class VerifyIdentity {

    private val TAG = VerifyIdentity::class.java.simpleName

    private var _blockstackSession: BlockstackSession? = BlockstackActivity.blockstackSession

    fun verifyKeys(recipient: Recipient, context: Context) {
        val optionsKey = GetFileOptions(decrypt = false, username = recipient.profileName, verify = true,
                                     zoneFileLookupURL = URL("https://core.blockstack.org/v1/names"),
                                     app = "https://blocksignal.netlify.com")
        val optionsNumber = GetFileOptions(decrypt = false, username = recipient.profileName,
                                     zoneFileLookupURL = URL("https://core.blockstack.org/v1/names"),
                                     app = "https://blocksignal.netlify.com")
        val identityDatabase = DatabaseFactory.getIdentityDatabase(context)


        _blockstackSession?.getFile("blockstack/app.key", optionsKey) { contentResult ->
            if (contentResult.hasValue) {
                _blockstackSession?.getFile("blockstack/phone.number",
                        optionsNumber, {
                    if (it.hasValue && (it.value.toString() == recipient.address.toPhoneString())) {
                        Toast.makeText(context, "Verified by Blockstack", Toast.LENGTH_LONG).show()
                        identityDatabase.setVerified(recipient.address,
                                identityDatabase.getIdentity(recipient.address).get().getIdentityKey(),
                                IdentityDatabase.VerifiedStatus.VERIFIED)
                    } else {
                        Log.d(TAG, it.error)
                        identityDatabase.setVerified(recipient.address,
                                identityDatabase.getIdentity(recipient.address).get().getIdentityKey(),
                                IdentityDatabase.VerifiedStatus.UNVERIFIED)

                        val dialog = AlertDialog.Builder(context)
                        dialog.setTitle("Critical Security Issue")
                        dialog.setMessage("We detected an attack threatening your security. So, this conversation will be terminated.")
                        dialog.setIconAttribute(R.attr.dialog_alert_icon)
                        val dialogClickListener = { _: DialogInterface, _: Int ->
                            val intent = Intent(context, ConversationListActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            context.startActivity(intent)
                        }
                        dialog.setPositiveButton(R.string.ok, dialogClickListener)
                        dialog.create().show()
                    }
                })
            } else {
                Log.d(TAG, contentResult.error)
                identityDatabase.setVerified(recipient.address,
                        identityDatabase.getIdentity(recipient.address).get().getIdentityKey(),
                        IdentityDatabase.VerifiedStatus.UNVERIFIED)

                val dialog = AlertDialog.Builder(context)
                dialog.setTitle("Critical Security Issue")
                dialog.setMessage("We detected an attack threatening your security. So, this conversation will be terminated.")
                dialog.setIconAttribute(R.attr.dialog_alert_icon)
                val dialogClickListener = { _: DialogInterface, _: Int ->
                    val intent = Intent(context, ConversationListActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    context.startActivity(intent)
                }
                dialog.setPositiveButton(R.string.ok, dialogClickListener)
                dialog.create().show()
            }
        }
    }

    fun storeNumber(number: String) {
        _blockstackSession?.putFile("blockstack/phone.number", number, PutFileOptions(false), { readURLResult ->
            if (readURLResult.hasValue) {
                val readURL = readURLResult.value!!
                Log.d(TAG, "File stored at: ${readURL}")
            } else {
                Log.e(TAG, "error: " + readURLResult.error)
            }
        })
    }
}