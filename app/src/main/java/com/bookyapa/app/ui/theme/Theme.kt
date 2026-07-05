package com.bookyapa.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF008080),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF006666),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF2D2D2D),
    onSecondary = Color.White,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFAAAAAA),
    outline = Color(0xFF262626),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF008080),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF00332E),
    secondary = Color(0xFFE0E0E0),
    onSecondary = Color(0xFF1A1A1A),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFF0F0F0),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF444444),
    outline = Color(0xFFCCCCCC),
)

private val SepiaColorScheme = lightColorScheme(
    primary = Color(0xFF8B4513),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7B88A),
    onPrimaryContainer = Color(0xFF3E1F00),
    secondary = Color(0xFFDCC8A0),
    onSecondary = Color(0xFF3E2723),
    background = Color(0xFFF5E6CA),
    onBackground = Color(0xFF3E2723),
    surface = Color(0xFFE8D5B0),
    onSurface = Color(0xFF3E2723),
    surfaceVariant = Color(0xFFDCC8A0),
    onSurfaceVariant = Color(0xFF5D4037),
    outline = Color(0xFFC4A87A),
)

@Composable
fun BookyapaTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeMode) {
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.SEPIA -> SepiaColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
