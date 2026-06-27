package com.mundial26.app.data.mock
import com.mundial26.app.domain.model.MatchStatus
import org.junit.Assert.*
import org.junit.Test

class FakeRepositoriesTest {
    private val matches = FakeMatchRepository()
    private val teams = FakeTeamRepository()
    private val news = FakeNewsRepository()

    @Test fun hay_partidos_en_vivo() {
        assertTrue(matches.liveMatches().isNotEmpty())
        assertTrue(matches.liveMatches().all { it.status == MatchStatus.LIVE })
    }
    @Test fun hay_cuatro_grupos() = assertEquals(4, teams.groups().size)
    @Test fun equipo_destacado_existe() = assertNotNull(teams.featured())
    @Test fun hay_noticias() = assertTrue(news.news().isNotEmpty())
    @Test fun lookup_por_id_funciona() {
        val first = teams.teams().first()
        assertEquals(first, teams.team(first.id))
    }

    @Test fun estrella_destacada_tiene_stats_de_mundial() {
        val star = teams.featured().star
        assertTrue("la estrella destacada debe tener goles > 0", star.stats.goals > 0)
        assertTrue("debe tener club", star.stats.club.isNotBlank())
        assertTrue("valoración en rango", star.stats.rating in 0.0..10.0)
    }

    @Test fun lookup_partido_por_id_funciona() {
        val first = matches.resultsAndUpcoming().firstOrNull() ?: matches.liveMatches().first()
        assertEquals(first, matches.match(first.id))
        assertNull(matches.match("no-existe"))
    }

    @Test fun lookup_noticia_por_id_funciona() {
        val first = news.news().first()
        assertEquals(first, news.item(first.id))
        assertNull(news.item("no-existe"))
    }
}
