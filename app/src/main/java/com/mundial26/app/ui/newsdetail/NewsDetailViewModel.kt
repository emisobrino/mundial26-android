package com.mundial26.app.ui.newsdetail

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

sealed interface NewsDetailUiState {
    data object Loading : NewsDetailUiState
    data class Success(val item: NewsItem) : NewsDetailUiState
    data class Error(val message: String) : NewsDetailUiState
}

class NewsDetailViewModel(
    private val newsRepository: NewsRepository,
    private val newsId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<NewsDetailUiState>(NewsDetailUiState.Loading)
    val uiState: StateFlow<NewsDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() = load()

    private fun load() {
        _uiState.value = NewsDetailUiState.Loading
        viewModelScope.launch {
            try {
                val item = newsRepository.item(newsId)
                _uiState.value = if (item != null) {
                    NewsDetailUiState.Success(item)
                } else {
                    NewsDetailUiState.Error("Noticia no encontrada")
                }
            } catch (e: Exception) {
                _uiState.value = NewsDetailUiState.Error(e.message ?: "Error al cargar la noticia")
            }
        }
    }

    companion object {
        fun Factory(newsRepository: NewsRepository, newsId: String) = viewModelFactory {
            initializer { NewsDetailViewModel(newsRepository, newsId) }
        }
    }
}
