package com.mundial26.app.ui.teams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mundial26.app.domain.model.Confederation
import com.mundial26.app.domain.model.Group
import com.mundial26.app.domain.model.Team
import com.mundial26.app.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Sub-vista seleccionada en la pantalla Equipos. */
enum class TeamsView { LIST, STANDINGS }

/** Filtro de confederación para la grilla de selecciones. */
enum class ConfFilter(val label: String, val conf: Confederation?) {
    ALL("Todos", null),
    UEFA("UEFA", Confederation.UEFA),
    CONMEBOL("CONMEBOL", Confederation.CONMEBOL),
    CONCACAF("CONCACAF", Confederation.CONCACAF)
}

/**
 * Estado de UI de la pantalla Equipos.
 */
sealed interface TeamsUiState {
    data object Loading : TeamsUiState
    data class Success(
        val teams: List<Team>,
        val groups: List<Group>
    ) : TeamsUiState
    data class Error(val message: String) : TeamsUiState
}

/**
 * ViewModel de la pantalla Equipos: agrega selecciones (ordenadas por ranking) y grupos,
 * y maneja la sub-vista activa (Selecciones / Grupos) y el filtro de confederación.
 */
class TeamsViewModel(
    private val teamRepository: TeamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TeamsUiState>(TeamsUiState.Loading)
    val uiState: StateFlow<TeamsUiState> = _uiState.asStateFlow()

    private val _view = MutableStateFlow(TeamsView.LIST)
    val view: StateFlow<TeamsView> = _view.asStateFlow()

    private val _conf = MutableStateFlow(ConfFilter.ALL)
    val conf: StateFlow<ConfFilter> = _conf.asStateFlow()

    init {
        load()
    }

    /** Reintenta la carga de datos tras un error. */
    fun retry() {
        load()
    }

    /** Cambia la sub-vista activa (Selecciones / Grupos). */
    fun selectView(view: TeamsView) {
        _view.value = view
    }

    /** Cambia el filtro de confederación activo. */
    fun selectConf(conf: ConfFilter) {
        _conf.value = conf
    }

    /** Filtra la lista de equipos por la confederación seleccionada (null = todos). */
    fun filteredTeams(all: List<Team>, conf: ConfFilter): List<Team> =
        if (conf.conf == null) all else all.filter { it.conf == conf.conf }

    private fun load() {
        _uiState.value = TeamsUiState.Loading
        viewModelScope.launch {
            try {
                _uiState.value = TeamsUiState.Success(
                    teams = teamRepository.teams().sortedBy { it.rank },
                    groups = teamRepository.groups()
                )
            } catch (e: Exception) {
                _uiState.value = TeamsUiState.Error(e.message ?: "Error al cargar Equipos")
            }
        }
    }

    companion object {
        fun Factory(teamRepository: TeamRepository) = viewModelFactory {
            initializer {
                TeamsViewModel(teamRepository)
            }
        }
    }
}
