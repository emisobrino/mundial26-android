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

private val KNOCKOUT_ORDER = listOf("LAST_32", "LAST_16", "QUARTER_FINALS", "SEMI_FINALS", "THIRD_PLACE", "FINAL")
private fun prettyStage(stage: String): String = when (stage) {
    "LAST_32" -> "16avos"
    "LAST_16" -> "Octavos"
    "QUARTER_FINALS" -> "Cuartos"
    "SEMI_FINALS" -> "Semifinales"
    "THIRD_PLACE" -> "Tercer puesto"
    "FINAL" -> "Final"
    else -> stage.replace("_", " ")
}

/** Construye el cuadro de eliminatorias a partir de los partidos de fase final (campo `stage`). */
fun List<MatchEntity>.toBracket(): List<BracketRound> =
    filter { it.stage in KNOCKOUT_ORDER }
        .groupBy { it.stage }
        .toList()
        .sortedBy { (stage, _) -> KNOCKOUT_ORDER.indexOf(stage) }
        .map { (stage, matches) ->
            BracketRound(
                round = prettyStage(stage),
                games = matches.sortedBy { it.utcDate }.map { m ->
                    BracketMatch(
                        a = m.homeTla, b = m.awayTla,
                        flagA = tlaToIso(m.homeTla), flagB = tlaToIso(m.awayTla),
                        sa = m.scoreHome, sb = m.scoreAway,
                        done = statusOf(m.status) == MatchStatus.FINISHED
                    )
                }
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
