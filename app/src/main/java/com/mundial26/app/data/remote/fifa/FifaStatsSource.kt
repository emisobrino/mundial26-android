package com.mundial26.app.data.remote.fifa

import com.mundial26.app.data.net.AppJson
import com.mundial26.app.data.net.tlaToIso
import com.mundial26.app.domain.model.Leader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Estadísticas oficiales de FIFA para el WC2026 (misma fuente que el panel de Google).
 * Endpoints públicos sin key, descubiertos 2026-07-01:
 * - `fdh-api.fifa.com/v1/stats/season/285023/players.json` → stats por jugador (incluye Assists/Goals).
 * - `api.fifa.com/api/v3/players/{id}` → nombre e `IdCountry` (TLA) del jugador.
 * Se usa para el ranking de asistidores (football-data free las trae incompletas).
 */
private const val SEASON_2026 = "285023"
private const val UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"

/** Extrae el top-N de una estadística del players.json de FIFA: lista de (idJugador, valor). */
fun parseTopStat(json: String, statName: String, n: Int): List<Pair<String, Int>> =
    AppJson.parseToJsonElement(json).jsonObject.mapNotNull { (id, stats) ->
        val value = stats.jsonArray.firstOrNull { stat ->
            stat.jsonArray.firstOrNull()?.jsonPrimitive?.content == statName
        }?.jsonArray?.getOrNull(1)?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null
        if (value > 0) id to value else null
    }.sortedByDescending { it.second }.take(n)

/** Top-N de asistidores: lista de (idJugador, asistencias). */
fun parseTopAssists(json: String, n: Int): List<Pair<String, Int>> = parseTopStat(json, "Assists", n)

/** Línea de stats de un jugador del torneo. */
data class PlayerStatLine(val goals: Int, val assists: Int, val yellow: Int, val red: Int)

/** Mapea el players.json completo a idJugador → stats (goles/asistencias/tarjetas). */
fun parseStatsById(json: String): Map<String, PlayerStatLine> =
    AppJson.parseToJsonElement(json).jsonObject.mapValues { (_, stats) ->
        val m = stats.jsonArray.mapNotNull { s ->
            runCatching {
                s.jsonArray[0].jsonPrimitive.content to (s.jsonArray[1].jsonPrimitive.content.toDoubleOrNull()?.toInt() ?: 0)
            }.getOrNull()
        }.toMap()
        PlayerStatLine(
            goals = m["Goals"] ?: 0, assists = m["Assists"] ?: 0,
            yellow = m["YellowCards"] ?: 0, red = m["RedCards"] ?: 0
        )
    }

/** Nombre y país (TLA) de un jugador desde /api/v3/players/{id}. */
data class FifaPlayerRef(val name: String, val country: String)

fun parsePlayerRef(json: String): FifaPlayerRef? = runCatching {
    val obj = AppJson.parseToJsonElement(json).jsonObject
    val name = obj["Name"]?.jsonArray?.firstOrNull()?.jsonObject?.get("Description")?.jsonPrimitive?.content
        ?: return null
    val country = obj["IdCountry"]?.jsonPrimitive?.content ?: ""
    FifaPlayerRef(titleCaseName(name), country)
}.getOrNull()

/** FIFA devuelve los nombres en mayúsculas ("BRUNO GUIMARAES") → "Bruno Guimaraes". */
fun titleCaseName(raw: String): String =
    raw.trim().lowercase().split(" ").filter { it.isNotBlank() }
        .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

/** Perfil de jugador según FIFA (no incluye club ni valor: eso no tiene fuente gratuita). */
data class FifaPlayerProfile(
    val name: String, val country: String,
    val caps: Int? = null,          // partidos con la selección
    val intlGoals: Int? = null,     // goles con la selección (histórico)
    val heightCm: Int? = null, val weightKg: Int? = null,
    val preferredFoot: Int? = null  // 1 = derecho, 2 = izquierdo
)

private fun normName(s: String): String =
    java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "").lowercase().trim()

/**
 * Elige del resultado de `/players/search` al jugador buscado: mismo país (TLA) y
 * nombre igual normalizado (o mismo apellido como fallback).
 */
fun parsePlayerSearch(json: String, wantedName: String, wantedTla: String): FifaPlayerProfile? = runCatching {
    val results = AppJson.parseToJsonElement(json).jsonObject["Results"]?.jsonArray ?: return null
    val wanted = normName(wantedName)
    val wantedLast = wanted.substringAfterLast(' ')
    val candidates = results.mapNotNull { el ->
        runCatching {
            val o = el.jsonObject
            fun str(k: String) = o[k]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonPrimitive?.content
            val name = o["Name"]?.jsonArray?.firstOrNull()?.jsonObject?.get("Description")?.jsonPrimitive?.content
                ?: return@runCatching null
            FifaPlayerProfile(
                name = titleCaseName(name), country = str("IdCountry") ?: "",
                caps = str("InternationalCaps")?.toDoubleOrNull()?.toInt(),
                intlGoals = str("Goals")?.toDoubleOrNull()?.toInt(),
                heightCm = str("Height")?.toDoubleOrNull()?.toInt(),
                weightKg = str("Weight")?.toDoubleOrNull()?.toInt(),
                preferredFoot = str("PreferredFoot")?.toDoubleOrNull()?.toInt()
            )
        }.getOrNull()
    }
    candidates.firstOrNull { it.country == wantedTla && normName(it.name) == wanted }
        ?: candidates.firstOrNull { it.country == wantedTla && normName(it.name).substringAfterLast(' ') == wantedLast }
}.getOrNull()

class FifaStatsSource(private val client: OkHttpClient = OkHttpClient()) {
    private val cache = mutableMapOf<String, Pair<Long, List<Leader>>>()   // statName → (timestamp, ranking)
    private val playerRefs = mutableMapOf<String, FifaPlayerRef>()          // idJugador → nombre/país
    private val ttlMs = 6 * 60 * 60 * 1000L

    private fun get(url: String): String? =
        client.newCall(Request.Builder().url(url).header("User-Agent", UA).build()).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        }

    private fun playerRef(id: String): FifaPlayerRef? = playerRefs[id] ?: run {
        get("https://api.fifa.com/api/v3/players/$id?language=es")?.let(::parsePlayerRef)
            ?.also { playerRefs[id] = it }
    }

    private suspend fun topLeaders(statName: String, n: Int): List<Leader> = withContext(Dispatchers.IO) {
        cache[statName]?.let { (at, leaders) ->
            if (System.currentTimeMillis() - at < ttlMs) return@withContext leaders
        }
        val result = runCatching {
            val statsJson = get("https://fdh-api.fifa.com/v1/stats/season/$SEASON_2026/players.json")
                ?: return@runCatching emptyList()
            parseTopStat(statsJson, statName, n).mapNotNull { (id, value) ->
                val ref = playerRef(id) ?: return@mapNotNull null
                Triple(ref.name, tlaToIso(ref.country), value)
            }.mapIndexed { i, (name, flag, value) -> Leader(rank = i + 1, name = name, flag = flag, value = value) }
        }.getOrDefault(emptyList())
        if (result.isNotEmpty()) cache[statName] = System.currentTimeMillis() to result
        result
    }

    private val profileCache = mutableMapOf<String, FifaPlayerProfile?>()

    /** Perfil FIFA de un jugador por nombre + selección (caps, goles internacionales, físico). */
    suspend fun playerProfile(name: String, tla: String): FifaPlayerProfile? = withContext(Dispatchers.IO) {
        val key = "$tla|$name"
        if (profileCache.containsKey(key)) return@withContext profileCache[key]
        val profile = runCatching {
            // La búsqueda de FIFA no tolera acentos ("Fernández" → 0 resultados).
            val query = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            get("https://api.fifa.com/api/v3/players/search?name=$encoded&language=es&count=50")
                ?.let { parsePlayerSearch(it, name, tla) }
        }.getOrNull()
        profileCache[key] = profile
        profile
    }

    private var statsByIdCache: Pair<Long, Map<String, PlayerStatLine>>? = null

    /** idJugador FIFA → stats del torneo (una request, cacheada 6 h). Vacío si falla. */
    suspend fun statsById(): Map<String, PlayerStatLine> = withContext(Dispatchers.IO) {
        statsByIdCache?.let { (at, m) -> if (System.currentTimeMillis() - at < ttlMs) return@withContext m }
        val result = runCatching {
            get("https://fdh-api.fifa.com/v1/stats/season/$SEASON_2026/players.json")?.let(::parseStatsById).orEmpty()
        }.getOrDefault(emptyMap())
        if (result.isNotEmpty()) statsByIdCache = System.currentTimeMillis() to result
        result
    }

    /** Top-N goleadores oficiales de FIFA; lista vacía si la red falla (el repo degrada a otra fuente). */
    suspend fun topScorers(n: Int = 10): List<Leader> = topLeaders("Goals", n)

    /** Top-N asistidores oficiales de FIFA; lista vacía si la red falla (el repo degrada a otra fuente). */
    suspend fun topAssists(n: Int = 10): List<Leader> = topLeaders("Assists", n)

    /** Top-N amonestados (tarjetas amarillas) oficiales de FIFA. */
    suspend fun topYellowCards(n: Int = 10): List<Leader> = topLeaders("YellowCards", n)

    /** Top-N expulsados (tarjetas rojas) oficiales de FIFA. */
    suspend fun topRedCards(n: Int = 10): List<Leader> = topLeaders("RedCards", n)
}
