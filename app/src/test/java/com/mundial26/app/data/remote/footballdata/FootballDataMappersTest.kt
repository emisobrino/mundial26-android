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
    @Test fun deriva_minuto_si_live() {
        val kickoff = "2026-06-28T18:00:00Z"
        val now = java.time.Instant.parse("2026-06-28T19:07:00Z").toEpochMilli()
        assertEquals(67, deriveMinute(kickoff, MatchStatus.LIVE, now))
        assertNull(deriveMinute(kickoff, MatchStatus.FINISHED, now))
    }
}
