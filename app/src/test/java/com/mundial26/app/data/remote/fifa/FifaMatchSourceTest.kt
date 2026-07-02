package com.mundial26.app.data.remote.fifa

import com.mundial26.app.domain.model.EventKind
import org.junit.Assert.*
import org.junit.Test

class FifaMatchSourceTest {

    // ---------- parseFifaMinute ----------

    @Test fun minuto_simple_y_con_descuento() {
        assertEquals(9, parseFifaMinute("9'"))
        assertEquals(92, parseFifaMinute("90'+2'"))
        assertEquals(50, parseFifaMinute("45'+5'"))
        assertNull(parseFifaMinute(null))
        assertNull(parseFifaMinute(""))
        assertNull(parseFifaMinute("basura"))
    }

    // ---------- extractEventPlayer ----------

    @Test fun extrae_goleador_amonestado_y_expulsado() {
        assertEquals("Julian Quinones", extractEventPlayer(0, "¡Goool de Julian QUINONES (México)!"))
        assertEquals("Teboho Mokoena", extractEventPlayer(2, "El árbitro amonesta a Teboho MOKOENA (Sudáfrica)."))
        assertEquals("Sphephelo Sithole", extractEventPlayer(3, "¡Tarjeta roja y expulsión de Sphephelo SITHOLE (Sudáfrica)!"))
    }

    @Test fun extrae_cambio_con_ambos_jugadores() {
        val p = extractEventPlayer(5, "Cambio: Thalente MBATHA entra en lugar de Lyle FOSTER (Sudáfrica)")
        assertEquals("Thalente Mbatha ⇄ Lyle Foster", p)
    }

    @Test fun descripcion_rara_devuelve_vacio() {
        assertEquals("", extractEventPlayer(0, "algo inesperado"))
        assertEquals("", extractEventPlayer(99, "¡Goool de X (Y)!"))
    }

    // ---------- parseFifaCalendar ----------

    private val calendarJson = """
        {"Results":[
          {"IdMatch":"400021443","IdStage":"289273","Date":"2026-06-11T19:00:00Z","MatchStatus":0,
           "Home":{"IdTeam":"43911","Score":2,"Abbreviation":"MEX"},
           "Away":{"IdTeam":"43883","Score":0,"Abbreviation":"RSA"}},
          {"IdMatch":"400021542","IdStage":"289291","Date":"2026-07-18T21:00:00Z","MatchStatus":1,
           "Home":null,"Away":null}
        ]}
    """.trimIndent()

    @Test fun parsea_calendario_y_omite_partidos_sin_equipos() {
        val refs = parseFifaCalendar(calendarJson)
        assertEquals(1, refs.size)
        val r = refs[0]
        assertEquals("400021443", r.idMatch)
        assertEquals("289273", r.idStage)
        assertEquals("2026-06-11", r.day)
        assertEquals("MEX", r.homeTla)
        assertEquals("RSA", r.awayTla)
        assertEquals("43911", r.homeTeamId)
        assertEquals(0, r.status)
        assertEquals(2, r.scoreHome)
        assertEquals(0, r.scoreAway)
    }

    // ---------- parseFifaTimeline ----------

    private val timelineJson = """
        {"Event":[
          {"Type":79,"MatchMinute":"0'","IdTeam":"43911","EventDescription":[{"Description":"Sorteo"}]},
          {"Type":0,"MatchMinute":"9'","IdTeam":"43911","EventDescription":[{"Description":"¡Goool de Julian QUINONES (México)!"}]},
          {"Type":2,"MatchMinute":"17'","IdTeam":"43883","EventDescription":[{"Description":"El árbitro amonesta a Teboho MOKOENA (Sudáfrica)."}]},
          {"Type":3,"MatchMinute":"90'+2'","IdTeam":"43911","EventDescription":[{"Description":"¡Tarjeta roja y expulsión de Cesar MONTES (México)!"}]},
          {"Type":5,"MatchMinute":"56'","IdTeam":"43883","EventDescription":[{"Description":"Cambio: Thalente MBATHA entra en lugar de Lyle FOSTER (Sudáfrica)"}]}
        ]}
    """.trimIndent()

    @Test fun timeline_mapea_tipos_lado_y_minuto() {
        val events = parseFifaTimeline(timelineJson, homeTeamId = "43911")
        assertEquals(4, events.size)  // el sorteo (79) se descarta

        val goal = events[0]
        assertEquals(EventKind.GOAL, goal.kind)
        assertEquals(9, goal.min)
        assertEquals('A', goal.side)
        assertEquals("Julian Quinones", goal.player)

        val yellow = events[1]
        assertEquals(EventKind.YELLOW, yellow.kind)
        assertEquals('B', yellow.side)

        val red = events[2]
        assertEquals(EventKind.RED, red.kind)
        assertEquals(92, red.min)

        val sub = events[3]
        assertEquals(EventKind.SUB, sub.kind)
        assertEquals("Thalente Mbatha ⇄ Lyle Foster", sub.player)
    }

    // ---------- parseFifaLive ----------

    private val liveJson = """
        {"Results":[
          {"IdMatch":"400021524","MatchTime":"63'","MatchStatus":3,
           "HomeTeam":{"IdTeam":"43921","Score":1,"Abbreviation":"USA"},
           "AwayTeam":{"IdTeam":"44037","Score":0,"Abbreviation":"BIH"}}
        ]}
    """.trimIndent()

    @Test fun live_trae_minuto_oficial_y_marcador() {
        val live = parseFifaLive(liveJson)
        assertEquals(1, live.size)
        assertEquals("USA", live[0].homeTla)
        assertEquals("BIH", live[0].awayTla)
        assertEquals(63, live[0].minute)
        assertEquals(1, live[0].scoreHome)
        assertEquals(0, live[0].scoreAway)
    }

    @Test fun json_invalido_no_explota() {
        assertTrue(parseFifaCalendar("{}").isEmpty())
        assertTrue(parseFifaTimeline("{}", "x").isEmpty())
        assertTrue(parseFifaLive("{}").isEmpty())
    }
}
