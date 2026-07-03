package com.mundial26.app.ui.playerdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mundial26.app.data.net.tlaToIso
import com.mundial26.app.data.remote.fifa.FifaStatsSource
import com.mundial26.app.data.remote.sportsdb.SportsDbSource
import com.mundial26.app.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlayerDetailUiState {
    data object Loading : PlayerDetailUiState
    /**
     * Vista unificada de jugador: sirve tanto para la "estrella" editorial del equipo
     * (con dorsal/nota/valoración/club/valor) como para un jugador del plantel real
     * (solo nombre/posición/edad/goles/asistencias — los campos editoriales van vacíos).
     */
    data class Success(
        val number: Int,          // dorsal editorial; 0 = sin dato → se muestran iniciales
        val name: String,
        val position: String,
        val note: String,         // "" para jugadores del plantel
        val teamName: String,
        val teamFlag: String,
        val goals: Int,
        val assists: Int,
        val age: Int,
        val rating: Double,       // 0.0 = sin dato → oculto
        val club: String,         // "" = oculto
        val marketValue: String,  // "" = oculto
        // Perfil FIFA (jugadores del plantel; null = sin dato → oculto)
        val caps: Int? = null,
        val intlGoals: Int? = null,
        val heightCm: Int? = null,
        val weightKg: Int? = null,
        val preferredFoot: Int? = null   // 1 = derecho, 2 = izquierdo
    ) : PlayerDetailUiState
    data class Error(val message: String) : PlayerDetailUiState
}

/** Modo de carga del detalle de jugador. */
private sealed interface Source {
    data class Star(val teamId: String) : Source
    data class SquadPlayer(val playerId: Long) : Source
}

class PlayerDetailViewModel private constructor(
    private val teamRepository: TeamRepository,
    private val source: Source,
    private val fifa: FifaStatsSource = FifaStatsSource(),
    private val sportsDb: SportsDbSource = SportsDbSource()
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerDetailUiState>(PlayerDetailUiState.Loading)
    val uiState: StateFlow<PlayerDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() = load()

    private fun load() {
        _uiState.value = PlayerDetailUiState.Loading
        viewModelScope.launch {
            try {
                _uiState.value = when (val s = source) {
                    is Source.Star -> loadStar(s.teamId)
                    is Source.SquadPlayer -> loadPlayer(s.playerId)
                }
            } catch (e: Exception) {
                _uiState.value = PlayerDetailUiState.Error(e.message ?: "Error al cargar el jugador")
            }
        }
    }

    private suspend fun loadStar(teamId: String): PlayerDetailUiState {
        val team = teamRepository.team(teamId) ?: return PlayerDetailUiState.Error("Jugador no encontrado")
        val st = team.star
        return PlayerDetailUiState.Success(
            number = st.num, name = st.name, position = st.pos, note = st.note,
            teamName = team.name, teamFlag = team.flag,
            goals = st.stats.goals, assists = st.stats.assists, age = st.stats.age,
            rating = st.stats.rating, club = st.stats.club, marketValue = st.stats.marketValue
        )
    }

    private suspend fun loadPlayer(playerId: Long): PlayerDetailUiState {
        val p = teamRepository.player(playerId) ?: return PlayerDetailUiState.Error("Jugador no encontrado")
        val team = teamRepository.team(p.teamCode)
        // Perfil FIFA por nombre+selección: caps, goles internacionales, altura, peso, pie.
        val profile = runCatching { fifa.playerProfile(p.name, p.teamCode) }.getOrNull()
        // Club y dorsal desde football-data /persons. Durante el Mundial, para algunos
        // jugadores currentTeam es la propia selección → completar con TheSportsDB.
        val clubInfo = runCatching { teamRepository.playerClub(playerId) }.getOrNull()
        val teamName = team?.name ?: p.teamCode
        val club = clubInfo?.club?.takeIf { it.isNotBlank() && !it.equals(teamName, true) }
            ?: runCatching { sportsDb.clubOf(p.name) }.getOrNull().orEmpty()
        return PlayerDetailUiState.Success(
            number = clubInfo?.shirtNumber ?: 0, name = p.name, position = p.position, note = "",
            teamName = teamName, teamFlag = team?.flag ?: tlaToIso(p.teamCode),
            goals = p.goals, assists = p.assists, age = p.age,
            rating = 0.0, club = club, marketValue = "",
            caps = profile?.caps, intlGoals = profile?.intlGoals,
            heightCm = profile?.heightCm, weightKg = profile?.weightKg,
            preferredFoot = profile?.preferredFoot
        )
    }

    companion object {
        /** Detalle de la estrella editorial del equipo (por teamId). */
        fun Factory(teamRepository: TeamRepository, teamId: String) = viewModelFactory {
            initializer { PlayerDetailViewModel(teamRepository, Source.Star(teamId)) }
        }

        /** Detalle de un jugador del plantel real (por id de Room). */
        fun FactoryForPlayer(teamRepository: TeamRepository, playerId: Long) = viewModelFactory {
            initializer { PlayerDetailViewModel(teamRepository, Source.SquadPlayer(playerId)) }
        }
    }
}
