package com.github.quarck.kriptileto.crypto

import com.github.quarck.kriptileto.dataprocessing.GZipBlob
import com.github.quarck.kriptileto.dataprocessing.UrlWrapper
import com.github.quarck.kriptileto.keysdb.KeyEntry
import org.bouncycastle.util.encoders.UrlBase64

class KriptiletoMessage() {

    val MESSAGE_FORMAT_PLAINTEXT: Byte = 0
    val MESSAGE_FORMAT_GZIP_PLAINTEXT: Byte = 1

    val binaryCryptor = CryptoBinaryMessage({ AESTwofishSerpentEngine() })

    private fun deriveKeyFromPassword(password: String) =
            DerivedKeyGenerator().generateForAESTwofishSerpent(password)

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

    fun packBinaryBlob(message: String): ByteArray {
        val utf8 = message.toByteArray(charset = Charsets.UTF_8)

        val gzipUtf8 = GZipBlob().deflate(utf8)

        if (utf8.size < gzipUtf8.size) {
            val binaryMessage = ByteArray(1 + utf8.size)
            binaryMessage[0] = MESSAGE_FORMAT_PLAINTEXT
            System.arraycopy(utf8, 0, binaryMessage, 1, utf8.size)
            return binaryMessage
        }
        else {
            val binaryMessage = ByteArray(1 + gzipUtf8.size)
            binaryMessage[0] = MESSAGE_FORMAT_GZIP_PLAINTEXT
            System.arraycopy(gzipUtf8, 0, binaryMessage, 1, gzipUtf8.size)
            return binaryMessage
        }
    }


    fun encrypt(message: String, key: ByteArray): String {
        return UrlWrapper.wrap(encryptBinaryBlob(packBinaryBlob(message), key))
    }

    fun encrypt(message: String, password: String): String {
        return UrlWrapper.wrap(encryptBinaryBlob(packBinaryBlob(message), password))
    }

    fun encrypt(message: String, key: KeyEntry): String {
        return UrlWrapper.wrap(encryptBinaryBlob(packBinaryBlob(message), key))
    }

    private fun unpackBlob(blob: ByteArray?): String? {
        if (blob == null)
            return null
        if (blob.size < 1)
            return null
        if (blob[0] == MESSAGE_FORMAT_PLAINTEXT) {
            return String(blob, 1, blob.size - 1, Charsets.UTF_8)
        }
        else if (blob[0] == MESSAGE_FORMAT_GZIP_PLAINTEXT) {
            val ungzip = GZipBlob().inflate(blob, 1, blob.size-1)
            return if (ungzip != null)
                       ungzip.toString(charset=Charsets.UTF_8)
                    else
                        null
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
