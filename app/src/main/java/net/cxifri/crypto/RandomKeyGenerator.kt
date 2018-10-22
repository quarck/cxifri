package com.github.quarck.kriptileto.crypto

import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.macs.CBCBlockCipherMac
import org.bouncycastle.crypto.params.KeyParameter
import java.security.SecureRandom

class RandomKeyGenerator {
    val random: SecureRandom by lazy { SecureRandom() }

    // Mac is used as CRC for code transmission, thus no need for any secure key
    val crcKey = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    fun generate(lenBytes: Int, withChecksum: Boolean): ByteArray {
        if (!withChecksum) {
            val key = ByteArray(lenBytes)
            random.nextBytes(key)
            return key
        }
        else {
            val key = ByteArray(lenBytes)
            random.nextBytes(key)
            val mac = CBCBlockCipherMac(AESEngine())

            mac.init(KeyParameter(crcKey))
            mac.update(key, 0, key.size)

            val macResult = ByteArray(mac.macSize)
            mac.doFinal(macResult, 0)

            return key + macResult
        }
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
}