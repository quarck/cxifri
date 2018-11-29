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
 * How efficient is it?
 * The way how base61 encoding is implemented, it would on average encode every 100 bytes of input as
 * 135 symbols of output. Compared to base64, base64 would encode 100 bytes as 100*8/6 = 133.333
 * bytes on average. Thus base61 is 1.25% less efficient.
 *
 * Why not base62 (thus, using also zero)?
 * base62 would not increase efficiency really significantly, but would complicate some implementation
 * details - the fact that 61 is a prime helps in some mathematical aspects of the current implementation.
 *
 * Did you take any drugs while creating this?
 * I understand why you are asking, but the answer is no.
 *
 */

package net.cxifri.encodings

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.experimental.and

class Base61Encoder {

    fun encode(data: ByteArray, offset: Int, length: Int, out: OutputStream) {

        var inputIndex = offset
        val endIndex = offset + length

        while ( inputIndex < endIndex) {
            val blkSize = Math.min(endIndex - inputIndex, BLOCK_SIZE)

            var accumulatorValidBits = 0
            var accumulator = 0
            var nextInp = 0

            while ( nextInp < blkSize || accumulatorValidBits >= BITS_PER_SYMBOL_LOWER) {

                if (accumulatorValidBits < BITS_PER_SYMBOL_LOWER) {

                    accumulator = accumulator.shl(8) + data[inputIndex+nextInp].toInt()
                    nextInp ++

                    accumulatorValidBits += BITS_PER_BYTE
                }
                else {

                    val chr = accumulator % BASE
                    accumulator /= BASE
                    out.write(ENCODING_TABLE[chr].toInt())

                    accumulatorValidBits -= BITS_PER_SYMBOL_LOWER
                }
            }

            out.write(ENCODING_TABLE[accumulator].toInt())

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

            var accumulatorValidBits = BITS_IN_THE_LAST_SYMBOL[blkSize-1]
            var accumulator = dblock[blkWPos].toInt()
            var nextInp = blkWPos+1
            var nextOutp = BLOCK_SIZE

            while ( nextInp < ENCODED_BLOCK_SIZE || accumulatorValidBits >= BITS_PER_BYTE) {

                if (accumulatorValidBits >= BITS_PER_BYTE) {
                    // decodingTable[data[i++].toInt()]
                    val chr = accumulator and 0xff
                    //out.write(chr)
                    nextOutp--
                    block[nextOutp] = chr.toByte()
                    accumulator = accumulator.ushr(8)
                    accumulatorValidBits -= BITS_PER_BYTE
                }
                else {
                    accumulator = accumulator * BASE + dblock[nextInp].toInt()
                    nextInp++
                    accumulatorValidBits += BITS_PER_SYMBOL_LOWER
                }
            }

            out.write(block, nextOutp, block.size - nextOutp)

//            out.write(ENCODING_TABLE[accumulator].toInt())
      }


//        var i = off
//        val end = off + length
//
//        while ( i < end) {
//            var blkSize = 0
//
//        }
//


//        val block = ByteArray(ENCODED_BLOCK_SIZE)
//        val outBlock = ByteArray(BLOCK_SIZE)
//        var blockLong: Long
//        var blkSize: Int
//
//
//        while ( i < end) {
//            blkSize = 0
//
//            for (blkIdx in 0 until ENCODED_BLOCK_SIZE) {
//                i = nextI(data, i, end)
//                if (i == -1)
//                    break
//                block[blkIdx] = decodingTable[data[i++].toInt()]
//                blkSize ++
//            }
//
//            if (blkSize < ENCODED_BLOCK_SIZE)
//                break
//            // decode the full size block now
//
//            blockLong = 0L
//            for (blkIdx in 0 until ENCODED_BLOCK_SIZE) {
//                val chr = block[ENCODED_BLOCK_SIZE-blkIdx-1]
//                blockLong = blockLong * BASE + chr
//            }
//
//            for (blkIdx in 0 until BLOCK_SIZE) {
//                outBlock[BLOCK_SIZE - blkIdx - 1] = (blockLong and 0xffL).toByte()
//                blockLong = blockLong.ushr(8)
//            }
//            out.write(outBlock)
//        }
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

    companion object {

        private val BITS_PER_SYMBOL_LOWER = 59307 // fixed point, floor(log2(61)*10000)
        private val BITS_PER_SYMBOL_UPPER = 59308 // fixed point, ceil(log2(61)*10000)
        private val BITS_PER_BYTE = 80000 // same fixed point

        // with the current precision, implementation is capable of correctly handling input
        // byte array lengths up to 752. We choose to split data into blocks of 100 as
        // it gives a nice boundary in terms of bit usage efficiency:
        // 100 input bytes  - 800 bits of entropy - are encoded as 135 output symbols.
        // on the other hand, 135 symbols can encode up to 135 * log2(61) bits of entrophy =
        // = 800.6495 bits. Thus, per each 100 bytes block we are only loosing 0.64 bits to
        // in-efficiency of the encoding.
        //
        private val BLOCK_SIZE = 100
        private val ENCODED_BLOCK_SIZE = 135

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

        private val BITS_IN_THE_LAST_SYMBOL = IntArray(ENCODED_BLOCK_SIZE)

        init {

            for (i in 0 until DECODING_TABLE.size) {
                DECODING_TABLE[i] = 0xff.toByte()
            }

            for (i in 0 until ENCODING_TABLE.size) {
                DECODING_TABLE[ENCODING_TABLE[i].toInt()] = i.toByte()
            }

            for (i in 0 until BITS_IN_THE_LAST_SYMBOL.size) {
                BITS_IN_THE_LAST_SYMBOL[i] = -1
            }

            var acc_bits_lower = 0
            var acc_bits_upper = 0
            var nextInp = 0
            var nextOutp = 0

            while ( nextInp < BLOCK_SIZE || acc_bits_lower >= BITS_PER_SYMBOL_LOWER) {

                if (acc_bits_lower < BITS_PER_SYMBOL_LOWER && acc_bits_upper < BITS_PER_SYMBOL_UPPER) {
                    acc_bits_lower += BITS_PER_BYTE
                    acc_bits_upper += BITS_PER_BYTE
                    // here we should be reading next byte inp[nextInp]
                    nextInp ++
                }
                else if (acc_bits_lower >= BITS_PER_SYMBOL_LOWER && acc_bits_upper >= BITS_PER_SYMBOL_UPPER) {
                    acc_bits_lower -= BITS_PER_SYMBOL_LOWER
                    acc_bits_upper -= BITS_PER_SYMBOL_UPPER
                    // here we should be writing res at outp[nextOutp]
                    nextOutp ++

                    // if prev input was the last byte of input - we have no more bytes left,
                    // thus we would need to output the current accumulator into the output, and
                    // amount of bits of info it would have is acc_bits_lower (while doing real encoding
                    // we would be using lower range)

                    BITS_IN_THE_LAST_SYMBOL[nextOutp] = acc_bits_lower
                }
                else {
                    throw Exception("Base61Encoder: Internal self consistency check failed: Precision is not sufficient")
                }
            }

        }
    }
}
