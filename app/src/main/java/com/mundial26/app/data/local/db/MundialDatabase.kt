package com.mundial26.app.data.local.db
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mundial26.app.data.local.db.dao.*
import com.mundial26.app.data.local.db.entities.*

@Database(entities = [TeamEntity::class, PlayerEntity::class, StandingEntity::class, MatchEntity::class, SyncMeta::class], version = 2, exportSchema = false)
abstract class MundialDatabase : RoomDatabase() {
    abstract fun teamDao(): TeamDao
    abstract fun playerDao(): PlayerDao
    abstract fun standingDao(): StandingDao
    abstract fun matchDao(): MatchDao
    abstract fun syncMetaDao(): SyncMetaDao
    companion object {
        fun build(context: Context): MundialDatabase =
            Room.databaseBuilder(context, MundialDatabase::class.java, "mundial.db")
                .fallbackToDestructiveMigration().build()
    }
}
