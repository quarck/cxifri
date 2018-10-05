package com.github.quarck.kriptileto

import org.junit.Test

import org.junit.Assert.*

class StringWrappingUnitTests {
    @Test
    fun stringWrappingIsCorrect() {
        val raw = "ABC"
        val wrapped = UrlWrapper.wrap(raw)
        assertEquals(wrapped, "https://privmsg.space/ABC")
    }

    @Test
    fun stringUnwrappingIsCorrect() {

        val raw1 = "https://privmsg.space/ABC"
        val raw2 = "Some Person:\nhttps://privmsg.space/ABC msg sent on: Today"
        val raw3 = "Some random text"
        val raw4 = "https://privmsg.space" // without anything else

        val str1 = UrlWrapper.unwrap(raw1)
        val str2 = UrlWrapper.unwrap(raw2)
        val str3 = UrlWrapper.unwrap(raw3)
        val str4 = UrlWrapper.unwrap(raw4)

        assertEquals(str1, "ABC")
        assertEquals(str2, "ABC")
        assertNull(str3)
        assertNull(str4)
    }
}
