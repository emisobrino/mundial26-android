package com.mundial26.app.ui.matchdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mundial26.app.data.remote.youtube.YoutubeHighlightsSource
import com.mundial26.app.domain.model.Match
import com.mundial26.app.domain.model.MatchLineups
import com.mundial26.app.domain.model.MatchStatus
import com.mundial26.app.domain.model.Nations
import com.mundial26.app.domain.model.Star
import com.mundial26.app.domain.model.WinProbability
import com.mundial26.app.domain.prediction.winProbabilityFromRanks
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
        val awayStar: Star? = null,
        val winProb: WinProbability? = null,   // probabilidad 90' por ranking FIFA (no finalizado)
        val highlightVideoId: String? = null,  // resumen en YouTube (partido finalizado)
        val lineups: MatchLineups? = null      // XI confirmados (FIFA, ~1 h antes / en vivo)
    ) : MatchDetailUiState
    data class Error(val message: String) : MatchDetailUiState
}

class MatchDetailViewModel(
    private val matchRepository: MatchRepository,
    private val teamRepository: TeamRepository,
    private val matchId: String,
    private val highlights: YoutubeHighlightsSource = YoutubeHighlightsSource()
) : ViewModel() {

    private val _uiState = MutableStateFlow<MatchDetailUiState>(MatchDetailUiState.Loading)
    val uiState: StateFlow<MatchDetailUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** Pull-to-refresh: recarga el partido (marcador, stats, timeline) sin el spinner de pantalla. */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                runCatching { buildState() }.getOrNull()?.let { _uiState.value = it }
                resolveHighlight()
                resolveLineups()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

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
                    // Si el partido acaba de finalizar, traer el resumen; si FIFA publicó los XI, traerlos.
                    launch { resolveHighlight() }
                    launch { resolveLineups() }
                }
            }
        }
    }

    fun retry() = load()

    /**
     * Estado base (rápido): partido, estrellas y probabilidad. NO resuelve el video de resumen,
     * que es lento (scraping de YouTube) y se completa aparte. Conserva el videoId ya resuelto.
     */
    private suspend fun buildState(): MatchDetailUiState {
        val match = matchRepository.match(matchId) ?: return MatchDetailUiState.Error("Partido no encontrado")
        val prev = _uiState.value as? MatchDetailUiState.Success
        val existingVideo = prev?.highlightVideoId
        val existingLineups = prev?.lineups
        // En un partido finalizado no se muestran estrellas ni probabilidad (son la previa):
        // evitamos las llamadas a team() para que las stats/timeline aparezcan al instante.
        if (match.status == MatchStatus.FINISHED) {
            return MatchDetailUiState.Success(match, null, null, null, existingVideo, existingLineups)
        }
        // Equipos para la previa (solo si el cruce ya tiene equipos definidos).
        val homeTeam = match.teamA.takeIf { it.isNotBlank() }
            ?.let { runCatching { teamRepository.team(it) }.getOrNull() }
        val awayTeam = match.teamB.takeIf { it.isNotBlank() }
            ?.let { runCatching { teamRepository.team(it) }.getOrNull() }
        // Probabilidad 90' por ranking FIFA: solo antes/durante el partido y con ambos ranks.
        val winProb = if (homeTeam != null && awayTeam != null && homeTeam.rank > 0 && awayTeam.rank > 0
        ) winProbabilityFromRanks(homeTeam.rank, awayTeam.rank) else null
        return MatchDetailUiState.Success(match, homeTeam?.star, awayTeam?.star, winProb, existingVideo, existingLineups)
    }

    /** Trae los XI confirmados (FIFA) y los inyecta en el estado ya visible; no-op si aún no hay. */
    private suspend fun resolveLineups() {
        val cur = _uiState.value as? MatchDetailUiState.Success ?: return
        if (cur.lineups != null) return
        val lineups = runCatching { matchRepository.lineups(matchId) }.getOrNull() ?: return
        (_uiState.value as? MatchDetailUiState.Success)?.let { _uiState.value = it.copy(lineups = lineups) }
    }

    /** Resuelve el resumen en video (partidos finalizados) y lo inyecta en el estado ya visible. */
    private suspend fun resolveHighlight() {
        val cur = _uiState.value as? MatchDetailUiState.Success ?: return
        val match = cur.match
        if (cur.highlightVideoId != null) return
        if (match.status != MatchStatus.FINISHED || match.teamA.isBlank() || match.teamB.isBlank()) return
        val query = "${Nations.name(match.teamA)} ${Nations.name(match.teamB)} Mundial 2026 resumen ESPN"
        val videoId = runCatching { highlights.firstVideoId(query) }.getOrNull() ?: return
        (_uiState.value as? MatchDetailUiState.Success)?.let { _uiState.value = it.copy(highlightVideoId = videoId) }
    }

    private fun load() {
        _uiState.value = MatchDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                buildState()
            } catch (e: Exception) {
                MatchDetailUiState.Error(e.message ?: "Error al cargar el partido")
            }
            // El video de resumen y los XI se resuelven aparte para no retrasar stats/timeline.
            launch { resolveHighlight() }
            launch { resolveLineups() }
        }
    }

    companion object {
        fun Factory(
            matchRepository: MatchRepository, teamRepository: TeamRepository, matchId: String,
            highlights: YoutubeHighlightsSource = YoutubeHighlightsSource()
        ) = viewModelFactory {
            initializer { MatchDetailViewModel(matchRepository, teamRepository, matchId, highlights) }
        }
    }
}
