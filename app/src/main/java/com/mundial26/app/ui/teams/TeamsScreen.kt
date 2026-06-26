package com.mundial26.app.ui.teams

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyColumnItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.mundial26.app.domain.model.Team
import com.mundial26.app.ui.components.FlagAvatar
import com.mundial26.app.ui.components.SegmentedControl
import com.mundial26.app.ui.components.StandingsTable
import com.mundial26.app.ui.theme.MundialTheme

/**
 * Pantalla Equipos: segmentado Selecciones / Grupos.
 */
@Composable
fun TeamsScreen(
    viewModel: TeamsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val view by viewModel.view.collectAsStateWithLifecycle()
    val conf by viewModel.conf.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MundialTheme.colors.appBg)
    ) {
        when (val current = state) {
            is TeamsUiState.Loading -> LoadingContent()
            is TeamsUiState.Error -> ErrorContent(
                message = current.message,
                onRetry = viewModel::retry
            )
            is TeamsUiState.Success -> TeamsContent(
                state = current,
                view = view,
                conf = conf,
                onSelectView = viewModel::selectView,
                onSelectConf = viewModel::selectConf,
                filteredTeams = { teams, c -> viewModel.filteredTeams(teams, c) }
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
private fun TeamsContent(
    state: TeamsUiState.Success,
    view: TeamsView,
    conf: ConfFilter,
    onSelectView: (TeamsView) -> Unit,
    onSelectConf: (ConfFilter) -> Unit,
    filteredTeams: (List<Team>, ConfFilter) -> List<Team>,
    modifier: Modifier = Modifier
) {
    when (view) {
        TeamsView.LIST -> TeamsListContent(
            state = state,
            view = view,
            conf = conf,
            onSelectView = onSelectView,
            onSelectConf = onSelectConf,
            teams = filteredTeams(state.teams, conf),
            modifier = modifier
        )
        TeamsView.STANDINGS -> StandingsContent(
            state = state,
            view = view,
            onSelectView = onSelectView,
            modifier = modifier
        )
    }
}

@Composable
private fun TeamsListContent(
    state: TeamsUiState.Success,
    view: TeamsView,
    conf: ConfFilter,
    onSelectView: (TeamsView) -> Unit,
    onSelectConf: (ConfFilter) -> Unit,
    teams: List<Team>,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Selecciones",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MundialTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                SegmentedControl(
                    options = listOf("Selecciones", "Grupos"),
                    selected = view.ordinal,
                    onSelect = { onSelectView(TeamsView.entries[it]) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                ConfFilterRow(
                    selected = conf,
                    onSelect = onSelectConf
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        items(teams, key = { it.id }) { team ->
            TeamCard(team = team)
        }
    }
}

@Composable
private fun ConfFilterRow(
    selected: ConfFilter,
    onSelect: (ConfFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ConfFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) MundialTheme.colors.brand else MundialTheme.colors.surface
                    )
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter.label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (isSelected) androidx.compose.ui.graphics.Color.White else MundialTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun TeamCard(team: Team, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MundialTheme.shapes.medium)
            .background(MundialTheme.colors.surface)
            .clickable { }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlagAvatar(iso = team.flag, code = team.code, size = 44.dp)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MundialTheme.colors.segmentInactive)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "#${team.rank}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MundialTheme.colors.textTertiary
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = team.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MundialTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = team.conf.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MundialTheme.colors.textTertiary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MundialTheme.colors.segmentInactive)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = team.styleLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MundialTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun StandingsContent(
    state: TeamsUiState.Success,
    view: TeamsView,
    onSelectView: (TeamsView) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Selecciones",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MundialTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                SegmentedControl(
                    options = listOf("Selecciones", "Grupos"),
                    selected = view.ordinal,
                    onSelect = { onSelectView(TeamsView.entries[it]) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        lazyColumnItems(state.groups, key = { it.name }) { group ->
            Column {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MundialTheme.colors.textPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                StandingsTable(group = group, compact = false, onRowClick = {})
            }
        }
        item {
            LegendRow()
        }
    }
}

@Composable
private fun LegendRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 14.dp)
                .background(MundialTheme.colors.win)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Clasifican a la siguiente fase",
            style = MaterialTheme.typography.bodyMedium,
            color = MundialTheme.colors.textSecondary
        )
    }
}
