package com.mundial26.app.data.repository

import com.mundial26.app.data.local.db.dao.MatchDao
import com.mundial26.app.data.local.db.toBracket
import com.mundial26.app.data.local.db.toDomain
import com.mundial26.app.data.mock.MockData
import com.mundial26.app.data.remote.openfootball.OpenFootballApi
import com.mundial26.app.data.remote.openfootball.findEventsFor
import com.mundial26.app.data.sync.SyncManager
import com.mundial26.app.domain.model.*
import com.mundial26.app.domain.repository.MatchRepository

class DefaultMatchRepository(
    private val sync: SyncManager, private val matchDao: MatchDao, private val of: OpenFootballApi
) : MatchRepository {
    private suspend fun all(): List<Match> {
        runCatching { sync.ensureFresh() }
        return matchDao.all().map { it.toDomain() }.ifEmpty { MockData.matches }
    }
    override suspend fun liveMatches() = all().filter { it.status == MatchStatus.LIVE }
    override suspend fun resultsAndUpcoming() = all().filter { it.status != MatchStatus.LIVE }
    override suspend fun byDay() = all().filter { it.day != null }.groupBy { it.day!! }
    override suspend fun bracket(): List<BracketRound> {
        runCatching { sync.ensureFresh() }
        return matchDao.all().toBracket().ifEmpty { MockData.bracket }
    }
    override suspend fun match(id: String): Match? {
        val m = all().firstOrNull { it.id == id } ?: return null
        val events = runCatching { of.worldCup().matches.findEventsFor(m.day ?: "", m.teamA, m.teamB) }.getOrDefault(emptyList())
        return m.copy(events = events)
    }
}
