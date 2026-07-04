package com.mundial26.app.ui.leaders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mundial26.app.domain.model.Leader
import com.mundial26.app.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Sub-vista del ranking de líderes. */
enum class LeadersTab { SCORERS, ASSISTS, YELLOW, RED }

sealed interface LeadersUiState {
    data object Loading : LeadersUiState
    data class Success(
        val scorers: List<Leader>,
        val assists: List<Leader>,
        val yellowCards: List<Leader> = emptyList(),
        val redCards: List<Leader> = emptyList()
    ) : LeadersUiState
    data class Error(val message: String) : LeadersUiState
}

/**
 * ViewModel de la pantalla dedicada de goleadores/asistidores del torneo:
 * lee los rankings derivados de los jugadores (Room, actualizados por sync).
 */
class LeadersViewModel(
    private val teamRepository: TeamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LeadersUiState>(LeadersUiState.Loading)
    val uiState: StateFlow<LeadersUiState> = _uiState.asStateFlow()

    private val _tab = MutableStateFlow(LeadersTab.SCORERS)
    val tab: StateFlow<LeadersTab> = _tab.asStateFlow()

    init { load() }

    fun retry() = load()

    fun selectTab(tab: LeadersTab) { _tab.value = tab }

    private fun load() {
        _uiState.value = LeadersUiState.Loading
        viewModelScope.launch {
            try {
                _uiState.value = LeadersUiState.Success(
                    scorers = teamRepository.topScorers(),
                    assists = teamRepository.topAssists(),
                    yellowCards = teamRepository.topYellowCards(),
                    redCards = teamRepository.topRedCards()
                )
            } catch (e: Exception) {
                _uiState.value = LeadersUiState.Error(e.message ?: "Error al cargar los líderes")
            }
        }
    }

    companion object {
        fun Factory(teamRepository: TeamRepository) = viewModelFactory {
            initializer { LeadersViewModel(teamRepository) }
        }
    }
}
