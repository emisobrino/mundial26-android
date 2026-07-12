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

    @Test fun hora_y_dia_en_zona_local_del_dispositivo() {
        val uy = java.time.ZoneId.of("America/Montevideo")   // UTC-3
        val e = MatchEntity("1","2026-07-02T19:00:00Z","TIMED","GROUP_STAGE","GROUP_A","ESP","AUT",null,null)
        val m = e.toDomain(zone = uy)
        assertEquals("16:00", m.time)
        assertEquals("2026-07-02", m.day)
    }

    @Test fun cerca_de_medianoche_cambia_el_dia_local() {
        val uy = java.time.ZoneId.of("America/Montevideo")
        val e = MatchEntity("2","2026-07-03T01:30:00Z","TIMED","GROUP_STAGE","GROUP_B","COL","GHA",null,null)
        val m = e.toDomain(zone = uy)
        assertEquals("22:30", m.time)
        assertEquals("2026-07-02", m.day)   // en Uruguay todavía es el día anterior
    }

    @Test fun fecha_invalida_cae_al_recorte_utc() {
        val e = MatchEntity("3","fecha-rota","TIMED","GROUP_STAGE","","ARG","MEX",null,null)
        val m = e.toDomain()
        assertEquals("fecha-rota", m.day)
    }

    @Test fun status_basura_con_marcador_y_fecha_pasada_es_finalizado() {
        // Caso real: la fuente trajo la fecha en el campo status → statusOf lo daba como próximo.
        val e = MatchEntity("4","2026-07-06T01:00:00Z","2026-07-06 01:00:00Z","LAST_16","","MEX","ENG",2,3)
        val now = java.time.Instant.parse("2026-07-11T12:00:00Z").toEpochMilli()
        assertEquals(MatchStatus.FINISHED, e.toDomain(now).status)
        // Un próximo de verdad (sin marcador) no se toca aunque el status sea raro.
        val e2 = MatchEntity("5","2026-07-14T19:00:00Z","TIMED","SEMI_FINALS","","FRA","ESP",null,null)
        assertEquals(MatchStatus.UPCOMING, e2.toDomain(now).status)
    }
}
