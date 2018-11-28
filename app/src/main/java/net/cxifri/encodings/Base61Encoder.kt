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
 * With base61, zero '0' symbol is reserved for future use. Can be used by the application code to
 * indicate special cases / etc.
 *
 * Encoded data is split into 8 byte blocks, and each 8 byte block is encoded with 11 symbols of
 * Base61 encoding: 2^(8*8) is less than 61 ^ 11, thus such encoding is possible.
 *
 * How efficient is it? Base64 would encode every 6 bytes as 8 symbols of base64 text, thus on average
 * every 8 bytes would be encoded as 8 * 8 / 6 = 10.66.. symbols. Compared to 11 symbols for base61
 * is not a huge loss, it is about 3.125% less efficient.
 *
 * Can it be more efficient? In theory - yes. Technically 11 symbols of base61 encoding would
 * encode 65.238 bits of information, while they are used for encoding just 64 bits of the source text.
 * Thus, if we want we can create more efficient encoding that would encode 8.155 bytes per 11 symbols
 * of base61, which is 1.93% more efficient that current approach. However the resulting implementation
 * would have O(n*n) computational complexity, compared to O(n) with the current implementation.
 * Decision was made to got for O(n).
 *
 * Why not base62? The thing it, it would still require 11 symbols per each 8 bytes of input or
 * O(n*n) complexity for more efficient encoding. Thus there is no need to use 61 symbols. And
 * On the other hand 61 is a prime number, which is kind of nice.
 *
 * How the last block is encoded? (The one that is smaller than 8 bytes).
 * We can easily calculate the following translation table:
 *
 *  __________________________________________________________________
 *  | Length of the    |   Required base61          | Rounded up
 *  |  last block      |    len to encode           | base 61 len
 *  |      N           |  N * log(256) / log(61)    |
 *  ------------------------------------------------------------------
 *  |      1           |       1.349                |      2
 *  |      2           |       2.698                |      3
 *  |      3           |       4.047                |      5
 *  |      4           |       5.397                |      6
 *  |      5           |       6.745                |      7
 *  |      6           |       8.093                |      9
 *  |      7           |       9.442                |      10
 *  |      8 -- full block -- encoded as usual      |      11
 *  ------------------------------------------------------------------
 *
 * Thus, based on the length of the encoded text we identify what is the length of the last block,
 * as there is a direct 1-to-1 relationship, and thus there is no need for special padding symbols
 * to be used.
 *
 * Did you take any drugs while creating this?
 * I understand why you are asking, but the answer is no.
 *
 */

package net.cxifri.encodings

import java.io.ByteArrayOutputStream
import java.io.OutputStream

class Base61Encoder {
    private  val encodingTable = byteArrayOf(
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

    private val decodingTable = ByteArray(128)

    private val ENCODED_BLOCK_SIZE = 11
    private val BLOCK_SIZE = 8
    private val BASE = 61

    init {

        for (i in 0 until decodingTable.size) {
            decodingTable[i] = 0xff.toByte()
        }

        for (i in 0 until encodingTable.size) {
            decodingTable[encodingTable[i].toInt()] = i.toByte()
        }
    }

    fun encode(data: ByteArray, off: Int, length: Int, out: OutputStream) {

        val modulus = length % 8
        val numFullBlocks = length / 8
        var i = off

        for (blk in 0 until numFullBlocks) {
            var block = 0L
            for (bi in 0 until BLOCK_SIZE) {
                block = block.shl(8) or (data[i].toLong() and 0xffL)
                i++
            }

            for (bo in 0 until ENCODED_BLOCK_SIZE) {
                val chr = block % BASE
                block /= BASE
                out.write(encodingTable[chr.toInt()].toInt())
            }
        }
    }

    fun encode(data: ByteArray, off: Int, length: Int): ByteArray {
        val bOut = ByteArrayOutputStream()
        encode(data, off, length, bOut)
        return bOut.toByteArray()
    }

    private fun ignore(
            c: Char): Boolean {
        return c == '\n' || c == '\r' || c == '\t' || c == ' '
    }

    fun decode(data: ByteArray, off: Int, length: Int, out: OutputStream) {

        val block = ByteArray(ENCODED_BLOCK_SIZE)
        val outBlock = ByteArray(BLOCK_SIZE)
        var blockLong: Long
        var blkSize: Int

        var i = off
        val end = off + length

        while ( i < end) {
            blkSize = 0

            for (blkIdx in 0 until ENCODED_BLOCK_SIZE) {
                i = nextI(data, i, end)
                if (i == -1)
                    break
                block[blkIdx] = decodingTable[data[i++].toInt()]
                blkSize ++
            }

            if (blkSize < ENCODED_BLOCK_SIZE)
                break
            // decode the full size block now

            blockLong = 0L
            for (blkIdx in 0 until ENCODED_BLOCK_SIZE) {
                val chr = block[ENCODED_BLOCK_SIZE-blkIdx-1]
                blockLong = blockLong * BASE + chr
            }

            for (blkIdx in 0 until BLOCK_SIZE) {
                outBlock[BLOCK_SIZE - blkIdx - 1] = (blockLong and 0xffL).toByte()
                blockLong = blockLong.ushr(8)
            }
            out.write(outBlock)
        }
    }


    fun decode(data: ByteArray, off: Int, length: Int): ByteArray {
        val bOut = ByteArrayOutputStream()
        decode(data, off, length, bOut)
        return bOut.toByteArray()
    }


    private fun nextI(data: ByteArray, i_in: Int, finish: Int): Int {
        var i = i_in
        while (i < finish && ignore(data[i].toChar())) {
            i++
        }
        return if (i < finish) i else -1
    }


//    override fun decode(
//            data: String,
//            out: OutputStream): Int {
//        var b1: Byte
//        var b2: Byte
//        var b3: Byte
//        var b4: Byte
//        var length = 0
//
//        var end = data.length
//
//        while (end > 0) {
//            if (!ignore(data[end - 1])) {
//                break
//            }
//
//            end--
//        }
//
//        // empty data!
//        if (end == 0) {
//            return 0
//        }
//
//        var i = 0
//        var finish = end
//
//        while (finish > 0 && i != 4) {
//            if (!ignore(data[finish - 1])) {
//                i++
//            }
//
//            finish--
//        }
//
//        i = nextI(data, 0, finish)
//
//        while (i < finish) {
//            b1 = decodingTable[data[i++]]
//
//            i = nextI(data, i, finish)
//
//            b2 = decodingTable[data[i++]]
//
//            i = nextI(data, i, finish)
//
//            b3 = decodingTable[data[i++]]
//
//            i = nextI(data, i, finish)
//
//            b4 = decodingTable[data[i++]]
//
//            if (b1.toInt() or b2.toInt() or b3.toInt() or b4.toInt() < 0) {
//                throw IOException("invalid characters encountered in base64 data")
//            }
//
//            out.write(b1 shl 2 or (b2 shr 4))
//            out.write(b2 shl 4 or (b3 shr 2))
//            out.write(b3 shl 6 or b4)
//
//            length += 3
//
//            i = nextI(data, i, finish)
//        }
//
//        val e0 = nextI(data, i, end)
//        val e1 = nextI(data, e0 + 1, end)
//        val e2 = nextI(data, e1 + 1, end)
//        val e3 = nextI(data, e2 + 1, end)
//
//        length += decodeLastBlock(out, data[e0], data[e1], data[e2], data[e3])
//
//        return length
//    }
//
//    @Throws(IOException::class)
//    private fun decodeLastBlock(out: OutputStream, c1: Char, c2: Char, c3: Char, c4: Char): Int {
//        val b1: Byte
//        val b2: Byte
//        val b3: Byte
//        val b4: Byte
//
//        if (c3.toByte() == padding) {
//            if (c4.toByte() != padding) {
//                throw IOException("invalid characters encountered at end of base64 data")
//            }
//
//            b1 = decodingTable[c1]
//            b2 = decodingTable[c2]
//
//            if (b1 or b2 < 0) {
//                throw IOException("invalid characters encountered at end of base64 data")
//            }
//
//            out.write(b1 shl 2 or (b2 shr 4))
//
//            return 1
//        } else if (c4.toByte() == padding) {
//            b1 = decodingTable[c1]
//            b2 = decodingTable[c2]
//            b3 = decodingTable[c3]
//
//            if (b1.toInt() or b2.toInt() or b3.toInt() < 0) {
//                throw IOException("invalid characters encountered at end of base64 data")
//            }
//
//            out.write(b1 shl 2 or (b2 shr 4))
//            out.write(b2 shl 4 or (b3 shr 2))
//
//            return 2
//        } else {
//            b1 = decodingTable[c1]
//            b2 = decodingTable[c2]
//            b3 = decodingTable[c3]
//            b4 = decodingTable[c4]
//
//            if (b1.toInt() or b2.toInt() or b3.toInt() or b4.toInt() < 0) {
//                throw IOException("invalid characters encountered at end of base64 data")
//            }
//
//            out.write(b1 shl 2 or (b2 shr 4))
//            out.write(b2 shl 4 or (b3 shr 2))
//            out.write(b3 shl 6 or b4)
//
//            return 3
//        }
//    }
//
//    private fun nextI(data: String, i: Int, finish: Int): Int {
//        var i = i
//        while (i < finish && ignore(data[i])) {
//            i++
//        }
//        return i
//    }
}
