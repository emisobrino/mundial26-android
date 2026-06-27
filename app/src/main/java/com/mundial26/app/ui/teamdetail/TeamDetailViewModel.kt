package com.mundial26.app.ui.teamdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mundial26.app.domain.model.Team
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
    private val teamId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<TeamDetailUiState>(TeamDetailUiState.Loading)
    val uiState: StateFlow<TeamDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() = load()

    private fun load() {
        _uiState.value = TeamDetailUiState.Loading
        viewModelScope.launch {
            try {
                val team = teamRepository.team(teamId)
                _uiState.value = if (team != null) {
                    TeamDetailUiState.Success(team)
                } else {
                    TeamDetailUiState.Error("Equipo no encontrado")
                }
            } catch (e: Exception) {
                _uiState.value = TeamDetailUiState.Error(e.message ?: "Error al cargar el equipo")
            }
        }
    }

    companion object {
        fun Factory(teamRepository: TeamRepository, teamId: String) = viewModelFactory {
            initializer { TeamDetailViewModel(teamRepository, teamId) }
        }
    }
}
