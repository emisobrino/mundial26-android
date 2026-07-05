package com.mundial26.app.data.remote.news
import com.mundial26.app.domain.model.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private val ITEM = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
private fun tag(b: String, n: String) = Regex("<$n[^>]*>(.*?)</$n>", RegexOption.DOT_MATCHES_ALL).find(b)?.groupValues?.get(1)?.replace("<![CDATA[","")?.replace("]]>","")?.trim() ?: ""

/** URL del atributo `url` de `<source ...>` (dominio del medio en Google News). */
private fun sourceUrl(b: String) = Regex("<source[^>]*url=\"([^\"]+)\"").find(b)?.groupValues?.get(1)?.trim() ?: ""

/** Logo del medio a partir de la URL de la fuente (servicio de favicons de Google, 128 px). */
fun sourceLogoUrl(url: String): String {
    val domain = url.substringAfter("://").substringBefore("/").removePrefix("www.")
    return if (domain.isBlank()) "" else "https://www.google.com/s2/favicons?sz=128&domain=$domain"
}

private val MESES = arrayOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/**
 * Convierte un `pubDate` RFC-1123 (ej. "Sun, 28 Jun 2026 18:00:00 GMT") a un texto corto en español:
 * "ahora", "hace N min", "hace N h", "ayer", "hace N d" o una fecha corta ("14 jun") si es más viejo.
 * Si no se puede parsear, devuelve el texto original tal cual.
 */
fun formatNewsTime(pubDate: String, nowEpochMs: Long = System.currentTimeMillis()): String {
    val zdt = runCatching {
        java.time.ZonedDateTime.parse(pubDate.trim(), java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
    }.getOrNull() ?: return pubDate.trim()
    val diff = nowEpochMs - zdt.toInstant().toEpochMilli()
    if (diff < 0) return "ahora"
    val min = diff / 60_000
    val hours = min / 60
    val days = hours / 24
    return when {
        min < 1L -> "ahora"
        min < 60L -> "hace $min min"
        hours < 24L -> "hace $hours h"
        days == 1L -> "ayer"
        days < 7L -> "hace $days d"
        else -> "${zdt.dayOfMonth} ${MESES[zdt.monthValue - 1]}"
    }
}

fun parseRss(xml: String, nowEpochMs: Long = System.currentTimeMillis()): List<NewsItem> = ITEM.findAll(xml).map { it.groupValues[1] }.map { b ->
    val source = tag(b, "source").ifBlank { "Noticias" }
    // Google News añade " - Fuente" al título; lo quitamos (la fuente va aparte).
    val title = tag(b, "title").removeSuffix(" - $source")
    // El <description> de Google News es HTML/links (no un resumen útil) → se omite.
    NewsItem(id = tag(b, "link"), tag = source, colorHex = 0xFF2A398D, time = formatNewsTime(tag(b, "pubDate"), nowEpochMs), title = title, summary = "", body = emptyList(), imageUrl = sourceLogoUrl(sourceUrl(b)))
}.toList()

private const val UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"

class NewsRssSource(private val client: OkHttpClient = OkHttpClient(), private val url: String = "https://news.google.com/rss/search?q=mundial%202026%20futbol&hl=es-419&gl=AR&ceid=AR:es-419") {
    suspend fun headlines(): List<NewsItem> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url).header("User-Agent", UA).build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) parseRss(resp.body?.string().orEmpty()) else emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
