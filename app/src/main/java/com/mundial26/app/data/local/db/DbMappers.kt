package com.mundial26.app.data.local.db
import com.mundial26.app.data.net.tlaToIso
import com.mundial26.app.data.remote.footballdata.deriveMinute
import com.mundial26.app.data.remote.footballdata.statusOf
import com.mundial26.app.data.local.db.entities.*
import com.mundial26.app.domain.model.*

private fun prettyGroup(stage: String, grp: String): String = when {
    grp.isNotBlank() -> grp.replace("GROUP_", "Grupo ").replace("Group ", "Grupo ")
    stage == "LAST_16" -> "Octavos"
    stage == "QUARTER_FINALS" -> "Cuartos"
    stage == "SEMI_FINALS" -> "Semifinal"
    stage == "THIRD_PLACE" -> "Tercer puesto"
    stage == "FINAL" -> "Final"
    else -> stage.replace("_", " ")
}

fun StandingEntity.toDomain() = GroupRow(code = tla, flag = tlaToIso(tla), pj = pj, g = g, e = e, p = p, gf = gf, gc = gc, pts = pts)

fun List<StandingEntity>.toGroups(): List<Group> =
    groupBy { it.grp }.map { (g, rows) -> Group(g.replace("Group ", "Grupo "), rows.sortedBy { it.position }.map { it.toDomain() }) }

fun MatchEntity.toDomain(nowEpochMs: Long = System.currentTimeMillis()): Match {
    val st = statusOf(status)
    return Match(
        id = id, status = st, minute = deriveMinute(utcDate, st, nowEpochMs),
        time = utcDate.takeIf { it.length >= 16 }?.substring(11, 16), day = utcDate.take(10),
        group = prettyGroup(stage, grp), stadium = "",
        teamA = homeTla, teamB = awayTla, flagA = tlaToIso(homeTla), flagB = tlaToIso(awayTla),
        scoreA = scoreHome, scoreB = scoreAway
    )
}

fun PlayerEntity.toLeader(rank: Int, value: Int) = Leader(rank, name, tlaToIso(teamTla), value)

fun TeamEntity.toDomain() = Team(
    id = tla.lowercase(), name = name, code = tla,
    conf = runCatching { Confederation.valueOf(conf) }.getOrDefault(Confederation.OTHER),
    rank = rank, tag = tag, flag = tlaToIso(tla), styleLabel = styleLabel,
    star = Star(starNum, starName, starPos, starNote, StarStats(0,0,0, starRating, starClub, 0, starValue)),
    pros = emptyList(), cons = emptyList(), form = emptyList()
)
