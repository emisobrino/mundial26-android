package com.mundial26.app.data.local.db
import com.mundial26.app.data.net.tlaToIso
import com.mundial26.app.data.remote.footballdata.deriveMinute
import com.mundial26.app.data.remote.footballdata.statusOf
import com.mundial26.app.data.local.db.entities.*
import com.mundial26.app.domain.model.*

private fun prettyGroup(stage: String, grp: String): String = when {
    grp.isNotBlank() -> grp.replace("GROUP_", "Grupo ").replace("Group ", "Grupo ")
    stage == "LAST_32" -> "16avos"
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

fun MatchEntity.toDomain(
    nowEpochMs: Long = System.currentTimeMillis(),
    zone: java.time.ZoneId = java.time.ZoneId.systemDefault()
): Match {
    val st = statusOf(status)
    // Hora y día en la zona horaria del dispositivo (la API da UTC).
    val local = runCatching { java.time.Instant.parse(utcDate).atZone(zone) }.getOrNull()
    return Match(
        id = id, status = st, minute = deriveMinute(utcDate, st, nowEpochMs),
        time = local?.let { "%02d:%02d".format(it.hour, it.minute) }
            ?: utcDate.takeIf { it.length >= 16 }?.substring(11, 16),
        day = local?.toLocalDate()?.toString() ?: utcDate.take(10),
        group = prettyGroup(stage, grp), stadium = "",
        teamA = homeTla, teamB = awayTla, flagA = tlaToIso(homeTla), flagB = tlaToIso(awayTla),
        scoreA = scoreHome, scoreB = scoreAway, penA = penHome, penB = penAway
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
                        done = statusOf(m.status) == MatchStatus.FINISHED,
                        pa = m.penHome, pb = m.penAway
                    )
                }
            )
        }

fun PlayerEntity.toLeader(rank: Int, value: Int) = Leader(rank, name, tlaToIso(teamTla), value)

/** Edad a partir de la fecha de nacimiento "yyyy-MM-dd" (0 si no hay dato). */
fun ageFromDob(dob: String?): Int {
    if (dob.isNullOrBlank()) return 0
    return runCatching {
        java.time.Period.between(java.time.LocalDate.parse(dob.take(10)), java.time.LocalDate.now()).years
    }.getOrDefault(0)
}

fun PlayerEntity.toPlayer() = Player(
    id = id, name = name, teamCode = teamTla, position = position,
    age = ageFromDob(dateOfBirth), goals = goals, assists = assists
)

fun TeamEntity.toDomain() = Team(
    id = tla.lowercase(), name = name, code = tla,
    conf = runCatching { Confederation.valueOf(conf) }.getOrDefault(Confederation.OTHER),
    rank = rank, tag = tag, flag = tlaToIso(tla), styleLabel = styleLabel,
    star = Star(starNum, starName, starPos, starNote, StarStats(0,0,0, starRating, starClub, 0, starValue)),
    pros = emptyList(), cons = emptyList(), form = emptyList()
)
