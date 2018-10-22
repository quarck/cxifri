package net.cxifri

import net.cxifri.crypto.DerivedKeyGenerator
import net.cxifri.dataprocessing.GZipBlob
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class KeyGenerationTests {
    @Test
    fun testKeyDependsOnInput() {
        val pass1 = "Hello"
        val pass2 = "hello"
        val generator = DerivedKeyGenerator()

        val key1aes = generator.generateForAES(pass1)
        val key2aes = generator.generateForAES(pass2)

        val key1ch = generator.generateForAESTwofishSerpent(pass1)
        val key2ch = generator.generateForAESTwofishSerpent(pass2)

        var aesKeysEqual = true
        for (i in 0 until key1aes.size) {
            if (key1aes[i] != key2aes[i])
                aesKeysEqual = false
        }

        assertFalse(aesKeysEqual)

        var chKeysEqual = true
        for (i in 0 until key1ch.size) {
            if (key1ch[i] != key2ch[i])
                chKeysEqual = false
        }

        assertFalse(chKeysEqual)
    }

    @Test
    fun testAESTwfSerpentKeysAreIndependent() {
        val generator = DerivedKeyGenerator()

        val key = generator.generateForAESTwofishSerpent("test")

        assertEquals(key.size, 32*3)

        for (i in 0 until 32) {
            assertNotEquals(key[i], key[i+32])
            assertNotEquals(key[i], key[i+64])
            assertNotEquals(key[i+32], key[i+64])
        }
    }
}
