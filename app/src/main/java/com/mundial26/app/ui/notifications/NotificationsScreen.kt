package com.mundial26.app.ui.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mundial26.app.notifications.NotifPrefs
import com.mundial26.app.notifications.syncMatchAlarms
import com.mundial26.app.ui.theme.MundialTheme
import kotlinx.coroutines.launch

@Composable
fun NotificationsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var reminder by remember { mutableStateOf(NotifPrefs.reminderEnabled(ctx)) }
    var goals by remember { mutableStateOf(NotifPrefs.goalsEnabled(ctx)) }

    // API 33+: pedir permiso de notificaciones al activar cualquier switch.
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    fun requestPermIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MundialTheme.colors.appBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MundialTheme.colors.surface)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = MundialTheme.colors.textPrimary)
            }
            Text("Notificaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MundialTheme.colors.textPrimary)
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SwitchRow(
                title = "Recordatorio 10 min antes del partido",
                subtitle = "Selecciones que juegan y hora de inicio",
                checked = reminder,
                onCheckedChange = {
                    reminder = it
                    NotifPrefs.setReminder(ctx, it)
                    if (it) requestPermIfNeeded()
                    scope.launch { syncMatchAlarms(ctx) }
                }
            )
            SwitchRow(
                title = "Notificación de goles",
                subtitle = "Jugador, país y minuto de cada gol en vivo",
                checked = goals,
                onCheckedChange = {
                    goals = it
                    NotifPrefs.setGoals(ctx, it)
                    if (it) requestPermIfNeeded()
                    scope.launch { syncMatchAlarms(ctx) }
                }
            )
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MundialTheme.colors.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MundialTheme.colors.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MundialTheme.colors.textSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
