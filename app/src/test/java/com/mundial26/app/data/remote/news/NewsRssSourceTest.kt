package com.mundial26.app.data.remote.news
import org.junit.Assert.*
import org.junit.Test

class NewsRssSourceTest {
    private val xml = """
        <rss><channel>
          <item>
            <title><![CDATA[Argentina venció a Jordania]]></title>
            <link>https://example.com/n1</link>
            <pubDate>Sun, 28 Jun 2026 18:00:00 GMT</pubDate>
            <source url="x">La Nación</source>
            <description><![CDATA[Resumen del partido]]></description>
          </item>
        </channel></rss>
    """.trimIndent()

    @Test fun parsea_items_rss() {
        val items = parseRss(xml)
        assertEquals(1, items.size)
        assertEquals("Argentina venció a Jordania", items[0].title)
        assertEquals("La Nación", items[0].tag)
        assertEquals("https://example.com/n1", items[0].id)
        assertTrue(items[0].body.isEmpty())
    }

    private fun epoch(rfc: String): Long =
        java.time.ZonedDateTime.parse(rfc, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
            .toInstant().toEpochMilli()

    @Test fun formatea_pubDate_relativo() {
        val now = epoch("Sun, 28 Jun 2026 20:00:00 GMT")
        assertEquals("hace 2 h", formatNewsTime("Sun, 28 Jun 2026 18:00:00 GMT", now))
        assertEquals("hace 5 min", formatNewsTime("Sun, 28 Jun 2026 19:55:00 GMT", now))
        assertEquals("ahora", formatNewsTime("Sun, 28 Jun 2026 19:59:40 GMT", now))
        assertEquals("ayer", formatNewsTime("Sat, 27 Jun 2026 20:00:00 GMT", now))
        assertEquals("hace 3 d", formatNewsTime("Thu, 25 Jun 2026 20:00:00 GMT", now))
    }

    @Test fun formatea_pubDate_fecha_corta_si_es_viejo() {
        val now = epoch("Sun, 28 Jun 2026 20:00:00 GMT")
        assertEquals("14 jun", formatNewsTime("Sun, 14 Jun 2026 12:00:00 GMT", now))
    }

    @Test fun pubDate_invalido_se_devuelve_igual() {
        assertEquals("", formatNewsTime("", 0L))
        assertEquals("basura", formatNewsTime("basura", 0L))
    }

    @Test fun parseRss_usa_el_tiempo_relativo() {
        val now = epoch("Sun, 28 Jun 2026 20:00:00 GMT")
        assertEquals("hace 2 h", parseRss(xml, now).first().time)
    }
}
