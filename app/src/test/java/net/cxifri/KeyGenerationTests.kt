package net.cxifri

import net.cxifri.crypto.CryptoFactory
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

        runKeyInequalityAsserts(
                CryptoFactory.deriveKeyFromPassword("Hello", ""),
                CryptoFactory.deriveKeyFromPassword("hello", "")
        )

        runKeyInequalityAsserts(
                CryptoFactory.deriveKeyFromPassword("hello1", ""),
                CryptoFactory.deriveKeyFromPassword("hello2", "")
        )

        runKeyInequalityAsserts(
                CryptoFactory.deriveKeyFromPassword("", ""),
                CryptoFactory.deriveKeyFromPassword(" ", "")
        )

        runKeyInequalityAsserts(
                CryptoFactory.deriveKeyFromPassword(
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", ""),
                CryptoFactory.deriveKeyFromPassword(
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaab", "")
        )
    }

    @Test
    fun testAESTwfSerpentKeysAreIndependent() {


        val textKey = CryptoFactory.deriveKeyFromPassword("test", "").binaryKey ?: throw Exception("")

        assertEquals(textKey.size, 32*3)

        var ineq_12 = 0
        var ineq_13 = 0
        var ineq_23 = 0

        for (i in 0 until 32) {
            if (textKey[i] != textKey[i+32])
                ineq_12 += 1

            if (textKey[i] != textKey[i+64])
                ineq_13 += 1

            if (textKey[i+32] != textKey[i+64])
                ineq_23 += 1
        }

        assertTrue(ineq_12 > 28)
        assertTrue(ineq_13 > 28)
        assertTrue(ineq_23 > 28)
    }
}
