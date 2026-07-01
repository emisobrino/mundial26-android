package com.mundial26.app.data.repository

import com.mundial26.app.data.editorial.EditorialSource
import com.mundial26.app.data.local.db.dao.*
import com.mundial26.app.data.local.db.ageFromDob
import com.mundial26.app.data.local.db.toDomain
import com.mundial26.app.data.local.db.toGroups
import com.mundial26.app.data.local.db.toLeader
import com.mundial26.app.data.local.db.toPlayer
import com.mundial26.app.data.mock.MockData
import com.mundial26.app.data.net.tlaToIso
import com.mundial26.app.data.sync.SyncManager
import com.mundial26.app.domain.model.*
import com.mundial26.app.domain.repository.TeamRepository

class DefaultTeamRepository(
    private val sync: SyncManager, private val teamDao: TeamDao,
    private val playerDao: PlayerDao, private val standingDao: StandingDao
) : TeamRepository {

    private suspend fun fresh() { runCatching { sync.ensureFresh() } }

    override suspend fun refresh() { runCatching { sync.forceRefresh() } }

    override suspend fun teams(): List<Team> {
        fresh()
        val rows = teamDao.all()
        if (rows.isEmpty()) return MockData.teams
        return rows.map { t ->
            val ed = EditorialSource.forCode(t.tla)
            val base = t.toDomain()
            // Enlace estrella↔jugador tolerante a acentos y formato de nombre ("L. Messi" vs "Lionel Messi").
            val squad = playerDao.byTeam(t.tla)
            val starP = squad.firstOrNull { it.name.equals(t.starName, true) }
                ?: squad.firstOrNull { nameMatches(it.name, t.starName) }
            base.copy(
                pros = ed?.pros ?: emptyList(),
                cons = ed?.cons ?: emptyList(),
                star = base.star.copy(stats = base.star.stats.copy(
                    goals = starP?.goals ?: 0,
                    assists = starP?.assists ?: 0,
                    age = ageFromDob(starP?.dateOfBirth)
                ))
            )
        }
    }

    override suspend fun featured(): Team =
        teams().firstOrNull { it.code == EditorialSource.featuredCode } ?: MockData.teams.first()

    override suspend fun groups(): List<Group> {
        fresh()
        return standingDao.all().toGroups().ifEmpty { MockData.groups }
    }

    override suspend fun team(id: String): Team? {
        val team = teams().firstOrNull { it.id == id || it.code.equals(id, true) } ?: return null
        val squad = playerDao.byTeam(team.code).map { it.toPlayer() }
        return team.copy(squad = squad)
    }

    override suspend fun player(playerId: Long): Player? {
        fresh()
        return playerDao.byId(playerId)?.toPlayer()
    }

    override suspend fun topScorers(): List<Leader> {
        fresh()
        return playerDao.topScorers(10).mapIndexed { i, p -> p.toLeader(i + 1, p.goals) }.ifEmpty { MockData.topScorers }
    }

    override suspend fun topAssists(): List<Leader> {
        fresh()
        // football-data da las asistencias incompletas → si hay override editorial, tiene prioridad.
        val editorial = EditorialSource.topAssists
        if (editorial.isNotEmpty()) {
            return editorial.mapIndexed { i, a -> Leader(i + 1, a.name, tlaToIso(a.tla), a.assists) }
        }
        return playerDao.topAssists(10).mapIndexed { i, p -> p.toLeader(i + 1, p.assists) }.ifEmpty { MockData.topAssists }
    }
}

/** Normaliza un nombre: sin acentos, minúsculas, sin espacios extra. */
private fun norm(s: String): String =
    java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "").lowercase().trim()

/**
 * Empareja el nombre editorial de la estrella con el del plantel real tolerando
 * acentos y formato ("L. Messi" ↔ "Lionel Messi" ↔ "Messi") comparando el apellido.
 */
fun nameMatches(playerName: String, starName: String): Boolean {
    val p = norm(playerName)
    val s = norm(starName)
    if (p == s) return true
    val pLast = p.substringAfterLast(' ')
    val sLast = s.substringAfterLast(' ')
    return pLast.length >= 3 && pLast == sLast
}
