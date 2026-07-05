package com.mundial26.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mundial26.app.R
import com.mundial26.app.data.mock.MockData
import com.mundial26.app.domain.model.Leader
import com.mundial26.app.ui.theme.MundialTheme

/**
 * Widget "Líderes del torneo" de la pantalla Inicio: tarjeta oscura con franja
 * tricolor, pestañas Goles / Asist. / 🟨 / 🟥 y top-5 con bandera, nombre,
 * barra de progreso proporcional y valor.
 */
@Composable
fun TournamentLeadersCard(
    scorers: List<Leader>,
    assists: List<Leader>,
    yellowCards: List<Leader> = emptyList(),
    redCards: List<Leader> = emptyList(),
    modifier: Modifier = Modifier
) {
    var tab by remember { mutableIntStateOf(0) }
    val leaders = when (tab) {
        0 -> scorers
        1 -> assists
        2 -> yellowCards
        else -> redCards
    }
    val accent = when (tab) {
        0 -> MundialTheme.colors.brand
        1 -> MundialTheme.colors.win
        2 -> MundialTheme.colors.yellow
        else -> MundialTheme.colors.live
    }
    val unit = when (tab) {
        0 -> "goles"
        1 -> "asist."
        2 -> "amar."
        else -> "rojas"
    }
    val maxValue = (leaders.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MundialTheme.shapes.large)
            .background(MundialTheme.colors.surfaceDark)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(4.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MundialTheme.colors.win))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MundialTheme.colors.live))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MundialTheme.colors.brandDeep))
        }
        Column(modifier = Modifier.padding(16.dp)) {
            LeadersToggle(selected = tab, onSelect = { tab = it })
            Spacer(modifier = Modifier.height(16.dp))
            if (leaders.isEmpty()) {
                Text(
                    text = "Sin datos todavía.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            leaders.take(5).forEachIndexed { index, leader ->
                LeaderRow(
                    leader = leader,
                    unit = unit,
                    accent = accent,
                    fraction = leader.value.toFloat() / maxValue
                )
                if (index < leaders.take(5).lastIndex) Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

/** Icono de tarjeta (amarilla/roja) para las pestañas del toggle. */
@Composable
fun CardTabIcon(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 11.dp, height = 15.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}

/** Pelota oficial del Mundial (PNG a color) para la pestaña de goles; se atenúa si no está activa. */
@Composable
fun BallTabIcon(active: Boolean, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_ball_wc),
        contentDescription = "Goles",
        modifier = modifier
            .size(24.dp)
            .alpha(if (active) 1f else 0.5f)
    )
}

/** Botín de fútbol (silueta PNG) para la pestaña de asistencias; se tiñe con el color dado. */
@Composable
fun BootTabIcon(tint: Color, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_boot),
        contentDescription = "Asistencias",
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(28.dp)
    )
}

@Composable
private fun LeadersToggle(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(4.dp)
    ) {
        // Goles ⚽, asistencias (botín) y tarjetas amarilla/roja, todo con icono.
        repeat(4) { index ->
            val active = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) MundialTheme.colors.brandDeep else Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                when (index) {
                    0 -> BallTabIcon(active = active)
                    1 -> BootTabIcon(tint = Color.White.copy(alpha = if (active) 1f else 0.5f))
                    2 -> CardTabIcon(color = MundialTheme.colors.yellow.copy(alpha = if (active) 1f else 0.55f))
                    else -> CardTabIcon(color = MundialTheme.colors.live.copy(alpha = if (active) 1f else 0.55f))
                }
            }
        }
    }
}

@Composable
private fun LeaderRow(
    leader: Leader,
    unit: String,
    accent: Color,
    fraction: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${leader.rank}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.width(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        FlagAvatar(iso = leader.flag, code = "", size = 32.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = leader.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${leader.value}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TournamentLeadersCardPreview() {
    MundialTheme {
        TournamentLeadersCard(
            scorers = MockData.topScorers,
            assists = MockData.topAssists,
            modifier = Modifier.padding(16.dp)
        )
    }
}
