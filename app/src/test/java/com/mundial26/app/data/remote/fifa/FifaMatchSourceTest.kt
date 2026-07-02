package com.mundial26.app.data.remote.fifa

import com.mundial26.app.domain.model.EventKind
import org.junit.Assert.*
import org.junit.Test

class FifaMatchSourceTest {

    // ---------- daysAround ----------

    @Test fun dias_vecinos_para_tolerancia_local_utc() {
        assertEquals(setOf("2026-07-01", "2026-07-02", "2026-07-03"), daysAround("2026-07-02"))
        assertEquals(setOf("raro"), daysAround("raro"))
        assertTrue(daysAround(null).isEmpty())
    }

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
           "MatchNumber":74,"Winner":"43911",
           "Home":{"IdTeam":"43911","Score":2,"Abbreviation":"MEX"},
           "Away":{"IdTeam":"43883","Score":0,"Abbreviation":"RSA"}},
          {"IdMatch":"400021542","IdStage":"289291","Date":"2026-07-18T21:00:00Z","MatchStatus":1,
           "MatchNumber":101,"PlaceHolderA":"W97","PlaceHolderB":"W98",
           "Home":null,"Away":null}
        ]}
    """.trimIndent()

    @Test fun parsea_calendario_incluyendo_cruces_sin_definir() {
        val refs = parseFifaCalendar(calendarJson)
        assertEquals(2, refs.size)
        val r = refs[0]
        assertEquals("400021443", r.idMatch)
        assertEquals("2026-06-11", r.day)
        assertEquals("2026-06-11T19:00:00Z", r.utcDate)
        assertEquals("MEX", r.homeTla)
        assertEquals(74, r.matchNumber)
        assertEquals("43911", r.winnerTeamId)
        assertEquals(2, r.scoreHome)

        val tbd = refs[1]
        assertEquals("", tbd.homeTla)
        assertEquals("W97", tbd.placeholderA)
        assertEquals("W98", tbd.placeholderB)
        assertEquals(101, tbd.matchNumber)
    }

    // ---------- resolveCross ----------

    private fun ref(num: Int, home: String = "", away: String = "", homeId: String = "", awayId: String = "", winner: String? = null) =
        FifaMatchRef("m$num", "s", "2026-07-06", "2026-07-06T19:00:00Z", home, away, homeId, awayId, 1, null, null, num, "", "", winner)

    @Test fun cruce_resuelto_al_ganador_o_perdedor_real() {
        val byNum = mapOf(83 to ref(83, "GER", "PAR", "43948", "43928", winner = "43928"))
        assertEquals("PAR", resolveCross("W83", byNum))
        assertEquals("GER", resolveCross("L83", byNum))
    }

    @Test fun cruce_posible_cuando_hay_equipos_sin_ganador() {
        val byNum = mapOf(85 to ref(85, "ESP", "AUT", "1", "2"))
        assertEquals("ESP/AUT", resolveCross("W85", byNum))
    }

    @Test fun cruce_sin_definir_muestra_ganador_del_numero() {
        assertEquals("Gan. 93", resolveCross("W93", emptyMap()))
        assertEquals("Perd. 97", resolveCross("L97", mapOf(97 to ref(97))))
        assertEquals("raro", resolveCross("raro", emptyMap()))
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
