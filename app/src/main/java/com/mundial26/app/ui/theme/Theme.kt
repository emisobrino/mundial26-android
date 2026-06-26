package com.mundial26.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val MundialColorScheme = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    secondary = Win,
    onSecondary = Color.White,
    tertiary = Yellow,
    onTertiary = TextPrimary,
    error = Live,
    onError = Color.White,
    background = AppBg,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = TableHeaderBg,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
    outlineVariant = BorderLight,
    inverseSurface = SurfaceDark,
    inverseOnSurface = Color.White
)

object MundialTheme {
    val colors: MundialColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMundialColors.current

    val typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    val shapes
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes
}

@Composable
fun MundialTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMundialColors provides MundialColors()) {
        MaterialTheme(
            colorScheme = MundialColorScheme,
            typography = MundialTypography,
            shapes = MundialShapes,
            content = content
        )
    }
}
