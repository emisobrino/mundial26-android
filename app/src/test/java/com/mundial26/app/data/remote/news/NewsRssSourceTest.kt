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
}
