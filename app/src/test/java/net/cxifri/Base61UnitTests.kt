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

        val bStr = b.map { it.toString() }.reduce{ x, y -> "$x,$y" }
        println("Inp: {$bStr}, encoded: $encoded")

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
        testEncodeDecode(encoder, byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))

        testEncodeDecode(encoder, byteArrayOf(1))
        testEncodeDecode(encoder, byteArrayOf(255.toByte()))
        testEncodeDecode(encoder, byteArrayOf(1, 2, 3))
        testEncodeDecode(encoder, byteArrayOf(255.toByte(), 254.toByte(), 253.toByte()))

        testEncodeDecode(encoder, byteArrayOf(
                255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(),
                255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(),
                255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(),
                255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(),
                255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(),
                255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(),
                255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(), 255.toByte()
        ))

        testEncodeDecode(encoder, byteArrayOf(
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte()
                ))

        testEncodeDecode(encoder, byteArrayOf(
                1, 1, 1, 1, 1, 1,
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                1, 1, 1, 1, 1, 1,
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                1, 1, 1, 1, 1, 1,
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte()
        ))


        testEncodeDecode(encoder, byteArrayOf(
                1, 1, 1, 1, 1, 1,
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                1, 1, 1, 1, 1, 1,
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                1, 1, 1, 1, 1, 1,
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                1, 1, 1, 1, 1, 1,
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                1, 1, 1, 1, 1, 1,
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                1, 1, 1, 1, 1, 1,
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                1, 1, 1, 1, 1, 1,
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                1, 1, 1, 1, 1, 1,
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                1, 1, 1, 1, 1, 1,
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte()
        ))

        testEncodeDecode(encoder, byteArrayOf(
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte(),
                255.toByte(), 254.toByte(), 253.toByte(), 255.toByte(), 254.toByte(), 253.toByte()
        ))

    }
}
