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
import androidx.compose.foundation.layout.Box
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

/** Píldora roja con el minuto en juego; el minuto nuevo entra rodando desde abajo. */
@Composable
fun LiveMinutePill(minute: Int?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MundialTheme.colors.live)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = minute ?: 0,
            transitionSpec = {
                (slideInVertically(animationSpec = tween(200, easing = FastOutSlowInEasing)) { it / 2 } +
                    fadeIn(tween(200))) togetherWith
                    (slideOutVertically(animationSpec = tween(150)) { -it / 2 } + fadeOut(tween(150)))
            },
            label = "minute"
        ) { min ->
            Text(
                text = "$min'",
                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 12.sp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
