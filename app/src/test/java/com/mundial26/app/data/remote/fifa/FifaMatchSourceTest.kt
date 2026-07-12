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

    // ---------- fifaLiveLabel / fifaAddedLabel ----------

    @Test fun tiempo_base_y_descuento_separados() {
        // Primer/segundo tiempo con descuento → base sin "+", globo azul "+N".
        assertEquals("45'", fifaLiveLabel(3, "45'+2'"))
        assertEquals("+2", fifaAddedLabel(3, "45'+2'"))
        assertEquals("90'", fifaLiveLabel(5, "90'+3'"))
        assertEquals("+3", fifaAddedLabel(5, "90'+3'"))
    }

    @Test fun sin_descuento_no_hay_globo_azul() {
        assertEquals("63'", fifaLiveLabel(5, "63'"))
        assertNull(fifaAddedLabel(5, "63'"))
    }

    @Test fun entretiempo_es_ET_sin_descuento() {
        assertEquals("ET", fifaLiveLabel(4, "45'"))
        assertNull(fifaAddedLabel(4, "45'+2'"))
    }

    // ---------- addedPillLabel (globo azul: anunciado fijo, superado por el corriendo) ----------

    @Test fun globo_azul_anunciado_fijo_y_estirado() {
        assertEquals("+9", addedPillLabel(9, "+2"))     // anunciaron 9, van +2 → fijo en +9
        assertEquals("+10", addedPillLabel(9, "+10"))   // se estiró más de lo anunciado → acompaña
        assertEquals("+3", addedPillLabel(null, "+3"))  // sin anunciado → el corriendo
        assertEquals("+9", addedPillLabel(9, null))     // anunciado recién publicado, sin corriendo aún
        assertNull(addedPillLabel(null, null))
    }

    // ---------- runningMinuteLabel (globo rojo corriendo en el descuento) ----------

    @Test fun minuto_corre_durante_el_descuento() {
        assertEquals("47'", runningMinuteLabel("45'", "+2"))
        assertEquals("95'", runningMinuteLabel("90'", "+5"))
        // Sin descuento o etiqueta no numérica: queda igual.
        assertEquals("63'", runningMinuteLabel("63'", null))
        assertEquals("ET", runningMinuteLabel("ET", "+2"))
    }

    // ---------- estimatedAddedLabel (descuento estimado con el reloj) ----------

    @Test fun descuento_estimado_cuando_fifa_no_lo_publica() {
        val ko = "2026-07-09T20:00:00Z"
        fun at(wallMin: Long) = java.time.Instant.parse(ko).toEpochMilli() + wallMin * 60_000
        // 2doT: reloj 108' = 15' de entretiempo + 93' de juego → +3.
        assertEquals("+3", estimatedAddedLabel(5, ko, at(108)))
        // 2doT: reloj 100' = minuto 85 de juego → sin descuento aún.
        assertNull(estimatedAddedLabel(5, ko, at(100)))
        // 1erT: reloj 47' → +2.
        assertEquals("+2", estimatedAddedLabel(3, ko, at(47)))
        // Fuera de 1erT/2doT (fin, alargue, penales) no se estima.
        assertNull(estimatedAddedLabel(10, ko, at(200)))
        assertNull(estimatedAddedLabel(5, null, at(108)))
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
        // Caso real ARG-SUI: expulsión por doble amarilla (Type 4).
        assertEquals("Breel Embolo", extractEventPlayer(4, "¡Breel EMBOLO (Suiza) es expulsado por doble amonestación!"))
    }

    @Test fun timeline_mapea_doble_amarilla_como_roja() {
        val json = """
            {"Event":[
              {"Type":4,"MatchMinute":"72'","IdTeam":"43971","EventDescription":[{"Description":"¡Breel EMBOLO (Suiza) es expulsado por doble amonestación!"}]}
            ]}
        """.trimIndent()
        val evs = parseFifaTimeline(json, homeTeamId = "43922")
        assertEquals(1, evs.size)
        assertEquals(EventKind.RED, evs[0].kind)
        assertEquals(72, evs[0].min)
        assertEquals("Breel Embolo", evs[0].player)
        assertEquals('B', evs[0].side)
    }

    @Test fun extrae_cambio_con_ambos_jugadores() {
        val p = extractEventPlayer(5, "Cambio: Thalente MBATHA entra en lugar de Lyle FOSTER (Sudáfrica)")
        assertEquals("Thalente Mbatha ⇄ Lyle Foster", p)
    }

    @Test fun descripcion_rara_devuelve_vacio() {
        assertEquals("", extractEventPlayer(0, "algo inesperado"))
        assertEquals("", extractEventPlayer(99, "¡Goool de X (Y)!"))
    }

    @Test fun extrae_goleador_de_penal_y_en_contra() {
        assertEquals("Breel Embolo", extractEventPlayer(41, "¡Breel EMBOLO (Suiza) convierte el penal!"))
        assertEquals("Damian Bobadilla", extractEventPlayer(34, "¡Damian BOBADILLA (Paraguay) anota en propia meta."))
    }

    @Test fun timeline_mapea_gol_de_penal_y_en_contra() {
        val json = """
            {"Event":[
              {"Type":41,"MatchMinute":"70'","IdTeam":"43911","EventDescription":[{"Description":"¡Kylian MBAPPE (Francia) convierte el penal!"}]},
              {"Type":34,"MatchMinute":"30'","IdTeam":"43883","EventDescription":[{"Description":"¡Damian BOBADILLA (Paraguay) anota en propia meta."}]}
            ]}
        """.trimIndent()
        val events = parseFifaTimeline(json, homeTeamId = "43911")
        assertEquals(2, events.size)
        assertTrue(events.all { it.kind == EventKind.GOAL })
        assertEquals("Kylian Mbappe", events[0].player)
        assertEquals("Damian Bobadilla", events[1].player)
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

    private fun tbdRef(num: Int, phA: String, phB: String) =
        FifaMatchRef("m$num", "s", "2026-07-10", "2026-07-10T19:00:00Z", "", "", "", "", 1, null, null, num, phA, phB, null)

    @Test fun cruce_recursivo_junta_los_posibles_paises() {
        // El 97 (cuartos) refiere al 93 (octavos TBD) que refiere a 83/84 ya jugados.
        val byNum = mapOf(
            83 to ref(83, "GER", "PAR", "1", "2", winner = "2"),   // ganó PAR
            84 to ref(84, "NED", "MAR", "3", "4", winner = "4"),   // ganó MAR
            93 to tbdRef(93, "W83", "W84"),
            94 to ref(94, "USA", "BEL", "5", "6"),                 // con equipos, sin jugar
            97 to tbdRef(97, "W93", "W94")
        )
        assertEquals("PAR/MAR", resolveCross("W93", byNum))
        assertEquals("PAR/MAR/USA/BEL", resolveCross("W97", byNum))
    }

    @Test fun cruce_con_demasiados_candidatos_cae_al_numero() {
        // Semifinal cuyo arbol junta más de 4 posibles → "Gan. 101".
        val byNum = mapOf(
            93 to ref(93, "AAA", "BBB", "1", "2"),
            94 to ref(94, "CCC", "DDD", "3", "4"),
            95 to ref(95, "EEE", "FFF", "5", "6"),
            96 to ref(96, "GGG", "HHH", "7", "8"),
            99 to tbdRef(99, "W93", "W94"),
            100 to tbdRef(100, "W95", "W96"),
            101 to tbdRef(101, "W99", "W100")
        )
        assertEquals("AAA/BBB/CCC/DDD", resolveCross("W99", byNum))
        assertEquals("Gan. 101", resolveCross("W101", byNum))
    }

    @Test fun ru_del_tercer_puesto_es_perdedor() {
        val byNum = mapOf(101 to ref(101, "ESP", "BRA", "1", "2", winner = "1"))
        assertEquals("BRA", resolveCross("RU101", byNum))
        assertEquals("Perd. 101", resolveCross("RU101", emptyMap()))
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
        assertTrue(live[0].inPlay)   // MatchStatus 3 = en juego
    }

    @Test fun descuento_anunciado_solo_en_el_periodo_que_corresponde() {
        val json = """
            {"Results":[
              {"IdMatch":"1","MatchTime":"90'+2'","MatchStatus":3,"Period":5,"SecondHalfExtraTime":9,
               "HomeTeam":{"IdTeam":"a","Score":1,"Abbreviation":"ARG"},"AwayTeam":{"IdTeam":"b","Score":0,"Abbreviation":"SUI"}},
              {"IdMatch":"2","MatchTime":"45'+1'","MatchStatus":3,"Period":3,"SecondHalfExtraTime":9,
               "HomeTeam":{"IdTeam":"c","Score":0,"Abbreviation":"FRA"},"AwayTeam":{"IdTeam":"d","Score":0,"Abbreviation":"ESP"}}
            ]}
        """.trimIndent()
        val live = parseFifaLive(json)
        // 2doT: anunciado +9 fijo; el corriendo "+2" queda aparte para el globo rojo (92').
        assertEquals(9, live[0].announcedExtra)
        assertEquals("+2", live[0].addedLabel)
        // 1erT: el SecondHalfExtraTime no aplica todavía.
        assertNull(live[1].announcedExtra)
    }

    @Test fun live_distingue_en_juego_de_por_empezar_y_terminado() {
        val json = """
            {"Results":[
              {"IdMatch":"1","MatchTime":"0'","MatchStatus":1,
               "HomeTeam":{"IdTeam":"a","Abbreviation":"POR"},"AwayTeam":{"IdTeam":"b","Abbreviation":"CRO"}},
              {"IdMatch":"2","MatchTime":"101'","MatchStatus":0,
               "HomeTeam":{"IdTeam":"c","Score":2,"Abbreviation":"USA"},"AwayTeam":{"IdTeam":"d","Score":0,"Abbreviation":"BIH"}}
            ]}
        """.trimIndent()
        val live = parseFifaLive(json)
        assertEquals(2, live.size)
        assertFalse(live[0].inPlay)   // por empezar
        assertFalse(live[1].inPlay)   // terminado
    }

    // ---------- parseFifaMatchStats ----------

    private val matchStatsJson = """
        {
          "43969": [["Possession",0.57,false],["AttemptAtGoal",11,false],["AttemptAtGoalOnTarget",5,false],
                    ["Passes",307,false],["PassesCompleted",279,false],
                    ["Corners",6,false],["FoulsFor",5,false],["Offsides",1,false],
                    ["YellowCards",0,false],["RedCards",0,false],["XG",1.1,false]],
          "43934": [["Possession",0.32,false],["AttemptAtGoal",3,false],["AttemptAtGoalOnTarget",1,false],
                    ["Passes",168,false],["PassesCompleted",134,false],
                    ["Corners",2,false],["FoulsFor",3,false],["Offsides",0,false],
                    ["YellowCards",2,false],["RedCards",1,false],["XG",0.03,false]]
        }
    """.trimIndent()

    @Test fun stats_oficiales_del_partido_con_lado_correcto() {
        val stats = parseFifaMatchStats(matchStatsJson, homeTeamId = "43969")
        val byLabel = stats.associate { it.label to (it.a to it.b) }

        // Posesión normalizada a 100 como Google: 0.57/(0.57+0.32) = 64%.
        assertEquals(64 to 36, byLabel["Posesión %"])
        assertEquals(11 to 3, byLabel["Remates"])
        assertEquals(5 to 1, byLabel["Remates al arco"])
        assertEquals(307 to 168, byLabel["Pases"])
        assertEquals(91 to 80, byLabel["Precisión de pases %"])   // 279/307, 134/168
        assertEquals(6 to 2, byLabel["Córners"])
        assertEquals(5 to 3, byLabel["Faltas"])
        assertEquals(1 to 0, byLabel["Posición adelantada"])
        assertEquals(0 to 2, byLabel["Amarillas"])
        assertEquals(0 to 1, byLabel["Rojas"])
        assertEquals(10, stats.size)
    }

    @Test fun stats_sin_equipo_local_devuelve_vacio() {
        assertTrue(parseFifaMatchStats(matchStatsJson, homeTeamId = "otro").isEmpty())
        assertTrue(parseFifaMatchStats("{}", "43969").isEmpty())
        assertTrue(parseFifaMatchStats("basura", "43969").isEmpty())
    }

    @Test fun json_invalido_no_explota() {
        assertTrue(parseFifaCalendar("{}").isEmpty())
        assertTrue(parseFifaTimeline("{}", "x").isEmpty())
        assertTrue(parseFifaLive("{}").isEmpty())
    }
}
