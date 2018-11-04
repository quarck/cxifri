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

package net.cxifri.utils

object EntrophyCalculator {
    fun getEntropyBits(string: String): Double {
        val log2 = Math.log(2.0)
        val counters = mutableMapOf<Char, Int>()

        for (chr in string) {
            counters[chr] = counters.getOrPut(chr, { -> 0 }) + 1
        }

        val len = string.length.toDouble()
        var entropy = 0.0

        for ((_, value) in counters) {
            entropy += value  * Math.log(value / len) / log2
        }

        return -entropy
    }
}