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

import org.bouncycastle.crypto.BlockCipher
import org.bouncycastle.crypto.CryptoException
import org.bouncycastle.crypto.InvalidCipherTextException
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.macs.CBCBlockCipherMac
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.modes.EAXBlockCipher
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import java.security.SecureRandom

class BinaryMessageHandler: BinaryMessageHandlerInterface {

    override fun encrypt(message: ByteArray, key: ByteArray): ByteArray {

        val random = SecureRandom()
        val cipher = EAXBlockCipher(AESTwofishSerpentEngine())

        val iv = ByteArray(cipher.blockSize)
        random.nextBytes(iv)

        // pad the message
        cipher.init(true, ParametersWithIV(KeyParameter(key), iv))

        val output = ByteArray(iv.size + cipher.getOutputSize(message.size))

        // Copy the IV into the output
        System.arraycopy(iv, 0, output, 0, iv.size)

        val outL = cipher.processBytes(message, 0, message.size, output, iv.size)
        val finalL = cipher.doFinal(output, iv.size+outL)

        val totalSize = iv.size + outL + finalL
        if (totalSize != output.size) {
            val finalOutput = ByteArray(totalSize)
            System.arraycopy(output, 0, finalOutput, 0, totalSize)
            return finalOutput
        }

        return output
    }

    override fun decrypt(message: ByteArray, key: ByteArray): ByteArray? {

        val cipher = EAXBlockCipher(AESTwofishSerpentEngine())

        try {
            val ivSize = cipher.blockSize
            if (ivSize >= message.size)
                throw CryptoException()

            val ivParam = ParametersWithIV(KeyParameter(key), message, 0, ivSize)
            cipher.init(false, ivParam)

            val decryptedRaw = ByteArray(cipher.getOutputSize(message.size - ivSize))

            val outL = cipher.processBytes(message, ivSize, message.size - ivSize, decryptedRaw, 0)
            val finalL = cipher.doFinal(decryptedRaw, outL)

            val decrypted = ByteArray(outL + finalL)
            System.arraycopy(decryptedRaw, 0, decrypted, 0, decrypted.size)
            return decrypted
        }
        catch (ex: InvalidCipherTextException) {
            return null
        }
    }
}
