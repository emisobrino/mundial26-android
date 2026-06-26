package com.mundial26.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mundial26.app.ui.theme.MundialTheme

/**
 * Logo de marca "26": caja redondeada oscura con "26" en itálica/negra
 * centrado en el área por encima de una franja tricolor (verde / rojo / azul)
 * en el borde inferior.
 */
@Composable
fun BrandLogo26(size: Dp = 40.dp) {
    val stripeHeight = size * 0.10f
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(MundialTheme.colors.surfaceDark)
    ) {
        // "26" centrado en el espacio por encima de la franja (reservamos su alto abajo).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = stripeHeight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "26",
                color = Color.White,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.46f).sp
            )
        }
        // Franja tricolor pegada al borde inferior.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(stripeHeight)
                .align(Alignment.BottomCenter)
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MundialTheme.colors.win))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MundialTheme.colors.live))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MundialTheme.colors.brand))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BrandLogo26Preview() {
    MundialTheme {
        BrandLogo26(size = 80.dp)
    }
}
