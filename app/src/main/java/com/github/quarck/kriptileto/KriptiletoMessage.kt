package com.github.quarck.kriptileto

import org.bouncycastle.crypto.BlockCipher
import org.bouncycastle.util.encoders.UrlBase64

class KriptiletoMessage() {

    val MESSAGE_FORMAT_PLAINTEXT: Byte = 0

    val binaryCryptor = CryptoBinaryMessage({AESTwofishSerpentEngine()})

    private fun deriveKeyFromPassword(password: String) =
        DerivedKeyGenerator.generateForAESTwofishSerpent(password)

    private fun encryptBinaryBlob(message: ByteArray, key: ByteArray): String {
        val encoded = binaryCryptor.encrypt(message, key)
        val base64 = UrlBase64.encode(encoded)
        return base64.toString(charset = Charsets.UTF_8)
    }

    private fun encryptBinaryBlob(message: ByteArray, password: String) =
            encryptBinaryBlob(message, deriveKeyFromPassword(password))


    private fun encryptBinaryBlob(message: ByteArray, key: KeyEntry) =
            encryptBinaryBlob(message, key.asDecryptedBinary ?: throw Exception("Key failed"))

    private fun decryptToBinaryBlob(message: String, key: ByteArray): ByteArray? {
        try {
            val unbase64 = UrlBase64.decode(UrlWrapper.unwrapOrKillSpaces(message))
            return binaryCryptor.decrypt(unbase64, key)
        }
        catch (ex: Exception) {
            return null
        }
    }

    private fun decryptToBinaryBlob(message: String, password: String) =
            decryptToBinaryBlob(message, deriveKeyFromPassword(password))

    private fun decryptToBinaryBlob(message: String, key: KeyEntry) =
            decryptToBinaryBlob(message, key.asDecryptedBinary ?: throw Exception("Key failed"))


    fun encrypt(message: String, key: ByteArray): String {
        val binaryMessage = byteArrayOf(MESSAGE_FORMAT_PLAINTEXT) + message.toByteArray(charset = Charsets.UTF_8)
        return encryptBinaryBlob(binaryMessage, key)
    }

    fun encrypt(message: String, password: String): String {
        val binaryMessage = byteArrayOf(MESSAGE_FORMAT_PLAINTEXT) + message.toByteArray(charset = Charsets.UTF_8)
        return encryptBinaryBlob(binaryMessage, password)
    }

    fun encrypt(message: String, key: KeyEntry): String {
        val binaryMessage = byteArrayOf(MESSAGE_FORMAT_PLAINTEXT) + message.toByteArray(charset = Charsets.UTF_8)
        return encryptBinaryBlob(binaryMessage, key)
    }

    private fun unpackBlob(blob: ByteArray?): String? {
        if (blob == null)
            return null
        if (blob.size <= 1)
            return null
        if (blob[0] == MESSAGE_FORMAT_PLAINTEXT) {
            return String(blob, 1, blob.size - 1, Charsets.UTF_8)
        }
        else {
            return null
        }
    }

    fun decrypt(message: String, key: ByteArray): String? {
        val blob = decryptToBinaryBlob(message, key)
        return unpackBlob(blob)
    }

    fun decrypt(message: String, password: String): String? {
        val blob = decryptToBinaryBlob(message, password)
        return unpackBlob(blob)
    }

    fun decrypt(message: String, key: KeyEntry): String? {
        val blob = decryptToBinaryBlob(message, key)
        return unpackBlob(blob)
    }
}
