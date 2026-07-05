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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.mundial26.app.ui.components.FlagAvatar
import com.mundial26.app.ui.theme.MundialTheme

/**
 * Pantalla Detalle de jugador: hero con dorsal (o iniciales) / nombre / posición,
 * estadísticas del Mundial y ficha. Sirve para la estrella editorial y para
 * cualquier jugador del plantel real.
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
            is PlayerDetailUiState.Success -> PlayerDetailContent(state = current, onBack = onBack)
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
    state: PlayerDetailUiState.Success,
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
        PlayerHero(state = state, onBack = onBack)
        WorldCupStatsSection(state = state)
        if (state.note.isNotBlank()) NoteSection(note = state.note)
        FichaSection(state = state)
    }
}

/** Iniciales a partir del nombre ("Julián Quiñones" → "JQ"). */
private fun initialsOf(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

@Composable
private fun PlayerHero(
    state: PlayerDetailUiState.Success,
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
                        .background(MundialTheme.colors.brandDeep),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.number > 0) "${state.number}" else initialsOf(state.name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FlagAvatar(iso = state.teamFlag, code = "", size = 20.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = state.teamName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    if (state.position.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MundialTheme.colors.win.copy(alpha = 0.16f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = state.position,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MundialTheme.colors.greenAccentLight
                            )
                        }
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
            .clip(CircleShape)
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
        Box(modifier = Modifier.weight(1f).fillMaxSize().background(MundialTheme.colors.brandDeep))
    }
}

private sealed interface StatItem
private data class ValueStat(val value: String, val label: String) : StatItem
private data class CardsStat(val yellow: Int, val red: Int) : StatItem

@Composable
private fun WorldCupStatsSection(state: PlayerDetailUiState.Success, modifier: Modifier = Modifier) {
    // Métricas adaptativas; el box de tarjetas va último (abajo a la derecha).
    val items = buildList {
        add(ValueStat("${state.goals}", "Goles"))
        add(ValueStat("${state.assists}", "Asistencias"))
        if (state.age > 0) add(ValueStat("${state.age}", "Edad"))
        add(CardsStat(state.yellow, state.red))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "EN EL MUNDIAL",
            style = MaterialTheme.typography.labelLarge,
            color = MundialTheme.colors.textTertiary
        )
        Spacer(modifier = Modifier.height(12.dp))
        items.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { item ->
                    when (item) {
                        is ValueStat -> StatCard(value = item.value, label = item.label, modifier = Modifier.weight(1f))
                        is CardsStat -> CardsStatCard(yellow = item.yellow, red = item.red, modifier = Modifier.weight(1f))
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/** Box de tarjetas del torneo: icono amarillo + nº y icono rojo + nº. */
@Composable
private fun CardsStatCard(yellow: Int, red: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(MundialTheme.shapes.medium)
            .background(MundialTheme.colors.surface)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CardCount(color = MundialTheme.colors.yellow, count = yellow)
            CardCount(color = MundialTheme.colors.live, count = red)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tarjetas",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MundialTheme.colors.textTertiary
        )
    }
}

@Composable
private fun CardCount(color: Color, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 15.dp, height = 20.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MundialTheme.colors.textPrimary
        )
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
private fun NoteSection(note: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "SCOUTING",
            style = MaterialTheme.typography.labelLarge,
            color = MundialTheme.colors.textTertiary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = note,
            style = MaterialTheme.typography.bodyMedium,
            color = MundialTheme.colors.textSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MundialTheme.shapes.medium)
                .background(MundialTheme.colors.surface)
                .padding(16.dp)
        )
    }
}

@Composable
private fun FichaSection(state: PlayerDetailUiState.Success, modifier: Modifier = Modifier) {
    // Ficha unificada para todos (estrellas y plantel): siempre las mismas filas,
    // con "—" cuando no hay dato.
    val dash = "—"
    val rows = listOf(
        "Club" to state.club.ifBlank { dash },
        "Partidos con la selección" to (state.caps?.toString() ?: dash),
        "Goles con la selección" to (state.intlGoals?.toString() ?: dash),
        "Altura" to (state.heightCm?.takeIf { it > 0 }?.let { "%.2f m".format(it / 100.0) } ?: dash),
        "Peso" to (state.weightKg?.takeIf { it > 0 }?.let { "$it kg" } ?: dash),
        "Pie hábil" to when (state.preferredFoot) {
            1 -> "Derecho"
            2 -> "Izquierdo"
            else -> dash
        }
    )

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
                    HorizontalDivider(color = MundialTheme.colors.border)
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
