package com.mundial26.app.ui.matches

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mundial26.app.domain.model.BracketMatch
import com.mundial26.app.domain.model.BracketRound
import com.mundial26.app.domain.model.Match
import com.mundial26.app.domain.model.Nations
import com.mundial26.app.ui.components.FlagAvatar
import com.mundial26.app.ui.components.MatchRow
import com.mundial26.app.ui.components.SegmentedControl
import com.mundial26.app.ui.theme.MundialTheme

/**
 * Pantalla Partidos: segmentado Partidos / Calendario / Llaves.
 */
@Composable
fun MatchesScreen(
    viewModel: MatchesViewModel,
    onMatchClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedView by viewModel.selectedView.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MundialTheme.colors.appBg)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Partidos",
            style = MaterialTheme.typography.headlineSmall,
            color = MundialTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        SegmentedControl(
            options = listOf("Partidos", "Calendario", "Llaves"),
            selected = selectedView.ordinal,
            onSelect = { viewModel.selectView(MatchesView.entries[it]) }
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (val current = state) {
            is MatchesUiState.Loading -> LoadingContent()
            is MatchesUiState.Error -> ErrorContent(
                message = current.message,
                onRetry = viewModel::retry
            )
            is MatchesUiState.Success -> MatchesContent(
                state = current,
                selectedView = selectedView,
                onMatchClick = onMatchClick
            )
        }
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
private fun MatchesContent(
    state: MatchesUiState.Success,
    selectedView: MatchesView,
    onMatchClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (selectedView) {
        MatchesView.LIST -> MatchesListContent(state = state, onMatchClick = onMatchClick, modifier = modifier)
        MatchesView.CALENDAR -> CalendarContent(state = state, onMatchClick = onMatchClick, modifier = modifier)
        MatchesView.BRACKET -> BracketContent(state = state, modifier = modifier)
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MundialTheme.colors.textTertiary,
        modifier = modifier
    )
}

@Composable
private fun MatchesListContent(
    state: MatchesUiState.Success,
    onMatchClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (state.live.isNotEmpty()) {
            item { SectionLabel(text = "En vivo") }
            items(state.live, key = { "live-${it.id}" }) { match ->
                MatchRow(match = match, useFullNames = true, liveBorder = true, onClick = { onMatchClick(match.id) })
            }
        }
        if (state.upcoming.isNotEmpty()) {
            item { SectionLabel(text = "Próximos", modifier = Modifier.padding(top = 6.dp)) }
            items(state.upcoming, key = { "upcoming-${it.id}" }) { match ->
                MatchRow(match = match, useFullNames = true, onClick = { onMatchClick(match.id) })
            }
        }
        if (state.finished.isNotEmpty()) {
            item { SectionLabel(text = "Finalizados", modifier = Modifier.padding(top = 6.dp)) }
            items(state.finished, key = { "finished-${it.id}" }) { match ->
                MatchRow(match = match, useFullNames = true, onClick = { onMatchClick(match.id) })
            }
        }
    }
}

@Composable
private fun CalendarContent(
    state: MatchesUiState.Success,
    onMatchClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        state.byDay.forEach { (day, matches) ->
            item(key = "day-$day") { SectionLabel(text = day, modifier = Modifier.padding(top = 6.dp)) }
            items(matches, key = { "day-$day-${it.id}" }) { match ->
                MatchRow(match = match, useFullNames = true, onClick = { onMatchClick(match.id) })
            }
        }
    }
}

@Composable
private fun BracketContent(
    state: MatchesUiState.Success,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        state.bracket.forEach { round ->
            item(key = "round-${round.round}") {
                SectionLabel(text = round.round, modifier = Modifier.padding(top = 6.dp))
            }
            items(round.games, key = { "round-${round.round}-${it.a}-${it.b}" }) { game ->
                BracketCard(game = game)
            }
        }
    }
}

@Composable
private fun BracketCard(game: BracketMatch, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MundialTheme.shapes.medium)
            .background(MundialTheme.colors.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val aWins = game.done && game.sa != null && game.sb != null && game.sa > game.sb
        val bWins = game.done && game.sa != null && game.sb != null && game.sb > game.sa
        BracketTeamRow(code = game.a, iso = game.flagA, score = game.sa, done = game.done, isWinner = aWins)
        BracketTeamRow(code = game.b, iso = game.flagB, score = game.sb, done = game.done, isWinner = bWins)
    }
}

@Composable
private fun BracketTeamRow(
    code: String,
    iso: String,
    score: Int?,
    done: Boolean,
    isWinner: Boolean
) {
    val textColor = if (!done) {
        MundialTheme.colors.textFaint
    } else if (isWinner) {
        MundialTheme.colors.textPrimary
    } else {
        MundialTheme.colors.textSecondary
    }
    val weight = if (done && isWinner) FontWeight.Bold else FontWeight.Normal

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlagAvatar(iso = iso, code = code, size = 24.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = Nations.name(code),
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontWeight = weight,
            modifier = Modifier.weight(1f)
        )
        if (done && score != null) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                fontWeight = weight
            )
        }
    }
}
