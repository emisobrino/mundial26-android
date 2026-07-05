package com.mundial26.app.data.remote.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"
private val VIDEO_ID = Regex("\"videoId\":\"([A-Za-z0-9_-]{11})\"")

/**
 * Del HTML de resultados de YouTube extrae el id del primer video, prefiriendo uno
 * cuyo bloque mencione "ESPN". Sin API key (scrapea `ytInitialData`), best-effort.
 */
fun extractPreferredVideoId(html: String): String? {
    var firstId: String? = null
    // Cada video es un "videoRenderer"; recorrerlos en orden y preferir el primero de ESPN.
    for (block in html.split("\"videoRenderer\"").drop(1)) {
        val id = VIDEO_ID.find(block)?.groupValues?.get(1) ?: continue
        if (firstId == null) firstId = id
        if (block.take(1200).contains("ESPN", ignoreCase = true)) return id
    }
    // Fallback: primer videoId del documento (por si cambia el markup de renderers).
    return firstId ?: VIDEO_ID.find(html)?.groupValues?.get(1)
}

/** Busca en YouTube el video de resumen de un partido y devuelve su id (cacheado por consulta). */
class YoutubeHighlightsSource(private val client: OkHttpClient = OkHttpClient()) {
    private val cache = mutableMapOf<String, String?>()

    suspend fun firstVideoId(query: String): String? = withContext(Dispatchers.IO) {
        cache[query]?.let { return@withContext it }
        if (cache.containsKey(query)) return@withContext null
        val id = runCatching {
            // sp=EgIQAQ%3D%3D → filtrar solo videos.
            val url = "https://www.youtube.com/results?search_query=" +
                URLEncoder.encode(query, "UTF-8") + "&sp=EgIQAQ%3D%3D"
            val req = Request.Builder().url(url)
                .header("User-Agent", UA)
                .header("Accept-Language", "es-419,es;q=0.9")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) extractPreferredVideoId(resp.body?.string().orEmpty()) else null
            }
        }.getOrNull()
        cache[query] = id
        id
    }
}
