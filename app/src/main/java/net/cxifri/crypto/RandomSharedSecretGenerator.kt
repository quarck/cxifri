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

import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.macs.CBCBlockCipherMac
import org.bouncycastle.crypto.params.KeyParameter
import java.security.SecureRandom

class RandomSharedSecretGenerator {
    val random: SecureRandom by lazy { SecureRandom() }

    // Mac is used as CRC for code transmission, thus no need for any secure key
    val crcKey = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    fun generate(): ByteArray {
        val key = ByteArray(SHARED_SECRET_LEN)
        random.nextBytes(key)
        return key
    }

    fun generateKeywithCSum(): Pair<ByteArray, ByteArray> {
        val key = ByteArray(SHARED_SECRET_LEN)
        random.nextBytes(key)
        val mac = CBCBlockCipherMac(AESEngine())

        mac.init(KeyParameter(crcKey))
        mac.update(key, 0, key.size)

        val macResult = ByteArray(mac.macSize)
        mac.doFinal(macResult, 0)

        return Pair(key, macResult)
    }

    fun verifyChecksum(keyIn: ByteArray): ByteArray? {

        val mac = CBCBlockCipherMac(AESEngine())

        if (keyIn.size <= mac.macSize )
            return null

        val keyLen = keyIn.size - mac.macSize

        mac.init(KeyParameter(crcKey))
        mac.update(keyIn, 0, keyLen)

        val macResult = ByteArray(mac.macSize)
        mac.doFinal(macResult, 0)

        for (i in 0 until mac.macSize) {
            if (macResult[i] != keyIn[i + keyLen])
                return null
        }

        val key = ByteArray(keyLen)
        System.arraycopy(keyIn, 0, key, 0, keyLen)
        return key
    }

    companion object {
        const val SHARED_SECRET_LEN = 80
    }
}