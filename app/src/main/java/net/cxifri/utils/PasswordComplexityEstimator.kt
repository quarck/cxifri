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

import android.content.Context
import net.cxifri.R

data class ExhaustiveSearchComplexity(
        val pcDays: Double,
        val gpuDays: Double,
        val clusterDays: Double
) {
    val isSecure: Boolean
        get() = isSecureAgainstPc && isSecureAgainstGpu && isSecureAgainstCluster

    val isSecureAgainstPc get() = pcDays >= SECURE_DAYS
    val isSecureAgainstGpu get() = gpuDays >= SECURE_DAYS
    val isSecureAgainstCluster get() = clusterDays >= SECURE_DAYS

    fun formatPc(ctx: Context) = formatComplexity(ctx, pcDays)
    fun formatGpu(ctx: Context) = formatComplexity(ctx, gpuDays)
    fun formatCluster(ctx: Context) = formatComplexity(ctx, clusterDays)

    companion object {
        val WEEK_DAYS = 7.0
        val MONTH_DAYS = 30.0
        val YEAR_DAYS = 365.0
        val SECURE_YEARS = 100.0
        val SECURE_DAYS = YEAR_DAYS * SECURE_YEARS

        fun formatComplexity(ctx: Context, cpxtyDays: Double): String {
            if (cpxtyDays < WEEK_DAYS)
                return ctx.getString(R.string.format_days).format(cpxtyDays)
            if (cpxtyDays < MONTH_DAYS)
                return ctx.getString(R.string.format_weeks).format(cpxtyDays / WEEK_DAYS)
            if (cpxtyDays < YEAR_DAYS)
                return ctx.getString(R.string.format_months).format(cpxtyDays / MONTH_DAYS)
            return ctx.getString(R.string.format_years).format(cpxtyDays / YEAR_DAYS)
        }
    }
}

object PasswordComplexityEstimator {

    val LOWER_CASE = "[a-z]".toRegex()  // 26 total
    val UPPER_CASE = "[A-Z]".toRegex()   // 26 total
    val NUMBERS = "\\d".toRegex()       // 10
    val SYMBOLS = "[.,!?@#$%^&*_+\\-=]".toRegex() // 15
    val SYMBOLS_EXTRA = "[\\\\/()\\[\\]<>{}\"'|;:~`]".toRegex() // 18

    val NON_BASIC_TXT_SYMBOLS = "[@#$%^&*_+\\-=]".toRegex()

    val NUM_LOWER_CASE = 26
    val NUM_UPPER_CASE = 26
    val NUM_NUMBERS = 10
    val NUM_SYMBOLS = 15
    val NUM_EXTRA_SYMBOLS = 18

    val FAILBACK_NON_ASCII_NUM_CHARS = 255

    val NUM_COMMOM_ENGLISH_WORDS = 7776
    val AVERAGE_ENGLISH_WORD_LENGTH = 5.1

    val ENGLISH_LETTER_FREQS = hashMapOf(
            'E' to	0.1202, 'T' to	0.091  , 'A' to	0.0812 , 'O' to	0.0768 ,
            'I' to	0.0731 , 'N' to	0.0695 , 'S' to	0.0628 , 'R' to	0.0602 ,
            'H' to	0.0592 , 'D' to	0.0432 , 'L' to	0.0398 , 'U' to	0.0288 ,
            'C' to	0.0271 , 'M' to	0.0261 , 'F' to	0.023  , 'Y' to	0.0211 ,
            'W' to	0.0209 , 'G' to	0.0203 , 'P' to	0.0182 , 'B' to	0.0149 ,
            'V' to	0.0111 , 'K' to	0.0069 , 'X' to	0.0017 , 'Q' to	0.0011 ,
            'J' to	0.001  , 'Z' to	0.0007
            )

    //
    // If someone is to attempt exhaustive search on the password, the most of the work would be
    // on deriving keys from the raw passwords, which takes 10000 iterations of SHA256 digest
    // per each password. We can ignore computational cost of actually trying derived key as
    // it is way less than amount of work to derive the key.
    //
    // Thanks to crypto-miners, we have a good idea about current hash-rates of different hardware:
    // Best non-specialized PC:
    //      4x Opteron 6174 -   115 Mhash/s
    //      A10-5800K       -   105 Mhash/s
    //      Xeon Phi 5100   -   140 Mhash/s
    //
    // GPUs:
    //      AMD 5970        -   863.4 Mhash/s
    //      AMD 6990        -   865   Mhash/s
    //      Nvidia Tesla S2070	- 749.23 Mhash/s
    //
    // Thus, estimates for number of passwords / second:
    // Single PC: 100Mhash/s / 10khash = 10000
    // Single GPU: 800Mhash/s / 10khash = 80000
    //
    // For cluster - we just assume someone having 1000 Nvidia Teslas.
    //
    // All above numbers are valid for 3/11/2018 (day of writing this). To compensate for
    // the future CPU / GPU development, moore-law-factor would be applied based on the current
    // date, assuming twice performance every 3 years.
    //

    const val ESTIMATE_PASSWORDS_PER_SECOND_CPU = 10000
    const val ESTIMATE_PASSWORDS_PER_SECOND_GPU = 80000
    const val ESTIMATE_PASSWORDS_PER_SECOND_CLUSTER = 1000 * ESTIMATE_PASSWORDS_PER_SECOND_GPU
    const val ESTIMATE_TIMESTAMP_MILLIS = 1541289111000 // estimates above were taken on 4/11/2018


    const val SECONDS_IN_YEAR = (365 * 24 * 3600).toFloat()

    const val ESTIMATE_PASSWORDS_PER_YEAR_PC = ESTIMATE_PASSWORDS_PER_SECOND_CPU.toFloat() * SECONDS_IN_YEAR
    const val ESTIMATE_PASSWORDS_PER_YEAR_GPU = ESTIMATE_PASSWORDS_PER_SECOND_GPU.toFloat() * SECONDS_IN_YEAR
    const val ESTIMATE_PASSWORDS_PER_YEAR_CLUSTER = ESTIMATE_PASSWORDS_PER_SECOND_CLUSTER.toFloat() * SECONDS_IN_YEAR

    val MOORE_LAW_YEAR_FACTOR = Math.pow(2.0, 1.0/3.0) // twice every 3 years
    const val MOORE_LAW_DECAY_FACTOR = 100.0


    fun isEnglishText(string: String): Boolean {
        if (string.contains(SYMBOLS_EXTRA))
            return false
        if (string.contains(NON_BASIC_TXT_SYMBOLS))
            return false
        if (string.contains(NUMBERS))
            return false
        if (!string.contains(LOWER_CASE) && !string.contains(UPPER_CASE))
            return false

        val counters = mutableMapOf<Char, Int>()

        for (chr in string) {
            val upChr = chr.toUpperCase()
            counters[upChr] = counters.getOrPut(upChr, { -> 0 }) + 1
        }

        var distanceSqr = 0.0
        for (chr in ENGLISH_LETTER_FREQS.keys) {
            val observedFreq = (counters.get(chr) ?: 0) / string.length.toDouble()
            val expectedFreq = ENGLISH_LETTER_FREQS[chr] ?: 0.0
            distanceSqr += Math.pow(observedFreq - expectedFreq, 2.0)
        }

        return Math.sqrt(distanceSqr) < 0.15
    }

    fun exhaustiveSearchIterationsForEnglishText(string: String): Double {
        val numWords = string.length / AVERAGE_ENGLISH_WORD_LENGTH
        return Math.pow(NUM_COMMOM_ENGLISH_WORDS.toDouble(), numWords)
    }

    fun exhaustiveSearchIterations(string: String): Double {

        if (isEnglishText(string))
            return exhaustiveSearchIterationsForEnglishText(string)

        var charsPosible = 0
        if (string.contains(LOWER_CASE))
            charsPosible += NUM_LOWER_CASE
        if (string.contains(UPPER_CASE))
            charsPosible += NUM_UPPER_CASE
        if (string.contains(NUMBERS))
            charsPosible += NUM_NUMBERS
        if (string.contains(SYMBOLS))
            charsPosible += NUM_SYMBOLS
        if (string.contains(SYMBOLS_EXTRA))
            charsPosible += NUM_EXTRA_SYMBOLS

        if (charsPosible == 0)
            charsPosible = FAILBACK_NON_ASCII_NUM_CHARS // not an ASCII password - treat as a random sequence of bytes

        return Math.pow(charsPosible.toDouble(), string.length.toDouble())
    }

    fun getExhaustiveSearchComplexity(string: String): ExhaustiveSearchComplexity {
        val iters = exhaustiveSearchIterations(string)

        var pcYears: Double? = null
        var gpuYears: Double? = null
        var clusterYears: Double? = null

        val yearsSinceEstimate = ((System.currentTimeMillis() - ESTIMATE_TIMESTAMP_MILLIS) / 1000.0 / SECONDS_IN_YEAR).toInt()
        var computationalProgress = 1.0 // MOORE_LAW_YEAR_FACTOR // Math.pow(MOORE_LAW_YEAR_FACTOR, yearsSinceEstimate)

        for (year in 0 until yearsSinceEstimate) {
            computationalProgress *= expDecay(year.toDouble(), MOORE_LAW_DECAY_FACTOR, MOORE_LAW_YEAR_FACTOR)
        }

        var totalIterationsPc = 0.0
        var totalIterationsGpu = 0.0
        var totalIterationsCluster = 0.0

        for (year in 1..300) {

            totalIterationsPc += computationalProgress * ESTIMATE_PASSWORDS_PER_YEAR_PC
            totalIterationsGpu += computationalProgress * ESTIMATE_PASSWORDS_PER_YEAR_GPU
            totalIterationsCluster += computationalProgress * ESTIMATE_PASSWORDS_PER_YEAR_CLUSTER

            if (pcYears == null && totalIterationsPc > iters) {
                pcYears = if (year > 1) year.toDouble() else iters / totalIterationsPc
            }

            if (gpuYears == null && totalIterationsGpu > iters) {
                gpuYears = if (year > 1) year.toDouble() else iters / totalIterationsGpu
            }

            if (clusterYears == null && totalIterationsCluster > iters) {
                clusterYears = if (year > 1) year.toDouble() else iters / totalIterationsCluster
            }

            if (pcYears != null && gpuYears != null && clusterYears != null)
                break

            val mooreLawFactor = expDecay((year + yearsSinceEstimate).toDouble(),
                    MOORE_LAW_DECAY_FACTOR, MOORE_LAW_YEAR_FACTOR)

            computationalProgress *= mooreLawFactor

        }

        if (pcYears == null) {
            val rate = computationalProgress * ESTIMATE_PASSWORDS_PER_YEAR_PC
            pcYears = 300.0f + (iters - totalIterationsPc) / rate
        }

        if (gpuYears == null) {
            val rate = computationalProgress * ESTIMATE_PASSWORDS_PER_YEAR_GPU
            gpuYears = 300.0f + (iters - totalIterationsGpu) / rate
        }

        if (clusterYears == null) {
            val rate = computationalProgress * ESTIMATE_PASSWORDS_PER_YEAR_CLUSTER
            clusterYears = 300.0f + (iters - totalIterationsCluster) / rate
        }

        return ExhaustiveSearchComplexity(
                pcYears * 365.0,
                gpuYears * 365.0,
                clusterYears * 365.0
        )
    }

    fun expDecay(years: Double, scale: Double, initial: Double) =
            1.0 + (initial-1.0) * Math.exp(-years/scale)
}