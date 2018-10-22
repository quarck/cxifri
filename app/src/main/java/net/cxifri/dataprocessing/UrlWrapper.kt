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

package net.cxifri.dataprocessing

import android.net.Uri

object UrlWrapper {

    fun wrap(text: String): String {
        return "$schemeHttps$text/"
    }

    fun unwrap(uri: Uri): String? {
        return uri.path
    }

    fun unwrap(raw: String): String? {
        val schemePos = raw.indexOf(schemeHttps)
        if (schemePos == -1)
            return null

        val firstPos = schemePos + schemeHttps.length
        val firstSlash = raw.indexOf('/', firstPos)

        val result =
            if (firstSlash == -1)
                raw.substring(firstPos)
            else
                raw.substring(firstPos, firstSlash)

        return result.replace("\\s".toRegex(), "")
    }

    fun unwrapOrKillSpaces(raw: String): String {
        val schemePos = raw.indexOf(schemeHttps)
        if (schemePos == -1)
            return raw.replace("\\s".toRegex(), "")

        val firstPos = schemePos + schemeHttps.length
        val firstSlash = raw.indexOf('/', firstPos)

        val result =
                if (firstSlash == -1)
                    raw.substring(firstPos)
                else
                    raw.substring(firstPos, firstSlash)

        return result.replace("\\s".toRegex(), "")
    }

    val schemeHttps = "https://cxifri.net/"
}