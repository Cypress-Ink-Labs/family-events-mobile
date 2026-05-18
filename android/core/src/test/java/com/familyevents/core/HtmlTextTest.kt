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
        assertEquals(" trim", decodeHtmlEntities("&nbsp;trim"))
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
}
