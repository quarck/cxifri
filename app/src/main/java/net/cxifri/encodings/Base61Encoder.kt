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

/*
 * Base61? What?
 * Well, idea is similar to base64, but this encoding avoids using any symbols outsize of
 * A-Z, a-z, 1-9 set. Thus, such encoding is useful in applications where any other symbols are better
 * to be avoided.
 *
 */

package net.cxifri.encodings

import java.io.ByteArrayOutputStream
import java.io.OutputStream

object Base61Encoder {

    private val BLOCK_SIZE = 8
    private val ENCODED_BLOCK_SIZE = 11

    private val ENCODING_TABLE = byteArrayOf(
            'A'.toByte(), 'B'.toByte(), 'C'.toByte(), 'D'.toByte(), 'E'.toByte(), 'F'.toByte(),
            'G'.toByte(), 'H'.toByte(), 'I'.toByte(), 'J'.toByte(), 'K'.toByte(), 'L'.toByte(),
            'M'.toByte(), 'N'.toByte(), 'O'.toByte(), 'P'.toByte(), 'Q'.toByte(), 'R'.toByte(),
            'S'.toByte(), 'T'.toByte(), 'U'.toByte(), 'V'.toByte(), 'W'.toByte(), 'X'.toByte(),
            'Y'.toByte(), 'Z'.toByte(),
            'a'.toByte(), 'b'.toByte(), 'c'.toByte(), 'd'.toByte(), 'e'.toByte(), 'f'.toByte(),
            'g'.toByte(), 'h'.toByte(), 'i'.toByte(), 'j'.toByte(), 'k'.toByte(), 'l'.toByte(),
            'm'.toByte(), 'n'.toByte(), 'o'.toByte(), 'p'.toByte(), 'q'.toByte(), 'r'.toByte(),
            's'.toByte(), 't'.toByte(), 'u'.toByte(), 'v'.toByte(), 'w'.toByte(), 'x'.toByte(),
            'y'.toByte(), 'z'.toByte(),
            '1'.toByte(), '2'.toByte(), '3'.toByte(),
            '4'.toByte(), '5'.toByte(), '6'.toByte(),
            '7'.toByte(), '8'.toByte(), '9'.toByte())

    private val DECODING_TABLE = ByteArray(128)

    private val BASE = 61
    private val LAST_ACC_MAX = IntArray(ENCODED_BLOCK_SIZE)

    init {
        for (i in 0 until DECODING_TABLE.size) {
            DECODING_TABLE[i] = 0xff.toByte()
        }

        for (i in 0 until ENCODING_TABLE.size) {
            DECODING_TABLE[ENCODING_TABLE[i].toInt()] = i.toByte()
        }

        for (i in 0 until LAST_ACC_MAX.size) {
            LAST_ACC_MAX[i] = -1
        }

        var i = 0
        var o = 0
        var accMax = 0

        while (i < BLOCK_SIZE || accMax >= BASE) {
            if (accMax < BASE) {
                i++
                accMax = accMax.shl(8) + 255
            }
            else {
                accMax /= BASE
                LAST_ACC_MAX[++o] = accMax
            }
        }
    }

    private fun encodeBlock(data: ByteArray, offset: Int, size: Int, out: OutputStream) {
        var i = offset
        val end = i + size
        var acc = 0
        var accMax = 0

        while (i < end || accMax >= BASE) {
            if (accMax < BASE) {
                acc = acc.shl(8) + ((data[i++].toInt() + 256) % 256)
                accMax = accMax.shl(8) + 255
            }
            else {
                val chr = acc % BASE
                out.write(ENCODING_TABLE[chr].toInt())
                acc /= BASE
                accMax /= BASE
            }
        }
        out.write(ENCODING_TABLE[acc].toInt())
    }

    private fun decodeBlock(inp: ByteArray, size: Int, outp: ByteArray): Int {

        if (size <= 1 || size > ENCODED_BLOCK_SIZE)
            throw Exception("Malformed block")

        var i = ENCODED_BLOCK_SIZE - size
        val end = ENCODED_BLOCK_SIZE
        var o = BLOCK_SIZE

        var accMax = LAST_ACC_MAX[size-1]
        if (accMax < 0)
            throw Exception("Malformed block")

        var acc =  inp[i++].toInt()

        while (i < end || accMax >= 255) {
            if (accMax < 255) {
                acc = acc * BASE + inp[i++].toInt()
                accMax = accMax * BASE + BASE - 1
            }
            else {
                val chr = acc and 0xff
                outp[--o] = chr.toByte()
                acc = acc.ushr(8)

                accMax = accMax.ushr(8)
            }
        }

        return o
    }

    fun encode(data: ByteArray, offset: Int, length: Int, out: OutputStream) {

        var inputIndex = offset
        val endIndex = offset + length

        while ( inputIndex < endIndex) {

            val blkSize = Math.min(endIndex - inputIndex, BLOCK_SIZE)
            encodeBlock(data, inputIndex, blkSize, out)
            inputIndex += blkSize
        }
    }

    fun encode(data: ByteArray, off: Int, length: Int): ByteArray {
        val bOut = ByteArrayOutputStream()
        encode(data, off, length, bOut)
        return bOut.toByteArray()
    }

    fun encode(data: ByteArray): ByteArray {
        return encode(data, 0, data.size)
    }

    private fun ignore(
            c: Char): Boolean {
        return c == '\n' || c == '\r' || c == '\t' || c == ' '
    }

    fun decode(data: ByteArray, offset: Int, length: Int, out: OutputStream) {

        val dblock = ByteArray(ENCODED_BLOCK_SIZE)
        val block = ByteArray(BLOCK_SIZE)

        var inputIndex = offset
        val endIndex = offset + length

        while ( inputIndex < endIndex) {

            var blkSize = 0
            var blkWPos = ENCODED_BLOCK_SIZE

            while (inputIndex < endIndex && blkSize < ENCODED_BLOCK_SIZE) {
                inputIndex = nextI(data, inputIndex, endIndex)
                if (inputIndex == -1)
                    break
                blkWPos --
                dblock[blkWPos] = DECODING_TABLE[data[inputIndex].toInt()]
                inputIndex++
                blkSize ++
            }

            if (blkSize == 0)
                break

            val o = decodeBlock(dblock, blkSize, block)
            out.write(block, o, block.size - o)
        }
    }


    fun decode(data: ByteArray, off: Int, length: Int): ByteArray {
        val bOut = ByteArrayOutputStream()
        decode(data, off, length, bOut)
        return bOut.toByteArray()
    }

    fun decode(data: ByteArray): ByteArray {
        return decode(data, 0, data.size)
    }


    private fun nextI(data: ByteArray, i_in: Int, finish: Int): Int {
        var i = i_in
        while (i < finish && ignore(data[i].toChar())) {
            i++
        }
        return if (i < finish) i else -1
    }
}
