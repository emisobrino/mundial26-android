package com.mundial26.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mundial26.app.data.mock.MockData
import com.mundial26.app.domain.model.Match
import com.mundial26.app.ui.theme.MundialTheme
import com.mundial26.app.ui.theme.ScoreboardCard

/**
 * Tarjeta de partido "en vivo" para el carrusel horizontal de Inicio.
 * Estilo oscuro con franja tricolor, a juego con el widget de Líderes del torneo.
 */
@Composable
fun LiveMatchCard(
    match: Match,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(MundialTheme.shapes.large)
            .background(MundialTheme.colors.surfaceDark)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().height(4.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MundialTheme.colors.win))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MundialTheme.colors.live))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MundialTheme.colors.brandDeep))
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.group.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.5f)
                )
                LiveMinuteRow(label = liveMinuteText(match.minuteLabel, match.minute), added = match.addedLabel)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tres tercios iguales → el marcador queda en el centro geométrico.
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FlagAvatar(iso = match.flagA, code = match.teamA, size = 44.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = match.teamA,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    // ¡Gol!: primero el confeti, ~0,9 s después rueda el marcador nuevo.
                    val shown = rememberCelebratedScore(match.scoreA, match.scoreB, live = true)
                    AnimatedContent(
                        targetState = "${shown.first} - ${shown.second}",
                        transitionSpec = {
                            (slideInVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) { it / 2 } +
                                fadeIn(tween(250))) togetherWith
                                (slideOutVertically(animationSpec = tween(180)) { -it / 2 } + fadeOut(tween(180)))
                        },
                        label = "score"
                    ) { score ->
                        Text(
                            text = score,
                            style = ScoreboardCard,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FlagAvatar(iso = match.flagB, code = match.teamB, size = 44.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = match.teamB,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            if (match.stadium.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = match.stadium,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
    // Festejo de gol por encima de toda la tarjeta.
    GoalConfetti(scoreA = match.scoreA, scoreB = match.scoreB, modifier = Modifier.matchParentSize())
    }
}

@Preview(showBackground = true)
@Composable
private fun LiveMatchCardPreview() {
    MundialTheme {
        LiveMatchCard(
            match = MockData.matches[0],
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}
