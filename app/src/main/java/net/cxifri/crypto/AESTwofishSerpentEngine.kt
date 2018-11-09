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
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.engines.SerpentEngine
import org.bouncycastle.crypto.engines.TwofishEngine
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.KeyParameter
import java.nio.charset.Charset

class AESTwofishSerpentEngine() : BlockCipher {

    val aesEngine = AESEngine()
    val twofishEngine = TwofishEngine()
    val serpentEngine = SerpentEngine()

    var encrypting: Boolean = false

    val aesBlock = ByteArray(aesEngine.blockSize)
    val twofishBlock = ByteArray(twofishEngine.blockSize)
    val serpentBlock = ByteArray(serpentEngine.blockSize)

    override fun init(forEncryption: Boolean, params: CipherParameters?) {

        if (params !is KeyParameter) {
            throw IllegalArgumentException("invalid parameter passed to AESTwofishSerpent init - " + params?.javaClass?.name)
        }

        val key: ByteArray = params.key
        if (key.size != KEY_LENGTH) {
            throw IllegalArgumentException("invalid parameter passed to AESTwofishSerpent init, key length is ${key.size}, supported key length: $KEY_LENGTH")
        }

        encrypting = forEncryption

        aesEngine.init(
                forEncryption,
                KeyParameter(key, 0 * KEY_LENGTH_UNDERLYING, KEY_LENGTH_UNDERLYING)
        )
        twofishEngine.init(
                forEncryption,
                KeyParameter(key, 1 * KEY_LENGTH_UNDERLYING, KEY_LENGTH_UNDERLYING)
        )
        serpentEngine.init(
                forEncryption,
                KeyParameter(key, 2 * KEY_LENGTH_UNDERLYING, KEY_LENGTH_UNDERLYING)
        )
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
            // aesBlock = E_aes(input)
            aesEngine.processBlock(input, inOff, aesBlock, 0)
            // twofishBlock = E_twofish(aesBlock) == E_twofish(E_aes(input))
            twofishEngine.processBlock(aesBlock, 0, twofishBlock, 0)
            // output = E_serpent(twofishBlock) == E_serpent(E_twofish(E_aes(input)))
            serpentEngine.processBlock(twofishBlock, 0, output, outOff)
        }
        else {
            // serpentBlock = D_serpent(input)
            serpentEngine.processBlock(input, inOff, serpentBlock, 0)
            // twofishBlock = D_twofish(serpentBlock) == D_twofish(D_serpent(input))
            twofishEngine.processBlock(serpentBlock, 0, twofishBlock, 0)
            // output = D_aes(twofishBlock) == D_aes(D_twofish(D_serpent(input)))
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

        val KEY_LENGTH_UNDERLYING = 32 // key length of each underlying cipher
        val KEY_LENGTH = KEY_LENGTH_UNDERLYING * 3 // each underlying cipher is using individual key
    }
}