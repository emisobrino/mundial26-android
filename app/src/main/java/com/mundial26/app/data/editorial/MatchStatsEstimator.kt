package com.mundial26.app.data.editorial

import com.mundial26.app.domain.model.Match
import com.mundial26.app.domain.model.MatchStat
import kotlin.random.Random

/**
 * Estimación EDITORIAL (no oficial) de las estadísticas de un partido.
 *
 * football-data.org (plan gratuito) no expone posesión/remates/córners/faltas por partido y no
 * existe una fuente gratuita para el WC2026. Para no dejar el detalle vacío, derivamos cifras
 * plausibles y **deterministas** (misma semilla → mismos números) correlacionadas con el marcador.
 * La UI las muestra claramente rotuladas como "estimación editorial · no oficial".
 */
fun estimatedStats(match: Match): List<MatchStat> {
    val a = match.scoreA ?: 0
    val b = match.scoreB ?: 0
    val rnd = Random(match.id.hashCode())

    // Posesión: 50/50 sesgada por la diferencia de goles + algo de ruido, acotada a un rango realista.
    val bias = ((a - b) * 4).coerceIn(-16, 16)
    val posA = (50 + bias + rnd.nextInt(-6, 7)).coerceIn(32, 68)
    val posB = 100 - posA

    // Remates: base + goles*2 + empuje por posesión + ruido. Nunca menos que los goles.
    val shotsA = (8 + a * 2 + rnd.nextInt(0, 6) + (posA - 50) / 5).coerceIn(4, 26)
    val shotsB = (8 + b * 2 + rnd.nextInt(0, 6) + (posB - 50) / 5).coerceIn(4, 26)
    val sotA = (a + rnd.nextInt(1, 4)).coerceIn(a, shotsA)
    val sotB = (b + rnd.nextInt(1, 4)).coerceIn(b, shotsB)

    val cornersA = (3 + rnd.nextInt(0, 6)).coerceIn(0, 12)
    val cornersB = (3 + rnd.nextInt(0, 6)).coerceIn(0, 12)
    val foulsA = 8 + rnd.nextInt(0, 8)
    val foulsB = 8 + rnd.nextInt(0, 8)

    return listOf(
        MatchStat("Posesión %", posA, posB),
        MatchStat("Remates", shotsA, shotsB),
        MatchStat("Remates al arco", sotA, sotB),
        MatchStat("Córners", cornersA, cornersB),
        MatchStat("Faltas", foulsA, foulsB),
    )
}
