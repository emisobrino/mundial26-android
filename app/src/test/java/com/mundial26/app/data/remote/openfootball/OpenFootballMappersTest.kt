package com.mundial26.app.data.remote.openfootball

import com.mundial26.app.domain.model.EventKind
import org.junit.Assert.*
import org.junit.Test

class OpenFootballMappersTest {
    @Test fun goles_a_eventos_por_lado() {
        val m = OfMatch(team1 = "Mexico", team2 = "South Africa",
            goals1 = listOf(OfGoal("Quiñones", 9)), goals2 = listOf(OfGoal("Mokoena", 80)))
        val events = m.toEvents()
        assertEquals(2, events.size)
        assertEquals('A', events[0].side); assertEquals(EventKind.GOAL, events[0].kind)
        assertEquals(9, events[0].min); assertEquals('B', events[1].side)
    }
    @Test fun arregla_encoding_doble() {
        assertEquals("Julián", fixEncoding("JuliÃ¡n"))
    }
}
