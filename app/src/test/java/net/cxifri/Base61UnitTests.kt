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

    fun testEncodeDecode(e: Base61Encoder, b: ByteArray) {
        val encoded = e.encode(b).toString(charset = Charsets.UTF_8)
        assertNotNull(encoded)

        println(encoded)

        val decoded = e.decode(encoded.toByteArray())
        assertNotNull(decoded)

        assertEquals(b.size, decoded.size)
        for (i in 0 until b.size) {
            assertEquals(b[i], decoded[i])
        }
    }

    @Test
    fun testBase61Encode() {
        val encoder = Base61Encoder()

        testEncodeDecode(encoder, byteArrayOf())
        testEncodeDecode(encoder, byteArrayOf(1))
        testEncodeDecode(encoder, byteArrayOf(-1))
        testEncodeDecode(encoder, byteArrayOf(1, 2))
        testEncodeDecode(encoder, byteArrayOf(-1, -2))
        testEncodeDecode(encoder, byteArrayOf(1, 2, 3))
        testEncodeDecode(encoder, byteArrayOf(-1, -2, -3))
        testEncodeDecode(encoder, byteArrayOf(1, 2, 3, 4))
        testEncodeDecode(encoder, byteArrayOf(-1, -2, -3, -4))
        testEncodeDecode(encoder, byteArrayOf(1, 2, 3, 4, 5))
        testEncodeDecode(encoder, byteArrayOf(-1, -2, -3, -4, -5))
        testEncodeDecode(encoder, byteArrayOf(1, 2, 3, 4, 5, 6))
        testEncodeDecode(encoder, byteArrayOf(-1, -2, -3, -4, -5, -6))
        testEncodeDecode(encoder, byteArrayOf(1, 2, 3, 4, 5, 6, 7))
        testEncodeDecode(encoder, byteArrayOf(-1, -2, -3, -4, -5, -6, -7))
        testEncodeDecode(encoder, byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
        testEncodeDecode(encoder, byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8))
        testEncodeDecode(encoder, byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9))
        testEncodeDecode(encoder, byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8, -9))
        testEncodeDecode(encoder, byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        testEncodeDecode(encoder, byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10))
        testEncodeDecode(encoder, byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11))
        testEncodeDecode(encoder, byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11))
        testEncodeDecode(encoder, byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12))
        testEncodeDecode(encoder, byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12))
        testEncodeDecode(encoder, byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13))
        testEncodeDecode(encoder, byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -13))
        testEncodeDecode(encoder, byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14))
        testEncodeDecode(encoder, byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -13, -14))
        testEncodeDecode(encoder, byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15))
        testEncodeDecode(encoder, byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -13, -14, -15))
        testEncodeDecode(encoder, byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16))
        testEncodeDecode(encoder, byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -13, -14, -15, -16))
        testEncodeDecode(encoder, byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
        testEncodeDecode(encoder, byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -13, -14, -15, -16, -17))
        testEncodeDecode(encoder, byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18))
        testEncodeDecode(encoder, byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -13, -14, -15, -16, -17, -18))


        testEncodeDecode(encoder, byteArrayOf(
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        ))

        testEncodeDecode(encoder, byteArrayOf(
                -1, -2, -3, -4, -5, -1, -2, -3, -4, -5, -1, -2, -3, -4, -5, -1, -2, -3, -4, -5, -1, -2, -3, -4, -5,
                -1, -2, -3, -4, -5, -1, -2, -3, -4, -5, -1, -2, -3, -4, -5, -1, -2, -3, -4, -5, -1, -2, -3, -4, -5
        ))

        testEncodeDecode(encoder, byteArrayOf(
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                -1, -2, -3, -4, -5, -6, -1, -2, -3, -4, -5, -6,
                1, 1, 1, 1, 1, 1, -1, -2, -3, -4, -5, -6,
                1, 1, 1, 1, 1, 1, -1, -2, -3, -4, -5, -6
        ))


        testEncodeDecode(encoder, byteArrayOf(
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

        testEncodeDecode(encoder, byteArrayOf(
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
