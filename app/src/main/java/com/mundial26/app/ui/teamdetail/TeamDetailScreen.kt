package com.mundial26.app.ui.teamdetail

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cancel
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mundial26.app.data.mock.FakeTeamRepository
import com.mundial26.app.domain.model.Player
import com.mundial26.app.domain.model.Team
import com.mundial26.app.ui.components.FlagAvatar
import com.mundial26.app.ui.theme.MundialTheme

/**
 * Pantalla Detalle de equipo: hero con bandera/ranking, estrella a destacar,
 * fortalezas/debilidades y racha reciente.
 */
@Composable
fun TeamDetailScreen(
    viewModel: TeamDetailViewModel,
    onBack: () -> Unit,
    onPlayerClick: (String) -> Unit,
    onSquadPlayerClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MundialTheme.colors.appBg)
    ) {
        TopBar(onBack = onBack)

        when (val current = state) {
            is TeamDetailUiState.Loading -> LoadingContent()
            is TeamDetailUiState.Error -> ErrorContent(
                message = current.message,
                onRetry = viewModel::retry
            )
            is TeamDetailUiState.Success -> TeamDetailContent(
                team = current.team,
                onPlayerClick = onPlayerClick,
                onSquadPlayerClick = onSquadPlayerClick
            )
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
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
private fun TeamDetailContent(
    team: Team,
    onPlayerClick: (String) -> Unit,
    onSquadPlayerClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { TeamHero(team = team) }
        item {
            StarSection(
                team = team,
                onClick = { onPlayerClick(team.id) }
            )
        }
        if (team.pros.isNotEmpty()) {
            item { ProsSection(pros = team.pros) }
        }
        if (team.cons.isNotEmpty()) {
            item { ConsSection(cons = team.cons) }
        }
        if (team.form.isNotEmpty()) {
            item { FormSection(form = team.form) }
        }
        if (team.squad.isNotEmpty()) {
            item { SquadSection(squad = team.squad, onSquadPlayerClick = onSquadPlayerClick) }
        }
    }
}

/** Orden y etiqueta de los cuatro grupos de posición del plantel. */
private data class PosGroup(val order: Int, val label: String)

private fun posGroupOf(position: String): PosGroup {
    val p = position.lowercase()
    return when {
        p.contains("keeper") || p.contains("goal") -> PosGroup(0, "Arqueros")
        p.contains("back") || p.contains("defen") -> PosGroup(1, "Defensores")
        p.contains("midfield") || p.contains("medio") -> PosGroup(2, "Mediocampistas")
        p.contains("forward") || p.contains("wing") || p.contains("offen") ||
            p.contains("strik") || p.contains("attack") -> PosGroup(3, "Delanteros")
        else -> PosGroup(4, "Otros")
    }
}

@Composable
private fun SquadSection(
    squad: List<Player>,
    onSquadPlayerClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Mediocampistas ofensivos contienen "midfield" → se agrupan como medios (se evalúa antes que ataque).
    val grouped = squad
        .groupBy { posGroupOf(it.position) }
        .toList()
        .sortedBy { (g, _) -> g.order }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "PLANTEL",
                style = MaterialTheme.typography.labelLarge,
                color = MundialTheme.colors.textTertiary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${squad.size} jugadores",
                style = MaterialTheme.typography.labelSmall,
                color = MundialTheme.colors.textFaint
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MundialTheme.shapes.medium)
                .background(MundialTheme.colors.surface)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            grouped.forEach { (group, players) ->
                Text(
                    text = group.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MundialTheme.colors.brand,
                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                )
                players.sortedBy { it.name }.forEach { player ->
                    SquadPlayerRow(player = player, onClick = { onSquadPlayerClick(player.id) })
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SquadPlayerRow(player: Player, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = player.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MundialTheme.colors.textPrimary
            )
            if (player.age > 0) {
                Text(
                    text = "${player.age} años",
                    style = MaterialTheme.typography.labelSmall,
                    color = MundialTheme.colors.textFaint
                )
            }
        }
        if (player.goals > 0) {
            StatChip(text = "${player.goals} G", tint = MundialTheme.colors.win)
            Spacer(modifier = Modifier.width(6.dp))
        }
        if (player.assists > 0) {
            StatChip(text = "${player.assists} A", tint = MundialTheme.colors.brand)
        }
    }
}

@Composable
private fun StatChip(text: String, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tint.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

@Composable
private fun TeamHero(team: Team, modifier: Modifier = Modifier) {
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
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlagAvatar(iso = team.flag, code = team.code, size = 64.dp)

            Column {
                Row {
                    Text(
                        text = team.conf.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MundialTheme.colors.win
                    )
                    Text(
                        text = " · FIFA #${team.rank}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MundialTheme.colors.textFaint
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = team.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = team.styleLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
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
private fun StarSection(
    team: Team,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "ESTRELLA A DESTACAR",
            style = MaterialTheme.typography.labelLarge,
            color = MundialTheme.colors.textTertiary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MundialTheme.shapes.medium)
                .background(MundialTheme.colors.surface)
                .clickable { onClick() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MundialTheme.colors.surfaceDark),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${team.star.num}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = team.star.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MundialTheme.colors.textPrimary
                )
                Text(
                    text = team.star.pos,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MundialTheme.colors.brand
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = team.star.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MundialTheme.colors.textSecondary,
                    maxLines = 3
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MundialTheme.colors.textFaint
            )
        }
    }
}

@Composable
private fun ProsSection(pros: List<String>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "FORTALEZAS",
            style = MaterialTheme.typography.labelLarge,
            color = MundialTheme.colors.win
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MundialTheme.shapes.medium)
                .background(MundialTheme.colors.surface)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            pros.forEachIndexed { index, pro ->
                BulletRow(
                    text = pro,
                    icon = Icons.Outlined.CheckCircle,
                    tint = MundialTheme.colors.win
                )
                if (index != pros.lastIndex) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun ConsSection(cons: List<String>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "DEBILIDADES",
            style = MaterialTheme.typography.labelLarge,
            color = MundialTheme.colors.textTertiary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MundialTheme.shapes.medium)
                .background(MundialTheme.colors.surface)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            cons.forEachIndexed { index, con ->
                BulletRow(
                    text = con,
                    icon = Icons.Outlined.Cancel,
                    tint = MundialTheme.colors.live
                )
                if (index != cons.lastIndex) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun BulletRow(
    text: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MundialTheme.colors.textPrimary
        )
    }
}

@Composable
private fun FormSection(form: List<Char>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "RACHA",
            style = MaterialTheme.typography.labelLarge,
            color = MundialTheme.colors.textTertiary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            form.forEach { result ->
                FormChip(result = result)
            }
        }
    }
}

@Composable
private fun FormChip(result: Char, modifier: Modifier = Modifier) {
    val color = when (result) {
        'V' -> MundialTheme.colors.win
        'D' -> MundialTheme.colors.live
        else -> MundialTheme.colors.yellow
    }
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = result.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TeamDetailScreenPreview() {
    MundialTheme {
        val viewModel: TeamDetailViewModel = viewModel(
            factory = TeamDetailViewModel.Factory(
                FakeTeamRepository(),
                "arg"
            )
        )
        TeamDetailScreen(
            viewModel = viewModel,
            onBack = {},
            onPlayerClick = {},
            onSquadPlayerClick = {}
        )
    }
}
