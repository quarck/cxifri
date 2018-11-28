package net.cxifri

import net.cxifri.encodings.GZipEncoder
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class GZipUnitTests {
    @Test
    fun gzipIsCorrect() {
        val input = byteArrayOf(1, 2, 3, 4, 5)

        val gz = GZipEncoder()

        val output = gz.deflate(input)

        val inputBack = gz.inflate(output)
        assertNotNull(inputBack)

        if (inputBack != null) {
            assertEquals(input.size, inputBack.size)
            for (i in 0 until input.size) {
                assertEquals(input[i], inputBack[i])
            }
        }
    }
}
