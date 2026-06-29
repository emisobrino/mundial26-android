package com.mundial26.app.data.remote.news
import com.mundial26.app.domain.model.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private val ITEM = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
private fun tag(b: String, n: String) = Regex("<$n[^>]*>(.*?)</$n>", RegexOption.DOT_MATCHES_ALL).find(b)?.groupValues?.get(1)?.replace("<![CDATA[","")?.replace("]]>","")?.trim() ?: ""

fun parseRss(xml: String): List<NewsItem> = ITEM.findAll(xml).map { it.groupValues[1] }.map { b ->
    val source = tag(b, "source").ifBlank { "Noticias" }
    // Google News añade " - Fuente" al título; lo quitamos (la fuente va aparte).
    val title = tag(b, "title").removeSuffix(" - $source")
    // El <description> de Google News es HTML/links (no un resumen útil) → se omite.
    NewsItem(id = tag(b, "link"), tag = source, colorHex = 0xFF2A398D, time = tag(b, "pubDate"), title = title, summary = "", body = emptyList())
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
