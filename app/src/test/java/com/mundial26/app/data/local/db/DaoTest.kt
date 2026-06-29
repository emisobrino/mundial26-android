package com.mundial26.app.data.local.db
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mundial26.app.data.local.db.entities.PlayerEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DaoTest {
    private lateinit var db: MundialDatabase
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MundialDatabase::class.java).allowMainThreadQueries().build() }
    @After fun close() = db.close()

    @Test fun top_scorers_ordenados() = runTest {
        db.playerDao().upsertAll(listOf(
            PlayerEntity(teamTla = "ARG", name = "Messi", goals = 5, assists = 3),
            PlayerEntity(teamTla = "FRA", name = "Mbappé", goals = 4, assists = 2),
        ))
        val top = db.playerDao().topScorers(10)
        assertEquals("Messi", top[0].name); assertEquals(5, top[0].goals)
    }
}
