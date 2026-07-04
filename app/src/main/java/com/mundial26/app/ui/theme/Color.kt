package com.mundial26.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Rediseño v4: tema oscuro único ("cancha de noche") ──────────────────────
// Identidad que se preserva: el negro de las tarjetas destacadas (SurfaceDark)
// y la franja tricolor win/live/brandDeep. Ver DESIGN.md.

/** Azul CLARO para acentos de texto sobre oscuro (horas, PTS, links, nav activa). */
val Brand = Color(0xFF8B99F0)

/** Azul identidad PROFUNDO para rellenos con texto blanco y la franja tricolor. */
val BrandDeep = Color(0xFF2A398D)

val Live = Color(0xFFE61D25)
val Win = Color(0xFF3CAC3B)
val Yellow = Color(0xFFE8B500)

val TextPrimary = Color(0xFFF4F5F5)
val TextSecondary = Color(0xFFC6C9C9)
val TextTertiary = Color(0xFF969C9C)
val TextFaint = Color(0xFF6C7272)

/** Tres niveles de superficie: fondo → tarjeta → tarjeta destacada. */
val AppBg = Color(0xFF121414)
val SurfaceLight = Color(0xFF1D2020)      // tarjetas y filas ("surface")
val SurfaceDark = Color(0xFF2A2C2C)       // destacadas: hero, live, líderes (identidad)

val BorderLight = Color(0xFF303434)
val SegmentInactive = Color(0xFF232626)
val GreenAccentLight = Color(0xFF9CE8A0)
val TableHeaderBg = Color(0xFF242727)
val NavInactive = Color(0xFF6C7272)

data class MundialColors(
    val brand: Color = Brand,
    val brandDeep: Color = BrandDeep,
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
