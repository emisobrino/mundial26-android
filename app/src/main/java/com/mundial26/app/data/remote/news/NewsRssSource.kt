package com.mundial26.app.data.remote.news
import com.mundial26.app.domain.model.NewsItem
import okhttp3.OkHttpClient
import okhttp3.Request

private val ITEM = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
private fun tag(b: String, n: String) = Regex("<$n[^>]*>(.*?)</$n>", RegexOption.DOT_MATCHES_ALL).find(b)?.groupValues?.get(1)?.replace("<![CDATA[","")?.replace("]]>","")?.trim() ?: ""

fun parseRss(xml: String): List<NewsItem> = ITEM.findAll(xml).map { it.groupValues[1] }.map { b ->
    NewsItem(id = tag(b,"link"), tag = tag(b,"source").ifBlank { "Noticias" }, colorHex = 0xFF2A398D, time = tag(b,"pubDate"), title = tag(b,"title"), summary = tag(b,"description"), body = emptyList())
}.toList()

class NewsRssSource(private val client: OkHttpClient = OkHttpClient(), private val url: String = "https://news.google.com/rss/search?q=mundial%202026%20futbol&hl=es-419&gl=AR&ceid=AR:es-419") {
    suspend fun headlines(): List<NewsItem> = try {
        client.newCall(Request.Builder().url(url).build()).execute().use { if (it.isSuccessful) parseRss(it.body?.string().orEmpty()) else emptyList() }
    } catch (e: Exception) { emptyList() }
}
