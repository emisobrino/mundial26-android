package com.mundial26.app.data.repository

import com.mundial26.app.data.editorial.estimatedStats
import com.mundial26.app.data.local.db.dao.MatchDao
import com.mundial26.app.data.local.db.toBracket
import com.mundial26.app.data.local.db.toDomain
import com.mundial26.app.data.mock.MockData
import com.mundial26.app.data.remote.fifa.FifaLiveInfo
import com.mundial26.app.data.remote.fifa.FifaMatchSource
import com.mundial26.app.data.remote.fifa.daysAround
import com.mundial26.app.data.remote.openfootball.OpenFootballApi
import com.mundial26.app.data.remote.openfootball.findEventsFor
import com.mundial26.app.data.sync.SyncManager
import com.mundial26.app.domain.model.*
import com.mundial26.app.domain.repository.MatchRepository

class DefaultMatchRepository(
    private val sync: SyncManager, private val matchDao: MatchDao, private val of: OpenFootballApi,
    private val fifa: FifaMatchSource = FifaMatchSource()
) : MatchRepository {
    private suspend fun all(): List<Match> {
        runCatching { sync.ensureFresh() }
        val entities = matchDao.all()
        if (entities.isEmpty()) return MockData.matches
        return entities.map { e ->
            val m = e.toDomain()
            // Cruces de eliminatorias: si FIFA ya confirmó un equipo que football-data
            // aún no tiene, usarlo; si el cruce sigue abierto, queda vacío → "A definir".
            if (m.teamA.isBlank() || m.teamB.isBlank()) {
                val cross = runCatching { fifa.crossesFor(e.utcDate) }.getOrNull()
                if (cross != null) {
                    val a = confirmedOrBlank(cross.first)
                    val b = confirmedOrBlank(cross.second)
                    m.copy(
                        teamA = a, teamB = b,
                        flagA = if (a.isBlank()) "" else com.mundial26.app.data.net.tlaToIso(a),
                        flagB = if (b.isBlank()) "" else com.mundial26.app.data.net.tlaToIso(b)
                    )
                } else m
            } else m
        }
    }

    /** Solo un equipo CONFIRMADO (TLA de 3 letras); "ESP/AUT" o "Gan. 93" → "" (A definir). */
    private fun confirmedOrBlank(label: String): String =
        if (Regex("[A-Z]{3}").matches(label)) label else ""
    override suspend fun refresh() { runCatching { sync.forceRefresh() } }

    /**
     * Superpone el estado en vivo de FIFA: minuto oficial, marcador y —clave— la
     * PROMOCIÓN a LIVE cuando FIFA ya lo da en juego pero football-data (nuestra DB,
     * que actualiza con retraso) todavía lo tiene como próximo.
     */
    private fun Match.withLive(live: List<FifaLiveInfo>): Match {
        val info = live.firstOrNull {
            (it.homeTla == teamA && it.awayTla == teamB) || (it.homeTla == teamB && it.awayTla == teamA)
        } ?: return this
        val flipped = info.homeTla != teamA
        val promoted = if (info.inPlay && status != MatchStatus.FINISHED) MatchStatus.LIVE else status
        return copy(
            status = promoted,
            minute = info.minute ?: minute ?: (if (promoted == MatchStatus.LIVE) 1 else null),
            // Etiqueta oficial de FIFA (frena en el ET); el descuento va en el globo azul.
            minuteLabel = info.timeLabel.takeIf { it.isNotBlank() },
            addedLabel = info.addedLabel,
            scoreA = (if (flipped) info.scoreAway else info.scoreHome) ?: scoreA,
            scoreB = (if (flipped) info.scoreHome else info.scoreAway) ?: scoreB
        )
    }

    private suspend fun allWithLive(): List<Match> {
        val list = all()
        val fifaLive = runCatching { fifa.liveNow() }.getOrDefault(emptyList())
        if (fifaLive.isEmpty()) return list
        return list.map { it.withLive(fifaLive) }
    }

    override suspend fun liveMatches() = allWithLive().filter { it.status == MatchStatus.LIVE }
    override suspend fun resultsAndUpcoming() = allWithLive().filter { it.status != MatchStatus.LIVE }
    override suspend fun byDay() = allWithLive().filter { it.day != null }.groupBy { it.day!! }
    override suspend fun bracket(): List<BracketRound> {
        runCatching { sync.ensureFresh() }
        val entities = matchDao.all().map { e ->
            if (e.homeTla.isBlank() || e.awayTla.isBlank()) {
                val cross = runCatching { fifa.crossesFor(e.utcDate) }.getOrNull()
                if (cross != null) e.copy(
                    homeTla = confirmedOrBlank(cross.first),
                    awayTla = confirmedOrBlank(cross.second)
                ) else e
            } else e
        }
        return entities.toBracket().ifEmpty { MockData.bracket }
    }
    override suspend fun lineups(id: String): MatchLineups? {
        val m = all().firstOrNull { it.id == id } ?: return null
        if (m.teamA.isBlank() || m.teamB.isBlank()) return null
        val ref = runCatching { fifa.findMatch(m.day, m.teamA, m.teamB) }.getOrNull() ?: return null
        suspend fun side(tla: String) = runCatching { fifa.matchLineup(ref, tla) }.getOrDefault(emptyList())
            .map { LineupPlayer(it.name, it.shirt, it.captain, it.position) }
        val home = side(m.teamA); val away = side(m.teamB)
        if (home.isEmpty() && away.isEmpty()) return null
        return MatchLineups(home, away)
    }

    override suspend fun match(id: String): Match? {
        val m = allWithLive().firstOrNull { it.id == id } ?: return null
        val ref = runCatching { fifa.findMatch(m.day, m.teamA, m.teamB) }.getOrNull()
        // Eventos: timeline oficial de FIFA (goles, tarjetas, cambios); openfootball como fallback.
        val events = (ref?.let { runCatching { fifa.events(it) }.getOrDefault(emptyList()) } ?: emptyList()).ifEmpty {
            runCatching { of.worldCup().matches.findEventsFor(daysAround(m.day), m.teamA, m.teamB) }.getOrDefault(emptyList())
        }
        // Stats: oficiales de FIFA (fdh); si no hay, estimación editorial (solo si ya se juega/jugó).
        var fifaStats = ref?.let { runCatching { fifa.matchStats(it) }.getOrDefault(emptyList()) } ?: emptyList()
        if (fifaStats.isNotEmpty() && ref != null && ref.homeTla != m.teamA) {
            fifaStats = fifaStats.map { it.copy(a = it.b, b = it.a) }   // FIFA invierte local/visitante
        }
        val (stats, estimated) = when {
            fifaStats.isNotEmpty() -> fifaStats to false
            m.status != MatchStatus.UPCOMING && m.stats.isEmpty() -> estimatedStats(m) to true
            else -> m.stats to m.statsEstimated
        }
        // Sede desde el calendario FIFA ("Estadio Akron · Guadalajara").
        val venue = ref?.takeIf { it.stadium.isNotBlank() }
            ?.let { r -> r.stadium + (r.city.takeIf { it.isNotBlank() && it != r.stadium }?.let { " · $it" } ?: "") }
            ?: m.stadium
        return m.copy(events = events, stats = stats, statsEstimated = estimated, stadium = venue)
    }
}
