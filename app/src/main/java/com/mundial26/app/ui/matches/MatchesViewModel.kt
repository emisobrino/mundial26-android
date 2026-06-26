package com.mundial26.app.ui.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mundial26.app.domain.model.BracketRound
import com.mundial26.app.domain.model.Match
import com.mundial26.app.domain.model.MatchStatus
import com.mundial26.app.domain.repository.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Sub-vista seleccionada en la pantalla Partidos. */
enum class MatchesView { LIST, CALENDAR, BRACKET }

/**
 * Estado de UI de la pantalla Partidos.
 */
sealed interface MatchesUiState {
    data object Loading : MatchesUiState
    data class Success(
        val live: List<Match>,
        val upcoming: List<Match>,
        val finished: List<Match>,
        val byDay: Map<String, List<Match>>,
        val bracket: List<BracketRound>
    ) : MatchesUiState
    data class Error(val message: String) : MatchesUiState
}

/**
 * ViewModel de la pantalla Partidos: agrega datos para las tres sub-vistas
 * (lista Partidos, Calendario, Llaves) y maneja la selección del segmento activo.
 */
class MatchesViewModel(
    private val matchRepository: MatchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MatchesUiState>(MatchesUiState.Loading)
    val uiState: StateFlow<MatchesUiState> = _uiState.asStateFlow()

    private val _selectedView = MutableStateFlow(MatchesView.LIST)
    val selectedView: StateFlow<MatchesView> = _selectedView.asStateFlow()

    init {
        load()
    }

    /** Reintenta la carga de datos tras un error. */
    fun retry() {
        load()
    }

    /** Cambia la sub-vista activa (Partidos / Calendario / Llaves). */
    fun selectView(view: MatchesView) {
        _selectedView.value = view
    }

    private fun load() {
        _uiState.value = MatchesUiState.Loading
        viewModelScope.launch {
            try {
                val resultsAndUpcoming = matchRepository.resultsAndUpcoming()
                _uiState.value = MatchesUiState.Success(
                    live = matchRepository.liveMatches(),
                    upcoming = resultsAndUpcoming.filter { it.status == MatchStatus.UPCOMING },
                    finished = resultsAndUpcoming.filter { it.status == MatchStatus.FINISHED },
                    byDay = matchRepository.byDay(),
                    bracket = matchRepository.bracket()
                )
            } catch (e: Exception) {
                _uiState.value = MatchesUiState.Error(e.message ?: "Error al cargar Partidos")
            }
        }
    }

    companion object {
        fun Factory(matchRepository: MatchRepository) = viewModelFactory {
            initializer {
                MatchesViewModel(matchRepository)
            }
        }
    }
}
