package com.mundial26.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.mundial26.app.ui.theme.MundialTheme
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Papelito de confeti: posición, arranque escalonado, vaivén, giro y color. */
private class ConfettiPiece(
    val x: Float,          // 0..1 (fracción del ancho)
    val delay: Float,      // 0..0.3 (fracción de la animación)
    val swayAmp: Float,    // amplitud del vaivén en px
    val swayFreq: Float,   // ciclos de vaivén durante la caída
    val rotDegrees: Float, // giro total (±)
    val w: Float, val h: Float,
    val colorIndex: Int    // 0 = blanco; 1..3 = tricolor
)

/**
 * Festejo de gol: cuando el marcador SUBE, cae una lluvia breve (~2,4 s) de confeti
 * blanco con papelitos de los colores de la franja del Mundial. Dibujar por encima
 * del contenido (matchParentSize); no intercepta toques. Al abrir la pantalla con
 * el partido ya empezado no dispara (solo reacciona a cambios).
 */
@Composable
fun GoalConfetti(scoreA: Int?, scoreB: Int?, modifier: Modifier = Modifier) {
    val total = (scoreA ?: 0) + (scoreB ?: 0)
    var prev by remember { mutableIntStateOf(-1) }
    var burst by remember { mutableIntStateOf(0) }
    LaunchedEffect(total) {
        if (prev in 0 until total) burst++
        prev = total
    }
    if (burst == 0) return

    val colors = listOf(Color.White, MundialTheme.colors.win, MundialTheme.colors.live, MundialTheme.colors.brandDeep)
    val pieces = remember(burst) {
        val rnd = Random(burst)
        List(70) { i ->
            ConfettiPiece(
                x = rnd.nextFloat(),
                delay = rnd.nextFloat() * 0.3f,
                swayAmp = 8f + rnd.nextFloat() * 22f,
                swayFreq = 1f + rnd.nextFloat() * 2f,
                rotDegrees = (rnd.nextFloat() - 0.5f) * 720f,
                w = 6f + rnd.nextFloat() * 8f,
                h = 10f + rnd.nextFloat() * 12f,
                colorIndex = if (i % 2 == 0) 0 else 1 + rnd.nextInt(3)   // mitad blanco, mitad tricolor
            )
        }
    }
    val progress = remember(burst) { Animatable(0f) }
    LaunchedEffect(burst) { progress.animateTo(1f, tween(2400, easing = LinearEasing)) }

    val t = progress.value
    if (t >= 1f) return
    Canvas(modifier = modifier) {
        pieces.forEach { p ->
            val lt = ((t - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (lt <= 0f || lt >= 1f) return@forEach
            val y = lt * (size.height + 60f) - 30f
            val x = p.x * size.width + sin(lt * p.swayFreq * 2f * PI.toFloat()) * p.swayAmp
            val alpha = min(1f, (1f - lt) * 4f)
            rotate(degrees = lt * p.rotDegrees, pivot = Offset(x, y)) {
                drawRect(
                    color = colors[p.colorIndex],
                    topLeft = Offset(x - p.w / 2f, y - p.h / 2f),
                    size = Size(p.w, p.h),
                    alpha = alpha
                )
            }
        }
    }
}
