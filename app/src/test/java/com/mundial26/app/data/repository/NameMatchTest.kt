package com.mundial26.app.data.repository

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
}
