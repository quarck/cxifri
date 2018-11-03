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

object StringEntropyEstimator {
    private fun log2(d: Double) = Math.log(d) / Math.log(2.0)

    fun getEntropyBits(string: String): Double {
        val counters = mutableMapOf<Char, Int>()

        for (chr in string) {
            counters[chr] = counters.getOrPut(chr, { -> 0 }) + 1
        }

        val len = string.length.toDouble()
        var entropy = 0.0

        for ((key, value) in counters) {
            val term = value / len
            entropy += term * log2(term)
        }

        return -entropy*len
    }

    fun getBruteforceNumIterations(string: String): Double {
        val entropy = getEntropyBits(string)
        if (entropy > 0)
            return Math.pow(2.0, entropy)
        return 0.0
    }

    fun getYearsToBruteforce(string: String): Pair<Double, Double> {
        val iters = getBruteforceNumIterations(string)
        val secondsSinglePc = iters / ESTIMATE_RATE_CLUSTER
        val secondsCluster = iters / ESTIMATE_RATE_CLUSTER
        return Pair(secondsSinglePc / SECONDS_IN_YEAR, secondsCluster / SECONDS_IN_YEAR)
    }

    const val ESTIMATE_RATE_SINGLE_PC = 1000
    const val ESTIMATE_RATE_CLUSTER = 10000 * ESTIMATE_RATE_SINGLE_PC
    const val SECONDS_IN_YEAR = 365 * 24 * 3600
}