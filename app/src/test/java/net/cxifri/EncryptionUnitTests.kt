package net.cxifri

import net.cxifri.crypto.AESTwofishSerpentEngine
import net.cxifri.crypto.BinaryMessageHandler
import net.cxifri.crypto.DerivedKeyGenerator
import net.cxifri.crypto.KeyEntry
import net.cxifri.keysdb.binaryKey
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.engines.SerpentEngine
import org.bouncycastle.crypto.engines.TwofishEngine
import org.bouncycastle.crypto.params.KeyParameter
import org.junit.Test

import org.junit.Assert.*

class EncryptionUnitTests {

    fun runAESTWSerpTestsForDataLen(len: Int) {

        val keyText = byteArrayOf(
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
        )

        val keyAuth = byteArrayOf(
                1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
        )

        val dataIn = ByteArray(len)
        for (i in 0 until len) {
            dataIn[i] = i.toByte()
        }

        val encrypted = BinaryMessageHandler { AESTwofishSerpentEngine() }.encrypt(dataIn, keyText, keyAuth)

        val decrypted = BinaryMessageHandler { AESTwofishSerpentEngine() }.decrypt(encrypted, keyText, keyAuth)

        assertNotNull(decrypted)

        for (i in 0 until len) {
            assertEquals(dataIn[i], decrypted!![i])
        }

        // deliberately destroy the key
        keyAuth[0] = 100

        val decrypted2 = BinaryMessageHandler { AESTwofishSerpentEngine() }.decrypt(encrypted, keyText, keyAuth)
        assertNull(decrypted2)
    }

//    @Test
//    fun testBinaryMessageAES() {
//        for (len in 1 until 2049) {
//            runAESTestsForDataLen(len)
//        }
//    }
//
    @Test
    fun TestBinaryMessageChained() {
        for (len in 1 until 2049) {
            runAESTWSerpTestsForDataLen(len)
        }
    }

    @Test
    fun chaininigTests() {

        val keyAes = byteArrayOf(
            0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1,
            0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1
        )

        val keyTwf = byteArrayOf(
                0, 0, 0, 0, 2, 2, 2, 2, 0, 0, 0, 0, 2, 2, 2, 2,
                0, 0, 0, 0, 2, 2, 2, 2, 0, 0, 0, 0, 2, 2, 2, 2
        )

        val keySerp = byteArrayOf(
                0, 0, 0, 0, 3, 3, 3, 3, 0, 0, 0, 0, 3, 3, 3, 3,
                0, 0, 0, 0, 3, 3, 3, 3, 0, 0, 0, 0, 3, 3, 3, 3
        )

        val keyChained = byteArrayOf(
                0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1,
                0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1,
                0, 0, 0, 0, 2, 2, 2, 2, 0, 0, 0, 0, 2, 2, 2, 2,
                0, 0, 0, 0, 2, 2, 2, 2, 0, 0, 0, 0, 2, 2, 2, 2,
                0, 0, 0, 0, 3, 3, 3, 3, 0, 0, 0, 0, 3, 3, 3, 3,
                0, 0, 0, 0, 3, 3, 3, 3, 0, 0, 0, 0, 3, 3, 3, 3
        )

        val aesEngine = AESEngine()
        val twofishEngine = TwofishEngine()
        val serpentEngine = SerpentEngine()
        val chainEngine = AESTwofishSerpentEngine()

        aesEngine.init(true, KeyParameter(keyAes))
        twofishEngine.init(true, KeyParameter(keyTwf))
        serpentEngine.init(true, KeyParameter(keySerp))
        chainEngine.init(true, KeyParameter(keyChained))

        val testBlock = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)

        val tmp1 = ByteArray(16)
        val tmp2 = ByteArray(16)

        val encryptedIndividual = ByteArray(16)
        val encryptedChained = ByteArray(16)

        aesEngine.processBlock(testBlock, 0, tmp1, 0)
        twofishEngine.processBlock(tmp1, 0, tmp2, 0)
        serpentEngine.processBlock(tmp2, 0, encryptedIndividual, 0)

        chainEngine.processBlock(testBlock, 0, encryptedChained, 0)

        for (i in 0 until 16) {
            assertEquals(encryptedIndividual[i], encryptedChained[i])
        }
    }

    fun ensureMacFail(k1: KeyEntry, k2: KeyEntry) {

        val dataIn = ByteArray(1010)
        for (i in 0 until dataIn.size) {
            dataIn[i] = i.toByte()
        }

        val (bk1t, bk1a) = k1.binaryKey ?: throw Exception("")
        val (bk2t, bk2a) = k2.binaryKey ?: throw Exception("")

        val encrypted = BinaryMessageHandler { AESTwofishSerpentEngine() }.encrypt(dataIn, bk1t, bk1a)

        val decrypted = BinaryMessageHandler { AESTwofishSerpentEngine() }.decrypt(encrypted, bk2t, bk2a)

        assertNull(decrypted)
    }

    fun ensureMacWorks(k1: KeyEntry) {

        val dataIn = ByteArray(1010)
        for (i in 0 until dataIn.size) {
            dataIn[i] = i.toByte()
        }

        val (bk1t, bk1a) = k1.binaryKey ?: throw Exception("")

        val encrypted = BinaryMessageHandler { AESTwofishSerpentEngine() }.encrypt(dataIn, bk1t, bk1a)

        val decrypted = BinaryMessageHandler { AESTwofishSerpentEngine() }.decrypt(encrypted, bk1t, bk1a)

        assertNotNull(decrypted)

        for (i in 0 until decrypted!!.size) {
            assertEquals(dataIn[i], decrypted!![i])
        }
    }

    @Test
    fun macValidityTest() {
        val generator = DerivedKeyGenerator()


        ensureMacFail(
            generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4), byteArrayOf(0, 1, 2, 3, 4)),
            generator.generateFromSharedSecret(byteArrayOf(1, 1, 2, 3, 4), byteArrayOf(0, 1, 2, 3, 4))
        )

        ensureMacFail(
                generator.generateFromSharedSecret(byteArrayOf(0, 0, 0, 0, 4), byteArrayOf(0, 1, 2, 3, 4)),
                generator.generateFromSharedSecret(byteArrayOf(0, 0, 0, 0, 5), byteArrayOf(0, 1, 2, 3, 4))
        )

        ensureMacFail(
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4), byteArrayOf(0, 1, 2, 3, 4)),
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4), byteArrayOf(1, 1, 2, 3, 4))
        )

        ensureMacFail(
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 4, 3, 4), byteArrayOf(0, 1, 2, 3, 4)),
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4), byteArrayOf(1, 1, 2, 3, 4))
        )

        ensureMacFail(
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4, 0, 1, 2, 3, 4)),
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4, 1, 1, 2, 3, 4))
        )

        ensureMacFail(
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4, 0, 1, 2, 3, 4)),
                generator.generateFromSharedSecret(byteArrayOf(0, 2, 2, 3, 4, 0, 1, 2, 3, 4))
        )

        ensureMacFail(
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4, 0, 1, 2, 6, 4)),
                generator.generateFromSharedSecret(byteArrayOf(0, 2, 2, 3, 4, 0, 1, 2, 3, 4))
        )

        ensureMacWorks(generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4, 0, 1, 2, 6, 4)))
    }
}
