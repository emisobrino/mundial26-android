package com.mundial26.app.data.mock
import com.mundial26.app.domain.model.MatchStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FakeRepositoriesTest {
    private val matches = FakeMatchRepository()
    private val teams = FakeTeamRepository()
    private val news = FakeNewsRepository()

    @Test fun hay_partidos_en_vivo() = runTest {
        assertTrue(matches.liveMatches().isNotEmpty())
        assertTrue(matches.liveMatches().all { it.status == MatchStatus.LIVE })
    }
    @Test fun hay_cuatro_grupos() = runTest { assertEquals(4, teams.groups().size) }
    @Test fun equipo_destacado_existe() = runTest { assertNotNull(teams.featured()) }
    @Test fun hay_noticias() = runTest { assertTrue(news.news().isNotEmpty()) }
    @Test fun lookup_por_id_funciona() = runTest {
        val first = teams.teams().first()
        assertEquals(first, teams.team(first.id))
    }

    @Test fun estrella_destacada_tiene_stats_de_mundial() = runTest {
        val star = teams.featured().star
        assertTrue("la estrella destacada debe tener goles > 0", star.stats.goals > 0)
        assertTrue("debe tener club", star.stats.club.isNotBlank())
        assertTrue("valoración en rango", star.stats.rating in 0.0..10.0)
    }

    @Test fun lookup_partido_por_id_funciona() = runTest {
        val first = matches.resultsAndUpcoming().firstOrNull() ?: matches.liveMatches().first()
        assertEquals(first, matches.match(first.id))
        assertNull(matches.match("no-existe"))
    }

    @Test fun lookup_noticia_por_id_funciona() = runTest {
        val first = news.news().first()
        assertEquals(first, news.item(first.id))
        assertNull(news.item("no-existe"))
    }

    @Test fun hay_goleadores_y_asistidores_ordenados() = runTest {
        val scorers = teams.topScorers()
        val assists = teams.topAssists()
        assertEquals(5, scorers.size)
        assertEquals(5, assists.size)
        // deben venir ordenados por valor descendente
        assertEquals(scorers.sortedByDescending { it.value }, scorers)
        assertEquals(assists.sortedByDescending { it.value }, assists)
        assertEquals("K. Mbappé", scorers.first().name)
        assertEquals(6, scorers.first().value)
    }
}
