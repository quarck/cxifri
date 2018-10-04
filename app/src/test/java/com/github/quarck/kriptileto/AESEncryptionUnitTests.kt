package com.github.quarck.kriptileto

import org.junit.Test

import org.junit.Assert.*

class AESEncryptionUnitTests {

    fun runTestsForDataLen(len: Int) {

        val key = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)

        val dataIn = ByteArray(len)
        for (i in 0 until len) {
            dataIn[i] = i.toByte()
        }

        val encrypted = AESBinaryMessage.encrypt(dataIn, key)

        val decrypted = AESBinaryMessage.decrypt(encrypted, key)

        assertNotNull(decrypted)

        for (i in 0 until len) {
            assertEquals(dataIn[i], decrypted!![i])
        }

        // deliberately destroy the key
        key[0] = 100

        val decrypted2 = AESBinaryMessage.decrypt(encrypted, key)
        assertNull(decrypted2)
    }

    @Test
    fun runTests() {
        for (len in 1 until 2049) {
            runTestsForDataLen(len)
        }
    }
}
