package net.cxifri

import net.cxifri.encodings.Base61Encoder
import org.junit.Test

import org.junit.Assert.*
import java.security.SecureRandom

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class Base61UnitTests {

    private fun testEncodeDecode(b: ByteArray, silent: Boolean = false) {
        val encoded = Base61Encoder.encode(b).toString(charset = Charsets.UTF_8)
        assertNotNull(encoded)

        if (!silent)
            println(encoded)

        val decoded = Base61Encoder.decode(encoded.toByteArray())
        assertNotNull(decoded)

        assertEquals(b.size, decoded.size)
        for (i in 0 until b.size) {
            assertEquals(b[i], decoded[i])
        }
    }

    @Test
    fun testSizes() {
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

    private fun testRandomArrays(maxSize: Int, timeSeconds: Int) {
        val start = System.currentTimeMillis()

        val rnd = SecureRandom()

        var num = 0
        var totLen = 0

        while (System.currentTimeMillis() - start < 1000L * timeSeconds) {

            val bytes = ByteArray(rnd.nextInt(maxSize) + 1)
            rnd.nextBytes(bytes)

            testEncodeDecode(bytes, silent = true)
            num += 1
            totLen += bytes.size
        }

        println("$num iters total, avg len ${totLen / num}")
    }

    @Test
    fun testRandomArrays_max128() {
        testRandomArrays(128, 30)
    }

    @Test
    fun testRandomArrays_max12() {
        testRandomArrays(12, 30)
    }

    @Test
    fun testRandomArrays_max17() {
        testRandomArrays(17, 30)
    }

    @Test
    fun testRandomArrays_max32() {
        testRandomArrays(32, 30)
    }
}
