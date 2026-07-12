package com.mundial26.app.ui.teamdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mundial26.app.domain.model.Match
import com.mundial26.app.domain.model.Team
import com.mundial26.app.domain.repository.MatchRepository
import com.mundial26.app.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TeamDetailUiState {
    data object Loading : TeamDetailUiState
    data class Success(val team: Team) : TeamDetailUiState
    data class Error(val message: String) : TeamDetailUiState
}

class TeamDetailViewModel(
    private val teamRepository: TeamRepository,
    private val teamId: String,
    private val matchRepository: MatchRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<TeamDetailUiState>(TeamDetailUiState.Loading)
    val uiState: StateFlow<TeamDetailUiState> = _uiState.asStateFlow()

    // Partidos de esta selección en el Mundial (jugados + próximos), en orden cronológico.
    private val _matches = MutableStateFlow<List<Match>>(emptyList())
    val matches: StateFlow<List<Match>> = _matches.asStateFlow()

    init { load() }

    fun retry() = load()

    private fun load() {
        _uiState.value = TeamDetailUiState.Loading
        viewModelScope.launch {
            try {
                // Primer render al instante con el plantel de la caché local; el enriquecido
                // FIFA (goles/tarjetas/capitán reales + DT) llega en segundo plano y actualiza.
                val base = teamRepository.teamBase(teamId)
                _uiState.value = if (base != null) {
                    TeamDetailUiState.Success(base)
                } else {
                    TeamDetailUiState.Error("Equipo no encontrado")
                    return@launch
                }
                runCatching { teamRepository.team(teamId) }.getOrNull()?.let {
                    _uiState.value = TeamDetailUiState.Success(it)
                }
            } catch (e: Exception) {
                _uiState.value = TeamDetailUiState.Error(e.message ?: "Error al cargar el equipo")
            }
        }
        val repo = matchRepository ?: return
        viewModelScope.launch {
            val code = teamId.uppercase()
            _matches.value = runCatching { repo.byDay() }.getOrDefault(emptyMap())
                .values.flatten()
                .filter { it.teamA == code || it.teamB == code }
                .sortedBy { it.kickoff }
        }
    }

    companion object {
        fun Factory(
            teamRepository: TeamRepository,
            teamId: String,
            matchRepository: MatchRepository? = null
        ) = viewModelFactory {
            initializer { TeamDetailViewModel(teamRepository, teamId, matchRepository) }
        }
    }
}
