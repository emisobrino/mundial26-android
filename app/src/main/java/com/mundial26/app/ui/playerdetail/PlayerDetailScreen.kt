package com.mundial26.app.ui.playerdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mundial26.app.data.mock.FakeTeamRepository
import com.mundial26.app.domain.model.Star
import com.mundial26.app.ui.components.FlagAvatar
import com.mundial26.app.ui.theme.MundialTheme

/**
 * Pantalla Detalle de jugador: hero con dorsal/nombre/posición de la estrella
 * del equipo, estadísticas del Mundial y ficha del jugador.
 */
@Composable
fun PlayerDetailScreen(
    viewModel: PlayerDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MundialTheme.colors.appBg)
    ) {
        when (val current = state) {
            is PlayerDetailUiState.Loading -> {
                LoadingContent()
                BackButton(onBack = onBack, modifier = Modifier.padding(12.dp))
            }
            is PlayerDetailUiState.Error -> {
                ErrorContent(message = current.message, onRetry = viewModel::retry)
                BackButton(onBack = onBack, modifier = Modifier.padding(12.dp))
            }
            is PlayerDetailUiState.Success -> PlayerDetailContent(
                star = current.star,
                teamName = current.teamName,
                teamFlag = current.teamFlag,
                onBack = onBack
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
private fun PlayerDetailContent(
    star: Star,
    teamName: String,
    teamFlag: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        PlayerHero(star = star, teamName = teamName, teamFlag = teamFlag, onBack = onBack)
        WorldCupStatsSection(star = star)
        FichaSection(star = star)
    }
}

@Composable
private fun PlayerHero(
    star: Star,
    teamName: String,
    teamFlag: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MundialTheme.shapes.large)
            .background(MundialTheme.colors.surfaceDark)
    ) {
        Column {
            TricolorStripe()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MundialTheme.colors.brand),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${star.num}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FlagAvatar(iso = teamFlag, code = "", size = 20.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = teamName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = star.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MundialTheme.colors.win.copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = star.pos,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MundialTheme.colors.greenAccentLight
                        )
                    }
                }
            }
        }

        BackButton(onBack = onBack, modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(MundialTheme.colors.surface),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = MundialTheme.colors.textPrimary
            )
        }
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
private fun WorldCupStatsSection(star: Star, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "EN EL MUNDIAL",
            style = MaterialTheme.typography.labelLarge,
            color = MundialTheme.colors.textTertiary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                value = "${star.stats.pj}",
                label = "Partidos",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "${star.stats.goals}",
                label = "Goles",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                value = "${star.stats.assists}",
                label = "Asistencias",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "%.1f".format(star.stats.rating),
                label = "Valoración",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(MundialTheme.shapes.medium)
            .background(MundialTheme.colors.surface)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MundialTheme.colors.brand
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MundialTheme.colors.textTertiary
        )
    }
}

@Composable
private fun FichaSection(star: Star, modifier: Modifier = Modifier) {
    val rows = buildList {
        if (star.stats.club.isNotBlank()) add("Club" to star.stats.club)
        if (star.stats.age > 0) add("Edad" to "${star.stats.age}")
        if (star.stats.marketValue.isNotBlank()) add("Valor" to star.stats.marketValue)
    }
    if (rows.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "FICHA",
            style = MaterialTheme.typography.labelLarge,
            color = MundialTheme.colors.textTertiary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MundialTheme.shapes.medium)
                .background(MundialTheme.colors.surface)
                .padding(horizontal = 16.dp)
        ) {
            rows.forEachIndexed { index, (label, value) ->
                FichaRow(label = label, value = value)
                if (index != rows.lastIndex) {
                    androidx.compose.material3.HorizontalDivider(color = MundialTheme.colors.border)
                }
            }
        }
    }
}

@Composable
private fun FichaRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MundialTheme.colors.textTertiary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MundialTheme.colors.textPrimary
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlayerDetailScreenPreview() {
    MundialTheme {
        val viewModel: PlayerDetailViewModel = viewModel(
            factory = PlayerDetailViewModel.Factory(
                FakeTeamRepository(),
                "arg"
            )
        )
        PlayerDetailScreen(
            viewModel = viewModel,
            onBack = {}
        )
    }
}
