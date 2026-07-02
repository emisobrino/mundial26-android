package com.mundial26.app.data.remote.footballdata
import com.mundial26.app.domain.model.MatchStatus
import org.junit.Assert.*
import org.junit.Test
class FootballDataMappersTest {
    @Test fun match_a_entity() {
        val m = FdMatch(1, "2026-06-11T19:00:00Z", "FINISHED", "GROUP_STAGE", "GROUP_A", FdTeamRef("MEX"), FdTeamRef("RSA"), FdScore(FdGoals(2,0)))
        val e = m.toEntity()
        assertEquals("1", e.id); assertEquals("MEX", e.homeTla); assertEquals(2, e.scoreHome); assertEquals("GROUP_A", e.grp)
    }
    @Test fun standings_a_entities() {
        val resp = FdStandingsResponse(listOf(
            FdStanding("TOTAL", "Group A", listOf(FdTableRow(1, FdTeamRef("MEX"), 3,3,0,0,9,6,0))),
            FdStanding("HOME", "Group A", emptyList())
        ))
        val es = resp.toEntities(); assertEquals(1, es.size); assertEquals("Group A", es[0].grp); assertEquals(9, es[0].pts)
    }
    @Test fun penales_separa_marcador_real_de_la_tanda() {
        // football-data suma los penales dentro de fullTime: GER 1-1 PAR (3-4 pen) llega como 4-5.
        val m = FdMatch(
            2, "2026-07-01T22:00:00Z", "FINISHED", "LAST_32", null,
            FdTeamRef("GER"), FdTeamRef("PAR"),
            FdScore(
                fullTime = FdGoals(4, 5), duration = "PENALTY_SHOOTOUT",
                regularTime = FdGoals(1, 1), extraTime = FdGoals(0, 0), penalties = FdGoals(3, 4)
            )
        )
        val e = m.toEntity()
        assertEquals(1, e.scoreHome); assertEquals(1, e.scoreAway)
        assertEquals(3, e.penHome); assertEquals(4, e.penAway)
    }

    @Test fun penales_sin_regularTime_cae_a_fullTime_menos_penales() {
        val m = FdMatch(
            3, "2026-07-01T22:00:00Z", "FINISHED", "LAST_32", null,
            FdTeamRef("A"), FdTeamRef("B"),
            FdScore(fullTime = FdGoals(4, 5), duration = "PENALTY_SHOOTOUT", penalties = FdGoals(3, 4))
        )
        val e = m.toEntity()
        assertEquals(1, e.scoreHome); assertEquals(1, e.scoreAway)
    }

    @Test fun sin_penales_el_marcador_es_fullTime() {
        val m = FdMatch(4, "2026-06-11T19:00:00Z", "FINISHED", "GROUP_STAGE", "GROUP_A",
            FdTeamRef("MEX"), FdTeamRef("RSA"), FdScore(FdGoals(2, 0)))
        val e = m.toEntity()
        assertEquals(2, e.scoreHome); assertEquals(0, e.scoreAway)
        assertNull(e.penHome); assertNull(e.penAway)
    }

    @Test fun deriva_minuto_si_live() {
        val kickoff = "2026-06-28T18:00:00Z"
        val now = java.time.Instant.parse("2026-06-28T19:07:00Z").toEpochMilli()
        assertEquals(67, deriveMinute(kickoff, MatchStatus.LIVE, now))
        assertNull(deriveMinute(kickoff, MatchStatus.FINISHED, now))
    }
}
