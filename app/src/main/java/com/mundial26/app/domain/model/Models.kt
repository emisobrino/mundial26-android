package com.mundial26.app.domain.model

enum class MatchStatus { LIVE, UPCOMING, FINISHED }
enum class Confederation(val id: String, val label: String) {
    UEFA("uefa", "UEFA"), CONMEBOL("conmebol", "CONMEBOL"),
    CONCACAF("concacaf", "CONCACAF"), AFC("afc", "AFC"),
    CAF("caf", "CAF"), OFC("ofc", "OFC"), OTHER("other", "Otra")
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
    val form: List<Char>,        // 'V' | 'E' | 'D'
    val squad: List<Player> = emptyList()   // plantel real (football-data /teams.squad)
)

data class MatchStat(val label: String, val a: Int, val b: Int)
enum class EventKind { GOAL, YELLOW, RED, SUB }
data class MatchEvent(val min: Int, val kind: EventKind, val side: Char, val player: String) // side 'A'|'B'

data class Match(
    val id: String, val status: MatchStatus, val minute: Int?,
    val minuteLabel: String? = null,   // etiqueta base oficial FIFA: "45'", "ET", "90'" (frena en cortes)
    val addedLabel: String? = null,    // descuento del tiempo actual: "+2" (globo azul), null si no hay
    val time: String?,
    val day: String?, val group: String, val stadium: String,
    val teamA: String, val teamB: String,      // códigos (ARG, MEX…)
    val flagA: String, val flagB: String,      // ISO-2
    val scoreA: Int?, val scoreB: Int?,        // marcador real (120' si hubo alargue)
    val penA: Int? = null, val penB: Int? = null,   // definición por penales
    val stats: List<MatchStat> = emptyList(),
    val statsEstimated: Boolean = false,            // true = estimación editorial (sin fuente oficial)
    val events: List<MatchEvent> = emptyList()
)

data class GroupRow(val code: String, val flag: String, val pj: Int, val g: Int, val e: Int, val p: Int, val gf: Int, val gc: Int, val pts: Int) {
    val dg: Int get() = gf - gc
}
data class Group(val name: String, val rows: List<GroupRow>)

data class BracketMatch(
    val a: String, val b: String, val flagA: String, val flagB: String,
    val sa: Int?, val sb: Int?, val done: Boolean,
    val pa: Int? = null, val pb: Int? = null,   // penales, si definieron la llave
    val id: String = "",                        // id del partido (navegar al detalle)
    val day: String? = null, val time: String? = null   // fecha/hora LOCAL del dispositivo
)
data class BracketRound(val round: String, val games: List<BracketMatch>)

data class NewsItem(val id: String, val tag: String, val colorHex: Long, val time: String, val title: String, val summary: String, val body: List<String>)

/** Fila del ranking de líderes del torneo (goleadores / asistidores). */
data class Leader(val rank: Int, val name: String, val flag: String, val value: Int)

/** Club actual y dorsal de un jugador (de football-data /persons). */
data class PlayerClubInfo(val club: String, val shirtNumber: Int?)

/** Jugador con stats acumuladas del torneo (persistido en Room, actualizado por sync). */
data class Player(
    val id: Long,
    val name: String,
    val teamCode: String,
    val position: String,
    val age: Int,
    val goals: Int,
    val assists: Int,
    val captain: Boolean = false
)
