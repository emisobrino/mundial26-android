package com.mundial26.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mundial26.app.domain.model.BracketMatch
import com.mundial26.app.domain.model.BracketRound
import com.mundial26.app.domain.model.Nations
import com.mundial26.app.ui.theme.MundialTheme
import kotlinx.coroutines.launch

private val DIAS_CORTOS = arrayOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

/** "2026-07-09" → "Jue 9/7"; si no parsea, el texto original. */
private fun shortDay(day: String?): String = runCatching {
    val d = java.time.LocalDate.parse(day)
    "${DIAS_CORTOS[d.dayOfWeek.value - 1]} ${d.dayOfMonth}/${d.monthValue}"
}.getOrDefault(day ?: "")

/**
 * Cuadro estilo bracket: una columna por ronda, paginada con swipe o flechas,
 * con la siguiente ronda asomando a la derecha.
 */
@Composable
fun BracketView(
    rounds: List<BracketRound>,
    onMatchClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (rounds.isEmpty()) return
    val pagerState = rememberPagerState { rounds.size }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                enabled = pagerState.currentPage > 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = "Ronda anterior",
                    tint = if (pagerState.currentPage > 0) MundialTheme.colors.textPrimary else MundialTheme.colors.textFaint
                )
            }
            Text(
                text = rounds[pagerState.currentPage].round,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MundialTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                enabled = pagerState.currentPage < rounds.lastIndex,
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Ronda siguiente",
                    tint = if (pagerState.currentPage < rounds.lastIndex) MundialTheme.colors.textPrimary else MundialTheme.colors.textFaint
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(end = 56.dp),
            verticalAlignment = Alignment.Top
        ) { page ->
            LazyColumn(
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(rounds[page].games, key = { "g-${rounds[page].round}-${it.id}-${it.a}-${it.b}" }) { game ->
                    BracketCard(game = game, onClick = { if (game.id.isNotBlank()) onMatchClick(game.id) })
                }
            }
        }
    }
}

@Composable
private fun BracketCard(game: BracketMatch, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MundialTheme.shapes.medium)
            .background(MundialTheme.colors.surface)
            .clickable { onClick() }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Cabecera: fecha (+hora si no se jugó) a la izquierda, chip Fin a la derecha.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = shortDay(game.day) + if (!game.done && game.time != null) " · ${game.time}" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MundialTheme.colors.textTertiary,
                modifier = Modifier.weight(1f)
            )
            if (game.done) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MundialTheme.colors.border)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (game.pa != null) "Fin (P)" else "Fin",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MundialTheme.colors.textSecondary
                    )
                }
            }
        }

        // Con marcador igualado, la llave se define por penales.
        val aWins = game.done && game.sa != null && game.sb != null &&
            (game.sa > game.sb || (game.sa == game.sb && (game.pa ?: 0) > (game.pb ?: 0)))
        val bWins = game.done && game.sa != null && game.sb != null &&
            (game.sb > game.sa || (game.sa == game.sb && (game.pb ?: 0) > (game.pa ?: 0)))
        BracketTeamRow(code = game.a, iso = game.flagA, score = game.sa, pen = game.pa, done = game.done, isWinner = aWins)
        BracketTeamRow(code = game.b, iso = game.flagB, score = game.sb, pen = game.pb, done = game.done, isWinner = bWins)
    }
}

@Composable
private fun BracketTeamRow(
    code: String,
    iso: String,
    score: Int?,
    pen: Int?,
    done: Boolean,
    isWinner: Boolean
) {
    val undecided = code.isBlank()
    val textColor = when {
        undecided -> MundialTheme.colors.textFaint
        !done -> MundialTheme.colors.textPrimary
        isWinner -> MundialTheme.colors.textPrimary
        else -> MundialTheme.colors.textSecondary
    }
    val weight = if (done && isWinner) FontWeight.Bold else FontWeight.Normal

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlagAvatar(iso = iso, code = code, size = 24.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = if (undecided) "A definir" else Nations.name(code),
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontWeight = weight,
            modifier = Modifier.weight(1f)
        )
        if (done && score != null) {
            Text(
                text = "$score" + (pen?.let { " ($it)" } ?: ""),
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                fontWeight = weight
            )
        }
    }
}
