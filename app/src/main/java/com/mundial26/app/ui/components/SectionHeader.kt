package com.mundial26.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mundial26.app.ui.theme.MundialTheme

/**
 * Encabezado de sección: título a la izquierda, link de acción opcional a la derecha
 * (ej. "Resultados & próximos" ... "Ver todo").
 */
@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MundialTheme.colors.textPrimary
        )
        if (actionText != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodyMedium,
                color = MundialTheme.colors.brand,
                modifier = Modifier.clickable(enabled = onAction != null) { onAction?.invoke() }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SectionHeaderPreview() {
    MundialTheme {
        SectionHeader(
            title = "Resultados & próximos",
            actionText = "Ver todo",
            onAction = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
