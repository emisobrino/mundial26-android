package com.mundial26.app.data.remote.sportsdb

import com.mundial26.app.data.net.AppJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * TheSportsDB (free tier, key de prueba "123"): búsqueda de jugador por nombre para
 * obtener el CLUB donde juega (Messi → Inter Miami, Enzo Fernández → Chelsea).
 * Complementa a football-data, que durante el Mundial devuelve la selección como
 * `currentTeam` para varios jugadores.
 */
private const val UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"

private fun norm(s: String): String =
    java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "").lowercase().trim()

/** Extrae el club del resultado de searchplayers: solo fútbol y nombre coincidente. */
fun parseSportsDbClub(json: String, wantedName: String): String? = runCatching {
    val players = AppJson.parseToJsonElement(json).jsonObject["player"]
        ?.takeIf { it !is JsonNull }?.jsonArray ?: return null
    val wanted = norm(wantedName)
    val wantedLast = wanted.substringAfterLast(' ')
    val soccer = players.mapNotNull { el ->
        runCatching {
            val o = el.jsonObject
            fun str(k: String) = o[k]?.takeIf { it !is JsonNull }?.jsonPrimitive?.content
            if (str("strSport") != "Soccer") return@runCatching null
            val name = str("strPlayer") ?: return@runCatching null
            name to str("strTeam").orEmpty()
        }.getOrNull()
    }
    val hit = soccer.firstOrNull { norm(it.first) == wanted }
        ?: soccer.firstOrNull { norm(it.first).substringAfterLast(' ') == wantedLast }
    hit?.second?.takeIf { it.isNotBlank() }
}.getOrNull()

class SportsDbSource(private val client: OkHttpClient = OkHttpClient()) {
    private val cache = mutableMapOf<String, String?>()

    /** Club actual del jugador según TheSportsDB; null si no se encuentra. Cacheado. */
    suspend fun clubOf(name: String): String? = withContext(Dispatchers.IO) {
        val key = norm(name)
        if (cache.containsKey(key)) return@withContext cache[key]
        val club = runCatching {
            val query = java.net.URLEncoder.encode(
                java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
                    .replace(Regex("\\p{Mn}+"), ""), "UTF-8"
            )
            client.newCall(
                Request.Builder()
                    .url("https://www.thesportsdb.com/api/v1/json/123/searchplayers.php?p=$query")
                    .header("User-Agent", UA).build()
            ).execute().use { resp ->
                if (resp.isSuccessful) parseSportsDbClub(resp.body?.string().orEmpty(), name) else null
            }
        }.getOrNull()
        cache[key] = club
        club
    }
}
