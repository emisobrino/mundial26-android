package com.mundial26.app.ui.matchdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mundial26.app.domain.model.Match
import com.mundial26.app.domain.model.MatchStatus
import com.mundial26.app.domain.model.Star
import com.mundial26.app.domain.repository.MatchRepository
import com.mundial26.app.domain.repository.TeamRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MatchDetailUiState {
    data object Loading : MatchDetailUiState
    data class Success(
        val match: Match,
        val homeStar: Star? = null,   // estrella de cada equipo (para la previa)
        val awayStar: Star? = null
    ) : MatchDetailUiState
    data class Error(val message: String) : MatchDetailUiState
}

class MatchDetailViewModel(
    private val matchRepository: MatchRepository,
    private val teamRepository: TeamRepository,
    private val matchId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<MatchDetailUiState>(MatchDetailUiState.Loading)
    val uiState: StateFlow<MatchDetailUiState> = _uiState.asStateFlow()

    init {
        load()
        // Partido en juego: refresca en silencio cada 30 s (minuto, marcador, stats y eventos).
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                val current = _uiState.value
                // En vivo o por empezar (para captar el arranque): refrescar en silencio.
                if (current is MatchDetailUiState.Success && current.match.status != MatchStatus.FINISHED) {
                    runCatching { buildState() }.getOrNull()?.let { _uiState.value = it }
                }
            }
        }
    }

    fun retry() = load()

    private suspend fun buildState(): MatchDetailUiState {
        val match = matchRepository.match(matchId) ?: return MatchDetailUiState.Error("Partido no encontrado")
        // Estrellas para la previa (solo si el cruce ya tiene equipos definidos).
        val homeStar = match.teamA.takeIf { it.isNotBlank() }
            ?.let { runCatching { teamRepository.team(it)?.star }.getOrNull() }
        val awayStar = match.teamB.takeIf { it.isNotBlank() }
            ?.let { runCatching { teamRepository.team(it)?.star }.getOrNull() }
        return MatchDetailUiState.Success(match, homeStar, awayStar)
    }

    private fun load() {
        _uiState.value = MatchDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                buildState()
            } catch (e: Exception) {
                MatchDetailUiState.Error(e.message ?: "Error al cargar el partido")
            }
        }
    }

    companion object {
        fun Factory(matchRepository: MatchRepository, teamRepository: TeamRepository, matchId: String) = viewModelFactory {
            initializer { MatchDetailViewModel(matchRepository, teamRepository, matchId) }
        }
    }
}
