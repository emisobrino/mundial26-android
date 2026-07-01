package com.mundial26.app.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mundial26.app.domain.model.NewsItem
import com.mundial26.app.domain.repository.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NewsUiState {
    data object Loading : NewsUiState
    data class Success(val news: List<NewsItem>) : NewsUiState
    data class Error(val message: String) : NewsUiState
}

class NewsViewModel(private val newsRepo: NewsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    /** Pull-to-refresh: invalida la caché de noticias y recarga. */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                newsRepo.refresh()
                loadData()
            } catch (_: Exception) {
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun load() {
        _uiState.value = NewsUiState.Loading
        viewModelScope.launch { loadData() }
    }

    private suspend fun loadData() {
        _uiState.value = try {
            val news = newsRepo.news()
            NewsUiState.Success(news)
        } catch (e: Exception) {
            NewsUiState.Error(e.message ?: "No se pudieron cargar las noticias")
        }
    }

    companion object {
        fun Factory(newsRepo: NewsRepository) = viewModelFactory {
            initializer {
                NewsViewModel(newsRepo)
            }
        }
    }
}
