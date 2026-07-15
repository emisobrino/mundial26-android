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

    @Test fun marcador_descarta_el_mismo_cruce_de_otra_competicion() {
        // Caso real FRA-ESP: el 5-4 es de la Nations League 2024; el 0-2 es la semi del Mundial.
        // Ambos son "RESUMEN" de ESPN Fans → sin el marcador empatan y se cuela el equivocado.
        val candidates = listOf(
            YtCandidate("nl00005040", "ESPN Fans", "DOBLETE DE LAMINE | España 5-4 Francia | RESUMEN"),
            YtCandidate("wc00000020", "ESPN Fans", "LA FURIA ROJA A LA FINAL | Francia 0-2 España | RESUMEN | M101")
        )
        val ranked = rankCandidates(candidates, scoreLine = "0-2")
        assertEquals("wc00000020", ranked[0].id)
    }

    @Test fun sin_marcador_cae_al_ranking_por_resumen_y_canal() {
        val candidates = listOf(
            YtCandidate("clip0000001", "ESPN Fans", "¡GOL DE OYARZABAL! Francia 0-1 España"),
            YtCandidate("resumen0001", "ESPN Fans", "Francia 0-2 España | RESUMEN")
        )
        assertEquals("resumen0001", rankCandidates(candidates)[0].id)
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
