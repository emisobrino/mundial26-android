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

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        _uiState.value = NewsUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val news = newsRepo.news()
                NewsUiState.Success(news)
            } catch (e: Exception) {
                NewsUiState.Error(e.message ?: "No se pudieron cargar las noticias")
            }
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
