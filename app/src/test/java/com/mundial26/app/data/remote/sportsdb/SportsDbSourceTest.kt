package com.mundial26.app.data.remote.sportsdb

import org.junit.Assert.*
import org.junit.Test

class SportsDbSourceTest {

    private val json = """
        {"player":[
          {"strPlayer":"Enzo Fernández","strTeam":"Chelsea","strNationality":"Argentina","strSport":"Soccer"},
          {"strPlayer":"Enzo Fernandez","strTeam":"Boxing Club","strNationality":"Argentina","strSport":"Boxing"},
          {"strPlayer":"Otro Enzo","strTeam":"Racing","strNationality":"Argentina","strSport":"Soccer"}
        ]}
    """.trimIndent()

    @Test fun devuelve_el_club_del_futbolista_con_nombre_coincidente() {
        // Con o sin acento, matchea al futbolista (no al boxeador homónimo).
        assertEquals("Chelsea", parseSportsDbClub(json, "Enzo Fernandez"))
        assertEquals("Chelsea", parseSportsDbClub(json, "Enzo Fernández"))
    }

    @Test fun matchea_por_apellido_si_el_nombre_difiere() {
        assertEquals("Chelsea", parseSportsDbClub(json, "E. Fernández"))
    }

    @Test fun sin_resultados_devuelve_null() {
        assertNull(parseSportsDbClub("""{"player":null}""", "Nadie"))
        assertNull(parseSportsDbClub("basura", "Nadie"))
        assertNull(parseSportsDbClub(json, "Jugador Inexistente"))
    }
}
