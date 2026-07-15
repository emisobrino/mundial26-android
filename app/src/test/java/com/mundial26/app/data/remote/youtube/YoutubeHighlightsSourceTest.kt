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

    @Test fun descarta_el_resumen_de_otro_partido_aunque_coincida_el_marcador() {
        // Caso real ENG-ARG: NOR-ENG tambien termino 1-2, asi que el marcador no alcanzaba;
        // el titulo de ese resumen no nombra a Argentina → queda fuera.
        val candidates = listOf(
            YtCandidate("noreng0001", "ESPN Fans", "EL DOBLETE DE BELLINGHAM | Noruega 1-2 Inglaterra | RESUMEN | M99"),
            YtCandidate("engarg0001", "ESPN Fans", "ARGENTINA SE QUEDÓ CON EL MEJOR PARTIDO | Inglaterra 1-2 Argentina | RESUMEN | M102")
        )
        val ranked = rankCandidates(candidates, "Inglaterra", "Argentina", "1-2")
        assertEquals(1, ranked.size)
        assertEquals("engarg0001", ranked[0].id)
    }

    @Test fun sin_resumen_de_espn_queda_el_de_dsports() {
        // Mientras ESPN no sube el resumen, DSports es el respaldo; los clips sueltos no.
        val candidates = listOf(
            YtCandidate("clip000001", "ESPN Fans", "GOLAZO DE ENZO PARA IGUALAR ANTE INGLATERRA | Inglaterra 1-1 Argentina"),
            YtCandidate("dsports001", "DSports", "#ARGENTINA EN LA FINAL!!! | Inglaterra 1-2 Argentina | Resumen")
        )
        val ranked = rankCandidates(candidates, "Inglaterra", "Argentina", "1-2")
        assertEquals("dsports001", ranked[0].id)
    }

    @Test fun cuando_espn_sube_el_suyo_le_gana_al_de_dsports() {
        val candidates = listOf(
            YtCandidate("dsports001", "DSports", "#ARGENTINA EN LA FINAL!!! | Inglaterra 1-2 Argentina | Resumen"),
            YtCandidate("espn000001", "ESPN Fans", "EL MEJOR PARTIDO | Inglaterra 1-2 Argentina | RESUMEN | M102")
        )
        val ranked = rankCandidates(candidates, "Inglaterra", "Argentina", "1-2")
        assertEquals("espn000001", ranked[0].id)
    }

    @Test fun alias_de_pais_en_el_titulo() {
        val candidates = listOf(
            YtCandidate("hol0000001", "ESPN Fans", "Holanda 2-1 Brasil | RESUMEN")
        )
        assertEquals(1, rankCandidates(candidates, "Países Bajos", "Brasil", "2-1").size)
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
