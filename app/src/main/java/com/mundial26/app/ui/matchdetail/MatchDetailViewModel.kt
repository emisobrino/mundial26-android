package com.mundial26.app.ui.matchdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mundial26.app.domain.model.Match
import com.mundial26.app.domain.model.MatchStatus
import com.mundial26.app.domain.repository.MatchRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MatchDetailUiState {
    data object Loading : MatchDetailUiState
    data class Success(val match: Match) : MatchDetailUiState
    data class Error(val message: String) : MatchDetailUiState
}

class MatchDetailViewModel(
    private val matchRepository: MatchRepository,
    private val matchId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<MatchDetailUiState>(MatchDetailUiState.Loading)
    val uiState: StateFlow<MatchDetailUiState> = _uiState.asStateFlow()

    init {
        load()
        // Partido en juego: refresca en silencio cada 60 s (minuto, marcador, stats y eventos).
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                val current = _uiState.value
                if (current is MatchDetailUiState.Success && current.match.status == MatchStatus.LIVE) {
                    runCatching { matchRepository.match(matchId) }.getOrNull()?.let {
                        _uiState.value = MatchDetailUiState.Success(it)
                    }
                }
            }
        }
    }

    fun retry() = load()

    private fun load() {
        _uiState.value = MatchDetailUiState.Loading
        viewModelScope.launch {
            try {
                val match = matchRepository.match(matchId)
                _uiState.value = if (match != null) {
                    MatchDetailUiState.Success(match)
                } else {
                    MatchDetailUiState.Error("Partido no encontrado")
                }
            } catch (e: Exception) {
                _uiState.value = MatchDetailUiState.Error(e.message ?: "Error al cargar el partido")
            }
        }
    }

    companion object {
        fun Factory(matchRepository: MatchRepository, matchId: String) = viewModelFactory {
            initializer { MatchDetailViewModel(matchRepository, matchId) }
        }
    }
}
