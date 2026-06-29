package com.mundial26.app.data.repository

import com.mundial26.app.data.editorial.EditorialSource
import com.mundial26.app.data.local.db.dao.*
import com.mundial26.app.data.local.db.toDomain
import com.mundial26.app.data.local.db.toGroups
import com.mundial26.app.data.local.db.toLeader
import com.mundial26.app.data.mock.MockData
import com.mundial26.app.data.sync.SyncManager
import com.mundial26.app.domain.model.*
import com.mundial26.app.domain.repository.TeamRepository

class DefaultTeamRepository(
    private val sync: SyncManager, private val teamDao: TeamDao,
    private val playerDao: PlayerDao, private val standingDao: StandingDao
) : TeamRepository {

    private suspend fun fresh() { runCatching { sync.ensureFresh() } }

    override suspend fun teams(): List<Team> {
        fresh()
        val rows = teamDao.all()
        if (rows.isEmpty()) return MockData.teams
        return rows.map { t ->
            val ed = EditorialSource.forCode(t.tla)
            val base = t.toDomain()
            val starP = playerDao.byTeam(t.tla).firstOrNull { it.name.equals(t.starName, true) }
            base.copy(
                pros = ed?.pros ?: emptyList(),
                cons = ed?.cons ?: emptyList(),
                star = base.star.copy(stats = base.star.stats.copy(
                    goals = starP?.goals ?: 0,
                    assists = starP?.assists ?: 0,
                    age = ageFrom(starP?.dateOfBirth)
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

    override suspend fun team(id: String): Team? =
        teams().firstOrNull { it.id == id || it.code.equals(id, true) }

    override suspend fun topScorers(): List<Leader> {
        fresh()
        return playerDao.topScorers(10).mapIndexed { i, p -> p.toLeader(i + 1, p.goals) }.ifEmpty { MockData.topScorers }
    }

    override suspend fun topAssists(): List<Leader> {
        fresh()
        return playerDao.topAssists(10).mapIndexed { i, p -> p.toLeader(i + 1, p.assists) }.ifEmpty { MockData.topAssists }
    }
}

private fun ageFrom(dob: String?): Int {
    if (dob.isNullOrBlank()) return 0
    return runCatching { java.time.Period.between(java.time.LocalDate.parse(dob.take(10)), java.time.LocalDate.now()).years }.getOrDefault(0)
}
