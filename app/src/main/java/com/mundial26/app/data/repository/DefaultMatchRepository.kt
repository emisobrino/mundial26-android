package com.mundial26.app.data.repository

import com.mundial26.app.data.editorial.estimatedStats
import com.mundial26.app.data.local.db.dao.MatchDao
import com.mundial26.app.data.local.db.toBracket
import com.mundial26.app.data.local.db.toDomain
import com.mundial26.app.data.mock.MockData
import com.mundial26.app.data.remote.fifa.FifaLiveInfo
import com.mundial26.app.data.remote.fifa.FifaMatchSource
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
        return matchDao.all().map { it.toDomain() }.ifEmpty { MockData.matches }
    }
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
        return matchDao.all().toBracket().ifEmpty { MockData.bracket }
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
            runCatching { of.worldCup().matches.findEventsFor(m.day ?: "", m.teamA, m.teamB) }.getOrDefault(emptyList())
        }
        // Sin fuente gratuita de stats por partido → estimación editorial (solo si ya se jugó).
        val stats = if (m.status != MatchStatus.UPCOMING && m.stats.isEmpty()) estimatedStats(m) else m.stats
        return m.copy(events = events, stats = stats)
    }
}
