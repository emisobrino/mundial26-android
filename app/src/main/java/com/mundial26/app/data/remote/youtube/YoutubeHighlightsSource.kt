package com.mundial26.app.data.remote.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"
private val VIDEO_ID = Regex("\"videoId\":\"([A-Za-z0-9_-]{11})\"")
private val OWNER = Regex("\"ownerText\":\\{\"runs\":\\[\\{\"text\":\"(.*?)\"")
private val TITLE = Regex("\"title\":\\{\"runs\":\\[\\{\"text\":\"(.*?)\"")

data class YtCandidate(val id: String, val channel: String, val title: String)

/** Candidatos (id, canal, título) de cada resultado de YouTube, en orden y sin duplicados. */
fun parseCandidates(html: String): List<YtCandidate> {
    val out = LinkedHashMap<String, YtCandidate>()
    for (block in html.split("\"videoRenderer\"").drop(1)) {
        val id = VIDEO_ID.find(block)?.groupValues?.get(1) ?: continue
        val channel = OWNER.find(block)?.groupValues?.get(1) ?: ""
        val title = TITLE.find(block)?.groupValues?.get(1) ?: ""
        out.putIfAbsent(id, YtCandidate(id, channel, title))
    }
    return out.values.toList()
}

// Competiciones que NO son este Mundial: mismos rivales, otro partido (ej. FRA-ESP 5-4 de
// la Nations League 2024). El marcador ya los distingue; esto es un desempate extra.
private val OTHER_COMP = listOf(
    "nations league", "liga de naciones", "amistoso", "friendly",
    "eurocopa", "eliminatori", "clasificatori", "qualifier"
)

/**
 * Ranking del resumen correcto. Señal decisiva: el MARCADOR real del partido, que el título
 * del resumen casi siempre lleva ("Francia 0-2 España"); así se descarta un mismo cruce de otra
 * competición (FRA-ESP 5-4 Nations League). Luego "resumen" en el título (vs. clips de gol),
 * el canal ESPN, y una penalización si el título menciona otro torneo.
 */
fun rankCandidates(candidates: List<YtCandidate>, scoreLine: String? = null): List<YtCandidate> {
    // Marcador en ambos órdenes: el título puede venir "0-2" o "2-0" según qué equipo liste primero.
    val scores = scoreLine?.split("-")?.map { it.trim() }?.takeIf { it.size == 2 && it.all(String::isNotBlank) }
        ?.let { (a, b) -> setOf("$a-$b", "$b-$a") } ?: emptySet()
    return candidates.sortedByDescending { c ->
        val title = c.title.lowercase()
        val noSpace = title.replace(" ", "")
        (if (scores.any { noSpace.contains(it) }) 8 else 0) +
            (if ("resumen" in title) 2 else 0) +
            (if (c.channel.contains("ESPN", ignoreCase = true)) 1 else 0) +
            (if (OTHER_COMP.any { it in title }) -6 else 0)
    }
}

/**
 * Busca en YouTube el resumen de un partido y devuelve el id de un video **embebible**,
 * priorizando el canal ESPN. Sin API key: scrapea resultados y comprueba la reproducción
 * embebida con el endpoint oEmbed (401 = el dueño no permite embeber). Cacheado por consulta.
 */
class YoutubeHighlightsSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        // Timeouts acotados: la página de resultados de YouTube es grande, damos margen de lectura
        // pero sin colgarnos indefinidamente si la red va lenta.
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(9, TimeUnit.SECONDS)
        .build(),
    // Caché persistente opcional: con prefs, la próxima apertura de la app resuelve los
    // resúmenes sin scraping (el intro se acorta de ~12 s a lo que tarde el resto).
    private val prefs: android.content.SharedPreferences? = null
) {
    // Solo se cachean resultados exitosos: un fallo transitorio no debe deshabilitar el resumen
    // para toda la sesión (se reintenta en la próxima carga/refresh).
    // ponytail: el id cacheado no expira; si ESPN sube un resumen mejor después, queda el primero.
    private val cache = mutableMapOf<String, String>().apply {
        prefs?.all?.forEach { (k, v) -> if (v is String) put(k, v) }
    }

    suspend fun firstVideoId(query: String, scoreLine: String? = null): String? = withContext(Dispatchers.IO) {
        cache[query]?.let { return@withContext it }
        val id = runCatching {
            val candidates = parseCandidates(searchHtml(query))
            if (candidates.isEmpty()) return@runCatching null
            val ordered = rankCandidates(candidates, scoreLine).take(4)
            // Comprobar embebibilidad en paralelo (antes eran hasta 6 peticiones oEmbed en serie).
            val checks = coroutineScope {
                ordered.map { c -> async { c.id to isEmbeddable(c.id) } }.awaitAll()
            }
            // Devolver el primero (en orden de prioridad) que permita embeber; si ninguno, el primero.
            checks.firstOrNull { it.second }?.first ?: ordered.first().id
        }.getOrNull()
        if (id != null) {
            cache[query] = id
            prefs?.edit()?.putString(query, id)?.apply()
        }
        id
    }

    private fun searchHtml(query: String): String {
        // sp=EgIQAQ%3D%3D → filtrar solo videos.
        val url = "https://www.youtube.com/results?search_query=" +
            URLEncoder.encode(query, "UTF-8") + "&sp=EgIQAQ%3D%3D"
        val req = Request.Builder().url(url)
            .header("User-Agent", UA)
            .header("Accept-Language", "es-419,es;q=0.9")
            .build()
        return client.newCall(req).execute().use { if (it.isSuccessful) it.body?.string().orEmpty() else "" }
    }

    /** true si el video permite reproducción embebida (oEmbed responde 200). */
    private fun isEmbeddable(videoId: String): Boolean = runCatching {
        val req = Request.Builder()
            .url("https://www.youtube.com/oembed?url=https://youtu.be/$videoId&format=json")
            .header("User-Agent", UA)
            .build()
        client.newCall(req).execute().use { it.isSuccessful }
    }.getOrDefault(false)
}
