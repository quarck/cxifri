package com.github.quarck.kriptileto

import org.bouncycastle.crypto.CryptoException
import org.bouncycastle.crypto.InvalidCipherTextException
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.macs.CBCBlockCipherMac
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import java.security.SecureRandom


object AESBinaryMessage{
    // Message layout:
    // [IV PLAIN TEXT] ENCRYPTED[ MAC, SALT, MESSAGE]
    // MAC = MAC of SALT + MESSAGE
    fun encrypt(message: ByteArray, key: ByteArray): ByteArray {

        val cipher = PaddedBufferedBlockCipher(CBCBlockCipher(AESEngine()))
        val mac = CBCBlockCipherMac(AESEngine())
        val cipherBlockSize = cipher.blockSize

        val iv = ByteArray(cipherBlockSize)
        val random = SecureRandom()
        random.nextBytes(iv)

        val salt = ByteArray(cipherBlockSize)
        random.nextBytes(salt)

        val params = ParametersWithIV(KeyParameter(key), iv)
        cipher.init(true, params)

        val outputSize = iv.size + cipher.getOutputSize(mac.macSize + salt.size +  message.size) // IV goes un-encrypted

        val output = ByteArray(outputSize)

        // Copy the IV into the output
        System.arraycopy(iv, 0, output, 0, iv.size)

        var wPos = iv.size
        var remSize = outputSize - iv.size

        mac.init(KeyParameter(key))
        mac.update(salt, 0, salt.size)
        mac.update(message, 0, message.size)

        val macResult = ByteArray(mac.macSize)
        mac.doFinal(macResult, 0)

        var outL = cipher.processBytes(macResult, 0, macResult.size, output, wPos)
        wPos += outL
        remSize -= outL

        outL = cipher.processBytes(salt, 0, salt.size, output, wPos)
        wPos += outL
        remSize -= outL

        outL = cipher.processBytes(message, 0, message.size, output, wPos)
        wPos += outL
        remSize -= outL

        outL = cipher.doFinal(output, wPos)
        wPos += outL
        remSize -= outL

        if (remSize < 0)
            throw Exception("Internal error")

        if (wPos != output.size) {
            val finalOutput = ByteArray(wPos)
            System.arraycopy(output, 0, finalOutput, 0, wPos)
            return finalOutput
        }

        return output
    }

    // Returns null if decryption fails or MAC check fails
    fun decrypt(message: ByteArray, key: ByteArray): ByteArray? {

        val cipher = PaddedBufferedBlockCipher(CBCBlockCipher(AESEngine()))
        val mac = CBCBlockCipherMac(AESEngine())
        val cipherBlockSize = cipher.blockSize

        try {
            val iv = ByteArray(cipherBlockSize)
            if (iv.size >= message.size)
                throw CryptoException()

            System.arraycopy(message, 0, iv, 0, iv.size)

            val params = ParametersWithIV(KeyParameter(key), iv)
            cipher.init(false, params)

            val outputSize = cipher.getOutputSize(message.size - iv.size)
            val decryptedRaw = ByteArray(outputSize)

            val outL = cipher.processBytes(message, iv.size, message.size - iv.size,
                    decryptedRaw, 0)

            val finalL = cipher.doFinal(decryptedRaw, outL)

            val decryptedRawL = outL + finalL

            mac.init(KeyParameter(key))

            val macCalculated = ByteArray(mac.macSize)
            val macMessage = ByteArray(mac.macSize)

            val salt = ByteArray(cipher.blockSize)

            if (salt.size + macMessage.size > decryptedRawL)
                return null

            System.arraycopy(decryptedRaw, 0, macMessage, 0, macMessage.size)
            mac.update(decryptedRaw, macMessage.size, decryptedRawL - macMessage.size)
            mac.doFinal(macCalculated, 0)

            var matchedBytes = 0
            for (i in 0 until macCalculated.size) {
                matchedBytes += if (macCalculated[i] == macMessage[i]) 1 else 0
            }

            if (matchedBytes != macCalculated.size)
                return null

            val decrypted = ByteArray(decryptedRawL - salt.size - macMessage.size)
            System.arraycopy(decryptedRaw, salt.size + macMessage.size, decrypted, 0, decrypted.size)

            return decrypted
        }
        catch (ex: InvalidCipherTextException) {
            return null
        }
    }

    val KEY_LEN_MIN = 16 // AES-128
    val KEY_LEN_MID = 24 // AES-192
    val KEY_LEN_MAX = 32 // AES-256
}
