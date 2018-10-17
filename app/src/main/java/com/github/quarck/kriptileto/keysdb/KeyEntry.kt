package com.github.quarck.kriptileto.keysdb

import com.github.quarck.kriptileto.aks.AndroidKeyStore
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
            if (encrypted && !AndroidKeyStore.isSupported) {
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
