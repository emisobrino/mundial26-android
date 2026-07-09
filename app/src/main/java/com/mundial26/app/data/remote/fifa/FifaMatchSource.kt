package com.mundial26.app.data.remote.fifa

import com.mundial26.app.data.net.AppJson
import com.mundial26.app.domain.model.EventKind
import com.mundial26.app.domain.model.MatchEvent
import com.mundial26.app.domain.model.MatchStat
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
    val utcDate: String,             // "2026-06-11T19:00:00Z"
    val homeTla: String, val awayTla: String,        // "" si el cruce aún no está definido
    val homeTeamId: String, val awayTeamId: String,
    val status: Int,                 // 0=finalizado, 3=en vivo, 1=próximo
    val scoreHome: Int?, val scoreAway: Int?,
    val matchNumber: Int? = null,
    val placeholderA: String = "", val placeholderB: String = "",   // "W83" = ganador del 83, "L97" = perdedor
    val winnerTeamId: String? = null,
    val idIfes: String = "",         // id interno del data hub (fdh-api) para stats del partido
    val stadium: String = "", val city: String = ""   // sede (localizada es-ES)
)

/** "W83" = ganador del 83; "L97"/"RU101" = perdedor (FIFA usa RU en el tercer puesto). */
private val CROSS_RX = Regex("(W|L|RU)(\\d+)")

/**
 * Resuelve un placeholder de cruce a un texto mostrable:
 * - partido referido terminado → TLA del ganador/perdedor real;
 * - con equipos pero sin jugar → "TLA1/TLA2";
 * - sin equipos → sigue la cadena de placeholders (recursivo) y muestra los
 *   posibles países ("PAR/NOR/USA/BEL"); con más de 4 candidatos, "Gan. N"/"Perd. N".
 */
fun resolveCross(placeholder: String, byNumber: Map<Int, FifaMatchRef>): String {
    val m = CROSS_RX.matchEntire(placeholder.trim()) ?: return placeholder
    val wantWinner = m.groupValues[1] == "W"
    val num = m.groupValues[2].toInt()
    val candidates = crossCandidates(num, wantWinner, byNumber, depth = 0)?.distinct()
    return if (candidates != null && candidates.isNotEmpty() && candidates.size <= 4) {
        candidates.joinToString("/")
    } else if (wantWinner) "Gan. $num" else "Perd. $num"
}

/** Posibles TLAs que pueden salir del partido `num` (null = imposible de determinar). */
private fun crossCandidates(num: Int, wantWinner: Boolean, byNumber: Map<Int, FifaMatchRef>, depth: Int): List<String>? {
    val t = byNumber[num] ?: return null
    if (t.homeTla.isNotBlank() && t.awayTla.isNotBlank()) {
        val winner = t.winnerTeamId
        if (winner != null) {
            val homeWon = winner == t.homeTeamId
            return listOf(if (homeWon == wantWinner) t.homeTla else t.awayTla)
        }
        return listOf(t.homeTla, t.awayTla)
    }
    if (depth >= 2) return null
    // El partido referido tampoco está definido: unir los candidatos de sus propios cruces
    // (el ganador o perdedor puede ser cualquiera de los que lleguen).
    val a = subCandidates(t.placeholderA, byNumber, depth + 1) ?: return null
    val b = subCandidates(t.placeholderB, byNumber, depth + 1) ?: return null
    return a + b
}

private fun subCandidates(placeholder: String, byNumber: Map<Int, FifaMatchRef>, depth: Int): List<String>? {
    val m = CROSS_RX.matchEntire(placeholder.trim()) ?: return null
    return crossCandidates(m.groupValues[2].toInt(), m.groupValues[1] == "W", byNumber, depth)
}

/** Estado en vivo de un partido según FIFA. El feed /live incluye también recién terminados y por empezar. */
data class FifaLiveInfo(
    val homeTla: String, val awayTla: String,
    val minute: Int?, val scoreHome: Int?, val scoreAway: Int?,
    val status: Int = 1,                // 3 = en juego, 0 = terminado, 1 = por empezar
    val period: Int = 0,                // 3=1erT, 4=ET, 5=2doT, 10=fin, 11/13=alargue, 12=descanso alargue
    val timeLabel: String = "",         // base: "45'", "ET", "90'" (frena en cortes)
    val addedLabel: String? = null      // descuento actual: "+2" (globo azul), null si no hay
) {
    val inPlay: Boolean get() = status == 3
}

/**
 * Etiqueta BASE de tiempo en vivo (globo rojo) a partir del período y el MatchTime
 * oficial de FIFA. FIFA congela el MatchTime en los cortes, solo hay que rotular
 * los descansos y quitar el descuento (que va en el globo azul aparte).
 */
fun fifaLiveLabel(period: Int, matchTime: String?): String = when (period) {
    4 -> "ET"           // entretiempo
    12 -> "Descanso"    // descanso del alargue
    14, 15 -> "Penales"
    else -> (matchTime ?: "").substringBefore("+").trim()   // "45'+2'" → "45'"
}

/** Descuento del tiempo actual (globo azul): "45'+2'" → "+2"; sin descuento → null. */
fun fifaAddedLabel(period: Int, matchTime: String?): String? {
    if (period == 4 || period == 12 || period == 14 || period == 15) return null
    val extra = Regex("\\+(\\d+)'?").find(matchTime ?: "")?.groupValues?.get(1) ?: return null
    return "+$extra"
}

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
        41, 34 -> Regex("¡(.+?) \\(")                                 // ¡Breel EMBOLO (Suiza) convierte el penal! / anota en propia meta.
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
            // Home/Away pueden venir null en cruces de eliminatorias aún no definidos.
            val home = side("Home")
            val away = side("Away")
            fun str(o: Map<String, kotlinx.serialization.json.JsonElement>?, k: String) =
                o?.get(k)?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonPrimitive?.content
            val date = str(m, "Date") ?: ""
            // Nombre localizado: campos tipo [{"Locale":"es-ES","Description":"Estadio X"}]
            fun localized(o: Map<String, kotlinx.serialization.json.JsonElement>?, k: String): String =
                o?.get(k)?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("Description")?.jsonPrimitive?.content ?: ""
            val stadiumObj = side("Stadium")
            FifaMatchRef(
                idMatch = str(m, "IdMatch") ?: return@runCatching null,
                idStage = str(m, "IdStage") ?: "",
                day = date.take(10), utcDate = date,
                homeTla = str(home, "Abbreviation") ?: "", awayTla = str(away, "Abbreviation") ?: "",
                homeTeamId = str(home, "IdTeam") ?: "", awayTeamId = str(away, "IdTeam") ?: "",
                status = str(m, "MatchStatus")?.toIntOrNull() ?: 1,
                scoreHome = str(home, "Score")?.toIntOrNull(), scoreAway = str(away, "Score")?.toIntOrNull(),
                matchNumber = str(m, "MatchNumber")?.toIntOrNull(),
                placeholderA = str(m, "PlaceHolderA") ?: "", placeholderB = str(m, "PlaceHolderB") ?: "",
                winnerTeamId = str(m, "Winner"),
                idIfes = str(side("Properties"), "IdIFES") ?: "",
                stadium = localized(stadiumObj, "Name"), city = localized(stadiumObj, "CityName")
            )
        }.getOrNull()
    } ?: emptyList()

/** Eventos de dominio del timeline FIFA; `homeTeamId` decide el lado A/B. */
fun parseFifaTimeline(json: String, homeTeamId: String): List<MatchEvent> =
    AppJson.parseToJsonElement(json).jsonObject["Event"]?.jsonArray?.mapNotNull { el ->
        runCatching {
            val e = el.jsonObject
            val kind = when (e["Type"]?.jsonPrimitive?.content?.toIntOrNull()) {
                0, 34, 41 -> EventKind.GOAL   // gol normal, en propia puerta, de penal
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

/**
 * Stats oficiales de un partido desde `fdh-api.../stats/match/{idIfes}/teams.json`:
 * `{ "<idEquipo>": [["Possession",0.57,bool], ...], ... }`. El lado A es `homeTeamId`.
 * Mismo set y criterio que el panel de Google (que usa esta fuente): la posesión
 * se normaliza a 100 (FIFA descuenta el tiempo disputado) y la precisión de pases
 * se deriva de PassesCompleted/Passes.
 */
fun parseFifaMatchStats(json: String, homeTeamId: String): List<MatchStat> = runCatching {
    val obj = AppJson.parseToJsonElement(json).jsonObject
    fun statsOf(id: String): Map<String, Double>? = obj[id]?.jsonArray?.mapNotNull { s ->
        runCatching {
            s.jsonArray[0].jsonPrimitive.content to (s.jsonArray[1].jsonPrimitive.content.toDoubleOrNull() ?: return@runCatching null)
        }.getOrNull()
    }?.toMap()

    val home = statsOf(homeTeamId) ?: return@runCatching emptyList()
    val awayId = obj.keys.firstOrNull { it != homeTeamId } ?: return@runCatching emptyList()
    val away = statsOf(awayId) ?: return@runCatching emptyList()

    val list = mutableListOf<MatchStat>()
    val posA = home["Possession"]
    val posB = away["Possession"]
    if (posA != null && posB != null && posA + posB > 0) {
        val a = Math.round(posA / (posA + posB) * 100).toInt()
        list += MatchStat("Posesión %", a, 100 - a)
    }
    fun add(label: String, key: String) {
        val a = home[key] ?: return
        val b = away[key] ?: return
        list += MatchStat(label, Math.round(a).toInt(), Math.round(b).toInt())
    }
    add("Remates", "AttemptAtGoal")
    add("Remates al arco", "AttemptAtGoalOnTarget")
    add("Pases", "Passes")
    val pcA = home["PassesCompleted"]; val ptA = home["Passes"]
    val pcB = away["PassesCompleted"]; val ptB = away["Passes"]
    if (pcA != null && pcB != null && ptA != null && ptB != null && ptA > 0 && ptB > 0) {
        list += MatchStat(
            "Precisión de pases %",
            Math.round(pcA / ptA * 100).toInt(),
            Math.round(pcB / ptB * 100).toInt()
        )
    }
    add("Córners", "Corners")
    add("Faltas", "FoulsFor")
    add("Posición adelantada", "Offsides")
    add("Amarillas", "YellowCards")
    add("Rojas", "RedCards")
    list
}.getOrDefault(emptyList())

/** Jugador de una alineación FIFA: id (= key de stats), nombre, capitán, dorsal y si es titular. */
data class FifaLineupPlayer(val fifaId: String, val name: String, val captain: Boolean, val shirt: Int?, val starter: Boolean = true)

/** Extrae la alineación (Players) del equipo con TLA [tla] del detalle live de un partido. */
fun parseTeamLineup(json: String, tla: String): List<FifaLineupPlayer> = runCatching {
    val obj = AppJson.parseToJsonElement(json).jsonObject
    fun team(key: String) = obj[key]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonObject
    val home = team("HomeTeam"); val away = team("AwayTeam")
    fun abbr(o: kotlinx.serialization.json.JsonObject?) =
        o?.get("Abbreviation")?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonPrimitive?.content
    val side = when (tla) { abbr(home) -> home; abbr(away) -> away; else -> null } ?: return@runCatching emptyList()
    side["Players"]?.jsonArray?.mapNotNull { el ->
        runCatching {
            val p = el.jsonObject
            val id = p["IdPlayer"]?.jsonPrimitive?.content ?: return@runCatching null
            val name = p["PlayerName"]?.jsonArray?.firstOrNull()?.jsonObject?.get("Description")?.jsonPrimitive?.content
                ?: return@runCatching null
            FifaLineupPlayer(
                fifaId = id, name = titleCaseName(name),
                captain = p["Captain"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
                shirt = p["ShirtNumber"]?.jsonPrimitive?.content?.toIntOrNull(),
                // Status 1 = titular en el feed live de FIFA; sin campo → titular (no rompe el roster).
                starter = p["Status"]?.jsonPrimitive?.content?.toIntOrNull()?.let { it == 1 } ?: true
            )
        }.getOrNull()
    } ?: emptyList()
}.getOrDefault(emptyList())

fun parseFifaLive(json: String): List<FifaLiveInfo> =
    AppJson.parseToJsonElement(json).jsonObject["Results"]?.jsonArray?.mapNotNull { el ->
        runCatching {
            val m = el.jsonObject
            fun team(key: String) = m[key]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonObject
            val home = team("HomeTeam") ?: return@runCatching null
            val away = team("AwayTeam") ?: return@runCatching null
            fun str(o: Map<String, kotlinx.serialization.json.JsonElement>, k: String) =
                o[k]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonPrimitive?.content
            val period = str(m, "Period")?.toIntOrNull() ?: 0
            val matchTime = str(m, "MatchTime")
            FifaLiveInfo(
                homeTla = str(home, "Abbreviation") ?: "", awayTla = str(away, "Abbreviation") ?: "",
                minute = parseFifaMinute(matchTime),
                scoreHome = str(home, "Score")?.toIntOrNull(), scoreAway = str(away, "Score")?.toIntOrNull(),
                status = str(m, "MatchStatus")?.toIntOrNull() ?: 1,
                period = period, timeLabel = fifaLiveLabel(period, matchTime),
                addedLabel = fifaAddedLabel(period, matchTime)
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
        if (tlaA.isBlank() || tlaB.isBlank()) return null
        val days = daysAround(day)
        return calendar().firstOrNull {
            it.day in days && ((it.homeTla == tlaA && it.awayTla == tlaB) || (it.homeTla == tlaB && it.awayTla == tlaA))
        }
    }

    /**
     * Cruce para un partido sin equipos en nuestra DB, por fecha-hora UTC exacta.
     * Cada lado se resuelve por separado: FIFA puede tener el partido totalmente
     * definido, parcialmente ("SUI vs W87") o solo con placeholders.
     */
    suspend fun crossesFor(utcDate: String): Pair<String, String>? {
        val cal = calendar()
        val entry = cal.firstOrNull { it.utcDate == utcDate } ?: return null
        val byNumber = cal.mapNotNull { r -> r.matchNumber?.let { it to r } }.toMap()
        val a = entry.homeTla.ifBlank {
            if (entry.placeholderA.isBlank()) return null else resolveCross(entry.placeholderA, byNumber)
        }
        val b = entry.awayTla.ifBlank {
            if (entry.placeholderB.isBlank()) return null else resolveCross(entry.placeholderB, byNumber)
        }
        return a to b
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

    private val statsCache = mutableMapOf<String, Pair<Long, List<MatchStat>>>()
    private val statsTtl = 60 * 1000L

    /** Stats oficiales del partido (posesión, remates, córners…); vacío si aún no hay datos. Caché 60 s. */
    suspend fun matchStats(ref: FifaMatchRef): List<MatchStat> = withContext(Dispatchers.IO) {
        if (ref.idIfes.isBlank()) return@withContext emptyList()
        statsCache[ref.idIfes]?.let { (at, s) -> if (System.currentTimeMillis() - at < statsTtl) return@withContext s }
        val stats = runCatching {
            get("https://fdh-api.fifa.com/v1/stats/match/${ref.idIfes}/teams.json")
                ?.let { parseFifaMatchStats(it, ref.homeTeamId) }.orEmpty()
        }.getOrDefault(emptyList())
        if (stats.isNotEmpty()) statsCache[ref.idIfes] = System.currentTimeMillis() to stats
        stats
    }

    private val lineupCache = mutableMapOf<String, List<FifaLineupPlayer>>()

    /** XI titular de [tla] en el partido [ref]; vacío si FIFA aún no publicó la alineación. */
    suspend fun matchLineup(ref: FifaMatchRef, tla: String): List<FifaLineupPlayer> = withContext(Dispatchers.IO) {
        val all = lineupOf(ref, tla)
        // ponytail: si el feed no trae Status (todos "titulares"), recortamos a los primeros 11
        // (FIFA lista titulares primero); suficiente hasta ver un caso real distinto.
        all.filter { it.starter }.take(11).ifEmpty { all.take(11) }
    }

    /** Alineación de un partido concreto del equipo [tla] (capitán, dorsal, id para stats). Cacheada por partido. */
    private fun lineupOf(ref: FifaMatchRef, tla: String): List<FifaLineupPlayer> =
        lineupCache["${ref.idMatch}|$tla"] ?: runCatching {
            get("https://api.fifa.com/api/v3/live/football/$COMP/$SEASON/${ref.idStage}/${ref.idMatch}?language=es")
                ?.let { parseTeamLineup(it, tla) }.orEmpty()
        }.getOrDefault(emptyList()).also { if (it.isNotEmpty()) lineupCache["${ref.idMatch}|$tla"] = it }

    private suspend fun playedMatches(tla: String): List<FifaMatchRef> = calendar()
        .filter { (it.homeTla == tla || it.awayTla == tla) && it.status != 1 && it.idMatch.isNotBlank() }
        .sortedBy { it.utcDate }

    /** Alineación más reciente del equipo [tla] (capitán, dorsal, id para stats). */
    suspend fun teamLineup(tla: String): List<FifaLineupPlayer> = withContext(Dispatchers.IO) {
        val ref = playedMatches(tla).lastOrNull() ?: return@withContext emptyList()
        lineupOf(ref, tla)
    }

    private val rosterCache = mutableMapOf<String, List<FifaLineupPlayer>>()

    /**
     * Unión de jugadores convocados por [tla] en TODOS sus partidos jugados. Sirve para
     * mapear nombre→id FIFA de todo el plantel que participó (no solo del último XI), de
     * modo que goles/asistencias del torneo se atribuyan también a rotados/suplentes.
     * Capitán = SOLO el del último partido jugado (si otro llevó la cinta un día que el
     * titular descansó, no deben quedar dos capitanes). Cacheada por sesión.
     */
    suspend fun teamRoster(tla: String): List<FifaLineupPlayer> = withContext(Dispatchers.IO) {
        rosterCache[tla]?.let { return@withContext it }
        val byId = LinkedHashMap<String, FifaLineupPlayer>()
        var captainId: String? = null
        for (ref in playedMatches(tla)) {
            val lineup = lineupOf(ref, tla)
            for (pl in lineup) byId[pl.fifaId] = pl
            // El último partido con capitán conocido define al capitán vigente.
            lineup.firstOrNull { it.captain }?.let { captainId = it.fifaId }
        }
        val roster = byId.values.map { it.copy(captain = it.fifaId == captainId) }
        if (roster.isNotEmpty()) rosterCache[tla] = roster
        roster
    }

    private var liveCache: Pair<Long, List<FifaLiveInfo>>? = null
    private val liveTtl = 12 * 1000L   // corto: un gol debe notificarse/verse lo antes posible

    /** Partidos en curso ahora según FIFA (minuto oficial + marcador); vacío si falla. */
    suspend fun liveNow(): List<FifaLiveInfo> = withContext(Dispatchers.IO) {
        liveCache?.let { (at, l) -> if (System.currentTimeMillis() - at < liveTtl) return@withContext l }
        val result = runCatching {
            get("https://api.fifa.com/api/v3/live/football?idCompetition=$COMP&idSeason=$SEASON&language=es&count=20")
                ?.let(::parseFifaLive).orEmpty()
        }.getOrDefault(emptyList())
        liveCache = System.currentTimeMillis() to result
        result
    }
}
