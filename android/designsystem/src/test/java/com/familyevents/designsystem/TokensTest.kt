package com.familyevents.designsystem

import com.familyevents.designsystem.generated.Tokens
import org.junit.Assert.assertEquals
import org.junit.Test

class TokensTest {
    @Test
    fun generatedTokensExposeRequiredValues() {
        assertEquals(44f, Tokens.Touch.Min.value)
        assertEquals(8f, Tokens.Radius.Md.value)
        assertEquals(640f, Tokens.Breakpoint.Md.value)
    }
}
