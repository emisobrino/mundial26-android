package com.mundial26.app.ui.newsdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mundial26.app.data.mock.FakeNewsRepository
import com.mundial26.app.data.mock.MockData
import com.mundial26.app.domain.model.NewsItem
import com.mundial26.app.ui.components.BrandLogo26
import com.mundial26.app.ui.theme.MicroLabel
import com.mundial26.app.ui.theme.MundialTheme

/**
 * Pantalla Detalle de noticia: hero tintado con badge de categoría, título,
 * byline y cuerpo en párrafos.
 */
@Composable
fun NewsDetailScreen(
    viewModel: NewsDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val current = state) {
        is NewsDetailUiState.Loading -> LoadingContent(onBack = onBack, modifier = modifier)
        is NewsDetailUiState.Error -> ErrorContent(
            message = current.message,
            onRetry = viewModel::retry,
            onBack = onBack,
            modifier = modifier
        )
        is NewsDetailUiState.Success -> NewsDetailContent(
            item = current.item,
            onBack = onBack,
            modifier = modifier
        )
    }
}

@Composable
private fun LoadingContent(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MundialTheme.colors.appBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MundialTheme.colors.brand)
        }
        BackButton(onBack = onBack, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MundialTheme.colors.appBg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MundialTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onRetry) {
                    Text("Reintentar")
                }
            }
        }
        BackButton(onBack = onBack, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun NewsDetailContent(
    item: NewsItem,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MundialTheme.colors.appBg)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color(item.colorHex).copy(alpha = 0.16f))
            ) {
                BackButton(
                    onBack = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                )
                CategoryBadge(
                    tag = item.tag,
                    colorHex = item.colorHex,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )
            }
        }
        item {
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MundialTheme.colors.textPrimary,
                modifier = Modifier.padding(16.dp)
            )
        }
        item {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrandLogo26(size = 22.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Redacción Mundial 26",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MundialTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "· ${item.time}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MundialTheme.colors.textTertiary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (item.summary.isNotBlank()) {
            item {
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MundialTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        items(item.body) { paragraph ->
            Text(
                text = paragraph,
                style = MaterialTheme.typography.bodyLarge,
                color = MundialTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (item.id.startsWith("http")) {
            item {
                val uriHandler = LocalUriHandler.current
                Button(
                    onClick = { uriHandler.openUri(item.id) },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Leer la nota")
                }
            }
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MundialTheme.colors.surface)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = MundialTheme.colors.textPrimary
            )
        }
    }
}

@Composable
private fun CategoryBadge(
    tag: String,
    colorHex: Long,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(colorHex))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = tag.uppercase(),
            style = MicroLabel,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NewsDetailScreenPreview() {
    MundialTheme {
        val viewModel: NewsDetailViewModel = viewModel(
            factory = NewsDetailViewModel.Factory(
                FakeNewsRepository(),
                MockData.news[0].id
            )
        )
        NewsDetailScreen(viewModel = viewModel, onBack = {})
    }
}
