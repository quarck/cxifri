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

package net.cxifri.crypto

import net.cxifri.encodings.GZipEncoder
import net.cxifri.keysdb.binaryKey
import org.bouncycastle.util.encoders.UrlBase64


class MessageHandler(
        private val binaryCryptor: BinaryMessageHandlerInterface
): MessageHandlerInterface {

    val MESSAGE_FORMAT_PLAINTEXT: Byte = 0
    val MESSAGE_FORMAT_GZIP_PLAINTEXT: Byte = 1
    val MESSAGE_FORMAT_KEY_REPLACEMENT: Byte = 2
    val MESSAGE_FORMAT_KEY_REVOKE: Byte = 3

    private fun packMessageForEncryption(message: MessageBase): ByteArray {

        return when (message) {
            is TextMessage ->
                packTextMessage(message)

            is KeyReplacementMessage ->
                packKeyReplacementMessage(message)

            is KeyRevokeMessage ->
                packKeyIsNoLongerSecureMessage(message)

            else ->
                throw NotImplementedError("No support for type ${message.javaClass.canonicalName}")
        }
    }

    private fun packTextMessage(message: TextMessage): ByteArray {

        val utf8 = message.text.toByteArray(charset = Charsets.UTF_8)

        val gzipUtf8 = GZipEncoder().deflate(utf8)

        if (utf8.size < gzipUtf8.size) {
            val binaryMessage = ByteArray(1 + utf8.size)
            binaryMessage[0] = MESSAGE_FORMAT_PLAINTEXT
            System.arraycopy(utf8, 0, binaryMessage, 1, utf8.size)
            return binaryMessage
        }

        val binaryMessage = ByteArray(1 + gzipUtf8.size)
        binaryMessage[0] = MESSAGE_FORMAT_GZIP_PLAINTEXT
        System.arraycopy(gzipUtf8, 0, binaryMessage, 1, gzipUtf8.size)
        return binaryMessage

    }

    private fun packKeyReplacementMessage(message: KeyReplacementMessage): ByteArray {
//        val newBinaryKey = message.newKey.asDecryptedBinary
//        if (newBinaryKey == null)
//            throw Exception("Key cannot be accessed")
//        val binaryMessage = ByteArray(2 + newBinaryKey.size)
//        binaryMessage[0] = MESSAGE_FORMAT_KEY_REPLACEMENT
//        binaryMessage[1] = if (message.receiverMustDeleteOldKey) 1 else 0
//        System.arraycopy(newBinaryKey, 0, binaryMessage, 2, newBinaryKey.size)
//        return binaryMessage
        TODO("packKeyReplacement message is not implemented yet")
    }

    private fun packKeyIsNoLongerSecureMessage(message: KeyRevokeMessage): ByteArray {
        // just a single byte - message code for this one
        return byteArrayOf(MESSAGE_FORMAT_KEY_REVOKE)
    }


    private fun unpackDecryptedMessage(key: KeyEntry, blob: ByteArray?): MessageBase? {
        if (blob == null || blob.size < 1)
            return null

        return when (blob[0]) {
            MESSAGE_FORMAT_PLAINTEXT, MESSAGE_FORMAT_GZIP_PLAINTEXT ->
                unpackTextMessage(key, blob)

            MESSAGE_FORMAT_KEY_REPLACEMENT ->
                unpackReplacementKeyMessage(key, blob)

            MESSAGE_FORMAT_KEY_REVOKE ->
                unpackKeyRevokeMessage(key, blob)

            else ->
                null
        }
    }

    private fun unpackTextMessage(key: KeyEntry, blob: ByteArray): TextMessage? {

        when (blob[0]) {
            MESSAGE_FORMAT_PLAINTEXT -> {
                return TextMessage(key, String(blob, 1, blob.size - 1, Charsets.UTF_8))
            }

            MESSAGE_FORMAT_GZIP_PLAINTEXT -> {
                val ungzip = GZipEncoder().inflate(blob, 1, blob.size-1)
                return ungzip?.let { TextMessage(key, ungzip.toString(charset=Charsets.UTF_8)) }
            }

            else ->
                return null
        }
    }

    private fun unpackReplacementKeyMessage(key: KeyEntry, blob: ByteArray): KeyReplacementMessage? {

//        if (blob.size <= 2)
//            return null
//
//        val receiverMustDeleteOldKey = blob[1] != 0.toByte()
//        val binaryKey = ByteArray(blob.size - 2)
//        System.arraycopy(blob, 2, binaryKey, 0, binaryKey.size)
//
//        return KeyReplacementMessage(key, KeyEntry(binaryKey), receiverMustDeleteOldKey)
        TODO("Unpack key replacement isnt implemented")
    }

    private fun unpackKeyRevokeMessage(key: KeyEntry, blob: ByteArray): KeyRevokeMessage? {
        // nothing to unpack and first byte was already verified
        return KeyRevokeMessage(key)
    }

    override fun encrypt(message: MessageBase, key: KeyEntry): String {
        val packed = packMessageForEncryption(message)
        val encoded = binaryCryptor.encrypt(packed,
                key.binaryKey ?: throw Exception("Cannot access encryption key"))
        val base64 = UrlBase64.encode(encoded)
        return base64.toString(charset = Charsets.UTF_8)
    }

    override fun decrypt(message: String, key: KeyEntry): MessageBase? {

        var decryptedBinary: ByteArray?

        try {
            val unbase64 = UrlBase64.decode(message)
            decryptedBinary = binaryCryptor.decrypt(unbase64,
                    key.binaryKey ?: throw Exception("Cannot access encryption key"))
        }
        catch (ex: Exception) {
            decryptedBinary = null
        }

        return if (decryptedBinary != null)
            unpackDecryptedMessage(key, decryptedBinary)
        else
            null
    }

    override fun decrypt(message: String, keys: List<KeyEntry>): MessageBase? {

        val unbase64: ByteArray

        try {
            unbase64 = UrlBase64.decode(message)
        }
        catch (ex: Exception) {
            return null
        }

        var decryptedBinary: ByteArray? = null
        var matchedKey: KeyEntry? = null

        for (key in keys) {
            try {
                val binaryKey = key.binaryKey
                if (binaryKey != null) {
                    decryptedBinary = binaryCryptor.decrypt(unbase64, binaryKey)
                    if (decryptedBinary != null) {
                        matchedKey = key
                        break
                    }
                }
            } catch (ex: Exception) {
                decryptedBinary = null
            }
        }

        return if (decryptedBinary != null && matchedKey != null)
            unpackDecryptedMessage(matchedKey, decryptedBinary)
        else
            null
    }
}
