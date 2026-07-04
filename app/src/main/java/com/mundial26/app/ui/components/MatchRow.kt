package com.mundial26.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mundial26.app.data.mock.MockData
import com.mundial26.app.domain.model.Match
import com.mundial26.app.domain.model.MatchStatus
import com.mundial26.app.domain.model.Nations
import com.mundial26.app.ui.theme.MundialTheme

/**
 * Fila de partido reutilizable.
 *
 * - [useFullNames] = false (default): estilo compacto horizontal de Inicio
 *   ("Resultados & próximos"): estado/día a la izq, código+bandera | marcador/hora | código+bandera.
 * - [useFullNames] = true: estilo de lista de Partidos/Calendario: nombres completos
 *   con banderas apiladas verticalmente, marcador o "vs" a la derecha.
 * - [liveBorder]: agrega borde izquierdo rojo de 4dp (usado en la lista "Partidos" para EN VIVO).
 */
@Composable
fun MatchRow(
    match: Match,
    useFullNames: Boolean = false,
    liveBorder: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MundialTheme.colors.surface)
            .then(
                if (liveBorder) {
                    Modifier
                } else Modifier
            )
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(start = if (liveBorder) 0.dp else 12.dp, top = 10.dp, bottom = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (liveBorder) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MundialTheme.colors.live)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        if (useFullNames) {
            FullNamesContent(match = match, modifier = Modifier.weight(1f))
        } else {
            CompactContent(match = match, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatusLabel(match: Match, alignEnd: Boolean = false) {
    val textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    when (match.status) {
        MatchStatus.LIVE -> Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!alignEnd) {
                    LivePulseDot(size = 6.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "${match.minute}'",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MundialTheme.colors.live,
                    fontWeight = FontWeight.Bold,
                    textAlign = textAlign
                )
            }
            Text(
                text = match.group,
                style = MaterialTheme.typography.labelSmall,
                color = MundialTheme.colors.textTertiary,
                textAlign = textAlign
            )
        }
        MatchStatus.FINISHED -> Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
            Text(
                text = "Final",
                style = MaterialTheme.typography.labelSmall,
                color = MundialTheme.colors.textTertiary,
                textAlign = textAlign
            )
            Text(
                text = match.group,
                style = MaterialTheme.typography.labelSmall,
                color = MundialTheme.colors.textTertiary,
                textAlign = textAlign
            )
        }
        MatchStatus.UPCOMING -> Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
            Text(
                text = match.time.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = MundialTheme.colors.brand,
                fontWeight = FontWeight.Bold,
                textAlign = textAlign
            )
            Text(
                text = match.day?.let(::shortDay) ?: match.group,
                style = MaterialTheme.typography.labelSmall,
                color = MundialTheme.colors.textTertiary,
                textAlign = textAlign
            )
        }
    }
}

/** Estilo compacto: estado a la izq, código+bandera | marcador/hora | código+bandera. */
@Composable
private fun CompactContent(match: Match, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(56.dp)) {
            StatusLabel(match = match)
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.teamA.ifBlank { "A definir" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (match.teamA.isBlank()) MundialTheme.colors.textFaint else MundialTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                FlagAvatar(iso = match.flagA, code = match.teamA, size = 24.dp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.width(56.dp), contentAlignment = Alignment.Center) {
                ScoreOrTime(match)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlagAvatar(iso = match.flagB, code = match.teamB, size = 24.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = match.teamB.ifBlank { "A definir" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (match.teamB.isBlank()) MundialTheme.colors.textFaint else MundialTheme.colors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun ScoreOrTime(match: Match) {
    if (match.scoreA != null && match.scoreB != null) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${match.scoreA} - ${match.scoreB}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MundialTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
            if (match.penA != null && match.penB != null) {
                Text(
                    text = "(${match.penA}-${match.penB} pen)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MundialTheme.colors.textTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Text(
            text = "vs",
            style = MaterialTheme.typography.bodyMedium,
            color = MundialTheme.colors.textTertiary,
            textAlign = TextAlign.Center
        )
    }
}

/** Estilo lleno: nombres completos con banderas apiladas verticalmente; estado a la derecha. */
@Composable
private fun FullNamesContent(match: Match, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            TeamLine(name = match.teamANameOrCode(), iso = match.flagA, code = match.teamA, dim = match.teamA.isBlank())
            Spacer(modifier = Modifier.height(8.dp))
            TeamLine(name = match.teamBNameOrCode(), iso = match.flagB, code = match.teamB, dim = match.teamB.isBlank())
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (match.scoreA != null && match.scoreB != null) {
                Text(
                    text = "${match.scoreA}" + (match.penA?.let { " ($it)" } ?: ""),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MundialTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "${match.scoreB}" + (match.penB?.let { " ($it)" } ?: ""),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MundialTheme.colors.textPrimary
                )
            } else {
                Text(
                    text = "vs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MundialTheme.colors.textTertiary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            StatusLabel(match = match, alignEnd = true)
        }
    }
}

@Composable
private fun TeamLine(name: String, iso: String, code: String, dim: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FlagAvatar(iso = iso, code = code, size = 22.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (dim) MundialTheme.colors.textFaint else MundialTheme.colors.textPrimary
        )
    }
}

private val DIAS_CORTOS = arrayOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

/** "2026-07-04" → "Vie 4/7"; si no parsea, el texto original. */
private fun shortDay(day: String): String = runCatching {
    val d = java.time.LocalDate.parse(day)
    "${DIAS_CORTOS[d.dayOfWeek.value - 1]} ${d.dayOfMonth}/${d.monthValue}"
}.getOrDefault(day)

// Los nombres completos no están en el modelo Match (solo códigos); se resuelven
// contra la tabla de referencia de naciones del dominio, con fallback al código.
// Cruce de eliminatorias aún sin equipo → "A definir".
private fun Match.teamANameOrCode(): String = if (teamA.isBlank()) "A definir" else Nations.name(teamA)

private fun Match.teamBNameOrCode(): String = if (teamB.isBlank()) "A definir" else Nations.name(teamB)

@Preview(showBackground = true)
@Composable
private fun MatchRowCompactPreview() {
    MundialTheme {
        Column(
            modifier = Modifier
                .background(MundialTheme.colors.appBg)
                .padding(12.dp)
        ) {
            MatchRow(match = MockData.matches[2]) // finished
            Spacer(modifier = Modifier.height(8.dp))
            MatchRow(match = MockData.matches[3]) // upcoming
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MatchRowFullNamesPreview() {
    MundialTheme {
        Column(
            modifier = Modifier
                .background(MundialTheme.colors.appBg)
                .padding(12.dp)
        ) {
            MatchRow(
                match = MockData.matches[0],
                useFullNames = true,
                liveBorder = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            MatchRow(
                match = MockData.matches[4],
                useFullNames = true
            )
        }
    }
}
