package com.mundial26.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mundial26.app.ui.theme.MicroLabel
import com.mundial26.app.ui.theme.MundialTheme

/**
 * Bandera circular de equipo. Mientras carga o si falla, muestra un placeholder
 * con borde y el código del equipo centrado.
 */
@Composable
fun FlagAvatar(
    iso: String,
    code: String,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    var showPlaceholder by remember(iso) { mutableStateOf(true) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (showPlaceholder) {
                    Modifier
                        .background(MundialTheme.colors.surface)
                        .border(1.dp, MundialTheme.colors.border, CircleShape)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (iso.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data("https://flagcdn.com/w160/$iso.png")
                    .crossfade(true)
                    .build(),
                contentDescription = code,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                onSuccess = { showPlaceholder = false },
                onError = { showPlaceholder = true },
                onLoading = { showPlaceholder = true }
            )
        }
        if (showPlaceholder) {
            androidx.compose.material3.Text(
                text = code,
                style = MicroLabel,
                color = MundialTheme.colors.textTertiary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FlagAvatarPreview() {
    MundialTheme {
        Box(modifier = Modifier.size(60.dp)) {
            FlagAvatar(iso = "ar", code = "ARG", size = 48.dp)
        }
    }
}
