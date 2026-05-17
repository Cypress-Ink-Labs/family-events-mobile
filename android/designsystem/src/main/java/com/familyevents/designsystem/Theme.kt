package com.familyevents.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.familyevents.designsystem.generated.Tokens

private val LightColors: ColorScheme = lightColorScheme(
    primary = Tokens.ColorLight.AccentPrimary,
    onPrimary = Tokens.ColorLight.Surface,
    secondary = Tokens.ColorLight.AccentSecondary,
    tertiary = Tokens.ColorLight.AccentTertiary,
    background = Tokens.ColorLight.Bg,
    surface = Tokens.ColorLight.Surface,
    surfaceVariant = Tokens.ColorLight.SurfaceRaised,
    onBackground = Tokens.ColorLight.TextPrimary,
    onSurface = Tokens.ColorLight.TextPrimary,
    outline = Tokens.ColorLight.Border,
    error = Tokens.ColorLight.Error,
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Tokens.ColorDark.AccentPrimary,
    onPrimary = Tokens.ColorDark.Bg,
    secondary = Tokens.ColorDark.AccentSecondary,
    tertiary = Tokens.ColorDark.AccentTertiary,
    background = Tokens.ColorDark.Bg,
    surface = Tokens.ColorDark.Surface,
    surfaceVariant = Tokens.ColorDark.SurfaceRaised,
    onBackground = Tokens.ColorDark.TextPrimary,
    onSurface = Tokens.ColorDark.TextPrimary,
    outline = Tokens.ColorDark.Border,
    error = Tokens.ColorDark.Error,
)

object FamilyTypography {
    val Display: FontFamily = FontFamily.Serif
    val BodyFamily: FontFamily = FontFamily.SansSerif
    val Editorial: FontFamily = FontFamily.Serif
    val Mono: FontFamily = FontFamily.Monospace

    val TitleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 32.sp)
    val TitleMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp)
    val Body = TextStyle(fontFamily = BodyFamily, fontSize = 16.sp, lineHeight = 25.sp)
    val BodySmall = TextStyle(fontFamily = BodyFamily, fontSize = 14.sp, lineHeight = 21.sp)
    val Caption = TextStyle(fontFamily = Mono, fontSize = 12.sp, lineHeight = 17.sp)
}

@Composable
fun FamilyEventsTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
