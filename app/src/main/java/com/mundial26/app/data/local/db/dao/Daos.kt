package com.mundial26.app.data.local.db.dao
import androidx.room.*
import com.mundial26.app.data.local.db.entities.*

@Dao interface TeamDao {
    @Upsert suspend fun upsertAll(items: List<TeamEntity>)
    @Query("SELECT * FROM teams") suspend fun all(): List<TeamEntity>
    @Query("SELECT * FROM teams WHERE tla = :tla") suspend fun byTla(tla: String): TeamEntity?
    @Query("SELECT COUNT(*) FROM teams") suspend fun count(): Int
}
@Dao interface PlayerDao {
    @Upsert suspend fun upsertAll(items: List<PlayerEntity>)
    @Query("SELECT * FROM players WHERE teamTla = :tla") suspend fun byTeam(tla: String): List<PlayerEntity>
    @Query("SELECT * FROM players WHERE id = :id") suspend fun byId(id: Long): PlayerEntity?
    @Query("SELECT * FROM players ORDER BY goals DESC LIMIT :n") suspend fun topScorers(n: Int): List<PlayerEntity>
    @Query("SELECT * FROM players WHERE assists > 0 ORDER BY assists DESC LIMIT :n") suspend fun topAssists(n: Int): List<PlayerEntity>
    @Query("UPDATE players SET goals = :goals, assists = :assists WHERE teamTla = :tla AND name = :name") suspend fun updateStats(tla: String, name: String, goals: Int, assists: Int)
}
@Dao interface StandingDao {
    @Upsert suspend fun upsertAll(items: List<StandingEntity>)
    @Query("SELECT * FROM standings ORDER BY grp, position") suspend fun all(): List<StandingEntity>
}
@Dao interface MatchDao {
    @Upsert suspend fun upsertAll(items: List<MatchEntity>)
    @Query("SELECT * FROM matches ORDER BY utcDate") suspend fun all(): List<MatchEntity>
    @Query("SELECT * FROM matches WHERE id = :id") suspend fun byId(id: String): MatchEntity?
}
@Dao interface SyncMetaDao {
    @Upsert suspend fun put(meta: SyncMeta)
    @Query("SELECT lastSyncEpochMs FROM sync_meta WHERE key = :key") suspend fun lastSync(key: String): Long?
}
