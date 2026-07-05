package com.mundial26.app.ui.matches

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mundial26.app.ui.components.MatchRow
import com.mundial26.app.ui.components.SegmentedControl
import com.mundial26.app.ui.theme.MundialTheme

/**
 * Pantalla Partidos: segmentado Partidos / Calendario.
 */
@Composable
fun MatchesScreen(
    viewModel: MatchesViewModel,
    onMatchClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedView by viewModel.selectedView.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

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
            options = listOf("Partidos", "Calendario"),
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
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchesContent(
    state: MatchesUiState.Success,
    selectedView: MatchesView,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onMatchClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        when (selectedView) {
            MatchesView.LIST -> MatchesListContent(state = state, onMatchClick = onMatchClick)
            MatchesView.CALENDAR -> CalendarContent(state = state, onMatchClick = onMatchClick)
        }
    }
}

private val DIAS = arrayOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
private val MESES = arrayOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")

/** "2026-07-02" → "Jueves 2 de julio"; si no parsea, devuelve el texto original. */
private fun prettyDay(day: String): String = runCatching {
    val d = java.time.LocalDate.parse(day)
    "${DIAS[d.dayOfWeek.value - 1]} ${d.dayOfMonth} de ${MESES[d.monthValue - 1]}"
}.getOrDefault(day)

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
                MatchRow(
                    match = match, useFullNames = true, liveBorder = true,
                    onClick = { onMatchClick(match.id) },
                    modifier = Modifier.animateItem()
                )
            }
        }
        if (state.upcoming.isNotEmpty()) {
            item { SectionLabel(text = "Próximos", modifier = Modifier.padding(top = 6.dp)) }
            items(state.upcoming, key = { "upcoming-${it.id}" }) { match ->
                MatchRow(
                    match = match, useFullNames = true,
                    onClick = { onMatchClick(match.id) },
                    modifier = Modifier.animateItem()
                )
            }
        }
        if (state.finished.isNotEmpty()) {
            item { SectionLabel(text = "Finalizados", modifier = Modifier.padding(top = 6.dp)) }
            items(state.finished, key = { "finished-${it.id}" }) { match ->
                MatchRow(
                    match = match, useFullNames = true,
                    onClick = { onMatchClick(match.id) },
                    modifier = Modifier.animateItem()
                )
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
            item(key = "day-$day") { SectionLabel(text = prettyDay(day), modifier = Modifier.padding(top = 6.dp)) }
            items(matches, key = { "day-$day-${it.id}" }) { match ->
                MatchRow(match = match, useFullNames = true, onClick = { onMatchClick(match.id) })
            }
        }
    }
}
