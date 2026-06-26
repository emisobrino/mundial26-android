package com.mundial26.app.domain.repository

import com.mundial26.app.domain.model.*

interface MatchRepository {
    fun liveMatches(): List<Match>
    fun resultsAndUpcoming(): List<Match>
    fun byDay(): Map<String, List<Match>>      // calendario
    fun bracket(): List<BracketRound>
    fun match(id: String): Match?
}

interface TeamRepository {
    fun teams(): List<Team>
    fun featured(): Team
    fun groups(): List<Group>
    fun team(id: String): Team?
}

interface NewsRepository {
    fun news(): List<NewsItem>
    fun item(id: String): NewsItem?
}
