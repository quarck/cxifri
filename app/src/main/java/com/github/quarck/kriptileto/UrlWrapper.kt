package com.github.quarck.kriptileto

import android.net.Uri

object UrlWrapper {

    fun wrap(text: String): String {
        return "$schemeHttps$text"
    }

    fun unwrap(uri: Uri): String? {
        return uri.path
    }

    fun unwrap(raw: String): String? {
        val schemePos = raw.indexOf(schemeHttps)
        if (schemePos == -1)
            return null

        val firstPos = schemePos + schemeHttps.length
        val spaces = charArrayOf(' ', '\t', '\r', '\n')
        val firstSpace = raw.indexOfAny(spaces, firstPos)
        if (firstSpace == -1)
            return raw.substring(firstPos)
        else
            return raw.substring(firstPos, firstSpace)
    }

    val schemeHttps = "https://privmsg.space/"
}