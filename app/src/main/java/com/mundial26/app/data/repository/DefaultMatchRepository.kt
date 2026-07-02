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
            // Cruces de eliminatorias sin definir: mostrar el posible cruce según FIFA
            // ("PAR", "ESP/AUT" o "Gan. 93") en lugar de una fila vacía.
            if (m.teamA.isBlank() || m.teamB.isBlank()) {
                val cross = runCatching { fifa.crossesFor(e.utcDate) }.getOrNull()
                if (cross != null) m.copy(
                    teamA = cross.first, teamB = cross.second,
                    flagA = knownIso(cross.first), flagB = knownIso(cross.second)
                ) else m
            } else m
        }
    }

    /** ISO-2 solo si el texto es un TLA real (3 letras); para "ESP/AUT" o "Gan. 93" no hay bandera. */
    private fun knownIso(label: String): String =
        if (Regex("[A-Z]{3}").matches(label)) com.mundial26.app.data.net.tlaToIso(label) else ""
    override suspend fun refresh() { runCatching { sync.forceRefresh() } }

    /** Superpone minuto oficial y marcador en vivo de FIFA (la DB puede tener hasta 6 h de atraso). */
    private fun Match.withLive(live: List<FifaLiveInfo>): Match {
        val info = live.firstOrNull {
            (it.homeTla == teamA && it.awayTla == teamB) || (it.homeTla == teamB && it.awayTla == teamA)
        } ?: return this
        val flipped = info.homeTla != teamA
        return copy(
            minute = info.minute ?: minute,
            scoreA = (if (flipped) info.scoreAway else info.scoreHome) ?: scoreA,
            scoreB = (if (flipped) info.scoreHome else info.scoreAway) ?: scoreB
        )
    }

    override suspend fun liveMatches(): List<Match> {
        val live = all().filter { it.status == MatchStatus.LIVE }
        if (live.isEmpty()) return live
        val fifaLive = runCatching { fifa.liveNow() }.getOrDefault(emptyList())
        return live.map { it.withLive(fifaLive) }
    }
    override suspend fun resultsAndUpcoming() = all().filter { it.status != MatchStatus.LIVE }
    override suspend fun byDay() = all().filter { it.day != null }.groupBy { it.day!! }
    override suspend fun bracket(): List<BracketRound> {
        runCatching { sync.ensureFresh() }
        val entities = matchDao.all().map { e ->
            if (e.homeTla.isBlank() || e.awayTla.isBlank()) {
                val cross = runCatching { fifa.crossesFor(e.utcDate) }.getOrNull()
                if (cross != null) e.copy(homeTla = cross.first, awayTla = cross.second) else e
            } else e
        }
        return entities.toBracket().ifEmpty { MockData.bracket }
    }
    override suspend fun match(id: String): Match? {
        var m = all().firstOrNull { it.id == id } ?: return null
        if (m.status == MatchStatus.LIVE) {
            val fifaLive = runCatching { fifa.liveNow() }.getOrDefault(emptyList())
            m = m.withLive(fifaLive)
        }
        // Eventos: timeline oficial de FIFA (goles, tarjetas, cambios); openfootball como fallback.
        val events = runCatching {
            fifa.findMatch(m.day, m.teamA, m.teamB)?.let { fifa.events(it) }.orEmpty()
        }.getOrDefault(emptyList()).ifEmpty {
            runCatching { of.worldCup().matches.findEventsFor(daysAround(m.day), m.teamA, m.teamB) }.getOrDefault(emptyList())
        }
        // Sin fuente gratuita de stats por partido → estimación editorial (solo si ya se jugó).
        val stats = if (m.status != MatchStatus.UPCOMING && m.stats.isEmpty()) estimatedStats(m) else m.stats
        return m.copy(events = events, stats = stats)
    }
}
