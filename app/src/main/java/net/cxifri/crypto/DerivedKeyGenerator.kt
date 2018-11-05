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

import net.cxifri.utils.wipe
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.KeyParameter

class DerivedKeyGenerator {

    // TODO: unit test it to make sure differnt secrets do produce different pws, as well
    // as different salts
    // also test byteArrayof(0, 1, 1, .... 1) vs byteArrayOf(0, 2, 2, ... 2), e.g. first zero
    fun generateKeyBits(secret: ByteArray, salt: ByteArray): ByteArray {
        val gen = PKCS5S2ParametersGenerator(SHA256Digest())
        gen.init(secret, salt, NUM_ITERATIONS)
        val param = gen.generateDerivedMacParameters(KEY_LEN_BYTES_EACH * 8) as KeyParameter
        return param.key
    }

    // TODO: ensure different keys produce different binary keys
    fun generateFromSharedSecret(textSecret: ByteArray, authSecret: ByteArray, name: String=""): KeyEntry {

        val textKey = generateKeyBits(textSecret, textSaltAES) +
                        generateKeyBits(textSecret, textSaltTwofish) +
                        generateKeyBits(textSecret, textSaltSerpent)

        val authKey = generateKeyBits(authSecret, macSaltAES) +
                generateKeyBits(authSecret, macSaltTwofish) +
                generateKeyBits(authSecret, macSaltSerpent)

        return KeyEntry(textKey, authKey, name)
    }

    fun generateFromSharedSecret(wholeSecret: ByteArray, name: String=""): KeyEntry {
        val textSecret = ByteArray(wholeSecret.size / 2)
        val authSecret = ByteArray(wholeSecret.size / 2)

        System.arraycopy(wholeSecret, 0, textSecret, 0, wholeSecret.size / 2)
        System.arraycopy(wholeSecret, wholeSecret.size / 2, authSecret, 0, wholeSecret.size / 2)

        val ret = generateFromSharedSecret(textSecret, authSecret, name)

        textSecret.wipe()
        authSecret.wipe()
        return ret
    }

    fun generateFromTextPassword(password: String, name: String=""): KeyEntry {
        val passAsByteArray = password.toByteArray(charset = Charsets.UTF_8)
        val ret = generateFromSharedSecret(passAsByteArray, passAsByteArray, name)
        passAsByteArray.wipe()
        return ret
    }

    companion object {

        const val NUM_ITERATIONS = 10000

        const val SALT_TEXT_AES = "cxifri-text-AES"
        const val SALT_TEXT_TWOFISH = "cxifri-text-Twofish"
        const val SALT_TEXT_SERPENT = "cxifri-text-Serpent"

        const val SALT_MAC_AES = "cxifri-mac-AES"
        const val SALT_MAC_TWOFISH = "cxifri-mac-Twofish"
        const val SALT_MAC_SERPENT = "cxifri-mac-Serpent"

        const val KEY_LEN_BYTES_EACH = 32

        val textSaltAES = SALT_TEXT_AES.toByteArray(charset = Charsets.UTF_8)
        val textSaltTwofish = SALT_TEXT_TWOFISH.toByteArray(charset = Charsets.UTF_8)
        val textSaltSerpent = SALT_TEXT_SERPENT.toByteArray(charset = Charsets.UTF_8)

        val macSaltAES = SALT_MAC_AES.toByteArray(charset = Charsets.UTF_8)
        val macSaltTwofish = SALT_MAC_TWOFISH.toByteArray(charset = Charsets.UTF_8)
        val macSaltSerpent = SALT_MAC_SERPENT.toByteArray(charset = Charsets.UTF_8)
    }
}
