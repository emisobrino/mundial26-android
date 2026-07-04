package com.mundial26.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mundial26.app.ui.theme.MundialTheme

/**
 * Control segmentado (ej. Partidos / Calendario / Llaves).
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(MundialTheme.colors.segmentInactive)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selected
            val interactionSource = remember { MutableInteractionSource() }
            // Feedback de selección: el fondo y el texto transicionan en 150 ms.
            val bg by animateColorAsState(
                targetValue = if (isSelected) MundialTheme.colors.brandDeep else Color.Transparent,
                animationSpec = tween(150, easing = FastOutSlowInEasing),
                label = "segBg"
            )
            val fg by animateColorAsState(
                targetValue = if (isSelected) Color.White else MundialTheme.colors.textTertiary,
                animationSpec = tween(150, easing = FastOutSlowInEasing),
                label = "segFg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = fg,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SegmentedControlPreview() {
    MundialTheme {
        var selected = 0
        SegmentedControl(
            options = listOf("Partidos", "Calendario", "Llaves"),
            selected = selected,
            onSelect = { selected = it },
            modifier = Modifier.padding(16.dp)
        )
    }
}
