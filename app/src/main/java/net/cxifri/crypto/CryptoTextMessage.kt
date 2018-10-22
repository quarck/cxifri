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

// Draft class for what become CxifriMessage lately


package net.cxifri.crypto

import net.cxifri.keysdb.KeyEntry
import org.bouncycastle.crypto.BlockCipher
import org.bouncycastle.util.encoders.UrlBase64

class CryptoTextMessage(var createEngine: ()->BlockCipher, val keyGenerator: (String) -> ByteArray) {

    val binaryCryptor = CryptoBinaryMessage(createEngine)

    fun encrypt(message: String, key: ByteArray): String {

        val binaryMessage = message.toByteArray(charset = Charsets.UTF_8)
        val encoded = binaryCryptor.encrypt(binaryMessage, key)
        val base64 = UrlBase64.encode(encoded)
        return base64.toString(charset = Charsets.UTF_8)
    }

    fun encrypt(message: String, password: String): String {

        val key = keyGenerator(password)
        return encrypt(message, key)
    }

    fun encrypt(message: String, key: KeyEntry): String {
        return encrypt(message, key.asDecryptedBinary ?: throw Exception("Key failed"))
    }

    fun decrypt(message: String, key: ByteArray): String? {
        try {
            val unbase64 = UrlBase64.decode(message)
            val decrypt = binaryCryptor.decrypt(unbase64, key)
            if (decrypt != null)
                return decrypt.toString(charset = Charsets.UTF_8)
        }
        catch (ex: Exception) {
            return null
        }
        return null
    }

    fun decrypt(message: String, password: String): String? {
        val key = keyGenerator(password)
        return decrypt(message, key)
    }

    fun decrypt(message: String, key: KeyEntry): String? {
        return decrypt(message, key.asDecryptedBinary ?: throw Exception("Key failed"))
    }
}
