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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.painterResource
import com.mundial26.app.R
import com.mundial26.app.data.mock.FakeMatchRepository
import com.mundial26.app.data.mock.MockData
import com.mundial26.app.data.repository.nameMatches
import com.mundial26.app.domain.model.EventKind
import com.mundial26.app.domain.model.LineupPlayer
import com.mundial26.app.domain.model.Match
import com.mundial26.app.domain.model.MatchEvent
import com.mundial26.app.domain.model.MatchLineups
import com.mundial26.app.domain.model.MatchStat
import com.mundial26.app.domain.model.MatchStatus
import com.mundial26.app.domain.model.Nations
import com.mundial26.app.domain.model.Star
import com.mundial26.app.domain.model.WinProbability
import com.mundial26.app.ui.components.GoalConfetti
import com.mundial26.app.ui.components.MatchHighlights
import com.mundial26.app.ui.components.FlagAvatar
import com.mundial26.app.ui.components.LiveMinuteRow
import com.mundial26.app.ui.components.liveMinuteText
import com.mundial26.app.ui.theme.MundialTheme
import com.mundial26.app.ui.theme.teamFlagColor
import com.mundial26.app.ui.theme.ScoreboardBig

/**
 * Pantalla Detalle de partido: hero con marcador, estadísticas y eventos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    viewModel: MatchDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

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
            is MatchDetailUiState.Success -> PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            ) {
                MatchDetailContent(state = current)
            }
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
            state.winProb?.let { prob -> item { WinProbabilitySection(match = match, prob = prob) } }
            item { InfoSection(match = match) }
            if (state.homeStar != null || state.awayStar != null) {
                item { StarsSection(match = match, homeStar = state.homeStar, awayStar = state.awayStar) }
            }
        }
        if (match.status == MatchStatus.FINISHED && match.teamA.isNotBlank() && match.teamB.isNotBlank()) {
            item { HighlightsSection(match = match, videoId = state.highlightVideoId) }
        }
        if (match.stats.isNotEmpty()) {
            item { StatsSection(stats = match.stats, estimated = match.statsEstimated) }
        }
        state.lineups?.let { item { LineupsSection(match = match, lineups = it) } }
        if (match.events.isNotEmpty()) {
            item { EventsSection(match = match) }
        }
    }
}

/**
 * XI titulares en la cancha (estilo Google): jugadores por línea con dorsal y nombre,
 * formación arriba, y swipe izq/der para cambiar de equipo.
 */
@Composable
private fun LineupsSection(match: Match, lineups: MatchLineups, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "ALINEACIONES",
            style = MaterialTheme.typography.labelLarge,
            color = MundialTheme.colors.textTertiary
        )
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalPager(state = pagerState) { page ->
            if (page == 0) PitchLineup(
                iso = match.flagA, code = match.teamA, players = lineups.home,
                events = match.events.filter { it.side == 'A' }, coach = lineups.homeCoach
            )
            else PitchLineup(
                iso = match.flagB, code = match.teamB, players = lineups.away,
                events = match.events.filter { it.side == 'B' }, coach = lineups.awayCoach
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        // Indicador de página (dos puntos): el equipo visible.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(2) { i ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (pagerState.currentPage == i) MundialTheme.colors.brand else MundialTheme.colors.surface)
                )
            }
        }
    }
}

private val PitchLine = Color(0x33FFFFFF)

/** "Kylian Mbappé" → "K. Mbappé"; nombres de una palabra quedan igual. */
private fun shortName(full: String): String {
    val parts = full.trim().split(" ")
    return if (parts.size < 2) full else "${parts.first().first()}. ${parts.drop(1).joinToString(" ")}"
}

@Composable
private fun PitchLineup(iso: String, code: String, players: List<LineupPlayer>, events: List<MatchEvent> = emptyList(), coach: String = "", modifier: Modifier = Modifier) {
    // Líneas de la cancha: arquero arriba, delanteros abajo. Sin dato de línea → mediocampo.
    val gk = players.filter { it.line == 0 }
    val def = players.filter { it.line == 1 }
    val mid = players.filter { it.line == 2 || it.line == null }
    val fwd = players.filter { it.line == 3 }
    val formation = listOf(def.size, mid.size, fwd.size).filter { it > 0 }.joinToString("-")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E5B3E))
    ) {
        // Cabecera: bandera + nombre + formación.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF174731))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlagAvatar(iso = iso, code = code, size = 22.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = Nations.name(code),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (coach.isNotBlank()) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    // Solo el apellido para que entre completo junto a la formación.
                    text = "DT: ${coach.trim().substringAfterLast(" ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB7E4C7),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (formation.isNotBlank()) Text(
                text = formation,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB7E4C7),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF2E7D52))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
        // La cancha: área del arco arriba y las líneas de jugadores.
        Box(modifier = Modifier.fillMaxWidth()) {
            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                // Área grande y chica del arco (arriba, centradas).
                drawRect(PitchLine, topLeft = androidx.compose.ui.geometry.Offset(w * 0.22f, 0f),
                    size = androidx.compose.ui.geometry.Size(w * 0.56f, 110f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                drawRect(PitchLine, topLeft = androidx.compose.ui.geometry.Offset(w * 0.36f, 0f),
                    size = androidx.compose.ui.geometry.Size(w * 0.28f, 45f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                // Línea media + círculo central (abajo, insinuado).
                drawLine(PitchLine, androidx.compose.ui.geometry.Offset(0f, size.height),
                    androidx.compose.ui.geometry.Offset(w, size.height), 2f)
                drawCircle(PitchLine, radius = 90f, center = androidx.compose.ui.geometry.Offset(w / 2f, size.height),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                listOf(gk, def, mid, fwd).filter { it.isNotEmpty() }.forEach { line ->
                    // Celdas de ancho igual: los círculos quedan en grilla pareja aunque
                    // los nombres midan distinto.
                    Row(modifier = Modifier.fillMaxWidth()) {
                        line.forEach { p -> PitchPlayer(p, events = events, modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PitchPlayer(p: LineupPlayer, events: List<MatchEvent> = emptyList(), modifier: Modifier = Modifier) {
    // Sucesos del partido para este jugador (mismos visuales que la sección EVENTOS).
    fun mine(kind: EventKind) = events.any { it.kind == kind && nameMatches(it.player, p.name) }
    val goals = events.count { it.kind == EventKind.GOAL && nameMatches(it.player, p.name) }
    val red = mine(EventKind.RED)
    val yellow = !red && mine(EventKind.YELLOW)
    // En los cambios el jugador que SALE es el 2º del "Entra ⇄ Sale".
    val subbedOut = events.any {
        it.kind == EventKind.SUB && nameMatches(it.player.substringAfter("⇄").trim(), p.name)
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF12331F)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = p.shirt?.toString() ?: "–",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            if (subbedOut) Icon(
                imageVector = Icons.Filled.ArrowDownward,
                contentDescription = "Sale",
                tint = MundialTheme.colors.live,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-5).dp, y = (-3).dp)
                    .size(14.dp)
            )
            if (yellow || red) Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-3).dp)
                    .size(width = 9.dp, height = 13.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (red) MundialTheme.colors.live else MundialTheme.colors.yellow)
            )
            if (goals > 0) Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = if (goals > 1) 13.dp else 5.dp, y = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_ball_wc),
                    contentDescription = "Gol",
                    modifier = Modifier.size(15.dp)
                )
                if (goals > 1) Text(
                    text = "×$goals",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = shortName(p.name) + if (p.captain) " Ⓒ" else "",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
        )
    }
}

private val DIAS = arrayOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
private val MESES = arrayOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")

/** "2026-07-03" → "Viernes 3 de julio"; si no parsea, el texto original. */
private fun prettyDate(day: String?): String = runCatching {
    val d = java.time.LocalDate.parse(day)
    "${DIAS[d.dayOfWeek.value - 1]} ${d.dayOfMonth} de ${MESES[d.monthValue - 1]}"
}.getOrDefault(day ?: "")

/** Resumen del partido: reproductor de YouTube embebido; si no se encontró, botón a YouTube. */
@Composable
private fun HighlightsSection(match: Match, videoId: String?, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "RESUMEN",
            style = MaterialTheme.typography.labelLarge,
            color = MundialTheme.colors.textTertiary
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (videoId != null) {
            MatchHighlights(videoId = videoId)
        } else {
            val query = "${Nations.name(match.teamA)} ${Nations.name(match.teamB)} Mundial 2026 resumen"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MundialTheme.shapes.medium)
                    .background(MundialTheme.colors.surface)
                    .clickable {
                        runCatching {
                            uriHandler.openUri("https://www.youtube.com/results?search_query=" + java.net.URLEncoder.encode(query, "UTF-8"))
                        }
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ver resumen en YouTube",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MundialTheme.colors.brand,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MundialTheme.colors.textFaint
                )
            }
        }
    }
}

/** Probabilidad de resultado en 90' (estimada por ranking FIFA), estilo panel de Google. */
@Composable
private fun WinProbabilitySection(match: Match, prob: WinProbability, modifier: Modifier = Modifier) {
    // Color predominante de cada bandera (ej. Brasil verde, Noruega rojo).
    val homeColor = teamFlagColor(match.teamA, MundialTheme.colors.win)
    val drawColor = MundialTheme.colors.textFaint
    val awayColor = teamFlagColor(match.teamB, MundialTheme.colors.brand)
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "PROBABILIDAD DE VICTORIA (90 MIN)",
            style = MaterialTheme.typography.labelLarge,
            color = MundialTheme.colors.textTertiary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MundialTheme.shapes.medium)
                .background(MundialTheme.colors.surface)
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                ProbLabel(match.teamA, "${prob.home}%", homeColor, TextAlign.Start, Modifier.weight(1f))
                ProbLabel("Tiempo extra", "${prob.draw}%", MundialTheme.colors.textTertiary, TextAlign.Center, Modifier.weight(1f))
                ProbLabel(match.teamB, "${prob.away}%", awayColor, TextAlign.End, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                Box(modifier = Modifier.weight(prob.home.coerceAtLeast(0).toFloat().coerceAtLeast(0.01f)).fillMaxHeight().background(homeColor))
                Box(modifier = Modifier.weight(prob.draw.coerceAtLeast(0).toFloat().coerceAtLeast(0.01f)).fillMaxHeight().background(drawColor))
                Box(modifier = Modifier.weight(prob.away.coerceAtLeast(0).toFloat().coerceAtLeast(0.01f)).fillMaxHeight().background(awayColor))
            }
        }
    }
}

@Composable
private fun ProbLabel(name: String, pct: String, tint: Color, align: TextAlign, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MundialTheme.colors.textSecondary,
            textAlign = align,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = pct,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = tint,
            textAlign = align,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

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
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            homeStar?.let { StarCard(star = it, iso = match.flagA, code = match.teamA, modifier = Modifier.weight(1f).fillMaxHeight()) }
            awayStar?.let { StarCard(star = it, iso = match.flagB, code = match.teamB, modifier = Modifier.weight(1f).fillMaxHeight()) }
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
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${star.stats.goals} goles · ${star.stats.assists} asist.",
            style = MaterialTheme.typography.labelSmall,
            color = MundialTheme.colors.textTertiary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MatchHero(match: Match, modifier: Modifier = Modifier) {
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
    // Festejo de gol sobre el marcador (solo reacciona cuando el score sube en vivo).
    if (match.status == MatchStatus.LIVE) {
        GoalConfetti(scoreA = match.scoreA, scoreB = match.scoreB, modifier = Modifier.matchParentSize())
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
