package com.mundial26.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mundial26.app.R

@OptIn(ExperimentalTextApi::class)
private fun archivo(weight: Int) = Font(
    R.font.archivo_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

val Archivo = FontFamily(
    archivo(500),
    archivo(600),
    archivo(700),
    archivo(800),
    archivo(900)
)

/**
 * Mapeo de tokens de tipografía del README/spec a slots de Material3 Typography
 * + estilos públicos extra para tamaños que no calzan en slots M3 (marcadores).
 *
 * - Título de pantalla: 24sp / 900 / -0.6sp -> headlineSmall / titleLarge
 * - Título de sección: 15sp / 900 / -0.2sp -> titleMedium
 * - Etiqueta de sección (mayúsculas en call site): 13sp / 900 / 0.3sp -> labelLarge
 * - Cuerpo: 14sp/500 -> bodyMedium, 15sp/600 -> bodyLarge
 * - Marcador grande (detalle): 46sp / 900 / -2sp -> displayMedium + ScoreboardBig
 * - Marcador tarjeta en vivo: 30sp / 900 -> ScoreboardCard
 * - Micro-labels: 11sp / 800 -> labelSmall
 */
val MundialTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight(900),
        fontSize = 28.sp,
        letterSpacing = (-0.6).sp
    ),
    titleLarge = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight(900),
        fontSize = 24.sp,
        letterSpacing = (-0.6).sp
    ),
    titleMedium = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight(900),
        fontSize = 15.sp,
        letterSpacing = (-0.2).sp
    ),
    labelLarge = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight(900),
        fontSize = 13.sp,
        letterSpacing = 0.3.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight(500),
        fontSize = 14.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight(600),
        fontSize = 15.sp
    ),
    displayMedium = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight(900),
        fontSize = 46.sp,
        letterSpacing = (-2).sp
    ),
    labelSmall = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight(800),
        fontSize = 11.sp
    )
)

/** Marcador grande de scoreboard en detalle de partido: 46sp / 900 / -2sp. */
val ScoreboardBig = TextStyle(
    fontFamily = Archivo,
    fontWeight = FontWeight(900),
    fontSize = 46.sp,
    letterSpacing = (-2).sp
)

/** Marcador de tarjeta "en vivo" en Inicio: 34sp / 900 (el marcador manda). */
val ScoreboardCard = TextStyle(
    fontFamily = Archivo,
    fontWeight = FontWeight(900),
    fontSize = 34.sp,
    letterSpacing = (-1).sp
)

/** Micro-label (badges, sellos pequeños): 9sp / 700. */
val MicroLabel = TextStyle(
    fontFamily = Archivo,
    fontWeight = FontWeight(700),
    fontSize = 9.sp
)
