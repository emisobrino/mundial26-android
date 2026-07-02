package com.mundial26.app.data.sync
import com.mundial26.app.data.editorial.EditorialSource
import com.mundial26.app.data.net.confOf
import com.mundial26.app.data.local.db.dao.*
import com.mundial26.app.data.local.db.entities.*
import com.mundial26.app.data.remote.footballdata.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SyncManager(
    private val api: FootballDataApi,
    private val teamDao: TeamDao, private val playerDao: PlayerDao,
    private val standingDao: StandingDao, private val matchDao: MatchDao,
    private val metaDao: SyncMetaDao,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val ttlMs: Long = 6 * 60 * 60 * 1000L
) {
    /**
     * Serializa seed/refresh: varias pantallas disparan ensureFresh() en paralelo al
     * arrancar y, sin lock, el seed corría dos veces duplicando los planteles
     * (PlayerEntity usa id autogenerado, así que el doble insert no deduplica).
     */
    private val mutex = Mutex()

    private suspend fun stale(key: String): Boolean {
        val last = metaDao.lastSync(key) ?: return true
        return last < now() - ttlMs
    }

    private suspend fun seedLocked() {
        if (teamDao.count() > 0) return
        runCatching {
            val teams = api.teams().teams
            teamDao.upsertAll(teams.map { t ->
                val ed = EditorialSource.forCode(t.tla)
                TeamEntity(tla = t.tla, name = t.name, conf = confOf(t.tla).name, rank = ed?.rank ?: 0,
                    tag = ed?.tag ?: "", styleLabel = ed?.styleLabel ?: "",
                    starNum = ed?.star?.num ?: 0, starName = ed?.star?.name ?: "", starPos = ed?.star?.pos ?: "",
                    starNote = ed?.star?.note ?: "", starClub = ed?.star?.stats?.club ?: "",
                    starRating = ed?.star?.stats?.rating ?: 0.0, starValue = ed?.star?.stats?.marketValue ?: "")
            })
            playerDao.upsertAll(teams.flatMap { it.toPlayerEntities() })
            metaDao.put(SyncMeta("teams", now()))
        }
    }

    private suspend fun refreshLocked(force: Boolean) {
        if (force || stale("standings")) runCatching { standingDao.upsertAll(api.standings().toEntities()); metaDao.put(SyncMeta("standings", now())) }
        if (force || stale("matches")) runCatching { matchDao.upsertAll(api.matches().toEntities()); metaDao.put(SyncMeta("matches", now())) }
        if (force || stale("scorers")) runCatching {
            api.scorers().scorers.forEach { s -> playerDao.updateStats(s.team.tla, s.player.name, s.goals ?: 0, s.assists ?: 0) }
            metaDao.put(SyncMeta("scorers", now()))
        }
    }

    suspend fun seedIfEmpty() = mutex.withLock { seedLocked() }

    suspend fun refreshIfStale() = mutex.withLock { refreshLocked(force = false) }

    suspend fun ensureFresh() = mutex.withLock { seedLocked(); refreshLocked(force = false) }

    /** Refresco forzado (pull-to-refresh): ignora el TTL y vuelve a traer standings/matches/scorers. */
    suspend fun forceRefresh() = mutex.withLock { seedLocked(); refreshLocked(force = true) }
}
