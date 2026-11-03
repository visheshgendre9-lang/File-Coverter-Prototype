package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light blue glass look. Panels are translucent white, so the sky gradient behind them shows through.
private val GlassColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E8FF),
    onPrimaryContainer = Color(0xFF0B2A5B),
    secondary = DeepCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0F2FB),
    onSecondaryContainer = Color(0xFF053B4F),
    tertiary = AccentPurple,
    onTertiary = Color.White,
    background = GlassSkyMid,
    onBackground = InkNavy,
    surface = Color(0xB3FFFFFF),
    onSurface = InkNavy,
    surfaceVariant = Color(0x99FFFFFF),
    onSurfaceVariant = InkSoft,
    // Opaque light-blue tones, used by menus and dialogs
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7FBFF),
    surfaceContainer = Color(0xFFF0F7FF),
    surfaceContainerHigh = Color(0xFFE8F2FF),
    surfaceContainerHighest = Color(0xFFDCE9FA),
    outline = Color(0xFF8FB0D9),
    outlineVariant = Color(0xFFCFE0F5)
)

/**
 * The app always uses the light glass theme. [darkTheme] is kept so existing callers still compile,
 * but it no longer changes the look.
 */
@Composable
fun OmniConvertTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GlassColorScheme,
        typography = Typography,
        content = content
    )
}
