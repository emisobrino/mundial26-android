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
        val enriched = applyPositionOverrides(team.code, enrichSquad(team.code, squad))
        val coach = runCatching { sync.coaches()[team.code] }.getOrNull().orEmpty()
        // La estrella toma goles/asistencias del plantel ya enriquecido con FIFA (igual que la
        // ficha del jugador), no de los datos crudos de football-data que quedan en teams().
        val starName = team.star.name
        val starP = enriched.firstOrNull { it.name.equals(starName, true) }
            ?: enriched.firstOrNull { nameMatches(it.name, starName) }
        val star = starP?.let {
            team.star.copy(stats = team.star.stats.copy(goals = it.goals, assists = it.assists))
        } ?: team.star
        return team.copy(squad = enriched, coach = coach, star = star)
    }

    /**
     * Corrige posiciones mal clasificadas por football-data según el override editorial.
     * Empareja por nombre completo (todos los tokens de la clave presentes en el nombre)
     * para no confundir homónimos (p. ej. Ronald vs. Maximiliano Araújo en Uruguay).
     */
    private fun applyPositionOverrides(tla: String, squad: List<Player>): List<Player> {
        val overrides = EditorialSource.positionOverrides(tla)
        if (overrides.isEmpty()) return squad
        return squad.map { p ->
            val n = norm(p.name)
            val newPos = overrides.entries
                .firstOrNull { (key, _) -> key.split(' ').all { n.contains(it) } }?.value
            if (newPos != null) p.copy(position = newPos) else p
        }
    }

    /**
     * Enriquece el plantel con datos FIFA: capitán y goles/asistencias REALES de todos los
     * jugadores (por id, no solo los del ranking de líderes). Usa el ROSTER completo (unión
     * de convocados en todos los partidos jugados), no solo el último XI, para que también
     * los rotados/suplentes que anotaron tengan sus stats. Empareja por nombre normalizado
     * (fallback por apellido). Si FIFA no responde, devuelve el plantel tal cual.
     */
    private suspend fun enrichSquad(tla: String, squad: List<Player>): List<Player> {
        val roster = runCatching { fifaMatch.teamRoster(tla) }.getOrDefault(emptyList())
        if (roster.isEmpty()) return squad
        val stats = runCatching { fifa.statsById() }.getOrDefault(emptyMap())
        val byExact = roster.associateBy { norm(it.name) }
        val byLast = roster.associateBy { norm(it.name).substringAfterLast(' ') }
        return squad.map { p ->
            val li = byExact[norm(p.name)] ?: byLast[norm(p.name).substringAfterLast(' ')] ?: return@map p
            val s = stats[li.fifaId]
            p.copy(
                captain = li.captain,
                // football-data (scorers) y FIFA a veces discrepan (uno tiene al jugador y el otro
                // no): tomamos el máximo, que es el conteo más completo (los goles solo crecen).
                goals = maxOf(p.goals, s?.goals ?: 0),
                assists = maxOf(p.assists, s?.assists ?: 0),
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

/** Normaliza un nombre: sin acentos, sin puntuación (punto de "Jr.", guiones), minúsculas. */
private fun norm(s: String): String =
    java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^\\p{L}\\p{Nd} ]"), " ")
        .replace(Regex("\\s+"), " ")
        .lowercase().trim()

/** Sufijos que football-data y el editorial abrevian distinto ("Jr." vs "Júnior"). */
private val NAME_SUFFIXES = setOf("jr", "jnr", "junior", "sr", "snr", "senior", "ii", "iii", "iv")

/** Nombre sin sufijos ("Vinícius Júnior" / "Vinícius Jr." → "vinícius"). */
private fun coreName(n: String): String =
    n.split(' ').filter { it.isNotBlank() && it !in NAME_SUFFIXES }.joinToString(" ")

/**
 * Empareja el nombre editorial de la estrella con el del plantel real tolerando acentos y
 * formato: "L. Messi" ↔ "Lionel Messi" ↔ "Messi" (apellido), y sufijos abreviados
 * "Vinícius Jr." ↔ "Vinícius Júnior" (ignorando Jr./Júnior/Sr./II…).
 */
fun nameMatches(playerName: String, starName: String): Boolean {
    val p = norm(playerName)
    val s = norm(starName)
    if (p == s) return true
    val pc = coreName(p)
    val sc = coreName(s)
    if (pc.isNotEmpty() && pc == sc) return true
    val pLast = p.substringAfterLast(' ')
    val sLast = s.substringAfterLast(' ')
    if (pLast.length >= 3 && pLast == sLast) return true
    // Iniciales ("L. Messi" ↔ "Lionel Messi"): mismo nº de tokens, cada uno prefijo del otro.
    val pt = pc.split(' ').filter { it.isNotBlank() }
    val st = sc.split(' ').filter { it.isNotBlank() }
    return pt.size == st.size && pt.size >= 2 &&
        pt.indices.all { i -> pt[i] == st[i] || pt[i].startsWith(st[i]) || st[i].startsWith(pt[i]) }
}
