package com.familyevents.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PostgrestFilterTest {
    @Test
    fun escapePostgrestIlikeEscapesLikeWildcardsAndBackslashes() {
        assertEquals("100\\%", escapePostgrestIlike("100%"))
        assertEquals("age\\_5", escapePostgrestIlike("age_5"))
        assertEquals("path\\\\name", escapePostgrestIlike("path\\name"))
    }

    @Test
    fun escapePostgrestIlikeRemovesFilterGrammarCharacters() {
        assertEquals("parks", escapePostgrestIlike("(parks)"))
        assertEquals("north side", escapePostgrestIlike("north, side"))
    }

    @Test
    fun escapePostgrestIlikeHandlesMixedInput() {
        assertEquals("50\\% off\\\\kids\\_night", escapePostgrestIlike("(50% off\\kids_night),"))
    }
}
