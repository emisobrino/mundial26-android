package com.mundial26.app.data.local.db
import com.mundial26.app.data.local.db.entities.*
import com.mundial26.app.domain.model.MatchStatus
import org.junit.Assert.*
import org.junit.Test
class DbMappersTest {
    @Test fun standing_entity_a_grouprow() {
        val row = StandingEntity("Group A","MEX",1,3,3,0,0,6,0,9).toDomain()
        assertEquals("MEX", row.code); assertEquals("mx", row.flag); assertEquals(6, row.dg)
    }
    @Test fun match_entity_a_dominio_con_minuto() {
        val e = MatchEntity("1","2026-06-28T18:00:00Z","IN_PLAY","GROUP_STAGE","GROUP_A","ARG","MEX",1,0)
        val now = java.time.Instant.parse("2026-06-28T18:30:00Z").toEpochMilli()
        val m = e.toDomain(now); assertEquals(MatchStatus.LIVE, m.status); assertEquals(30, m.minute)
    }
}
