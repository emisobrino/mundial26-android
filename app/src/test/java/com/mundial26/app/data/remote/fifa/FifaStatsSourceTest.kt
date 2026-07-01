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
}
