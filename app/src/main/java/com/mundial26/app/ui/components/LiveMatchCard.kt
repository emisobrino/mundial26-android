package com.mundial26.app.ui.components

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
    Column(
        modifier = modifier
            .width(268.dp)
            .clip(MundialTheme.shapes.large)
            .background(MundialTheme.colors.surfaceDark)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(4.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MundialTheme.colors.win))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MundialTheme.colors.live))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MundialTheme.colors.brand))
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MundialTheme.colors.live)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    LivePulseDot(size = 6.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${match.minute}'",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.widthIn(min = 56.dp)
                ) {
                    FlagAvatar(iso = match.flagA, code = match.teamA, size = 40.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = match.teamA,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "${match.scoreA ?: 0} - ${match.scoreB ?: 0}",
                    style = ScoreboardCard,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.widthIn(min = 56.dp)
                ) {
                    FlagAvatar(iso = match.flagB, code = match.teamB, size = 40.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = match.teamB,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
}

@Preview(showBackground = true)
@Composable
private fun LiveMatchCardPreview() {
    MundialTheme {
        LiveMatchCard(
            match = MockData.matches[0],
            modifier = Modifier.padding(16.dp)
        )
    }
}
