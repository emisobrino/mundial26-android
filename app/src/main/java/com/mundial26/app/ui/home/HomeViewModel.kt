package com.mundial26.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.mundial26.app.data.remote.youtube.YoutubeHighlightsSource
import com.mundial26.app.domain.model.BracketRound
import com.mundial26.app.domain.model.Group
import com.mundial26.app.domain.model.Leader
import com.mundial26.app.domain.model.Match
import com.mundial26.app.domain.model.MatchStatus
import com.mundial26.app.domain.model.Nations
import com.mundial26.app.domain.model.NewsItem
import com.mundial26.app.domain.model.Team
import com.mundial26.app.domain.repository.MatchRepository
import com.mundial26.app.domain.repository.NewsRepository
import com.mundial26.app.domain.repository.TeamRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Resumen en video de un partido reciente (para el carrusel de Inicio). */
data class MatchHighlight(val match: Match, val videoId: String)

/**
 * Estado de UI de la pantalla Inicio.
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val live: List<Match>,
        val results: List<Match>,
        val upcoming: List<Match>,
        val featured: Team,
        val groups: List<Group>,
        val scorers: List<Leader>,
        val assists: List<Leader>,
        val yellowCards: List<Leader> = emptyList(),
        val redCards: List<Leader> = emptyList(),
        val bracket: List<BracketRound> = emptyList(),
        val highlights: List<MatchHighlight> = emptyList(),
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

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val highlightsSource = YoutubeHighlightsSource()

    /** Resuelve (en paralelo, cacheado) el video de resumen de los resultados recientes. */
    private suspend fun resolveHighlights(recent: List<Match>): List<MatchHighlight> = coroutineScope {
        recent.take(6)
            .filter { it.teamA.isNotBlank() && it.teamB.isNotBlank() }
            .map { m ->
                async {
                    val query = "${Nations.name(m.teamA)} ${Nations.name(m.teamB)} Mundial 2026 resumen ESPN"
                    runCatching { highlightsSource.firstVideoId(query) }.getOrNull()
                        ?.let { MatchHighlight(m, it) }
                }
            }
            .awaitAll()
            .filterNotNull()
    }

    init {
        load()
        // Refresco silencioso cada 30 s: goles/minuto en vivo y promoción automática
        // de partidos que arrancan, sin necesidad de pull-to-refresh.
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30_000)
                if (_uiState.value is HomeUiState.Success) loadData(silent = true)
            }
        }
    }

    /** Reintenta la carga de datos tras un error. */
    fun retry() {
        load()
    }

    /** Pull-to-refresh: fuerza resync y recarga sin mostrar el spinner de pantalla completa. */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                matchRepository.refresh()
                teamRepository.refresh()
                newsRepository.refresh()
                loadData()
            } catch (_: Exception) {
                // Mantiene el contenido actual si el refresh falla.
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun load() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch { loadData() }
    }

    private suspend fun loadData(silent: Boolean = false) {
        try {
            val groups = teamRepository.groups()
                // Resultados recientes primero (más nuevos arriba), luego los próximos por jugarse.
                val resultsAndUpcoming = matchRepository.resultsAndUpcoming()
                val recentFinished = resultsAndUpcoming
                    .filter { it.status == MatchStatus.FINISHED }
                    .sortedByDescending { (it.day ?: "") + (it.time ?: "") }
                val nextUpcoming = resultsAndUpcoming
                    .filter { it.status == MatchStatus.UPCOMING }
                    .sortedBy { (it.day ?: "") + (it.time ?: "") }
                _uiState.value = HomeUiState.Success(
                    live = matchRepository.liveMatches(),
                    results = recentFinished.take(5),
                    upcoming = nextUpcoming.take(3),
                    featured = teamRepository.featured(),
                    groups = groups,
                    scorers = teamRepository.topScorers(),
                    assists = teamRepository.topAssists(),
                    yellowCards = teamRepository.topYellowCards(),
                    redCards = teamRepository.topRedCards(),
                    bracket = matchRepository.bracket(),
                    highlights = runCatching { resolveHighlights(recentFinished) }.getOrDefault(emptyList()),
                    news = newsRepository.news().take(4)
                )
        } catch (e: Exception) {
            // En un refresh silencioso de fondo, un fallo transitorio no debe tapar el contenido.
            if (!silent) _uiState.value = HomeUiState.Error(e.message ?: "Error al cargar Inicio")
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
