package com.mundial26.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color predominante de la bandera de cada selección (TLA FIFA), para teñir elementos
 * como la barra de probabilidad. Tonos elegidos con luminosidad suficiente para leerse
 * sobre el fondo oscuro. Si no está el equipo, se usa el color de reserva.
 */
private val teamFlagColors: Map<String, Color> = mapOf(
    "ARG" to Color(0xFF75AADB),  // celeste
    "FRA" to Color(0xFF2B5BD0),  // azul
    "BRA" to Color(0xFF1D9E4B),  // verde
    "ESP" to Color(0xFFE03A46),  // rojo
    "ENG" to Color(0xFFE0424F),  // rojo (cruz de San Jorge)
    "MEX" to Color(0xFF178C4E),  // verde
    "POR" to Color(0xFFE0362C),  // rojo
    "GER" to Color(0xFFE23B3B),  // rojo
    "NED" to Color(0xFFF3823B),  // naranja
    "CRO" to Color(0xFFE0344A),  // rojo (damero)
    "URU" to Color(0xFF4C8FD6),  // azul celeste
    "JPN" to Color(0xFFE04256),  // rojo (sol)
    "USA" to Color(0xFF3C78D8),  // azul
    "CAN" to Color(0xFFE0424F),  // rojo (hoja de arce)
    "MAR" to Color(0xFF2FA35B),  // verde (estrella) / rojo — verde para contraste
    "SEN" to Color(0xFF2FA35B),  // verde
    "BEL" to Color(0xFFF3C640),  // amarillo
    "COL" to Color(0xFFF3C640),  // amarillo (mitad superior)
    "NOR" to Color(0xFFE0424F),  // rojo
    "EGY" to Color(0xFFE0424F),  // rojo
    "KOR" to Color(0xFFE0424F),  // rojo (taeguk)
    "IRN" to Color(0xFF2FA35B),  // verde
    "SUI" to Color(0xFFE0344A),  // rojo (cruz blanca)
    "ECU" to Color(0xFFF3C640),  // amarillo (mitad superior)
    "AUT" to Color(0xFFE0424F),  // rojo
    "SWE" to Color(0xFF3C86C4),  // azul
    "TUR" to Color(0xFFE0424F),  // rojo
    "CZE" to Color(0xFFE0424F),  // rojo
    "SCO" to Color(0xFF3C78D8),  // azul
    "AUS" to Color(0xFF3C78D8),  // azul
    "QAT" to Color(0xFF9E2A50),  // granate
    "ALG" to Color(0xFF1D9E4B),  // verde
    "CIV" to Color(0xFFF3823B),  // naranja
    "TUN" to Color(0xFFE0424F),  // rojo
    "PAN" to Color(0xFF3C78D8),  // azul
    "PAR" to Color(0xFFE0424F),  // rojo
    "KSA" to Color(0xFF1D9E4B),  // verde
    "COD" to Color(0xFF4CB5E0),  // celeste
    "UZB" to Color(0xFF3CA6C4),  // azul claro
    "IRQ" to Color(0xFFE0424F),  // rojo
    "RSA" to Color(0xFF1D9E4B),  // verde
    "JOR" to Color(0xFF2FA35B),  // verde
    "CPV" to Color(0xFF3C78D8),  // azul
    "GHA" to Color(0xFF1D9E4B),  // verde
    "BIH" to Color(0xFF3C78D8),  // azul
    "CUW" to Color(0xFF3C78D8),  // azul
    "HAI" to Color(0xFF3C78D8),  // azul
    "NZL" to Color(0xFF3C78D8),  // azul
)

/** Color de bandera de una selección; si no está mapeada, devuelve [fallback]. */
fun teamFlagColor(tla: String, fallback: Color): Color =
    teamFlagColors[tla.uppercase()] ?: fallback
