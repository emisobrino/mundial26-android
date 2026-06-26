package com.mundial26.app.data.mock
import com.mundial26.app.domain.model.*
import com.mundial26.app.domain.repository.*

class FakeMatchRepository : MatchRepository {
    override fun liveMatches() = MockData.matches.filter { it.status == MatchStatus.LIVE }
    override fun resultsAndUpcoming() = MockData.matches.filter { it.status != MatchStatus.LIVE }
    override fun byDay() = MockData.matches.filter { it.day != null }.groupBy { it.day!! }
    override fun bracket() = MockData.bracket
    override fun match(id: String) = MockData.matches.find { it.id == id }
}
class FakeTeamRepository : TeamRepository {
    override fun teams() = MockData.teams
    override fun featured() = MockData.teams.first { it.id == MockData.featuredTeamId }
    override fun groups() = MockData.groups
    override fun team(id: String) = MockData.teams.find { it.id == id }
}
class FakeNewsRepository : NewsRepository {
    override fun news() = MockData.news
    override fun item(id: String) = MockData.news.find { it.id == id }
}
