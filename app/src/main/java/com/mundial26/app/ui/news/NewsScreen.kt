package com.mundial26.app.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mundial26.app.domain.model.NewsItem
import com.mundial26.app.ui.components.NewsCardLarge
import com.mundial26.app.ui.theme.MundialTheme

/**
 * Pantalla Noticias: lista vertical de tarjetas grandes con el detalle de cada noticia.
 */
@Composable
fun NewsScreen(
    viewModel: NewsViewModel,
    onNewsClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    when (val current = state) {
        is NewsUiState.Loading -> LoadingContent(modifier)
        is NewsUiState.Error -> ErrorContent(
            message = current.message,
            onRetry = viewModel::retry,
            modifier = modifier,
        )
        is NewsUiState.Success -> NewsContent(
            news = current.news,
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            onNewsClick = onNewsClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MundialTheme.colors.appBg),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MundialTheme.colors.brand)
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MundialTheme.colors.appBg),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MundialTheme.colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text("Reintentar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewsContent(
    news: List<NewsItem>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onNewsClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MundialTheme.colors.appBg),
    ) {
        Text(
            text = "Noticias",
            style = MaterialTheme.typography.headlineSmall,
            color = MundialTheme.colors.textPrimary,
            modifier = Modifier.padding(16.dp),
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(news, key = { it.id }) { item ->
                    NewsCardLarge(item = item, onClick = { onNewsClick(item.id) })
                }
            }
        }
    }
}
