package com.mundial26.app.domain.model

enum class MatchStatus { LIVE, UPCOMING, FINISHED }
enum class Confederation(val id: String, val label: String) {
    UEFA("uefa", "UEFA"), CONMEBOL("conmebol", "CONMEBOL"),
    CONCACAF("concacaf", "CONCACAF"), OTHER("other", "Otra")
}

data class StarStats(
    val pj: Int = 0,
    val goals: Int = 0,
    val assists: Int = 0,
    val rating: Double = 0.0,
    val club: String = "",
    val age: Int = 0,
    val marketValue: String = ""   // ej. "€120M"
)

data class Star(
    val num: Int,
    val name: String,
    val pos: String,
    val note: String,
    val stats: StarStats = StarStats()
)

data class Team(
    val id: String, val name: String, val code: String,
    val conf: Confederation, val rank: Int, val tag: String,
    val flag: String,            // código ISO-2 para flagcdn (ej. "ar")
    val styleLabel: String,
    val star: Star,
    val pros: List<String>, val cons: List<String>,
    val form: List<Char>         // 'V' | 'E' | 'D'
)

data class MatchStat(val label: String, val a: Int, val b: Int)
enum class EventKind { GOAL, YELLOW, RED, SUB }
data class MatchEvent(val min: Int, val kind: EventKind, val side: Char, val player: String) // side 'A'|'B'

data class Match(
    val id: String, val status: MatchStatus, val minute: Int?, val time: String?,
    val day: String?, val group: String, val stadium: String,
    val teamA: String, val teamB: String,      // códigos (ARG, MEX…)
    val flagA: String, val flagB: String,      // ISO-2
    val scoreA: Int?, val scoreB: Int?,
    val stats: List<MatchStat> = emptyList(),
    val events: List<MatchEvent> = emptyList()
)

data class GroupRow(val code: String, val flag: String, val pj: Int, val g: Int, val e: Int, val p: Int, val gf: Int, val gc: Int, val pts: Int) {
    val dg: Int get() = gf - gc
}
data class Group(val name: String, val rows: List<GroupRow>)

data class BracketMatch(val a: String, val b: String, val flagA: String, val flagB: String, val sa: Int?, val sb: Int?, val done: Boolean)
data class BracketRound(val round: String, val games: List<BracketMatch>)

data class NewsItem(val id: String, val tag: String, val colorHex: Long, val time: String, val title: String, val summary: String, val body: List<String>)

/** Fila del ranking de líderes del torneo (goleadores / asistidores). */
data class Leader(val rank: Int, val name: String, val flag: String, val value: Int)
