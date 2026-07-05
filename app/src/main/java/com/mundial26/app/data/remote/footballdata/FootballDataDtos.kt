package com.mundial26.app.data.remote.footballdata
import kotlinx.serialization.Serializable

@Serializable data class FdTeamRef(val tla: String = "", val name: String = "", val crest: String = "")
@Serializable data class FdGoals(val home: Int? = null, val away: Int? = null)
@Serializable data class FdScore(
    val fullTime: FdGoals = FdGoals(),
    val duration: String = "REGULAR",          // REGULAR | EXTRA_TIME | PENALTY_SHOOTOUT
    val regularTime: FdGoals? = null,
    val extraTime: FdGoals? = null,
    val penalties: FdGoals? = null
)
@Serializable data class FdMatch(val id: Int, val utcDate: String = "", val status: String = "", val stage: String = "", val group: String? = null, val homeTeam: FdTeamRef = FdTeamRef(), val awayTeam: FdTeamRef = FdTeamRef(), val score: FdScore = FdScore())
@Serializable data class FdMatchesResponse(val matches: List<FdMatch> = emptyList())
@Serializable data class FdTableRow(val position: Int = 0, val team: FdTeamRef = FdTeamRef(), val playedGames: Int = 0, val won: Int = 0, val draw: Int = 0, val lost: Int = 0, val points: Int = 0, val goalsFor: Int = 0, val goalsAgainst: Int = 0)
@Serializable data class FdStanding(val type: String = "", val group: String? = null, val table: List<FdTableRow> = emptyList())
@Serializable data class FdStandingsResponse(val standings: List<FdStanding> = emptyList())
@Serializable data class FdSquadPlayer(val id: Long? = null, val name: String = "", val position: String? = null, val dateOfBirth: String? = null)
@Serializable data class FdCurrentTeam(val name: String = "")
@Serializable data class FdPerson(val name: String = "", val shirtNumber: Int? = null, val currentTeam: FdCurrentTeam? = null)
@Serializable data class FdCoach(val name: String = "")
@Serializable data class FdTeam(val tla: String = "", val name: String = "", val crest: String = "", val squad: List<FdSquadPlayer> = emptyList(), val coach: FdCoach = FdCoach())
@Serializable data class FdTeamsResponse(val teams: List<FdTeam> = emptyList())
@Serializable data class FdPlayer(val name: String = "")
@Serializable data class FdScorer(val player: FdPlayer = FdPlayer(), val team: FdTeamRef = FdTeamRef(), val goals: Int? = null, val assists: Int? = null)
@Serializable data class FdScorersResponse(val scorers: List<FdScorer> = emptyList())
