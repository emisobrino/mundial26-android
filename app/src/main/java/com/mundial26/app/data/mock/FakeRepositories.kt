package com.mundial26.app.data.mock
import com.mundial26.app.domain.model.*
import com.mundial26.app.domain.repository.*

class FakeMatchRepository : MatchRepository {
    override suspend fun liveMatches() = MockData.matches.filter { it.status == MatchStatus.LIVE }
    override suspend fun resultsAndUpcoming() = MockData.matches.filter { it.status != MatchStatus.LIVE }
    override suspend fun byDay() = MockData.matches.filter { it.day != null }.groupBy { it.day!! }
    override suspend fun bracket() = MockData.bracket
    override suspend fun match(id: String) = MockData.matches.find { it.id == id }
}
class FakeTeamRepository : TeamRepository {
    override suspend fun teams() = MockData.teams
    override suspend fun featured() = MockData.teams.first { it.id == MockData.featuredTeamId }
    override suspend fun groups() = MockData.groups
    override suspend fun team(id: String) = MockData.teams.find { it.id == id }
    override suspend fun topScorers() = MockData.topScorers
    override suspend fun topAssists() = MockData.topAssists
}
class FakeNewsRepository : NewsRepository {
    override suspend fun news() = MockData.news
    override suspend fun item(id: String) = MockData.news.find { it.id == id }
}
