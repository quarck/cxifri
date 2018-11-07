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

        encrypting = forEncryption

        if (params is KeyParameter) {
            val key = params.key
            if (key.size == KEY_LENGTH) {

                val genA = PKCS5S2ParametersGenerator(SHA256Digest())
                val genT = PKCS5S2ParametersGenerator(SHA256Digest())
                val genS = PKCS5S2ParametersGenerator(SHA256Digest())

                genA.init(key, SALT_AES, NUM_PCKS_ITERS)
                genT.init(key, SALT_TWOFISH, NUM_PCKS_ITERS)
                genS.init(key, SALT_SERPENT, NUM_PCKS_ITERS)

                val keyParamA = genA.generateDerivedParameters(KEY_LENGTH_UND * 8) as KeyParameter
                val keyParamT = genT.generateDerivedParameters(KEY_LENGTH_UND * 8) as KeyParameter
                val keyParamS = genS.generateDerivedParameters(KEY_LENGTH_UND * 8) as KeyParameter

                aesEngine.init(forEncryption, keyParamA)
                twofishEngine.init(forEncryption, keyParamT)
                serpentEngine.init(forEncryption, keyParamS)

            } else {
                throw IllegalArgumentException("invalid parameter passed to AESTwofishSerpent init, key length is ${key.size}, supported key length: $KEY_LENGTH")
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

        val KEY_LENGTH_UND = 32 // key length of each underlying cipher
        val KEY_LENGTH = 48

        val SALT_AES = "cxifri-text-AES-ZzeqOthuLXNpK5BU1XT/c".toByteArray(charset = Charsets.UTF_8)
        val SALT_TWOFISH = "cxifri-text-Twofish-YKwa3IqWv/MTZ0qhaK5CA".toByteArray(charset = Charsets.UTF_8)
        val SALT_SERPENT = "cxifri-text-Serpent-PAQThlVjYhYqCcJFHz0BT".toByteArray(charset = Charsets.UTF_8)

        val NUM_PCKS_ITERS = 10
    }
}