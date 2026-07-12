package com.mundial26.app.data.remote.youtube

import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeHighlightsSourceTest {
    @Test fun resumen_del_canal_le_gana_al_clip_de_gol() {
        // Caso real ARG-SUI: el primer resultado de ESPN era el clip del gol, no el resumen.
        val candidates = listOf(
            YtCandidate("clip0000001", "ESPN Fans", "¡GOL DE MAC ALLISTER! Argentina 1-0 Suiza"),
            YtCandidate("otro0000001", "Random", "Argentina vs Suiza reacción en vivo"),
            YtCandidate("resumen0001", "ESPN Fans", "Argentina 3-1 Suiza | RESUMEN | Cuartos de final"),
            YtCandidate("resumen0002", "Otro Canal", "Resumen Argentina vs Suiza")
        )
        val ranked = rankCandidates(candidates)
        assertEquals("resumen0001", ranked[0].id)   // resumen + ESPN
        assertEquals("resumen0002", ranked[1].id)   // resumen
        assertEquals("clip0000001", ranked[2].id)   // solo ESPN
    }

    @Test fun parsea_id_canal_y_titulo() {
        val html = """
            {"videoRenderer":{"videoId":"abcdefghijk","title":{"runs":[{"text":"Argentina 3-1 Suiza | RESUMEN"}]},
             "ownerText":{"runs":[{"text":"ESPN Fans"}]}}}
        """.trimIndent()
        val c = parseCandidates(html).single()
        assertEquals("abcdefghijk", c.id)
        assertEquals("ESPN Fans", c.channel)
        assertEquals("Argentina 3-1 Suiza | RESUMEN", c.title)
    }
}
