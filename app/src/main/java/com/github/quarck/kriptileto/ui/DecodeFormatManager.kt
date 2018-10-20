/*
 * Copyright (C) 2010 ZXing authors
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

package com.github.quarck.kriptileto.ui

import android.content.Intent
import android.net.Uri
import com.google.zxing.BarcodeFormat

import java.util.Arrays
import java.util.EnumSet
import java.util.HashMap
import java.util.regex.Pattern

internal object DecodeFormatManager {

    private val COMMA_PATTERN = Pattern.compile(",")

    val PRODUCT_FORMATS: Set<BarcodeFormat>
    val INDUSTRIAL_FORMATS: Set<BarcodeFormat>
    private val ONE_D_FORMATS: MutableSet<BarcodeFormat>
    val QR_CODE_FORMATS: Set<BarcodeFormat> = EnumSet.of(BarcodeFormat.QR_CODE)
    val DATA_MATRIX_FORMATS: Set<BarcodeFormat> = EnumSet.of(BarcodeFormat.DATA_MATRIX)
    val AZTEC_FORMATS: Set<BarcodeFormat> = EnumSet.of(BarcodeFormat.AZTEC)
    val PDF417_FORMATS: Set<BarcodeFormat> = EnumSet.of(BarcodeFormat.PDF_417)

    init {
        PRODUCT_FORMATS = EnumSet.of(BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.RSS_14,
                BarcodeFormat.RSS_EXPANDED)
        INDUSTRIAL_FORMATS = EnumSet.of(BarcodeFormat.CODE_39,
                BarcodeFormat.CODE_93,
                BarcodeFormat.CODE_128,
                BarcodeFormat.ITF,
                BarcodeFormat.CODABAR)
        ONE_D_FORMATS = EnumSet.copyOf(PRODUCT_FORMATS)
        ONE_D_FORMATS.addAll(INDUSTRIAL_FORMATS)
    }

    fun parseDecodeFormats(intent: Intent): Set<BarcodeFormat> {
        return QR_CODE_FORMATS
    }

    fun parseDecodeFormats(inputUri: Uri): Set<BarcodeFormat> {
        return QR_CODE_FORMATS
    }

    private fun parseDecodeFormats(scanFormats: Iterable<String>, decodeMode: String): Set<BarcodeFormat> {
        return QR_CODE_FORMATS
    }

}//  private static final Map<String,Set<BarcodeFormat>> FORMATS_FOR_MODE;
//  static {
//    FORMATS_FOR_MODE = new HashMap<>();
//    FORMATS_FOR_MODE.put(Intents.Scan.ONE_D_MODE, ONE_D_FORMATS);
//    FORMATS_FOR_MODE.put(Intents.Scan.PRODUCT_MODE, PRODUCT_FORMATS);
//    FORMATS_FOR_MODE.put(Intents.Scan.QR_CODE_MODE, QR_CODE_FORMATS);
//    FORMATS_FOR_MODE.put(Intents.Scan.DATA_MATRIX_MODE, DATA_MATRIX_FORMATS);
//    FORMATS_FOR_MODE.put(Intents.Scan.AZTEC_MODE, AZTEC_FORMATS);
//    FORMATS_FOR_MODE.put(Intents.Scan.PDF417_MODE, PDF417_FORMATS);
//  }
