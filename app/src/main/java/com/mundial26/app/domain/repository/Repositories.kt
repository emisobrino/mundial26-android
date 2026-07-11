package com.mundial26.app.domain.repository

import com.mundial26.app.domain.model.*

interface MatchRepository {
    suspend fun liveMatches(): List<Match>
    suspend fun resultsAndUpcoming(): List<Match>
    suspend fun byDay(): Map<String, List<Match>>      // calendario
    suspend fun bracket(): List<BracketRound>
    suspend fun match(id: String): Match?
    /** XI titulares confirmados del partido; null si FIFA aún no los publicó. */
    suspend fun lineups(id: String): MatchLineups? = null
    /** Fuerza una resincronización (pull-to-refresh), ignorando el TTL. */
    suspend fun refresh() {}
}

interface TeamRepository {
    suspend fun teams(): List<Team>
    suspend fun featured(): Team
    suspend fun groups(): List<Group>
    suspend fun team(id: String): Team?
    /** Versión rápida de [team]: plantel de la caché local SIN enriquecer con FIFA (para primer render). */
    suspend fun teamBase(id: String): Team? = team(id)
    /** Jugador del plantel por su id de Room (para el detalle de jugador navegable). */
    suspend fun player(playerId: Long): Player? = null
    /** Club actual y dorsal del jugador (red, cacheado); null si no hay dato. */
    suspend fun playerClub(playerId: Long): PlayerClubInfo? = null
    suspend fun topScorers(): List<Leader>
    suspend fun topAssists(): List<Leader>
    /** Ranking de tarjetas amarillas del torneo (FIFA); vacío si no hay fuente. */
    suspend fun topYellowCards(): List<Leader> = emptyList()
    /** Ranking de tarjetas rojas del torneo (FIFA); vacío si no hay fuente. */
    suspend fun topRedCards(): List<Leader> = emptyList()
    /** Fuerza una resincronización (pull-to-refresh), ignorando el TTL. */
    suspend fun refresh() {}
}

interface NewsRepository {
    suspend fun news(): List<NewsItem>
    suspend fun item(id: String): NewsItem?
    /** Invalida la caché de noticias para volver a traer el RSS. */
    suspend fun refresh() {}
}
