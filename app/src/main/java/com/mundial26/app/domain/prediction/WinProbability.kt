package com.mundial26.app.domain.prediction

import com.mundial26.app.domain.model.WinProbability
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Estima la probabilidad de resultado en los 90' a partir del ranking FIFA de cada
 * equipo, con un modelo tipo Elo (cada puesto del ranking ~6 puntos). El "empate" en
 * eliminatorias equivale a ir a tiempo extra. Devuelve local/empate/visitante en % (suma 100).
 */
fun winProbabilityFromRanks(rankHome: Int, rankAway: Int): WinProbability {
    val elo = { rank: Int -> 2000.0 - rank * 6.0 }
    val dh = elo(rankHome) - elo(rankAway)
    val expHome = 1.0 / (1.0 + 10.0.pow(-dh / 400.0))       // expectativa de puntos del local (0..1)
    val draw = (0.30 - 0.28 * abs(expHome - 0.5)).coerceIn(0.14, 0.30)  // más parejo → más empate
    val rest = 1.0 - draw
    val home = (rest * expHome * 100).roundToInt()
    val drawPct = (draw * 100).roundToInt()
    return WinProbability(home = home, draw = drawPct, away = 100 - home - drawPct)
}
