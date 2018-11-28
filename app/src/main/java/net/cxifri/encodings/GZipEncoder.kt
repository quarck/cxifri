/*
 * Copyright (C) 2018 Sergey Parshin (quarck@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package net.cxifri.encodings

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class GZipEncoder {
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