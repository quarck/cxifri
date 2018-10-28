/*
 * Copyright (C) 2018 Sergey Parshin (quarck@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.cxifri.keysdb

import android.content.Context
import android.os.Build
import net.cxifri.R
import net.cxifri.aks.AndroidKeyStore
import net.cxifri.crypto.KeyEntry
import org.bouncycastle.util.encoders.UrlBase64

val KeyEntry.asDecryptedBinary: ByteArray?
    get() {
        if (encrypted && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)) {
            return null // no encryption is supported - can't decrypt
        }
        val unbase64 = UrlBase64.decode(value)
        if (encrypted) {
            try {
                val aks = AndroidKeyStore()
                return aks.decrypt(id, unbase64)
            }
            catch (ex: Exception) {
                return null
            }
        } else {
            return unbase64
        }
    }

fun KeyEntry.toStringDetails(context: Context): String {

    if (revoked)
        return context.getString(R.string.revoked_key)
    else if (encrypted)
        return context.getString(R.string.encrypted_by_aks_key)
    else
        return context.getString(R.string.stored_as_pt_key)
}


class KeyHelper {

    fun saveKey(context: Context, name: String, key: ByteArray, preferAndroidKeyStore: Boolean) {
        KeysDatabase(context).use {
            db ->

            // temp name to make sure it was updated
            val keyForName = KeyEntry(-1, "_", "", false)
            val id = db.add(keyForName)

            val updatedKeyEntry =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && preferAndroidKeyStore) {
                        val aks = AndroidKeyStore()
                        aks.createKey(id) // create matching keystore key that would be encrypting this key in DB
                        val encryptedKey = aks.encrypt(id, key)
                        val encryptedBase64Key = UrlBase64.encode(encryptedKey)
                        KeyEntry(id, name, encryptedBase64Key.toString(charset = Charsets.UTF_8), true)
                    }
                    else {
                        val base64Key = UrlBase64.encode(key)
                        KeyEntry(id, name, base64Key.toString(charset = Charsets.UTF_8), false)
                    }

            db.update(updatedKeyEntry)
        }
    }

}