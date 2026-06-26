package com.mundial26.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mundial26.app.ui.theme.MundialTheme

/**
 * Punto rojo pulsante para indicar estado "en vivo".
 * Única animación de loop que se mantiene en la app.
 */
@Composable
fun LivePulseDot(size: Dp = 8.dp) {
    val transition = rememberInfiniteTransition(label = "live-pulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "live-pulse-scale"
    )
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "live-pulse-alpha"
    )

    Box(
        modifier = androidx.compose.ui.Modifier.size(size * 2),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        // halo pulsante
        Box(
            modifier = androidx.compose.ui.Modifier
                .size(size)
                .scale(scale)
                .alpha(alpha)
                .background(MundialTheme.colors.live, CircleShape)
        )
        // punto fijo
        Box(
            modifier = androidx.compose.ui.Modifier
                .size(size)
                .background(MundialTheme.colors.live, CircleShape)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LivePulseDotPreview() {
    MundialTheme {
        LivePulseDot()
    }
}
