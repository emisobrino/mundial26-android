package com.mundial26.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val MundialColorScheme = darkColorScheme(
    primary = BrandDeep,          // botones/controles M3: azul identidad con texto blanco
    onPrimary = Color.White,
    secondary = Win,
    onSecondary = Color.White,
    tertiary = Yellow,
    onTertiary = Color(0xFF1C1E1E),
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
    inverseSurface = TextPrimary,
    inverseOnSurface = AppBg
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
