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

class Base61Encoder {

    private fun Int.lbyte(data: ByteArray, i: Int): Int {
        return this.shl(8) + ((data[i].toInt() + 256) % 256)
    }

    private fun Int.sbyte(data: ByteArray, i: Int): Int {
        val chr = this and 0xff
        data[i] = chr.toByte()
        return this.ushr(8)
    }

    private fun Int.osymbol(out: OutputStream): Int {
        val chr = this % BASE
        out.write(ENCODING_TABLE[chr].toInt())
        return this / BASE
    }

    private fun Int.isymbol(inp: ByteArray, i: Int): Int {
        return this * BASE + inp[i].toInt()
    }

    private fun encodeBlock(data: ByteArray, offset: Int, out: OutputStream) {
        var i = offset
        var acc = 0

        acc = acc.lbyte(data, i++)
        acc = acc.osymbol(out)
        acc = acc.lbyte(data, i++)
        acc = acc.osymbol(out)
        acc = acc.lbyte(data, i++)
        acc = acc.osymbol(out)
        acc = acc.osymbol(out)

        acc = acc.lbyte(data, i++)
        acc = acc.osymbol(out)
        acc = acc.lbyte(data, i++)
        acc = acc.osymbol(out)
        acc = acc.lbyte(data, i++)
        acc = acc.osymbol(out)
        acc = acc.osymbol(out)

        acc = acc.lbyte(data, i++)
        acc = acc.osymbol(out)
        acc = acc.lbyte(data, i++)
        acc = acc.osymbol(out)
        acc.osymbol(out)
    }

    private fun decodeBlock(inp: ByteArray, outp: ByteArray) {

        var acc = 0
        var i = 0
        var o = BLOCK_SIZE - 1

        acc = acc.isymbol(inp, i++)
        acc = acc.isymbol(inp, i++)
        acc = acc.sbyte(outp, o--)
        acc = acc.isymbol(inp, i++)
        acc = acc.sbyte(outp, o--)

        acc = acc.isymbol(inp, i++)
        acc = acc.isymbol(inp, i++)
        acc = acc.sbyte(outp, o--)
        acc = acc.isymbol(inp, i++)
        acc = acc.sbyte(outp, o--)
        acc = acc.isymbol(inp, i++)
        acc = acc.sbyte(outp, o--)

        acc = acc.isymbol(inp, i++)
        acc = acc.isymbol(inp, i++)
        acc = acc.sbyte(outp, o--)
        acc = acc.isymbol(inp, i++)
        acc = acc.sbyte(outp, o--)
        acc = acc.isymbol(inp, i)
        acc.sbyte(outp, o)
    }

    private fun encodePartialBlock(data: ByteArray, offset: Int, len: Int, out: OutputStream) {

        var i = offset
        val end = offset + len
        var acc = 0

        if (i < end) {
            acc = acc.lbyte(data, i++)
            acc = acc.osymbol(out)
            if (i < end) {
                acc = acc.lbyte(data, i++)
                acc = acc.osymbol(out)

                if (i < end) {
                    acc = acc.lbyte(data, i++)
                    acc = acc.osymbol(out)
                    acc = acc.osymbol(out)

                    if (i < end) {
                        acc = acc.lbyte(data, i++)
                        acc = acc.osymbol(out)

                        if (i < end) {
                            acc = acc.lbyte(data, i++)
                            acc = acc.osymbol(out)

                            if (i < end) {
                                acc = acc.lbyte(data, i++)
                                acc = acc.osymbol(out)
                                acc = acc.osymbol(out)

                                if (i < end) {
                                    acc = acc.lbyte(data, i++)
                                    acc = acc.osymbol(out)
                                    if (i < end) {
                                        acc = acc.lbyte(data, i++)
                                        acc = acc.osymbol(out)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        acc.osymbol(out)

    }

    private fun decodePartialBlock(inp: ByteArray, inpLen: Int, outp: ByteArray): Int {

        var acc = 0
        var i = ENCODED_BLOCK_SIZE - inpLen
        var o = BLOCK_SIZE

        if (inpLen == 0)
            throw Exception("Malformed block")

        acc = acc.isymbol(inp, i++)

        if (inpLen >= 11) {
            acc = acc.isymbol(inp, i++)
            acc = acc.sbyte(outp, --o)
        }

        if (inpLen >= 10) {
            acc = acc.isymbol(inp, i++)
            acc = acc.sbyte(outp, --o)
        }

        if (inpLen >= 9) {
            acc = acc.isymbol(inp, i++)
        }

        if (inpLen >= 8) {
            acc = acc.isymbol(inp, i++)
            acc = acc.sbyte(outp, --o)
        }

        if (inpLen >= 7) {
            acc = acc.isymbol(inp, i++)
            acc = acc.sbyte(outp, --o)
        }

        if (inpLen >= 6) {
            acc = acc.isymbol(inp, i++)
            acc = acc.sbyte(outp, --o)
        }

        if (inpLen >= 5) {
            acc = acc.isymbol(inp, i++)
        }

        if (inpLen >= 4) {
            acc = acc.isymbol(inp, i++)
            acc = acc.sbyte(outp, --o)
        }

        if (inpLen >= 3) {
            acc = acc.isymbol(inp, i++)
            acc = acc.sbyte(outp, --o)
        }

        if (inpLen >= 2) {
            acc = acc.isymbol(inp, i)
            acc.sbyte(outp, --o)
        }

        return o
    }


    fun encode(data: ByteArray, offset: Int, length: Int, out: OutputStream) {

        var inputIndex = offset
        val endIndex = offset + length

        while ( inputIndex < endIndex) {

            val blkSize = Math.min(endIndex - inputIndex, BLOCK_SIZE)

            if (blkSize == BLOCK_SIZE) {
                encodeBlock(data, inputIndex, out)
            } else {
                encodePartialBlock(data, inputIndex, blkSize, out)
            }

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

            if (blkSize == ENCODED_BLOCK_SIZE) {
                decodeBlock(dblock, block)
                out.write(block, 0, block.size)
            }
            else {
                val o = decodePartialBlock(dblock, blkSize, block)
                out.write(block, o, block.size - o)
            }
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

    companion object {

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

        private val MAX_ACC_IN_THE_LAST_SYMBOL = IntArray(ENCODED_BLOCK_SIZE)

        init {
            for (i in 0 until DECODING_TABLE.size) {
                DECODING_TABLE[i] = 0xff.toByte()
            }

            for (i in 0 until ENCODING_TABLE.size) {
                DECODING_TABLE[ENCODING_TABLE[i].toInt()] = i.toByte()
            }

            for (i in 0 until MAX_ACC_IN_THE_LAST_SYMBOL.size) {
                MAX_ACC_IN_THE_LAST_SYMBOL[i] = -1
            }

            var max_acc = 0
            var nextInp = 0
            var nextOutp = 0

            while ( nextInp < BLOCK_SIZE || max_acc >= BASE) {

                if (max_acc < BASE) {
                    max_acc = max_acc * 256 + 255
                    nextInp ++
                }
                else {
                    max_acc /= BASE
                    nextOutp ++
                    MAX_ACC_IN_THE_LAST_SYMBOL[nextOutp] = max_acc
                }
            }

        }
    }
}
