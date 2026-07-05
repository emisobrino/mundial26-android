package com.mundial26.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mundial26.app.ui.theme.MundialTheme

/**
 * Fila de tiempo en vivo: globo rojo con el minuto ("63'", "ET") y, si hay
 * descuento, un globo azul con el añadido ("+2") al lado.
 */
@Composable
fun LiveMinuteRow(label: String, added: String? = null, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiveMinutePill(label = label)
        if (!added.isNullOrBlank()) {
            LiveMinutePill(label = added, containerColor = MundialTheme.colors.brandDeep)
        }
    }
}

/**
 * Píldora del tiempo en juego. Roja por defecto (minuto); azul para el descuento.
 * El valor nuevo entra rodando desde abajo.
 */
@Composable
fun LiveMinutePill(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MundialTheme.colors.live
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = label,
            transitionSpec = {
                (slideInVertically(animationSpec = tween(200, easing = FastOutSlowInEasing)) { it / 2 } +
                    fadeIn(tween(200))) togetherWith
                    (slideOutVertically(animationSpec = tween(150)) { -it / 2 } + fadeOut(tween(150)))
            },
            label = "minute"
        ) { txt ->
            Text(
                text = txt,
                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 12.sp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Etiqueta a mostrar en la píldora: la de FIFA si existe, si no el minuto derivado. */
fun liveMinuteText(minuteLabel: String?, minute: Int?): String =
    minuteLabel?.takeIf { it.isNotBlank() } ?: "${minute ?: 0}'"
