package net.cxifri

import net.cxifri.encodings.Base61Encoder
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class Base61UnitTests {

    fun testEncodeDecode(b: ByteArray) {
        val encoded = Base61Encoder.encode(b).toString(charset = Charsets.UTF_8)
        assertNotNull(encoded)

        println(encoded)

        val decoded = Base61Encoder.decode(encoded.toByteArray())
        assertNotNull(decoded)

        assertEquals(b.size, decoded.size)
        for (i in 0 until b.size) {
            assertEquals(b[i], decoded[i])
        }
    }

    @Test
    fun testBase61Encode() {
        testEncodeDecode(byteArrayOf())
        testEncodeDecode(byteArrayOf(1))
        testEncodeDecode(byteArrayOf(-1))
        testEncodeDecode(byteArrayOf(1, 2))
        testEncodeDecode(byteArrayOf(-1, -2))
        testEncodeDecode(byteArrayOf(1, 2, 3))
        testEncodeDecode(byteArrayOf(-1, -2, -3))
        testEncodeDecode(byteArrayOf(1, 2, 3, 4))
        testEncodeDecode(byteArrayOf(-1, -2, -3, -4))
        testEncodeDecode(byteArrayOf(1, 2, 3, 4, 5))
        testEncodeDecode(byteArrayOf(-1, -2, -3, -4, -5))
        testEncodeDecode(byteArrayOf(1, 2, 3, 4, 5, 6))
        testEncodeDecode(byteArrayOf(-1, -2, -3, -4, -5, -6))
        testEncodeDecode(byteArrayOf(1, 2, 3, 4, 5, 6, 7))
        testEncodeDecode(byteArrayOf(-1, -2, -3, -4, -5, -6, -7))
        testEncodeDecode(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
        testEncodeDecode(byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8))
        testEncodeDecode(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9))
        testEncodeDecode(byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8, -9))
        testEncodeDecode(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        testEncodeDecode(byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10))
        testEncodeDecode(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11))
        testEncodeDecode(byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11))
        testEncodeDecode(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12))
        testEncodeDecode(byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12))
        testEncodeDecode(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13))
        testEncodeDecode(byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -13))
        testEncodeDecode(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14))
        testEncodeDecode(byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -13, -14))
        testEncodeDecode(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15))
        testEncodeDecode(byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -13, -14, -15))
        testEncodeDecode(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16))
        testEncodeDecode(byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -13, -14, -15, -16))
        testEncodeDecode(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
        testEncodeDecode(byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -13, -14, -15, -16, -17))
        testEncodeDecode(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18))
        testEncodeDecode(byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -13, -14, -15, -16, -17, -18))


        testEncodeDecode(byteArrayOf(
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        ))

        testEncodeDecode(byteArrayOf(
                -1, -2, -3, -4, -5, -1, -2, -3, -4, -5, -1, -2, -3, -4, -5, -1, -2, -3, -4, -5, -1, -2, -3, -4, -5,
                -1, -2, -3, -4, -5, -1, -2, -3, -4, -5, -1, -2, -3, -4, -5, -1, -2, -3, -4, -5, -1, -2, -3, -4, -5
        ))

        testEncodeDecode(byteArrayOf(
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                -1, -2, -3, -4, -5, -6, -1, -2, -3, -4, -5, -6,
                1, 1, 1, 1, 1, 1, -1, -2, -3, -4, -5, -6,
                1, 1, 1, 1, 1, 1, -1, -2, -3, -4, -5, -6
        ))


        testEncodeDecode(byteArrayOf(
                1, 1, 1, 1, 1, 1,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                1, 1, 1, 1, 1, 1,
                -1, -2, -3, -4, -5, -6,
                1, 1, 1, 1, 1, 1,
                -1, -2, -3, -4, -5, -6,
                1, 1, 1, 1, 1, 1,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                1, 1, 1, 1, 1, 1,
                -1, -2, -3, -4, -5, -6,
                1, 1, 1, 1, 1, 1,
                -1, -2, -3, -4, -5, -6,
                1, 1, 1, 1, 1, 1,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                1, 1, 1, 1, 1, 1,
                -1, -2, -3, -4, -5, -6,
                1, 1, 1, 1, 1, 1,
                -1, -2, -3, -4, -5, -6
        ))

        testEncodeDecode(byteArrayOf(
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6,
                -1, -2, -3, -4, -5, -6
        ))

    }
}
