package net.cxifri

import net.cxifri.crypto.DerivedKeyGenerator
import net.cxifri.crypto.KeyEntry
import net.cxifri.dataprocessing.GZipBlob
import net.cxifri.keysdb.binaryKey
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class KeyGenerationTests {

    fun runKeyInequalityAsserts(k1: KeyEntry, k2: KeyEntry) {
        val k1t = k1.binaryKey ?: throw Exception("")
        val k2t = k2.binaryKey ?: throw Exception("")

        var tKeysEqualNum = 0
        for (i in 0 until k1t.size) {
            if (k1t[i] != k2t[i])
                tKeysEqualNum += 1
        }

        assertTrue(tKeysEqualNum > k1t.size / 4)
    }

    @Test
    fun testKeyDependsOnInput() {
        val generator = DerivedKeyGenerator()

        runKeyInequalityAsserts(
                generator.generateFromTextPassword("Hello"),
                generator.generateFromTextPassword("hello")
        )

        runKeyInequalityAsserts(
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4, 0, 1, 2, 3, 4), ""),
                generator.generateFromSharedSecret(byteArrayOf(1, 1, 2, 3, 4, 0, 1, 2, 3, 4), "")
        )

        runKeyInequalityAsserts(
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4, 0, 1, 2, 3, 4), ""),
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4, 1, 1, 2, 3, 4), "")
        )

        runKeyInequalityAsserts(
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 4, 3, 4, 0, 1, 2, 3, 4), ""),
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4, 1, 1, 2, 3, 4), "")
        )

        runKeyInequalityAsserts(
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4, 0, 1, 2, 3, 4), ""),
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4, 1, 1, 2, 3, 4), "")
        )

        runKeyInequalityAsserts(
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4, 0, 1, 2, 3, 4), ""),
                generator.generateFromSharedSecret(byteArrayOf(0, 2, 2, 3, 4, 0, 1, 2, 3, 4), "")
        )

        runKeyInequalityAsserts(
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4, 0, 1, 2, 6, 4), ""),
                generator.generateFromSharedSecret(byteArrayOf(0, 2, 2, 3, 4, 0, 1, 2, 3, 4), "")
        )
    }

    @Test
    fun testAESTwfSerpentKeysAreIndependent() {
        val generator = DerivedKeyGenerator()

        val textKey = generator.generateFromTextPassword("test").binaryKey ?: throw Exception("")

        assertEquals(textKey.size, 32*3)

        for (i in 0 until 32) {
            assertNotEquals(textKey[i], textKey[i+32])
            assertNotEquals(textKey[i], textKey[i+64])
            assertNotEquals(textKey[i+32], textKey[i+64])
        }
    }

    @Test
    fun testChainKeyGen() {
        // TBD
    }
}
