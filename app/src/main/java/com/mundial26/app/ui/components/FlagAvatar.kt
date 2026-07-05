package com.mundial26.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mundial26.app.ui.theme.MicroLabel
import com.mundial26.app.ui.theme.MundialTheme

/**
 * Bandera circular de equipo. Las banderas están empaquetadas como recursos
 * (drawable `flag_<iso>`), así que cargan al instante, sin red ni fallos.
 * Si el ISO no tiene bandera, muestra un placeholder con el código del equipo.
 */
@Composable
fun FlagAvatar(
    iso: String,
    code: String,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resId = remember(iso) {
        if (iso.isBlank()) 0
        else context.resources.getIdentifier(
            "flag_" + iso.replace("-", "_"), "drawable", context.packageName
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (resId == 0) {
                    Modifier
                        .background(MundialTheme.colors.surface)
                        .border(1.dp, MundialTheme.colors.border, CircleShape)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (resId != 0) {
            Image(
                painter = painterResource(resId),
                contentDescription = code,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
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
