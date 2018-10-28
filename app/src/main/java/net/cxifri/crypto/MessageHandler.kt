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

import net.cxifri.dataprocessing.GZipBlob
import net.cxifri.keysdb.asDecryptedBinary
import org.bouncycastle.util.encoders.UrlBase64


class MessageHandler(
        private val binaryCryptor: BinaryMessageHandlerInterface
): MessageHandlerInterface {

    // TODO: we certainly need "tryDecryt" method receiving an array of keys as an argument
    // so to avoid repeated un-base64-ing of the message every time

    val MESSAGE_FORMAT_PLAINTEXT: Byte = 0
    val MESSAGE_FORMAT_GZIP_PLAINTEXT: Byte = 1
    val MESSAGE_FORMAT_REPLACEMENT_KEY: Byte = 2
    val MESSAGE_FORMAT_KEY_IS_NO_LONGER_SECURE: Byte = 3

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

    private fun packKeyReplacementMessage(message: KeyReplacementMessage): ByteArray {
        TODO("packKeyReplacementMessage is not implemented")
    }

    private fun packKeyIsNoLongerSecureMessage(message: KeyRevokeMessage): ByteArray {
        TODO("packKeyIsNoLongerSecureMessage is not implemented")
    }


    private fun unpackDecryptedMessage(key: KeyEntry, blob: ByteArray?): MessageBase? {
        if (blob == null)
            return null
        if (blob.size < 1)
            return null

        return when (blob[0]) {
            MESSAGE_FORMAT_PLAINTEXT, MESSAGE_FORMAT_GZIP_PLAINTEXT ->
                unpackTextMessage(key, blob)
            MESSAGE_FORMAT_REPLACEMENT_KEY ->
                unpackReplacementKeyMessage(key, blob)
            MESSAGE_FORMAT_KEY_IS_NO_LONGER_SECURE ->
                unpackKeyRevokeMessage(key, blob)
            else ->
                null
        }
    }

    private fun unpackTextMessage(key: KeyEntry, blob: ByteArray): TextMessage? {
        if (blob[0] == MESSAGE_FORMAT_PLAINTEXT) {
            return TextMessage(key, String(blob, 1, blob.size - 1, Charsets.UTF_8))
        }
        else if (blob[0] == MESSAGE_FORMAT_GZIP_PLAINTEXT) {
            val ungzip = GZipBlob().inflate(blob, 1, blob.size-1)
            return if (ungzip != null)
                TextMessage(key, ungzip.toString(charset=Charsets.UTF_8))
            else
                null
        }
        else {
            return null
        }
    }

    private fun unpackReplacementKeyMessage(key: KeyEntry, blob: ByteArray): KeyReplacementMessage? {
        TODO("NOt implemented")
    }

    private fun unpackKeyRevokeMessage(key: KeyEntry, blob: ByteArray): KeyRevokeMessage? {
        TODO("NOt Implemented")
    }

    override fun encrypt(message: MessageBase, key: KeyEntry): String {
        val packed = packMessageForEncryption(message)
        val encoded = binaryCryptor.encrypt(packed, key.asDecryptedBinary ?: throw Exception("Key failed"))
        val base64 = UrlBase64.encode(encoded)
        return base64.toString(charset = Charsets.UTF_8)
    }

    override fun decrypt(message: String, key: KeyEntry): MessageBase? {

        var decryptedBinary: ByteArray? = null

        try {
            val unbase64 = UrlBase64.decode(message)
            decryptedBinary = binaryCryptor.decrypt(unbase64, key.asDecryptedBinary ?: throw Exception("Key failed"))
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
                decryptedBinary = binaryCryptor.decrypt(unbase64, key.asDecryptedBinary
                        ?: throw Exception("Key failed"))
                matchedKey = key
                break
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
