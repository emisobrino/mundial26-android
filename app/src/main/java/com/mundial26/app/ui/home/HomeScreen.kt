package com.mundial26.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mundial26.app.domain.model.Team
import com.mundial26.app.ui.components.FlagAvatar
import com.mundial26.app.ui.components.LiveMatchCard
import com.mundial26.app.ui.components.MatchRow
import com.mundial26.app.ui.components.NewsCardCompact
import com.mundial26.app.ui.components.SectionHeader
import com.mundial26.app.ui.components.StandingsTable
import com.mundial26.app.ui.components.TournamentLeadersCard
import com.mundial26.app.ui.components.BrandLogo26
import com.mundial26.app.ui.navigation.TopDestination
import com.mundial26.app.ui.theme.MundialTheme

/**
 * Pantalla Inicio: resumen de En vivo, Resultados & próximos, Equipo destacado,
 * Posiciones del Grupo B y Noticias del Mundial.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToTab: (String) -> Unit,
    onMatchClick: (String) -> Unit,
    onNewsClick: (String) -> Unit,
    onTeamClick: (String) -> Unit,
    onLeadersClick: () -> Unit,
    onSeeTable: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    when (val current = state) {
        is HomeUiState.Loading -> LoadingContent(modifier)
        is HomeUiState.Error -> ErrorContent(
            message = current.message,
            onRetry = viewModel::retry,
            modifier = modifier
        )
        is HomeUiState.Success -> HomeContent(
            state = current,
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            onNavigateToTab = onNavigateToTab,
            onMatchClick = onMatchClick,
            onNewsClick = onNewsClick,
            onTeamClick = onTeamClick,
            onLeadersClick = onLeadersClick,
            onSeeTable = onSeeTable,
            modifier = modifier
        )
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MundialTheme.colors.appBg),
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
        modifier = modifier
            .fillMaxSize()
            .background(MundialTheme.colors.appBg),
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
private fun HomeContent(
    state: HomeUiState.Success,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onNavigateToTab: (String) -> Unit,
    onMatchClick: (String) -> Unit,
    onNewsClick: (String) -> Unit,
    onTeamClick: (String) -> Unit,
    onLeadersClick: () -> Unit,
    onSeeTable: () -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .background(MundialTheme.colors.appBg)
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            HomeHeader(modifier = Modifier.padding(horizontal = 16.dp))
        }

        item {
            SectionHeader(
                title = "En vivo",
                actionText = "Ver todo",
                onAction = { onNavigateToTab(TopDestination.MATCHES.route) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (state.live.isNotEmpty()) {
            item {
                // A ancho completo; si hay varios en vivo se recorren con swipe.
                val livePager = rememberPagerState { state.live.size }
                HorizontalPager(
                    state = livePager,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 12.dp
                ) { page ->
                    val match = state.live[page]
                    LiveMatchCard(
                        match = match,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onMatchClick(match.id) }
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionHeader(title = "Resultados & próximos")
                Spacer(modifier = Modifier.height(2.dp))
                state.results.forEach { match ->
                    MatchRow(match = match, onClick = { onMatchClick(match.id) })
                }
            }
        }

        item {
            FeaturedTeamCard(
                team = state.featured,
                onClick = { onTeamClick(state.featured.id) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (state.groups.isNotEmpty()) {
            item {
                // Pager: swipe horizontal para recorrer los 12 grupos (arranca en el B).
                val pagerState = rememberPagerState(
                    initialPage = state.groups.indexOfFirst { it.name.contains("B") }.coerceAtLeast(0)
                ) { state.groups.size }
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeader(
                        title = "Posiciones · ${state.groups[pagerState.currentPage].name}",
                        actionText = "Ver tabla",
                        onAction = onSeeTable
                    )
                    HorizontalPager(state = pagerState, pageSpacing = 12.dp) { page ->
                        StandingsTable(
                            group = state.groups[page],
                            compact = true,
                            onRowClick = { onSeeTable() }
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        repeat(state.groups.size) { i ->
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i == pagerState.currentPage) MundialTheme.colors.brand
                                        else MundialTheme.colors.border
                                    )
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader(
                    title = "Líderes del torneo",
                    actionText = "Ver todo",
                    onAction = onLeadersClick
                )
                TournamentLeadersCard(
                    scorers = state.scorers,
                    assists = state.assists
                )
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionHeader(
                    title = "Noticias del Mundial",
                    actionText = "Ver todo",
                    onAction = { onNavigateToTab(TopDestination.NEWS.route) }
                )
                state.news.forEach { item ->
                    NewsCardCompact(item = item, onClick = { onNewsClick(item.id) })
                }
            }
        }
    }
    }
}

@Composable
private fun HomeHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MundialTheme.shapes.large)
            .background(MundialTheme.colors.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrandLogo26()
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "Mundial 26",
                style = MaterialTheme.typography.titleMedium,
                color = MundialTheme.colors.textPrimary
            )
            Text(
                text = "USA · MÉXICO · CANADÁ",
                style = MaterialTheme.typography.labelSmall,
                color = MundialTheme.colors.textTertiary
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Box {
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notificaciones",
                    tint = MundialTheme.colors.textPrimary
                )
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .clip(CircleShape)
                    .background(MundialTheme.colors.live)
            )
        }
    }
}

@Composable
private fun FeaturedTeamCard(
    team: Team,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MundialTheme.shapes.large)
            .background(MundialTheme.colors.surfaceDark)
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(4.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxSize().background(MundialTheme.colors.win))
            Box(modifier = Modifier.weight(1f).fillMaxSize().background(MundialTheme.colors.live))
            Box(modifier = Modifier.weight(1f).fillMaxSize().background(MundialTheme.colors.brand))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlagAvatar(iso = team.flag, code = team.code, size = 56.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(MundialTheme.shapes.small)
                        .background(MundialTheme.colors.win)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = team.tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = team.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Estrella · ${team.star.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}
