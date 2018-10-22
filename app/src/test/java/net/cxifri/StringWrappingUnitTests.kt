package net.cxifri

import net.cxifri.dataprocessing.UrlWrapper
import org.junit.Test

import org.junit.Assert.*

class StringWrappingUnitTests {
    @Test
    fun stringWrappingIsCorrect() {
        val raw = "ABC"
        val wrapped = UrlWrapper.wrap(raw)
        assertEquals(wrapped, "https://cxifri.net/ABC/")
    }

    @Test
    fun stringUnwrappingIsCorrect() {

        val raw1 = "https://cxifri.net/ABC/"
        val raw2 = "Some Person:\nhttps://cxifri.net/ABC/ msg sent on: Today"
        val raw22 = "Some Person:\nhttps://cxifri.net/ABC msg sent on: Today"
        val raw3 = "Some random text"
        val raw4 = "https://cxifri.net" // without anything else
        val raw41 = "https://cxifri.net/" // without anything else
        val raw42 = "https://cxifri.net//" // without anything else

        val str1 = UrlWrapper.unwrap(raw1)
        val str2 = UrlWrapper.unwrap(raw2)
        val str22 = UrlWrapper.unwrap(raw22)
        val str3 = UrlWrapper.unwrap(raw3)
        val str4 = UrlWrapper.unwrap(raw4)
        val str41 = UrlWrapper.unwrap(raw41)
        val str42 = UrlWrapper.unwrap(raw42)

        assertEquals(str1, "ABC")
        assertEquals(str2, "ABC")
        assertEquals(str22, "ABCmsgsenton:Today")
        assertNull(str3)
        assertNull(str4)
        assertEquals(str41, "")
        assertEquals(str42, "")
    }
}
