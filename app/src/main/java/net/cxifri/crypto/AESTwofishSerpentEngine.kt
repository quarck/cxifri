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
import org.bouncycastle.crypto.BlockCipher
import org.bouncycastle.crypto.CipherParameters
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.engines.SerpentEngine
import org.bouncycastle.crypto.engines.TwofishEngine
import org.bouncycastle.crypto.params.KeyParameter

class AESTwofishSerpentEngine() : BlockCipher {


    val aesEngine = AESEngine()
    val twofishEngine = TwofishEngine()
    val serpentEngine = SerpentEngine()
    var encrypting: Boolean = false

    val aesBlock = ByteArray(aesEngine.blockSize)
    val twofishBlock = ByteArray(twofishEngine.blockSize)
    val serpentBlock = ByteArray(serpentEngine.blockSize)

    override fun init(forEncryption: Boolean, params: CipherParameters?) {

        encrypting = forEncryption

        if (params is KeyParameter) {
            val key = params.key
            if (key.size == KEY_LENGTH_BYTES) {

                val aesKey = ByteArray(KEY_LENGTH_AES)
                val twofishKey = ByteArray(KEY_LENGTH_TWOFISH)
                val serpentKey = ByteArray(KEY_LENGTH_SERPENT)

                var srcPos = 0
                System.arraycopy(key, srcPos, aesKey, 0, KEY_LENGTH_AES)
                srcPos += KEY_LENGTH_AES

                System.arraycopy(key, srcPos, twofishKey, 0, KEY_LENGTH_TWOFISH)
                srcPos += KEY_LENGTH_TWOFISH

                System.arraycopy(key, srcPos, serpentKey, 0, KEY_LENGTH_SERPENT)

                aesEngine.init(forEncryption, KeyParameter(aesKey))
                twofishEngine.init(forEncryption, KeyParameter(twofishKey))
                serpentEngine.init(forEncryption, KeyParameter(serpentKey))

            } else {
                throw IllegalArgumentException("invalid parameter passed to AESTwofishSerpent init, key length is ${key.size}, supported key length: $KEY_LENGTH_BYTES")
            }
            return
        }

        throw IllegalArgumentException("invalid parameter passed to AESTwofishSerpent init - " + params?.javaClass?.name)
    }

    override fun getAlgorithmName(): String {
        return "${aesEngine.algorithmName}-${twofishEngine.algorithmName}-${serpentEngine.algorithmName}"
    }

    override fun getBlockSize(): Int {
        val blockSize = BLOCK_SIZE
        if (aesEngine.blockSize != blockSize)
            throw Exception("oops")
        if (twofishEngine.blockSize != blockSize)
            throw Exception("oops")
        if (serpentEngine.blockSize != blockSize)
            throw Exception("oops")
        return blockSize
    }

    override fun processBlock(input: ByteArray, inOff: Int, output: ByteArray, outOff: Int): Int {

        if (encrypting) {
            aesEngine.processBlock(input, inOff, aesBlock, 0)
            twofishEngine.processBlock(aesBlock, 0, twofishBlock, 0)
            serpentEngine.processBlock(twofishBlock, 0, output, outOff)
        }
        else {
            serpentEngine.processBlock(input, inOff, serpentBlock, 0)
            twofishEngine.processBlock(serpentBlock, 0, twofishBlock, 0)
            aesEngine.processBlock(twofishBlock, 0, output, outOff)
        }

        return BLOCK_SIZE
    }

    override fun reset() {
        aesEngine.reset()
        twofishEngine.reset()
        serpentEngine.reset()

        aesBlock.wipe()
        twofishBlock.wipe()
        serpentBlock.wipe()
    }

    companion object {
        val BLOCK_SIZE = 16

        val KEY_LENGTH_AES = 32
        val KEY_LENGTH_TWOFISH = 32
        val KEY_LENGTH_SERPENT = 32
        val KEY_LENGTH_BYTES = KEY_LENGTH_AES + KEY_LENGTH_TWOFISH + KEY_LENGTH_SERPENT
    }
}