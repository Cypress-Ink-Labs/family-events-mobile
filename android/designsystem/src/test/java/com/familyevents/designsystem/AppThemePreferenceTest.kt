package com.familyevents.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemePreferenceTest {
    @Test
    fun invalidRawValueDefaultsToSystem() {
        assertEquals(AppThemePreference.System, AppThemePreference.fromRawValue("bad"))
        assertEquals(AppThemePreference.System, AppThemePreference.fromRawValue(null))
    }

    @Test
    fun resolvesDarkTheme() {
        assertTrue(AppThemePreference.System.useDarkTheme(systemDark = true))
        assertFalse(AppThemePreference.System.useDarkTheme(systemDark = false))
        assertFalse(AppThemePreference.Light.useDarkTheme(systemDark = true))
        assertTrue(AppThemePreference.Dark.useDarkTheme(systemDark = false))
    }
}
