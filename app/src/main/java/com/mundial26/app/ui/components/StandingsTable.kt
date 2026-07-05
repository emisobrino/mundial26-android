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
import com.mundial26.app.domain.model.Group
import com.mundial26.app.domain.model.GroupRow
import com.mundial26.app.domain.model.Nations
import com.mundial26.app.ui.theme.MundialTheme

/**
 * Tabla de posiciones de un grupo.
 * - [compact] = true (Inicio): columnas `# EQUIPO PJ DG PTS`, nombres completos.
 * - [compact] = false (Grupos): columnas completas `# EQUIPO PJ G E P DG PTS`, códigos.
 */
@Composable
fun StandingsTable(
    group: Group,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    qualifiedThirds: Set<String> = emptySet(),
    onRowClick: ((GroupRow) -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MundialTheme.shapes.medium)
            .background(MundialTheme.colors.surface)
    ) {
        HeaderRow(compact = compact)
        group.rows.forEachIndexed { index, row ->
            // Clasifican los dos primeros y, además, los mejores terceros del torneo.
            val classifies = index < 2 || (index == 2 && qualifiedThirds.contains(row.code))
            DataRow(
                row = row,
                position = index + 1,
                classifies = classifies,
                compact = compact,
                onClick = onRowClick?.let { { it(row) } }
            )
        }
    }
}

/**
 * Códigos de los mejores terceros que clasifican (Mundial 2026: los 8 mejores de los 12
 * grupos), ordenados por puntos, diferencia de gol y goles a favor.
 */
fun bestThirdCodes(groups: List<Group>, count: Int = 8): Set<String> =
    groups.mapNotNull { it.rows.getOrNull(2) }
        .sortedWith(
            compareByDescending<GroupRow> { it.pts }
                .thenByDescending { it.dg }
                .thenByDescending { it.gf }
        )
        .take(count)
        .map { it.code }
        .toSet()

@Composable
private fun HeaderRow(compact: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MundialTheme.colors.tableHeaderBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#",
            style = MaterialTheme.typography.labelSmall,
            color = MundialTheme.colors.textTertiary,
            modifier = Modifier.width(20.dp)
        )
        Text(
            text = "EQUIPO",
            style = MaterialTheme.typography.labelSmall,
            color = MundialTheme.colors.textTertiary,
            modifier = Modifier.weight(1f).padding(start = 8.dp)
        )
        NumCell(text = "PJ", header = true)
        if (!compact) {
            NumCell(text = "G", header = true)
            NumCell(text = "E", header = true)
            NumCell(text = "P", header = true)
        }
        NumCell(text = "DG", header = true)
        NumCell(
            text = "PTS",
            header = true,
            color = MundialTheme.colors.brand
        )
    }
}

@Composable
private fun DataRow(
    row: GroupRow,
    position: Int,
    classifies: Boolean,
    compact: Boolean,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.width(20.dp + 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .background(
                        if (classifies) MundialTheme.colors.win else MundialTheme.colors.textFaint
                    )
            )
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = "$position",
                style = MaterialTheme.typography.bodyMedium,
                color = MundialTheme.colors.textPrimary,
                modifier = Modifier.width(20.dp)
            )
        }

        Row(
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlagAvatar(iso = row.flag, code = row.code, size = 22.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = teamLabel(row, compact),
                style = MaterialTheme.typography.bodyLarge,
                color = MundialTheme.colors.textPrimary
            )
        }

        NumCell(text = "${row.pj}")
        if (!compact) {
            NumCell(text = "${row.g}")
            NumCell(text = "${row.e}")
            NumCell(text = "${row.p}")
        }
        NumCell(text = signed(row.dg))
        NumCell(
            text = "${row.pts}",
            bold = true,
            color = MundialTheme.colors.brand
        )
    }
}

private fun teamLabel(row: GroupRow, compact: Boolean): String =
    if (compact) Nations.name(row.code) else row.code

private fun signed(value: Int): String = when {
    value > 0 -> "+$value"
    else -> "$value"
}

@Composable
private fun NumCell(
    text: String,
    header: Boolean = false,
    bold: Boolean = false,
    color: androidx.compose.ui.graphics.Color? = null
) {
    val resolvedColor = color
        ?: if (header) MundialTheme.colors.textTertiary else MundialTheme.colors.textPrimary
    Text(
        text = text,
        style = if (header) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
        color = resolvedColor,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.End,
        modifier = Modifier.width(32.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun StandingsTableFullPreview() {
    MundialTheme {
        StandingsTable(
            group = MockData.groups[0],
            compact = false,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StandingsTableCompactPreview() {
    MundialTheme {
        StandingsTable(
            group = MockData.groups[1],
            compact = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}
