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

    fun runKeyInequalityAsserts(k1: KeyEntry, k2: KeyEntry, skipTEQ: Boolean, skipAEQ: Boolean) {
        val (k1t, k1a) = k1.binaryKey ?: throw Exception("")
        val (k2t, k2a) = k2.binaryKey ?: throw Exception("")

        var tKeysEqual = true
        var aKeysEqual = true

        var ta1KeysEqual = true
        var ta2KeysEqual = true

        for (i in 0 until k1t.size) {
            if (k1t[i] != k2t[i])
                tKeysEqual = false

            if (k1a[i] != k2a[i])
                aKeysEqual = false

            if (k1a[i] != k1t[i])
                ta1KeysEqual = false

            if (k2a[i] != k2t[i])
                ta2KeysEqual = false
        }

        if (!skipTEQ)
            assertFalse(tKeysEqual)
        if (!skipAEQ)
            assertFalse(aKeysEqual)
        assertFalse(ta1KeysEqual)
        assertFalse(ta2KeysEqual)
    }

    @Test
    fun testKeyDependsOnInput() {
        val generator = DerivedKeyGenerator()

        runKeyInequalityAsserts(
                generator.generateFromTextPassword("Hello"),
                generator.generateFromTextPassword("hello"),
                false, false
        )

        runKeyInequalityAsserts(
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4), byteArrayOf(0, 1, 2, 3, 4)),
                generator.generateFromSharedSecret(byteArrayOf(1, 1, 2, 3, 4), byteArrayOf(0, 1, 2, 3, 4)),
                false, true
        )

        runKeyInequalityAsserts(
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4), byteArrayOf(0, 1, 2, 3, 4)),
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4), byteArrayOf(1, 1, 2, 3, 4)),
                true, false
        )

        runKeyInequalityAsserts(
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 4, 3, 4), byteArrayOf(0, 1, 2, 3, 4)),
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4), byteArrayOf(1, 1, 2, 3, 4)),
                false, false
        )

        runKeyInequalityAsserts(
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4, 0, 1, 2, 3, 4)),
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4, 1, 1, 2, 3, 4)),
                true, false
        )

        runKeyInequalityAsserts(
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4, 0, 1, 2, 3, 4)),
                generator.generateFromSharedSecret(byteArrayOf(0, 2, 2, 3, 4, 0, 1, 2, 3, 4)),
                false, true
        )

        runKeyInequalityAsserts(
                generator.generateFromSharedSecret(byteArrayOf(0, 1, 2, 3, 4, 0, 1, 2, 6, 4)),
                generator.generateFromSharedSecret(byteArrayOf(0, 2, 2, 3, 4, 0, 1, 2, 3, 4)),
                false, false
        )
    }

    @Test
    fun testAESTwfSerpentKeysAreIndependent() {
        val generator = DerivedKeyGenerator()

        val (textKey, authKey) = generator.generateFromTextPassword("test").binaryKey ?: throw Exception("")

        assertEquals(textKey.size, 32*3)
        assertEquals(authKey.size, 32*3)

        for (i in 0 until 32) {
            assertNotEquals(textKey[i], textKey[i+32])
            assertNotEquals(textKey[i], textKey[i+64])
            assertNotEquals(textKey[i+32], textKey[i+64])

            assertNotEquals(authKey[i], authKey[i+32])
            assertNotEquals(authKey[i], authKey[i+64])
            assertNotEquals(authKey[i+32], authKey[i+64])
        }
    }
}
