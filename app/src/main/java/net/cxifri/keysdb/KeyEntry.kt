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

import android.os.Build
import net.cxifri.aks.AndroidKeyStore
import org.bouncycastle.util.encoders.UrlBase64

data class KeyEntry(
        val id: Long,
        var name: String,
        var value: String,
        var encrypted: Boolean,
        var replaceRequested: Boolean = false,
        var replacementKeyId: Long = 0,
        var deleteAfter: Long = 0
) {
    fun toStringDetails(): String {
        if (encrypted)
            return "Encrypted by AndroidKeyStore"
        else
            return "No AndroidKeyStore support - stored as PT"
    }

    val asDecryptedBinary: ByteArray?
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

    companion object {
        fun forName(name: String) = KeyEntry(-1, name, "", false)

//        fun fromBinaryPTKey(name: String, key: ByteArray): KeyEntry {
//            if (AndroidKeyStore.isSupported) {
//                val aks = AndroidKeyStore()
//                val encryptedKey = aks.encrypt(key)
//                val encryptedBase64Key = UrlBase64.encode(encryptedKey)
//                return KeyEntry(-1, name, encryptedBase64Key.toString(charset = Charsets.UTF_8), true)
//            }
//            else {
//                val base64Key = UrlBase64.encode(key)
//                return KeyEntry(-1, name, base64Key.toString(charset = Charsets.UTF_8), false)
//            }
//        }
    }
}
