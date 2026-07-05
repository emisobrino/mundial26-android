package com.mundial26.app.data.remote.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"
private val VIDEO_ID = Regex("\"videoId\":\"([A-Za-z0-9_-]{11})\"")
private val OWNER = Regex("\"ownerText\":\\{\"runs\":\\[\\{\"text\":\"(.*?)\"")

/** (videoId, canal) de cada resultado de YouTube, en orden y sin duplicados. */
fun parseCandidates(html: String): List<Pair<String, String>> {
    val out = LinkedHashMap<String, String>()
    for (block in html.split("\"videoRenderer\"").drop(1)) {
        val id = VIDEO_ID.find(block)?.groupValues?.get(1) ?: continue
        val channel = OWNER.find(block)?.groupValues?.get(1) ?: ""
        out.putIfAbsent(id, channel)
    }
    return out.entries.map { it.key to it.value }
}

/**
 * Busca en YouTube el resumen de un partido y devuelve el id de un video **embebible**,
 * priorizando el canal ESPN. Sin API key: scrapea resultados y comprueba la reproducción
 * embebida con el endpoint oEmbed (401 = el dueño no permite embeber). Cacheado por consulta.
 */
class YoutubeHighlightsSource(private val client: OkHttpClient = OkHttpClient()) {
    private val cache = mutableMapOf<String, String?>()

    suspend fun firstVideoId(query: String): String? = withContext(Dispatchers.IO) {
        if (cache.containsKey(query)) return@withContext cache[query]
        val id = runCatching {
            val candidates = parseCandidates(searchHtml(query))
            if (candidates.isEmpty()) return@runCatching null
            // Prioridad: canales ESPN primero (orden estable), luego el resto.
            val ordered = candidates.sortedByDescending { it.second.contains("ESPN", ignoreCase = true) }
            // Devolver el primero que permita embeber; si ninguno, el primero (la UI cae a miniatura).
            ordered.take(6).firstOrNull { isEmbeddable(it.first) }?.first
                ?: ordered.first().first
        }.getOrNull()
        cache[query] = id
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
