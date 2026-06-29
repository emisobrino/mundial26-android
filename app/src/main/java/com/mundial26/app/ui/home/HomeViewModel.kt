package com.mundial26.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.mundial26.app.domain.model.Group
import com.mundial26.app.domain.model.Leader
import com.mundial26.app.domain.model.Match
import com.mundial26.app.domain.model.NewsItem
import com.mundial26.app.domain.model.Team
import com.mundial26.app.domain.repository.MatchRepository
import com.mundial26.app.domain.repository.NewsRepository
import com.mundial26.app.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado de UI de la pantalla Inicio.
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val live: List<Match>,
        val results: List<Match>,
        val featured: Team,
        val groupB: Group,
        val scorers: List<Leader>,
        val assists: List<Leader>,
        val news: List<NewsItem>
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

/**
 * ViewModel de la pantalla Inicio: agrega datos de partidos en vivo, resultados/próximos,
 * equipo destacado, tabla del Grupo B y noticias recientes.
 */
class HomeViewModel(
    private val matchRepository: MatchRepository,
    private val teamRepository: TeamRepository,
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Reintenta la carga de datos tras un error. */
    fun retry() {
        load()
    }

    private fun load() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            try {
                val groups = teamRepository.groups()
                val groupB = groups.firstOrNull { it.name.contains("B") } ?: groups[1]
                _uiState.value = HomeUiState.Success(
                    live = matchRepository.liveMatches(),
                    results = matchRepository.resultsAndUpcoming().take(8),
                    featured = teamRepository.featured(),
                    groupB = groupB,
                    scorers = teamRepository.topScorers(),
                    assists = teamRepository.topAssists(),
                    news = newsRepository.news().take(4)
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Error al cargar Inicio")
            }
        }
    }

    companion object {
        fun Factory(
            matchRepository: MatchRepository,
            teamRepository: TeamRepository,
            newsRepository: NewsRepository
        ) = viewModelFactory {
            initializer {
                HomeViewModel(matchRepository, teamRepository, newsRepository)
            }
        }
    }
}
