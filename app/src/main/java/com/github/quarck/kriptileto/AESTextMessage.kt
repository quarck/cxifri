package com.github.quarck.kriptileto

import org.bouncycastle.crypto.CryptoException
import org.bouncycastle.util.encoders.UrlBase64

object AESTextMessage {

    fun encrypt(message: String, key: ByteArray): String {

        val binaryMessage = message.toByteArray(charset = Charsets.UTF_8)
        val encoded = AESBinaryMessage.encrypt(binaryMessage, key)
        val base64 = UrlBase64.encode(encoded)
        return base64.toString(charset = Charsets.UTF_8)
    }

    fun encrypt(message: String, password: String): String {

        val key = DerivedKeyGenerator.generate(password, "", 0, AESBinaryMessage.KEY_LEN_MAX)
                ?: throw CryptoException("Failed to derive key")
        return encrypt(message, key)
    }

    fun encrypt(message: String, key: KeyEntry): String {
        return encrypt(message, key.asDecryptedBinary ?: throw Exception("Key failed"))
    }

    fun decrypt(message: String, key: ByteArray): String? {
        try {
            val unbase64 = UrlBase64.decode(message)
            val decrypt = AESBinaryMessage.decrypt(unbase64, key)
            if (decrypt != null)
                return decrypt.toString(charset = Charsets.UTF_8)
        }
        catch (ex: Exception) {
            return null
        }
        return null
    }

    fun decrypt(message: String, password: String): String? {
        val key = DerivedKeyGenerator.generate(password, "", 0, AESBinaryMessage.KEY_LEN_MAX)
                ?: throw CryptoException("Failed to derive key")
        return decrypt(message, key)
    }

    fun decrypt(message: String, key: KeyEntry): String? {
        return decrypt(message, key.asDecryptedBinary ?: throw Exception("Key failed"))
    }
}
