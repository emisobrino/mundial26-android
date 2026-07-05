package com.mundial26.app.ui.matchdetail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.SwapHoriz
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
import com.mundial26.app.domain.model.Star
import com.mundial26.app.ui.components.FlagAvatar
import com.mundial26.app.ui.components.LiveMinuteRow
import com.mundial26.app.ui.components.liveMinuteText
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
            is MatchDetailUiState.Success -> MatchDetailContent(state = current)
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
private fun MatchDetailContent(state: MatchDetailUiState.Success, modifier: Modifier = Modifier) {
    val match = state.match
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { MatchHero(match = match) }
        if (match.status == MatchStatus.UPCOMING) {
            item { InfoSection(match = match) }
            if (state.homeStar != null || state.awayStar != null) {
                item { StarsSection(match = match, homeStar = state.homeStar, awayStar = state.awayStar) }
            }
        }
        if (match.stats.isNotEmpty()) {
            item { StatsSection(stats = match.stats, estimated = match.statsEstimated) }
        }
        if (match.events.isNotEmpty()) {
            item { EventsSection(match = match) }
        }
    }
}

private val DIAS = arrayOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
private val MESES = arrayOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")

/** "2026-07-03" → "Viernes 3 de julio"; si no parsea, el texto original. */
private fun prettyDate(day: String?): String = runCatching {
    val d = java.time.LocalDate.parse(day)
    "${DIAS[d.dayOfWeek.value - 1]} ${d.dayOfMonth} de ${MESES[d.monthValue - 1]}"
}.getOrDefault(day ?: "")

/** Ficha de la previa: fase, fecha, hora y estadio. */
@Composable
private fun InfoSection(match: Match, modifier: Modifier = Modifier) {
    val rows = buildList {
        if (match.group.isNotBlank()) add("Fase" to match.group)
        if (match.day != null) add("Fecha" to prettyDate(match.day))
        if (match.time != null) add("Hora" to match.time)
        if (match.stadium.isNotBlank()) {
            val parts = match.stadium.split(" · ")
            add("Estadio" to parts[0])
            if (parts.size > 1) add("Ciudad" to parts[1])
        }
    }
    if (rows.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "DATOS DEL PARTIDO",
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
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
                        color = MundialTheme.colors.textPrimary,
                        textAlign = TextAlign.End
                    )
                }
                if (index != rows.lastIndex) HorizontalDivider(color = MundialTheme.colors.border)
            }
        }
    }
}

/** Previa: la estrella de cada equipo, lado a lado. */
@Composable
private fun StarsSection(match: Match, homeStar: Star?, awayStar: Star?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "FIGURAS DEL PARTIDO",
            style = MaterialTheme.typography.labelLarge,
            color = MundialTheme.colors.textTertiary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            homeStar?.let { StarCard(star = it, iso = match.flagA, code = match.teamA, modifier = Modifier.weight(1f)) }
            awayStar?.let { StarCard(star = it, iso = match.flagB, code = match.teamB, modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun StarCard(star: Star, iso: String, code: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(MundialTheme.shapes.medium)
            .background(MundialTheme.colors.surface)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FlagAvatar(iso = iso, code = code, size = 36.dp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = star.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MundialTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = star.pos,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MundialTheme.colors.brand,
            textAlign = TextAlign.Center
        )
        if (star.stats.goals > 0 || star.stats.assists > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${star.stats.goals} goles · ${star.stats.assists} asist.",
                style = MaterialTheme.typography.labelSmall,
                color = MundialTheme.colors.textTertiary,
                textAlign = TextAlign.Center
            )
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tres tercios iguales → el marcador y la píldora quedan en el centro geométrico.
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                TeamColumn(iso = match.flagA, code = match.teamA)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                // ¡Gol!: el marcador nuevo entra rodando desde abajo (250 ms, ease-out).
                AnimatedContent(
                    targetState = if (match.status == MatchStatus.UPCOMING) "VS"
                        else "${match.scoreA ?: 0} - ${match.scoreB ?: 0}",
                    transitionSpec = {
                        (slideInVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) { it / 2 } +
                            fadeIn(tween(250))) togetherWith
                            (slideOutVertically(animationSpec = tween(180)) { -it / 2 } + fadeOut(tween(180)))
                    },
                    label = "score"
                ) { score ->
                    Text(
                        text = score,
                        style = ScoreboardBig,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
                if (match.status == MatchStatus.UPCOMING && match.day != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = prettyDate(match.day) + (match.time?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
                if (match.penA != null && match.penB != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "(${match.penA} - ${match.penB} penales)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
                if (match.status == MatchStatus.LIVE && (match.minute != null || match.minuteLabel != null)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LiveMinuteRow(label = liveMinuteText(match.minuteLabel, match.minute), added = match.addedLabel)
                }
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                TeamColumn(iso = match.flagB, code = match.teamB)
            }
        }

        if (match.stadium.isNotBlank()) {
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
private fun StatsSection(stats: List<MatchStat>, estimated: Boolean = false, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "ESTADÍSTICAS",
            style = MaterialTheme.typography.labelLarge,
            color = MundialTheme.colors.textTertiary
        )
        Spacer(modifier = Modifier.height(12.dp))
        // Tarjeta oscura con franja tricolor, a juego con el hero y los widgets del Inicio.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MundialTheme.shapes.large)
                .background(MundialTheme.colors.surfaceDark)
        ) {
            TricolorStripe()
            Column(modifier = Modifier.padding(16.dp)) {
                stats.forEachIndexed { index, stat ->
                    StatRow(stat = stat)
                    if (index != stats.lastIndex) {
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (estimated) "Estimación editorial · no oficial" else "Datos oficiales FIFA",
            style = MaterialTheme.typography.labelSmall,
            color = MundialTheme.colors.textFaint
        )
    }
}

@Composable
private fun StatRow(stat: MatchStat, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stat.a}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = stat.label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = "${stat.b}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        val total = (stat.a + stat.b).coerceAtLeast(1)
        // La barra se desliza suave cuando la stat cambia en un refresh en vivo.
        val weightA by animateFloatAsState(
            targetValue = (stat.a.toFloat() / total).coerceAtLeast(0.001f),
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            label = "statBar"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .weight(weightA)
                    .fillMaxSize()
                    .background(MundialTheme.colors.brand)
            )
            Box(
                modifier = Modifier
                    .weight((1f - weightA).coerceAtLeast(0.001f))
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.25f))
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

    val minuteLabel: @Composable () -> Unit = {
        Text(
            text = minuteText,
            style = MaterialTheme.typography.bodyMedium,
            color = MundialTheme.colors.textTertiary
        )
    }
    // Cambio: dos líneas con flecha verde (entra) y roja (sale).
    val subParts = if (event.kind == EventKind.SUB) event.player.split(" ⇄ ") else null
    val content: @Composable () -> Unit = if (subParts != null && subParts.size == 2) {
        { SubPlayers(inName = subParts[0], outName = subParts[1], isSideA = isSideA) }
    } else {
        {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSideA) {
                    EventIcon(kind = event.kind)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = event.player,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MundialTheme.colors.textPrimary,
                    textAlign = if (isSideA) TextAlign.Start else TextAlign.End
                )
                if (!isSideA) {
                    Spacer(modifier = Modifier.width(8.dp))
                    EventIcon(kind = event.kind)
                }
            }
        }
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (isSideA) {
            minuteLabel()
            Spacer(modifier = Modifier.width(8.dp))
            content()
        } else {
            content()
            Spacer(modifier = Modifier.width(8.dp))
            minuteLabel()
        }
    }
}

/** Sustitución: el que entra con flecha verde y el que sale con flecha roja. */
@Composable
private fun SubPlayers(inName: String, outName: String, isSideA: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (isSideA) Alignment.Start else Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.ArrowUpward,
                contentDescription = "Entra",
                tint = MundialTheme.colors.win,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = inName,
                style = MaterialTheme.typography.bodyLarge,
                color = MundialTheme.colors.textPrimary
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.ArrowDownward,
                contentDescription = "Sale",
                tint = MundialTheme.colors.live,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = outName,
                style = MaterialTheme.typography.bodyMedium,
                color = MundialTheme.colors.textSecondary
            )
        }
    }
}

/** Icono por tipo de evento: pelota (gol), tarjeta amarilla/roja, flechas (cambio). */
@Composable
private fun EventIcon(kind: EventKind, modifier: Modifier = Modifier) {
    when (kind) {
        EventKind.GOAL -> Icon(
            imageVector = Icons.Outlined.SportsSoccer,
            contentDescription = "Gol",
            tint = MundialTheme.colors.textPrimary,
            modifier = modifier.size(18.dp)
        )
        EventKind.YELLOW -> Box(
            modifier = modifier
                .size(width = 11.dp, height = 15.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MundialTheme.colors.yellow)
        )
        EventKind.RED -> Box(
            modifier = modifier
                .size(width = 11.dp, height = 15.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MundialTheme.colors.live)
        )
        EventKind.SUB -> Icon(
            imageVector = Icons.Outlined.SwapHoriz,
            contentDescription = "Cambio",
            tint = MundialTheme.colors.textTertiary,
            modifier = modifier.size(18.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MatchDetailScreenPreview() {
    MundialTheme {
        val viewModel: MatchDetailViewModel = viewModel(
            factory = MatchDetailViewModel.Factory(
                FakeMatchRepository(),
                com.mundial26.app.data.mock.FakeTeamRepository(),
                MockData.matches[0].id
            )
        )
        MatchDetailScreen(viewModel = viewModel, onBack = {})
    }
}
