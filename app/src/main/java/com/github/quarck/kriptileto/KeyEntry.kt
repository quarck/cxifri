package com.github.quarck.kriptileto

import org.bouncycastle.util.encoders.UrlBase64

data class KeyEntry(
        val id: Long,
        val name: String,
        val value: String,
        val encrypted: Boolean,
        val replaceRequested: Boolean = false,
        val replacementKeyId: Long = 0,
        val deleteAfter: Long = 0
) {
    val binaryPTKey: ByteArray?
        get() {
            if (encrypted && !AndroidKeyStore.isSupported) {
                return null // no encryption is supported - can't decrypt
            }
            val unbase64 = UrlBase64.decode(value)
            if (encrypted) {
                val aks = AndroidKeyStore()
                return aks.decrypt(unbase64)
            } else {
                return unbase64
            }
        }

    companion object {
        fun fromBinaryPTKey(name: String, key: ByteArray): KeyEntry {
            if (AndroidKeyStore.isSupported) {
                val aks = AndroidKeyStore()
                val encryptedKey = aks.encrypt(key)
                val encryptedBase64Key = UrlBase64.encode(encryptedKey)
                return KeyEntry(-1, name, encryptedBase64Key.toString(charset = Charsets.UTF_8), true)
            }
            else {
                val base64Key = UrlBase64.encode(key)
                return KeyEntry(-1, name, base64Key.toString(charset = Charsets.UTF_8), false)
            }
        }
    }
}
