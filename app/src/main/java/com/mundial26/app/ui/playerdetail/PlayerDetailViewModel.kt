package com.mundial26.app.ui.playerdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mundial26.app.domain.model.Star
import com.mundial26.app.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlayerDetailUiState {
    data object Loading : PlayerDetailUiState
    data class Success(
        val star: Star,
        val teamName: String,
        val teamFlag: String
    ) : PlayerDetailUiState
    data class Error(val message: String) : PlayerDetailUiState
}

class PlayerDetailViewModel(
    private val teamRepository: TeamRepository,
    private val teamId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerDetailUiState>(PlayerDetailUiState.Loading)
    val uiState: StateFlow<PlayerDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() = load()

    private fun load() {
        _uiState.value = PlayerDetailUiState.Loading
        viewModelScope.launch {
            try {
                val team = teamRepository.team(teamId)
                _uiState.value = if (team != null) {
                    PlayerDetailUiState.Success(team.star, team.name, team.flag)
                } else {
                    PlayerDetailUiState.Error("Jugador no encontrado")
                }
            } catch (e: Exception) {
                _uiState.value = PlayerDetailUiState.Error(e.message ?: "Error al cargar el jugador")
            }
        }
    }

    companion object {
        fun Factory(teamRepository: TeamRepository, teamId: String) = viewModelFactory {
            initializer { PlayerDetailViewModel(teamRepository, teamId) }
        }
    }
}
