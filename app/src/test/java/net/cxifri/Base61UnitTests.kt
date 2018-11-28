package net.cxifri

import net.cxifri.encodings.Base61Encoder
import org.bouncycastle.util.encoders.EncoderException
import org.junit.Test

import org.junit.Assert.*
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class Base61UnitTests {

    @Test
    fun testBase61Encode() {

        val encoder = Base61Encoder()
        val dataIn = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)

        val dataOut = encoder.encode(dataIn, 0, dataIn.size)
        val strOut = dataOut.toString(charset = Charsets.UTF_8)
        assertEquals("Mx4CpSQdBAA", strOut)

        val decoded = encoder.decode(dataOut, 0, dataOut.size)

        for (i in 0 until 8) {
            assertEquals(
                    dataIn[i],
                    decoded[i]
            )
        }

    }
}
