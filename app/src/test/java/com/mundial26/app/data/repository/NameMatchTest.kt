package com.mundial26.app.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NameMatchTest {
    @Test fun viniciusJrMatchesJunior() {
        assertTrue(nameMatches("Vinicius Junior", "Vinícius Jr."))
    }

    @Test fun messiInitialMatches() {
        assertTrue(nameMatches("Lionel Messi", "L. Messi"))
    }

    @Test fun leaoMatches() {
        assertTrue(nameMatches("Rafael Leão", "Rafael Leao"))
    }

    @Test fun soloApellidoMatchea() {
        assertTrue(nameMatches("Lionel Messi", "Messi"))
    }

    @Test fun homonimos_por_apellido_no_matchean() {
        // Caso real ARG-SUI: gol de Lautaro adjudicado a Emiliano (arquero) en la alineación.
        assertFalse(nameMatches("Lautaro Martinez", "Emiliano Martinez"))
        assertTrue(nameMatches("L. Martinez", "Lautaro Martinez"))
    }
}
