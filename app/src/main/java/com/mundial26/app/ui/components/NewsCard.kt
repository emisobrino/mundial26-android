package com.mundial26.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mundial26.app.data.mock.MockData
import com.mundial26.app.domain.model.NewsItem
import com.mundial26.app.ui.theme.MicroLabel
import com.mundial26.app.ui.theme.MundialTheme

/**
 * Tarjeta grande de noticia (pantalla Noticias): banda de tinte arriba con badge
 * de categoría, título, resumen y hora.
 */
@Composable
fun NewsCardLarge(
    item: NewsItem,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MundialTheme.shapes.large)
            .background(MundialTheme.colors.surface)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(Color(item.colorHex).copy(alpha = 0.16f)),
            contentAlignment = Alignment.BottomStart
        ) {
            if (item.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.tag,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(52.dp)
                )
            }
            CategoryBadge(
                tag = item.tag,
                colorHex = item.colorHex,
                modifier = Modifier.padding(16.dp)
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MundialTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MundialTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = item.time,
                style = MicroLabel,
                color = MundialTheme.colors.textTertiary
            )
        }
    }
}

/**
 * Tarjeta compacta de noticia (Inicio): miniatura cuadrada tintada a la izquierda,
 * badge + título + hora a la derecha.
 */
@Composable
fun NewsCardCompact(
    item: NewsItem,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MundialTheme.shapes.medium)
            .background(MundialTheme.colors.surface)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(item.colorHex).copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            if (item.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.tag,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            CategoryBadge(tag = item.tag, colorHex = item.colorHex)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MundialTheme.colors.textPrimary,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.time,
                style = MicroLabel,
                color = MundialTheme.colors.textTertiary
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
private fun NewsCardLargePreview() {
    MundialTheme {
        NewsCardLarge(
            item = MockData.news[0],
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NewsCardCompactPreview() {
    MundialTheme {
        NewsCardCompact(
            item = MockData.news[1],
            modifier = Modifier.padding(16.dp)
        )
    }
}
