package com.mundial26.app.data.sync
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mundial26.app.data.local.db.MundialDatabase
import com.mundial26.app.data.remote.footballdata.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class FakeApi : FootballDataApi {
    override suspend fun matches(c: String) = FdMatchesResponse(listOf(
        FdMatch(1, "2026-06-11T19:00:00Z", "FINISHED", "GROUP_STAGE", "GROUP_A", FdTeamRef("MEX"), FdTeamRef("RSA"), FdScore(FdGoals(2,0)))))
    override suspend fun standings(c: String) = FdStandingsResponse(listOf(
        FdStanding("TOTAL", "Group A", listOf(FdTableRow(1, FdTeamRef("MEX"), 1,1,0,0,3,2,0)))))
    override suspend fun teams(c: String) = FdTeamsResponse(listOf(
        FdTeam("ARG", "Argentina", "", listOf(FdSquadPlayer("Lionel Messi", "Offence", "1987-06-24")))))
    override suspend fun scorers(c: String, limit: Int) = FdScorersResponse(listOf(
        FdScorer(FdPlayer("Lionel Messi"), FdTeamRef("ARG"), goals = 5, assists = 3)))
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncManagerTest {
    private lateinit var db: MundialDatabase
    private lateinit var sync: SyncManager
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MundialDatabase::class.java).allowMainThreadQueries().build()
        sync = SyncManager(FakeApi(), db.teamDao(), db.playerDao(), db.standingDao(), db.matchDao(), db.syncMetaDao(), now = { 1_000_000L })
    }
    @After fun close() = db.close()

    @Test fun seed_y_refresh() = runTest {
        sync.seedIfEmpty()
        assertTrue(db.teamDao().count() > 0)
        assertTrue(db.playerDao().byTeam("ARG").isNotEmpty())
        sync.refreshIfStale()
        assertEquals(5, db.playerDao().topScorers(1)[0].goals)
        assertEquals("Argentina", db.teamDao().byTla("ARG")?.name)
    }
}
