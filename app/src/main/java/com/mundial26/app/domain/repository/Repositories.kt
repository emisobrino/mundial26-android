package com.mundial26.app.domain.repository

import com.mundial26.app.domain.model.*

interface MatchRepository {
    suspend fun liveMatches(): List<Match>
    suspend fun resultsAndUpcoming(): List<Match>
    suspend fun byDay(): Map<String, List<Match>>      // calendario
    suspend fun bracket(): List<BracketRound>
    suspend fun match(id: String): Match?
}

interface TeamRepository {
    suspend fun teams(): List<Team>
    suspend fun featured(): Team
    suspend fun groups(): List<Group>
    suspend fun team(id: String): Team?
    suspend fun topScorers(): List<Leader>
    suspend fun topAssists(): List<Leader>
}

interface NewsRepository {
    suspend fun news(): List<NewsItem>
    suspend fun item(id: String): NewsItem?
}
