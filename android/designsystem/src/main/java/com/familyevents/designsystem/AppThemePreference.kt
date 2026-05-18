package com.familyevents.designsystem

enum class AppThemePreference(val rawValue: String, val label: String) {
    System("system", "System"),
    Light("light", "Light"),
    Dark("dark", "Dark");

    fun useDarkTheme(systemDark: Boolean): Boolean = when (this) {
        System -> systemDark
        Light -> false
        Dark -> true
    }

    companion object {
        fun fromRawValue(rawValue: String?): AppThemePreference =
            entries.firstOrNull { it.rawValue == rawValue } ?: System
    }
}
