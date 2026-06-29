package com.mundial26.app.data.remote.footballdata
import com.mundial26.app.data.local.db.entities.*
import com.mundial26.app.domain.model.MatchStatus
import java.time.Instant

fun statusOf(s: String): MatchStatus = when (s) {
    "FINISHED" -> MatchStatus.FINISHED
    "IN_PLAY","PAUSED","LIVE" -> MatchStatus.LIVE
    else -> MatchStatus.UPCOMING
}

fun deriveMinute(utcDate: String, status: MatchStatus, nowEpochMs: Long): Int? {
    if (status != MatchStatus.LIVE || utcDate.isBlank()) return null
    return try { (((nowEpochMs - Instant.parse(utcDate).toEpochMilli()) / 60000L).toInt()).coerceIn(1,120) }
    catch (e: Exception) { null }
}

fun FdMatch.toEntity() = MatchEntity(
    id = id.toString(), utcDate = utcDate, status = status, stage = stage,
    grp = group ?: "", homeTla = homeTeam.tla, awayTla = awayTeam.tla,
    scoreHome = score.fullTime.home, scoreAway = score.fullTime.away
)
fun FdMatchesResponse.toEntities() = matches.map { it.toEntity() }

fun FdStandingsResponse.toEntities(): List<StandingEntity> =
    standings.filter { it.type == "TOTAL" }.flatMap { s ->
        s.table.map { r -> StandingEntity(s.group ?: "", r.team.tla, r.position, r.playedGames, r.won, r.draw, r.lost, r.goalsFor, r.goalsAgainst, r.points) }
    }

fun FdTeam.toPlayerEntities(): List<PlayerEntity> =
    squad.map { PlayerEntity(teamTla = tla, name = it.name, position = it.position ?: "", dateOfBirth = it.dateOfBirth ?: "") }
