package com.mundial26.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Brand = Color(0xFF2A398D)
val Live = Color(0xFFE61D25)
val Win = Color(0xFF3CAC3B)
val Yellow = Color(0xFFE8B500)
val TextPrimary = Color(0xFF1C1E1E)
val TextSecondary = Color(0xFF474A4A)
val TextTertiary = Color(0xFF6B6F6F)
val TextFaint = Color(0xFF9A9E9E)
val AppBg = Color(0xFFF2F3F3)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF2A2C2C)
val BorderLight = Color(0xFFEAEBEB)
val SegmentInactive = Color(0xFFEEF0F4)
val GreenAccentLight = Color(0xFF9CE8A0)
val TableHeaderBg = Color(0xFFF7F8F8)
val NavInactive = Color(0xFFA2A6A6)

data class MundialColors(
    val brand: Color = Brand,
    val live: Color = Live,
    val win: Color = Win,
    val yellow: Color = Yellow,
    val textPrimary: Color = TextPrimary,
    val textSecondary: Color = TextSecondary,
    val textTertiary: Color = TextTertiary,
    val textFaint: Color = TextFaint,
    val appBg: Color = AppBg,
    val surface: Color = SurfaceLight,
    val surfaceDark: Color = SurfaceDark,
    val border: Color = BorderLight,
    val segmentInactive: Color = SegmentInactive,
    val greenAccentLight: Color = GreenAccentLight,
    val tableHeaderBg: Color = TableHeaderBg,
    val navInactive: Color = NavInactive
)

val LocalMundialColors = staticCompositionLocalOf { MundialColors() }
