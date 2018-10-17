package com.github.quarck.kriptileto.dataprocessing

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

    val schemeHttps = "https://privmsg.space/"
}