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
}
