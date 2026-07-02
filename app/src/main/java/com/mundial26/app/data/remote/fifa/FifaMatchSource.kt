package com.mundial26.app.data.remote.fifa

import com.mundial26.app.data.net.AppJson
import com.mundial26.app.domain.model.EventKind
import com.mundial26.app.domain.model.MatchEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Partidos oficiales de FIFA para el WC2026 (endpoints públicos sin key):
 * - `api.fifa.com/api/v3/calendar/matches` → los 104 partidos (id FIFA, stage, TLAs, estado, marcador).
 * - `api.fifa.com/api/v3/timelines/{comp}/{season}/{stage}/{match}` → eventos reales
 *   (gol/amarilla/roja/cambio) con minuto oficial y nombre del jugador en la descripción.
 * - `api.fifa.com/api/v3/live/football` → partidos en curso con `MatchTime` (minuto oficial) y marcador.
 * Mapeo con nuestros partidos (ids de football-data) por (día UTC, TLA local, TLA visitante).
 */
private const val COMP = "17"
private const val SEASON = "285023"
private const val UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"

/** Referencia de un partido en el calendario FIFA. */
data class FifaMatchRef(
    val idMatch: String, val idStage: String,
    val day: String,                 // "2026-06-11" (UTC)
    val homeTla: String, val awayTla: String,
    val homeTeamId: String, val awayTeamId: String,
    val status: Int,                 // 0=finalizado, 3=en vivo, 1=próximo
    val scoreHome: Int?, val scoreAway: Int?
)

/** Estado en vivo de un partido según FIFA. */
data class FifaLiveInfo(val homeTla: String, val awayTla: String, val minute: Int?, val scoreHome: Int?, val scoreAway: Int?)

/**
 * Día dado ± 1 (para emparejar un día LOCAL del dispositivo con el día UTC de las
 * fuentes: cerca de medianoche pueden diferir). "2026-07-02" → {01, 02, 03}.
 */
fun daysAround(day: String?): Set<String> {
    val d = runCatching { java.time.LocalDate.parse(day) }.getOrNull() ?: return setOfNotNull(day)
    return setOf(d.minusDays(1).toString(), d.toString(), d.plusDays(1).toString())
}

/** "9'" → 9; "90'+2'" → 92; null/raro → null. */
fun parseFifaMinute(raw: String?): Int? {
    if (raw.isNullOrBlank()) return null
    val m = Regex("(\\d+)'(?:\\+(\\d+)')?").find(raw) ?: return null
    val base = m.groupValues[1].toIntOrNull() ?: return null
    val extra = m.groupValues[2].toIntOrNull() ?: 0
    return base + extra
}

/** Nombre del jugador desde la descripción localizada del evento; "" si no matchea. */
fun extractEventPlayer(type: Int, desc: String): String {
    val rx = when (type) {
        0 -> Regex("de (.+?) \\(")                                    // ¡Goool de Julian QUINONES (México)!
        2 -> Regex("amonesta a (.+?) \\(")                            // El árbitro amonesta a X (Equipo).
        3 -> Regex("expulsión de (.+?) \\(")                          // ¡Tarjeta roja y expulsión de X (Equipo)!
        5 -> Regex("Cambio: (.+?) entra en lugar de (.+?) \\(")        // Cambio: X entra en lugar de Y (Equipo)
        else -> return ""
    }
    val m = rx.find(desc) ?: return ""
    return if (type == 5) "${titleCaseName(m.groupValues[1])} ⇄ ${titleCaseName(m.groupValues[2])}"
    else titleCaseName(m.groupValues[1])
}

fun parseFifaCalendar(json: String): List<FifaMatchRef> =
    AppJson.parseToJsonElement(json).jsonObject["Results"]?.jsonArray?.mapNotNull { el ->
        runCatching {
            val m = el.jsonObject
            fun side(key: String) = m[key]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonObject
            val home = side("Home") ?: return@runCatching null
            val away = side("Away") ?: return@runCatching null
            fun str(o: Map<String, kotlinx.serialization.json.JsonElement>, k: String) =
                o[k]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonPrimitive?.content
            FifaMatchRef(
                idMatch = str(m, "IdMatch") ?: return@runCatching null,
                idStage = str(m, "IdStage") ?: "",
                day = (str(m, "Date") ?: "").take(10),
                homeTla = str(home, "Abbreviation") ?: "", awayTla = str(away, "Abbreviation") ?: "",
                homeTeamId = str(home, "IdTeam") ?: "", awayTeamId = str(away, "IdTeam") ?: "",
                status = str(m, "MatchStatus")?.toIntOrNull() ?: 1,
                scoreHome = str(home, "Score")?.toIntOrNull(), scoreAway = str(away, "Score")?.toIntOrNull()
            )
        }.getOrNull()
    } ?: emptyList()

/** Eventos de dominio del timeline FIFA; `homeTeamId` decide el lado A/B. */
fun parseFifaTimeline(json: String, homeTeamId: String): List<MatchEvent> =
    AppJson.parseToJsonElement(json).jsonObject["Event"]?.jsonArray?.mapNotNull { el ->
        runCatching {
            val e = el.jsonObject
            val kind = when (e["Type"]?.jsonPrimitive?.content?.toIntOrNull()) {
                0 -> EventKind.GOAL
                2 -> EventKind.YELLOW
                3 -> EventKind.RED
                5 -> EventKind.SUB
                else -> return@runCatching null
            }
            val min = parseFifaMinute(e["MatchMinute"]?.jsonPrimitive?.content) ?: return@runCatching null
            val desc = e["EventDescription"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("Description")?.jsonPrimitive?.content ?: ""
            val side = if (e["IdTeam"]?.jsonPrimitive?.content == homeTeamId) 'A' else 'B'
            val type = e["Type"]?.jsonPrimitive?.content?.toIntOrNull() ?: -1
            MatchEvent(min = min, kind = kind, side = side, player = extractEventPlayer(type, desc))
        }.getOrNull()
    } ?: emptyList()

fun parseFifaLive(json: String): List<FifaLiveInfo> =
    AppJson.parseToJsonElement(json).jsonObject["Results"]?.jsonArray?.mapNotNull { el ->
        runCatching {
            val m = el.jsonObject
            fun team(key: String) = m[key]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonObject
            val home = team("HomeTeam") ?: return@runCatching null
            val away = team("AwayTeam") ?: return@runCatching null
            fun str(o: Map<String, kotlinx.serialization.json.JsonElement>, k: String) =
                o[k]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonPrimitive?.content
            FifaLiveInfo(
                homeTla = str(home, "Abbreviation") ?: "", awayTla = str(away, "Abbreviation") ?: "",
                minute = parseFifaMinute(str(m, "MatchTime")),
                scoreHome = str(home, "Score")?.toIntOrNull(), scoreAway = str(away, "Score")?.toIntOrNull()
            )
        }.getOrNull()
    } ?: emptyList()

class FifaMatchSource(private val client: OkHttpClient = OkHttpClient()) {
    private var calendarCache: List<FifaMatchRef> = emptyList()
    private var calendarAt = 0L
    private val calendarTtl = 6 * 60 * 60 * 1000L
    private val timelineCache = mutableMapOf<String, Pair<Long, List<MatchEvent>>>()
    private val timelineTtl = 2 * 60 * 1000L

    private fun get(url: String): String? =
        client.newCall(Request.Builder().url(url).header("User-Agent", UA).build()).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        }

    private suspend fun calendar(): List<FifaMatchRef> = withContext(Dispatchers.IO) {
        if (calendarCache.isNotEmpty() && System.currentTimeMillis() - calendarAt < calendarTtl) return@withContext calendarCache
        val parsed = runCatching {
            get("https://api.fifa.com/api/v3/calendar/matches?idSeason=$SEASON&idCompetition=$COMP&language=es&count=200")
                ?.let(::parseFifaCalendar).orEmpty()
        }.getOrDefault(emptyList())
        if (parsed.isNotEmpty()) { calendarCache = parsed; calendarAt = System.currentTimeMillis() }
        parsed
    }

    /** Busca el partido FIFA por día (±1, por diferencia local↔UTC) + TLAs en cualquier orden. */
    suspend fun findMatch(day: String?, tlaA: String, tlaB: String): FifaMatchRef? {
        val days = daysAround(day)
        return calendar().firstOrNull {
            it.day in days && ((it.homeTla == tlaA && it.awayTla == tlaB) || (it.homeTla == tlaB && it.awayTla == tlaA))
        }
    }

    /** Eventos reales (goles, tarjetas, cambios) del partido; vacío si falla. */
    suspend fun events(ref: FifaMatchRef): List<MatchEvent> = withContext(Dispatchers.IO) {
        timelineCache[ref.idMatch]?.let { (at, ev) ->
            if (System.currentTimeMillis() - at < timelineTtl) return@withContext ev
        }
        val events = runCatching {
            get("https://api.fifa.com/api/v3/timelines/$COMP/$SEASON/${ref.idStage}/${ref.idMatch}?language=es")
                ?.let { parseFifaTimeline(it, ref.homeTeamId) }.orEmpty()
        }.getOrDefault(emptyList())
        if (events.isNotEmpty()) timelineCache[ref.idMatch] = System.currentTimeMillis() to events
        events
    }

    /** Partidos en curso ahora según FIFA (minuto oficial + marcador); vacío si falla. */
    suspend fun liveNow(): List<FifaLiveInfo> = withContext(Dispatchers.IO) {
        runCatching {
            get("https://api.fifa.com/api/v3/live/football?idCompetition=$COMP&idSeason=$SEASON&language=es&count=20")
                ?.let(::parseFifaLive).orEmpty()
        }.getOrDefault(emptyList())
    }
}
