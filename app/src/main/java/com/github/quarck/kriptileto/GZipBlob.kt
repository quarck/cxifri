package com.github.quarck.kriptileto

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class GZipBlob {
    fun deflate(inp: ByteArray): ByteArray {
        val byteStream = ByteArrayOutputStream()
        val gzipStream = GZIPOutputStream(byteStream)
        gzipStream.write(inp, 0, inp.size)
        gzipStream.finish()
        val ret = byteStream.toByteArray()
        gzipStream.close()
        byteStream.close()
        return ret
    }

    fun inflate(inp: ByteArray): ByteArray? {
        val byteStream = ByteArrayInputStream(inp)
        val gzipStream = GZIPInputStream(byteStream)
        val blob = gzipStream.readBytes()
        gzipStream.close()
        byteStream.close()
        return blob
    }


    fun inflate(inp: ByteArray, offset: Int, size: Int): ByteArray? {
        val byteStream = ByteArrayInputStream(inp, offset, size)
        val gzipStream = GZIPInputStream(byteStream)
        val blob = gzipStream.readBytes()
        gzipStream.close()
        byteStream.close()
        return blob
    }
}