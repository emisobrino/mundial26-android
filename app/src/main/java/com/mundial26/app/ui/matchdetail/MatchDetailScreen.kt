package com.mundial26.app.ui.matchdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mundial26.app.data.mock.FakeMatchRepository
import com.mundial26.app.data.mock.MockData
import com.mundial26.app.domain.model.EventKind
import com.mundial26.app.domain.model.Match
import com.mundial26.app.domain.model.MatchEvent
import com.mundial26.app.domain.model.MatchStat
import com.mundial26.app.domain.model.MatchStatus
import com.mundial26.app.ui.components.FlagAvatar
import com.mundial26.app.ui.components.LivePulseDot
import com.mundial26.app.ui.theme.MundialTheme
import com.mundial26.app.ui.theme.ScoreboardBig

/**
 * Pantalla Detalle de partido: hero con marcador, estadísticas y eventos.
 */
@Composable
fun MatchDetailScreen(
    viewModel: MatchDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MundialTheme.colors.appBg)
    ) {
        TopBar(title = (state as? MatchDetailUiState.Success)?.match?.group, onBack = onBack)

        when (val current = state) {
            is MatchDetailUiState.Loading -> LoadingContent()
            is MatchDetailUiState.Error -> ErrorContent(
                message = current.message,
                onRetry = viewModel::retry
            )
            is MatchDetailUiState.Success -> MatchDetailContent(match = current.match)
        }
    }
}

@Composable
private fun TopBar(title: String?, onBack: () -> Unit, modifier: Modifier = Modifier) {
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
            text = title ?: "",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MundialTheme.colors.textPrimary
        )
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MundialTheme.colors.brand)
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MundialTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text("Reintentar")
            }
        }
    }
}

@Composable
private fun MatchDetailContent(match: Match, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { MatchHero(match = match) }
        if (match.stats.isNotEmpty()) {
            item { StatsSection(stats = match.stats) }
        }
        if (match.events.isNotEmpty()) {
            item { EventsSection(match = match) }
        }
    }
}

@Composable
private fun MatchHero(match: Match, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MundialTheme.shapes.large)
            .background(MundialTheme.colors.surfaceDark)
    ) {
        TricolorStripe()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamColumn(iso = match.flagA, code = match.teamA)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(min = 100.dp)
            ) {
                Text(
                    text = "${match.scoreA ?: 0} - ${match.scoreB ?: 0}",
                    style = ScoreboardBig,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                if (match.penA != null && match.penB != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "(${match.penA} - ${match.penB} penales)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
                if (match.status == MatchStatus.LIVE && match.minute != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MundialTheme.colors.live)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
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
            }

            TeamColumn(iso = match.flagB, code = match.teamB)
        }

        Text(
            text = match.stadium,
            style = MaterialTheme.typography.bodyMedium,
            color = MundialTheme.colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        )
    }
}

@Composable
private fun TricolorStripe(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxSize().background(MundialTheme.colors.win))
        Box(modifier = Modifier.weight(1f).fillMaxSize().background(MundialTheme.colors.live))
        Box(modifier = Modifier.weight(1f).fillMaxSize().background(MundialTheme.colors.brand))
    }
}

@Composable
private fun TeamColumn(iso: String, code: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.widthIn(min = 72.dp)
    ) {
        FlagAvatar(iso = iso, code = code, size = 64.dp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = code,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}

@Composable
private fun StatsSection(stats: List<MatchStat>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "ESTADÍSTICAS",
            style = MaterialTheme.typography.labelLarge,
            color = MundialTheme.colors.textTertiary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MundialTheme.shapes.medium)
                .background(MundialTheme.colors.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            stats.forEachIndexed { index, stat ->
                StatRow(stat = stat)
                if (index != stats.lastIndex) {
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Estimación editorial · no oficial",
            style = MaterialTheme.typography.labelSmall,
            color = MundialTheme.colors.textFaint
        )
    }
}

@Composable
private fun StatRow(stat: MatchStat, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stat.a}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MundialTheme.colors.brand
            )
            Text(
                text = stat.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MundialTheme.colors.textSecondary
            )
            Text(
                text = "${stat.b}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MundialTheme.colors.textPrimary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        val total = (stat.a + stat.b).coerceAtLeast(1)
        val weightA = stat.a.toFloat() / total
        val weightB = stat.b.toFloat() / total
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .weight(weightA.coerceAtLeast(0.001f))
                    .fillMaxSize()
                    .background(MundialTheme.colors.brand)
            )
            Box(
                modifier = Modifier
                    .weight(weightB.coerceAtLeast(0.001f))
                    .fillMaxSize()
                    .background(MundialTheme.colors.border)
            )
        }
    }
}

@Composable
private fun EventsSection(match: Match, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "EVENTOS",
            style = MaterialTheme.typography.labelLarge,
            color = MundialTheme.colors.textTertiary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MundialTheme.shapes.medium)
                .background(MundialTheme.colors.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            match.events.forEach { event ->
                EventRow(event = event)
            }
        }
    }
}

@Composable
private fun EventRow(event: MatchEvent, modifier: Modifier = Modifier) {
    val isSideA = event.side == 'A'
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isSideA) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        EventContent(event = event, isSideA = isSideA)
    }
}

@Composable
private fun EventContent(event: MatchEvent, isSideA: Boolean, modifier: Modifier = Modifier) {
    val minuteText = "${event.min}'"
    val dotColor = when (event.kind) {
        EventKind.GOAL -> MundialTheme.colors.win
        EventKind.YELLOW -> MundialTheme.colors.yellow
        EventKind.RED -> MundialTheme.colors.live
        EventKind.SUB -> MundialTheme.colors.textTertiary
    }

    val minuteLabel: @Composable () -> Unit = {
        Text(
            text = minuteText,
            style = MaterialTheme.typography.bodyMedium,
            color = MundialTheme.colors.textTertiary
        )
    }
    val dot: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
    }
    val playerLabel: @Composable () -> Unit = {
        Text(
            text = event.player,
            style = MaterialTheme.typography.bodyLarge,
            color = MundialTheme.colors.textPrimary,
            textAlign = if (isSideA) TextAlign.Start else TextAlign.End
        )
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (isSideA) {
            minuteLabel()
            Spacer(modifier = Modifier.width(8.dp))
            dot()
            Spacer(modifier = Modifier.width(8.dp))
            playerLabel()
        } else {
            playerLabel()
            Spacer(modifier = Modifier.width(8.dp))
            dot()
            Spacer(modifier = Modifier.width(8.dp))
            minuteLabel()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MatchDetailScreenPreview() {
    MundialTheme {
        val viewModel: MatchDetailViewModel = viewModel(
            factory = MatchDetailViewModel.Factory(
                FakeMatchRepository(),
                MockData.matches[0].id
            )
        )
        MatchDetailScreen(viewModel = viewModel, onBack = {})
    }
}
