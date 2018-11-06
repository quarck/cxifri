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
import org.bouncycastle.crypto.macs.CBCBlockCipherMac
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import java.security.SecureRandom

class BinaryMessageHandler(val createEngine: ()->BlockCipher): BinaryMessageHandlerInterface {

    override fun encrypt(message: ByteArray, textKey: ByteArray, authKey: ByteArray): ByteArray {

        val cipher = PaddedBufferedBlockCipher(CBCBlockCipher(createEngine()))
        val mac = CBCBlockCipherMac(createEngine())

        val iv = ByteArray(cipher.blockSize)
        val random = SecureRandom()
        random.nextBytes(iv)

        cipher.init(true, ParametersWithIV(KeyParameter(textKey), iv))

        val output = ByteArray(iv.size + mac.macSize + cipher.getOutputSize(message.size))

        // Copy the IV into the output
        System.arraycopy(iv, 0, output, 0, iv.size)

        var wPos = iv.size
        var outL = cipher.processBytes(message, 0, message.size, output, wPos)
        wPos += outL

        outL = cipher.doFinal(output, wPos)
        wPos += outL

        if (output.size  < mac.macSize + wPos)
            throw Exception("Internal error")

        // Mac of the encrypted output
        mac.init(KeyParameter(authKey))
        mac.update(/*in*/output, 0, wPos)
        mac.doFinal(/*out*/output, wPos)
        wPos += mac.macSize

        if (wPos != output.size) {
            val finalOutput = ByteArray(wPos)
            System.arraycopy(output, 0, finalOutput, 0, wPos)
            return finalOutput
        }

        return output
    }

    override fun decrypt(message: ByteArray, textKey: ByteArray, authKey: ByteArray): ByteArray? {

        val mac = CBCBlockCipherMac(createEngine())

        if (message.size <= mac.macSize)
            return null

        mac.init(KeyParameter(authKey))
        mac.update(message, 0, message.size - mac.macSize)

        val macResult = ByteArray(mac.macSize)
        mac.doFinal(macResult, 0)

        var matchedBytes = 0
        val macOffset = message.size - mac.macSize
        for (i in 0 until macResult.size) {
            matchedBytes += if (macResult[i] == message[i + macOffset]) 1 else 0
        }
        if (matchedBytes != macResult.size)
            return null

        val cipher = PaddedBufferedBlockCipher(CBCBlockCipher(createEngine()))

        val remainingSize = message.size - mac.macSize

        try {
            val iv = ByteArray(cipher.blockSize)

            if (iv.size >= remainingSize)
                throw CryptoException()

            System.arraycopy(message, 0, iv, 0, iv.size)

            cipher.init(false, ParametersWithIV(KeyParameter(textKey), iv))

            val outputSize = cipher.getOutputSize(remainingSize - iv.size)
            val decryptedRaw = ByteArray(outputSize)

            val outL = cipher.processBytes(message, iv.size, remainingSize - iv.size,
                    decryptedRaw, 0)

            val finalL = cipher.doFinal(decryptedRaw, outL)

            return if (outL + finalL != decryptedRaw.size) {
                val decrypted = ByteArray(outL + finalL)
                System.arraycopy(decryptedRaw, 0, decrypted, 0, decrypted.size)
                decrypted
            }
            else
                decryptedRaw
        }
        catch (ex: InvalidCipherTextException) {
            return null
        }
    }
}
