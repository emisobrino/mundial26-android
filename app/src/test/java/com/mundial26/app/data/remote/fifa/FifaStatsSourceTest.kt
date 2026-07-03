package com.mundial26.app.data.remote.fifa

import org.junit.Assert.*
import org.junit.Test

class FifaStatsSourceTest {

    private val playersJson = """
        {
          "430605": [["Assists",4,true],["AttemptAtGoal",6,true],["Goals",1,true]],
          "111111": [["Assists",5,true],["Goals",2,true]],
          "222222": [["Assists",0,true],["Goals",3,true]],
          "333333": [["AttemptAtGoal",2,true]],
          "444444": [["Assists",3,true]]
        }
    """.trimIndent()

    @Test fun extrae_top_asistidores_ordenados() {
        val top = parseTopAssists(playersJson, 10)
        assertEquals(listOf("111111" to 5, "430605" to 4, "444444" to 3), top)
    }

    @Test fun extrae_top_goleadores_con_stat_generica() {
        val top = parseTopStat(playersJson, "Goals", 10)
        assertEquals(listOf("222222" to 3, "111111" to 2, "430605" to 1), top)
    }

    @Test fun respeta_el_limite_y_excluye_cero_o_sin_stat() {
        val top = parseTopAssists(playersJson, 2)
        assertEquals(2, top.size)
        assertTrue(top.none { it.first == "222222" || it.first == "333333" })
    }

    @Test fun json_invalido_no_explota() {
        assertTrue(runCatching { parseTopAssists("basura", 5) }.isFailure || parseTopAssists("{}", 5).isEmpty())
    }

    @Test fun parsea_nombre_y_pais_del_jugador() {
        val json = """{"IdPlayer":"430605","Name":[{"Locale":"es-ES","Description":"BRUNO GUIMARAES"}],"IdCountry":"BRA"}"""
        val ref = parsePlayerRef(json)
        assertEquals("Bruno Guimaraes", ref?.name)
        assertEquals("BRA", ref?.country)
    }

    @Test fun player_json_invalido_devuelve_null() {
        assertNull(parsePlayerRef("no-json"))
        assertNull(parsePlayerRef("{}"))
    }

    @Test fun title_case_de_nombres_fifa() {
        assertEquals("Bruno Guimaraes", titleCaseName("BRUNO GUIMARAES"))
        assertEquals("Martin Odegaard", titleCaseName("  MARTIN ODEGAARD "))
    }

    private val searchJson = """
        {"Results":[
          {"IdPlayer":"1","Name":[{"Description":"Khaled Salah"}],"IdCountry":"KSA",
           "InternationalCaps":3,"Goals":0,"Height":175.0,"Weight":70.0,"PreferredFoot":1},
          {"IdPlayer":"2","Name":[{"Description":"MOHAMED SALAH"}],"IdCountry":"EGY",
           "InternationalCaps":104,"Goals":59,"Height":175.0,"Weight":71.0,"PreferredFoot":2}
        ]}
    """.trimIndent()

    @Test fun busqueda_elige_por_pais_y_nombre() {
        val p = parsePlayerSearch(searchJson, wantedName = "Mohamed Salah", wantedTla = "EGY")
        assertEquals("Mohamed Salah", p?.name)
        assertEquals(104, p?.caps)
        assertEquals(59, p?.intlGoals)
        assertEquals(175, p?.heightCm)
        assertEquals(2, p?.preferredFoot)
    }

    @Test fun busqueda_matchea_por_apellido_si_el_nombre_difiere() {
        val p = parsePlayerSearch(searchJson, wantedName = "M. Salah", wantedTla = "EGY")
        assertEquals("Mohamed Salah", p?.name)
    }

    @Test fun busqueda_sin_candidato_del_pais_devuelve_null() {
        assertNull(parsePlayerSearch(searchJson, "Mohamed Salah", "ARG"))
        assertNull(parsePlayerSearch("basura", "X", "EGY"))
    }
}
