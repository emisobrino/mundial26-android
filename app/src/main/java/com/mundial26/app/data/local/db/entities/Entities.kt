package com.mundial26.app.data.local.db.entities
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey val tla: String, val name: String, val group: String = "",
    val conf: String = "OTHER", val rank: Int = 0, val tag: String = "", val styleLabel: String = "",
    val starNum: Int = 0, val starName: String = "", val starPos: String = "", val starNote: String = "",
    val starClub: String = "", val starRating: Double = 0.0, val starValue: String = ""
)

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teamTla: String, val name: String, val position: String = "",
    val dateOfBirth: String = "", val goals: Int = 0, val assists: Int = 0
)

@Entity(tableName = "standings", primaryKeys = ["grp", "tla"])
data class StandingEntity(
    val grp: String, val tla: String, val position: Int,
    val pj: Int, val g: Int, val e: Int, val p: Int, val gf: Int, val gc: Int, val pts: Int
)

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String, val utcDate: String, val status: String,
    val stage: String, val grp: String, val homeTla: String, val awayTla: String,
    val scoreHome: Int?, val scoreAway: Int?,
    val penHome: Int? = null, val penAway: Int? = null   // definición por penales
)

@Entity(tableName = "sync_meta")
data class SyncMeta(@PrimaryKey val key: String, val lastSyncEpochMs: Long)
