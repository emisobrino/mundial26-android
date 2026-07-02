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

fun FdMatch.toEntity(): MatchEntity {
    // Con penales, football-data suma los penales dentro de fullTime (1-1 + 3-4 pen → "4-5").
    // Guardamos el marcador real de los 120' y los penales aparte.
    val pens = score.penalties.takeIf { score.duration == "PENALTY_SHOOTOUT" }
    val home = if (pens != null) {
        (score.regularTime?.home ?: ((score.fullTime.home ?: 0) - (pens.home ?: 0))) + (score.extraTime?.home ?: 0)
    } else score.fullTime.home
    val away = if (pens != null) {
        (score.regularTime?.away ?: ((score.fullTime.away ?: 0) - (pens.away ?: 0))) + (score.extraTime?.away ?: 0)
    } else score.fullTime.away
    return MatchEntity(
        id = id.toString(), utcDate = utcDate, status = status, stage = stage,
        grp = group ?: "", homeTla = homeTeam.tla, awayTla = awayTeam.tla,
        scoreHome = home, scoreAway = away,
        penHome = pens?.home, penAway = pens?.away
    )
}
fun FdMatchesResponse.toEntities() = matches.map { it.toEntity() }

fun FdStandingsResponse.toEntities(): List<StandingEntity> =
    standings.filter { it.type == "TOTAL" }.flatMap { s ->
        s.table.map { r -> StandingEntity(s.group ?: "", r.team.tla, r.position, r.playedGames, r.won, r.draw, r.lost, r.goalsFor, r.goalsAgainst, r.points) }
    }

fun FdTeam.toPlayerEntities(): List<PlayerEntity> =
    squad.map { PlayerEntity(teamTla = tla, name = it.name, position = it.position ?: "", dateOfBirth = it.dateOfBirth ?: "") }
