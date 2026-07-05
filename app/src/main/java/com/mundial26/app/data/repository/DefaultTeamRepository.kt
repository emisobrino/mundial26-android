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
import com.mundial26.app.data.remote.fifa.FifaMatchSource
import com.mundial26.app.data.remote.fifa.FifaStatsSource
import com.mundial26.app.data.sync.SyncManager
import com.mundial26.app.domain.model.*
import com.mundial26.app.domain.repository.TeamRepository

class DefaultTeamRepository(
    private val sync: SyncManager, private val teamDao: TeamDao,
    private val playerDao: PlayerDao, private val standingDao: StandingDao,
    private val fifa: FifaStatsSource = FifaStatsSource(),
    private val fifaMatch: FifaMatchSource = FifaMatchSource()
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
        val enriched = markInjured(team.code, enrichSquad(team.code, squad))
        val coach = runCatching { sync.coaches()[team.code] }.getOrNull().orEmpty()
        return team.copy(squad = enriched, coach = coach)
    }

    /** Marca lesionados según la lista editorial (empareja por nombre normalizado). */
    private fun markInjured(tla: String, squad: List<Player>): List<Player> {
        val injured = EditorialSource.injured(tla)
        if (injured.isEmpty()) return squad
        return squad.map { p ->
            val n = norm(p.name)
            p.copy(injured = injured.any { n.contains(it) })
        }
    }

    /**
     * Enriquece el plantel con datos FIFA: capitán (de la alineación) y goles/asistencias
     * REALES de todos los jugadores (por id, no solo los que están en el ranking de líderes).
     * Empareja por nombre normalizado (con fallback por apellido). Si FIFA no responde,
     * devuelve el plantel tal cual (goles/asistencias de football-data).
     */
    private suspend fun enrichSquad(tla: String, squad: List<Player>): List<Player> {
        val lineup = runCatching { fifaMatch.teamLineup(tla) }.getOrDefault(emptyList())
        if (lineup.isEmpty()) return squad
        val stats = runCatching { fifa.statsById() }.getOrDefault(emptyMap())
        val byExact = lineup.associateBy { norm(it.name) }
        val byLast = lineup.associateBy { norm(it.name).substringAfterLast(' ') }
        return squad.map { p ->
            val li = byExact[norm(p.name)] ?: byLast[norm(p.name).substringAfterLast(' ')] ?: return@map p
            val s = stats[li.fifaId]
            p.copy(
                captain = li.captain,
                goals = s?.goals ?: p.goals,
                assists = s?.assists ?: p.assists,
                yellow = s?.yellow ?: p.yellow,
                red = s?.red ?: p.red
            )
        }
    }

    override suspend fun player(playerId: Long): Player? {
        fresh()
        return playerDao.byId(playerId)?.toPlayer()
    }

    private val clubCache = mutableMapOf<Long, PlayerClubInfo?>()

    override suspend fun playerClub(playerId: Long): PlayerClubInfo? {
        if (clubCache.containsKey(playerId)) return clubCache[playerId]
        val info = runCatching {
            val fdId = playerDao.byId(playerId)?.fdId ?: return@runCatching null
            val person = fdApiPerson(fdId) ?: return@runCatching null
            val club = person.currentTeam?.name.orEmpty()
            if (club.isBlank() && person.shirtNumber == null) null
            else PlayerClubInfo(club = club, shirtNumber = person.shirtNumber)
        }.getOrNull()
        clubCache[playerId] = info
        return info
    }

    private suspend fun fdApiPerson(fdId: Long) = runCatching { sync.person(fdId) }.getOrNull()

    override suspend fun topScorers(): List<Leader> {
        fresh()
        // Prioridad: FIFA oficial (misma fuente que el panel de Google); DB de football-data como fallback.
        val fifaTop = runCatching { fifa.topScorers(10) }.getOrDefault(emptyList())
        if (fifaTop.isNotEmpty()) return fifaTop
        return playerDao.topScorers(10).mapIndexed { i, p -> p.toLeader(i + 1, p.goals) }.ifEmpty { MockData.topScorers }
    }

    override suspend fun topYellowCards(): List<Leader> =
        runCatching { fifa.topYellowCards(10) }.getOrDefault(emptyList())

    override suspend fun topRedCards(): List<Leader> =
        runCatching { fifa.topRedCards(10) }.getOrDefault(emptyList())

    override suspend fun topAssists(): List<Leader> {
        fresh()
        // football-data da las asistencias incompletas → prioridad: FIFA oficial (en vivo),
        // luego el override editorial (offline), y al final lo que haya en la DB.
        val fifaTop = runCatching { fifa.topAssists(10) }.getOrDefault(emptyList())
        if (fifaTop.isNotEmpty()) return fifaTop
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
