package com.familyevents.core

import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlTextTest {

    @Test
    fun decimalEntity() {
        assertEquals("Music Mosaic – Indie", decodeHtmlEntities("Music Mosaic &#8211; Indie"))
    }

    @Test
    fun namedAmp() {
        assertEquals("Tom & Jerry", decodeHtmlEntities("Tom &amp; Jerry"))
    }

    @Test
    fun hexEntity() {
        assertEquals("Hex — em-dash", decodeHtmlEntities("Hex &#x2014; em-dash"))
    }

    @Test
    fun nbspBecomesSpace() {
        assertEquals(" trim", decodeHtmlEntities("&nbsp;trim"))
    }

    @Test
    fun noEntities() {
        assertEquals("no entities here", decodeHtmlEntities("no entities here"))
    }

    @Test
    fun emptyString() {
        assertEquals("", decodeHtmlEntities(""))
    }

    @Test
    fun unknownNamedEntityPassesThrough() {
        assertEquals("Unknown &fakething; passes through", decodeHtmlEntities("Unknown &fakething; passes through"))
    }

    @Test
    fun malformedDecimalPassesThrough() {
        assertEquals("Malformed &#abc; passes through", decodeHtmlEntities("Malformed &#abc; passes through"))
    }

    @Test
    fun mixedEntities() {
        assertEquals("Mixed & ’ ok", decodeHtmlEntities("Mixed &amp; &#8217; ok"))
    }

    @Test
    fun quotedEntity() {
        assertEquals("\"quoted\"", decodeHtmlEntities("&quot;quoted&quot;"))
    }

    @Test
    fun supplementaryEmojiCodepoint() {
        assertEquals("Happy 😀 face", decodeHtmlEntities("Happy &#128512; face"))
    }

    @Test
    fun loneSurrogatePassesThrough() {
        assertEquals("Bad &#xD800; entity", decodeHtmlEntities("Bad &#xD800; entity"))
    }

    @Test
    fun emptyHexPassesThrough() {
        assertEquals("Empty &#x; entity", decodeHtmlEntities("Empty &#x; entity"))
    }

    @Test
    fun ltEntity() {
        assertEquals("<less>", decodeHtmlEntities("&lt;less&gt;"))
    }

    @Test
    fun aposEntity() {
        assertEquals("it's", decodeHtmlEntities("it&apos;s"))
    }

    @Test
    fun uppercaseHexPrefix() {
        // &#X2014; (uppercase X) should decode the same as &#x2014;
        assertEquals("—", decodeHtmlEntities("&#X2014;"))
    }

    @Test
    fun emptyDecimalPassesThrough() {
        // &#; has no digits — should pass through verbatim
        assertEquals("Empty &#; entity", decodeHtmlEntities("Empty &#; entity"))
    }

    @Test
    fun consecutiveEntitiesNoSeparator() {
        assertEquals("&<>", decodeHtmlEntities("&amp;&lt;&gt;"))
    }

    @Test
    fun ampWithNoSemicolonAtEndOfString() {
        // A lone & at the end (no semicolon) passes through
        assertEquals("trailing &", decodeHtmlEntities("trailing &"))
    }

    @Test
    fun maxValidCodePoint() {
        // U+10FFFF is the highest valid Unicode scalar — should decode successfully
        val result = decodeHtmlEntities("&#x10FFFF;")
        assertEquals(1, result.codePoints().count().toInt())
        assertEquals(0x10FFFF, result.codePointAt(0))
    }

    @Test
    fun aboveMaxCodePointPassesThrough() {
        // U+110000 is out of range; should pass through verbatim
        assertEquals("&#x110000;", decodeHtmlEntities("&#x110000;"))
    }

    @Test
    fun stringWithOnlyEntities() {
        assertEquals("& < >", decodeHtmlEntities("&amp; &lt; &gt;"))
    }
}
