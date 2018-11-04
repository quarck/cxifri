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

/*
Diceware Words List               7,776
*/
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

        return Math.sqrt(distanceSqr) < 0.05
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

        val yearsSinceEstimate = (System.currentTimeMillis() - ESTIMATE_TIMESTAMP_MILLIS) / 1000.0 / SECONDS_IN_YEAR.toDouble()

        val mooreLawFactor = Math.pow(MOORE_LAW_YEAR_FACTOR, yearsSinceEstimate)

        for ((yr, factor) in MOORE_LAW_ADJUSTED_ITERATIONS_PER_N_YEARS_FACTOR.withIndex()) {

            if (pcYears == null) {
                val iterationsByThisYear = factor * ESTIMATE_PASSWORDS_PER_YEAR_PC * mooreLawFactor
                if (iterationsByThisYear > iters) {
                    pcYears = if (yr > 1) yr.toDouble() else iters / iterationsByThisYear
                }
            }

            if (gpuYears == null) {
                val iterationsByThisYear = factor * ESTIMATE_PASSWORDS_PER_YEAR_GPU * mooreLawFactor
                if (iterationsByThisYear > iters) {
                    gpuYears = if (yr > 1) yr.toDouble() else iters / iterationsByThisYear
                }
            }

            if (clusterYears == null) {
                val iterationsByThisYear = factor * ESTIMATE_PASSWORDS_PER_YEAR_CLUSTER * mooreLawFactor
                if (iterationsByThisYear > iters) {
                    clusterYears = if (yr > 1) yr.toDouble() else iters / iterationsByThisYear
                }
            }

            if (pcYears != null && gpuYears != null && clusterYears != null)
                break
        }

        if (pcYears == null) {
            val startRate = ESTIMATE_PASSWORDS_PER_YEAR_PC
            val finalRate = startRate * OVER_300_YEARS_ITERATIONS_PER_YEAR_BOOST
            val remainingIterations = iters - startRate * MOORE_LAW_ADJUSTED_ITERATIONS_PER_N_YEARS_FACTOR.last()
            pcYears = (300.0f + remainingIterations / finalRate).toDouble()
        }

        if (gpuYears == null) {
            val startRate = ESTIMATE_PASSWORDS_PER_YEAR_GPU
            val finalRate = startRate * OVER_300_YEARS_ITERATIONS_PER_YEAR_BOOST
            val remainingIterations = iters - startRate * MOORE_LAW_ADJUSTED_ITERATIONS_PER_N_YEARS_FACTOR.last()
            gpuYears = (300.0f + remainingIterations / finalRate).toDouble()
        }

        if (clusterYears == null) {
            val startRate = ESTIMATE_PASSWORDS_PER_YEAR_CLUSTER
            val finalRate = startRate * OVER_300_YEARS_ITERATIONS_PER_YEAR_BOOST
            val remainingIterations = iters - startRate * MOORE_LAW_ADJUSTED_ITERATIONS_PER_N_YEARS_FACTOR.last()
            clusterYears = (300.0f + remainingIterations / finalRate).toDouble()
        }

        return ExhaustiveSearchComplexity(pcYears * 365.0, gpuYears * 365.0, clusterYears * 365.0)
    }

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

    const val MOORE_LAW_YEAR_FACTOR = 1.259921

    /*The code below is auto-generated by the following pythong3 script:
    *
        import math

        MOORE_LAW_YEAR_FACTOR = 1.259921
        MOORE_LAW_DECAY_FACTOR = 100

        def exp_decay(yr, scale, initial):
           return 1 + (initial-1) * math.exp(-yr/scale)

        ipyr_boost = 1.0 / MOORE_LAW_YEAR_FACTOR   # delay moore law start by 1 yr
        years_scale_factor = 0.0

        print ("val MOORE_LAW_ADJUSTED_ITERATIONS_PER_N_YEARS_FACTOR = floatArrayOf(")

        line = "\t\t"

        for year in range(0,300):
           line += "{0:1.5f}f, ".format(years_scale_factor)
           if len(line) > 80:
               print (line)
               line = "\t\t"
           mooref = exp_decay(year, MOORE_LAW_DECAY_FACTOR, MOORE_LAW_YEAR_FACTOR)
           ipyr_boost *= mooref
           years_scale_factor += ipyr_boost

        print (line, "\n)")

        print ("val OVER_300_YEARS_ITERATIONS_PER_YEAR_BOOST =", ipyr_boost)
    */

    val MOORE_LAW_ADJUSTED_ITERATIONS_PER_N_YEARS_FACTOR = floatArrayOf(
            0.00000f, 1.00000f, 2.25733f, 3.83501f, 5.81063f, 8.27962f, 11.35906f, 15.19229f,
            19.95451f, 25.85936f, 33.16690f, 42.19308f, 53.32097f, 67.01416f, 83.83262f, 104.45146f,
            129.68307f, 160.50323f, 198.08182f, 243.81888f, 299.38685f, 366.77999f, 448.37203f,
            546.98349f, 665.95978f, 809.26210f, 981.57263f, 1188.41635f, 1436.30166f, 1732.88256f,
            2087.14531f, 2509.62285f, 3012.64077f, 3610.59899f, 4320.29370f, 5161.28482f, 6156.31458f,
            7331.78376f, 8718.29240f, 10351.25287f, 12271.58384f, 14526.49458f, 17170.36983f,
            20265.76680f, 23884.53650f, 28109.08316f, 33033.77638f, 38766.53235f, 45430.58150f,
            53166.44164f, 62134.11734f, 72515.54760f, 84517.32616f, 98373.72005f, 114350.01463f,
            132746.21472f, 153901.13421f, 178196.90832f, 206063.96535f, 237986.49689f, 274508.46834f,
            316240.21389f, 363865.66298f, 418150.24791f, 479949.54529f, 550218.70670f, 630022.73702f,
            720547.68193f, 823112.78898f, 939183.70987f, 1070386.81453f, 1218524.69079f, 1385592.90649f,
            1573798.11387f, 1785577.57930f, 2023620.22412f, 2290889.26565f, 2590646.54974f,
            2926478.66967f, 3302324.96801f, 3722507.52131f, 4191763.20916f, 4715277.97183f,
            5298723.36227f, 5948295.50024f, 6670756.53766f, 7473478.74577f, 8364491.33531f,
            9352530.12210f, 10447090.15024f, 11658481.38568f, 12997887.59226f, 14477428.50194f,
            16110225.38962f, 17910470.16171f, 19893498.06578f, 22075864.12600f, 24475423.40699f,
            27111415.20485f, 30004551.26100f, 33177108.09025f, 36653023.50984f, 40457997.45134f,
            44619597.13175f, 49167366.65407f, 54132941.10121f, 59550165.18036f, 65455216.46714f,
            71886733.29143f, 78885947.29822f, 86496820.70810f, 94766188.29300f, 103743904.07302f,
            113482992.73034f, 124039805.72587f, 135474182.09359f, 147849613.87649f, 161233416.15684f,
            175696901.62173f, 191315559.59326f, 208169239.44058f, 226342338.27872f, 245923992.84695f,
            267008275.44683f, 289694393.80767f, 314086894.73432f, 340295871.38001f, 368437173.97400f,
            398632623.82161f, 431010230.38180f, 465704411.21508f, 502856214.58294f, 542613544.46764f,
            585131387.77029f, 630572043.43351f, 679105353.22441f, 730908933.90291f, 786168410.49073f,
            845077650.34633f, 907838997.74255f, 974663508.63460f, 1045771185.29840f, 1121391210.51165f,
            1201762180.94326f, 1287132339.41070f, 1377759805.65892f, 1473912805.31021f, 1575869896.62977f,
            1683920194.74854f, 1798363592.98219f, 1919510980.88315f, 2047684458.66147f, 2183217547.60985f,
            2326455396.16848f, 2477754981.26683f, 2637485304.58091f, 2806027583.34777f, 2983775435.38220f,
            3171135057.94490f, 3368525400.11649f, 3576378328.33754f, 3795138784.78111f, 4025264938.23179f,
            4267228327.15299f, 4521513994.63315f, 4788620614.91035f, 5069060611.18541f, 5363360264.44363f,
            5672059813.01673f, 5995713542.62839f, 6334889866.67880f, 6690171396.53677f, 7062155001.62097f,
            7451451859.06580f, 7858687492.78135f, 8284501801.73171f, 8729549077.27039f, 9194498009.38720f,
            9680031681.73610f, 10186847555.32936f, 10715657440.79926f, 11267187459.14463f,
            11842177990.89565f, 12441383613.64671f, 13065573027.92341f, 13715528971.36621f,
            14392048121.22944f, 15095940985.21087f, 15828031780.64300f, 16589158302.09363f,
            17380171777.43890f, 18201936712.48791f, 19055330724.25354f, 19941244362.97934f,
            20860580923.04742f, 21814256242.90699f, 22803198494.17744f, 23828347960.09421f,
            24890656803.47886f, 25991088824.42856f, 27130619207.93261f, 28310234261.63609f,
            29530931143.98293f, 30793717582.98177f, 32099611585.84906f, 33449641139.79451f,
            34844843904.22357f, 36286266894.64130f, 37774966158.55067f, 39312006443.64677f,
            40898460858.61604f, 42535410526.85681f, 44223944233.44426f, 45965158065.66863f,
            47760155047.48134f, 49610044768.18822f, 51515943005.73376f, 53478971344.92375f,
            55500256790.93709f, 57580931378.48001f, 59722131776.93830f, 61924998891.88433f,
            64190677463.29703f, 66520315660.85310f, 68915064676.64803f, 71376078315.70462f,
            73904512584.62592f, 76501525278.74767f, 79168275568.14365f, 81905923582.83446f,
            84715629997.54768f, 87598555616.37384f, 90555860957.65869f, 93588705839.46848f,
            96698248965.95988f, 99885647514.98181f, 103152056727.23042f, 106498629497.27362f,
            109926515966.75499f, 113436863120.08095f, 117030814382.88840f, 120709509223.58331f,
            124474082758.23361f, 128325665359.09239f, 132265382267.02003f, 136294353208.06601f,
            140413692014.46335f, 144624506250.28049f, 148927896841.96725f, 153324957714.02316f,
            157816775430.00775f, 162404428839.10431f, 167088988728.43942f, 171871517481.35220f,
            176753068741.79816f, 181734687085.06390f, 186817407694.96021f, 192002256047.65152f,
            197290247602.27179f, 202682387498.46738f, 208179670260.99857f, 213783079511.52328f,
            219493587687.67703f, 225312155769.55478f, 231239733013.69183f, 237277256694.63248f,
            243425651854.16602f, 249685831058.30234f, 256058694162.05038f, 262545128082.05487f,
            269146006577.13870f, 275862190036.79071f, 282694525277.63062f, 289643845347.87518f,
            296710969339.82306f, 303896702210.36700f, 311201834609.53711f, 318627142717.06976f,
            326173388086.99176f, 333841317500.20148f, 341631662825.02332f, 349545140885.70496f,
            357582453338.82111f, 365744286557.54175f, 374031311523.71667f, 382444183727.72382f,
            390983543076.02185f, 399650013806.34454f, 408444204410.46747f, 417366707564.47485f,
            426418100066.44818f, 435598942781.49512f, 444909780594.03284f, 454351142367.23541f,
            463923540909.55243f, 473627472948.20081f, 483463419109.53027f, 493431843906.15826f,
            503533195730.76825f, 513767906856.46167f
            )

    val OVER_300_YEARS_ITERATIONS_PER_YEAR_BOOST = 10368486587.090324
}