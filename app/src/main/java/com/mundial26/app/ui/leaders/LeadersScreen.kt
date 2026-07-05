package com.mundial26.app.ui.leaders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mundial26.app.domain.model.Leader
import com.mundial26.app.ui.components.BallTabIcon
import com.mundial26.app.ui.components.BootTabIcon
import com.mundial26.app.ui.components.CardTabIcon
import com.mundial26.app.ui.components.FlagAvatar
import com.mundial26.app.ui.theme.MundialTheme

/**
 * Pantalla dedicada de Líderes del torneo: ranking completo de goleadores y
 * asistidores con segmentado, derivado de los jugadores reales.
 */
@Composable
fun LeadersScreen(
    viewModel: LeadersViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tab by viewModel.tab.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MundialTheme.colors.appBg)
    ) {
        TopBar(onBack = onBack)

        when (val current = state) {
            is LeadersUiState.Loading -> LoadingContent()
            is LeadersUiState.Error -> ErrorContent(message = current.message, onRetry = viewModel::retry)
            is LeadersUiState.Success -> LeadersContent(
                state = current,
                tab = tab,
                onSelectTab = viewModel::selectTab
            )
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MundialTheme.colors.surface)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = MundialTheme.colors.textPrimary
            )
        }
        Text(
            text = "Líderes del torneo",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MundialTheme.colors.textPrimary
        )
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MundialTheme.colors.brand)
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MundialTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("Reintentar") }
        }
    }
}

@Composable
private fun LeadersContent(
    state: LeadersUiState.Success,
    tab: LeadersTab,
    onSelectTab: (LeadersTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val leaders = when (tab) {
        LeadersTab.SCORERS -> state.scorers
        LeadersTab.ASSISTS -> state.assists
        LeadersTab.YELLOW -> state.yellowCards
        LeadersTab.RED -> state.redCards
    }
    val accent = when (tab) {
        LeadersTab.SCORERS -> MundialTheme.colors.brand
        LeadersTab.ASSISTS -> MundialTheme.colors.win
        LeadersTab.YELLOW -> MundialTheme.colors.yellow
        LeadersTab.RED -> MundialTheme.colors.live
    }
    val unit = when (tab) {
        LeadersTab.SCORERS -> "goles"
        LeadersTab.ASSISTS -> "asist."
        LeadersTab.YELLOW -> "amar."
        LeadersTab.RED -> "rojas"
    }
    val maxValue = (leaders.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Toggle(tab = tab, onSelectTab = onSelectTab) }
        if (leaders.isEmpty()) {
            item {
                Text(
                    text = "Sin datos disponibles todavía.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MundialTheme.colors.textTertiary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        items(leaders, key = { "${it.rank}-${it.name}" }) { leader ->
            LeaderRow(leader = leader, unit = unit, accent = accent, fraction = leader.value.toFloat() / maxValue)
        }
    }
}

@Composable
private fun Toggle(tab: LeadersTab, onSelectTab: (LeadersTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MundialTheme.colors.surface)
            .padding(4.dp)
    ) {
        // Goles ⚽, asistencias (botín) y tarjetas amarilla/roja, todo con icono.
        LeadersTab.entries.forEach { value ->
            val active = value == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) MundialTheme.colors.brandDeep else Color.Transparent)
                    .clickable { onSelectTab(value) },
                contentAlignment = Alignment.Center
            ) {
                when (value) {
                    LeadersTab.SCORERS -> BallTabIcon(active = active)
                    LeadersTab.ASSISTS -> BootTabIcon(
                        tint = if (active) Color.White else MundialTheme.colors.textTertiary
                    )
                    LeadersTab.YELLOW -> CardTabIcon(
                        color = MundialTheme.colors.yellow.copy(alpha = if (active) 1f else 0.55f)
                    )
                    LeadersTab.RED -> CardTabIcon(
                        color = MundialTheme.colors.live.copy(alpha = if (active) 1f else 0.55f)
                    )
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
        modifier = modifier
            .fillMaxWidth()
            .clip(MundialTheme.shapes.medium)
            .background(MundialTheme.colors.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${leader.rank}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MundialTheme.colors.textFaint,
            modifier = Modifier.width(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        FlagAvatar(iso = leader.flag, code = "", size = 34.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = leader.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MundialTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MundialTheme.colors.border)
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
                color = MundialTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MundialTheme.colors.textTertiary,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
    }
}
