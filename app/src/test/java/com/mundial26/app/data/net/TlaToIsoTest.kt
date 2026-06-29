package com.mundial26.app.data.net
import org.junit.Assert.assertEquals
import org.junit.Test
class TlaToIsoTest {
    @Test fun conocidos() { assertEquals("ar", tlaToIso("ARG")); assertEquals("gb-eng", tlaToIso("ENG")) }
    @Test fun desconocido() { assertEquals("xyz", tlaToIso("XYZ")) }
}
