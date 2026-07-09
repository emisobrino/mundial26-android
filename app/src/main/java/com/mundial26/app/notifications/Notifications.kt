package com.mundial26.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mundial26.app.MainActivity

/** Preferencias de notificaciones (dos switches) + set de goles ya avisados. SharedPreferences: dos flags no ameritan DataStore. */
object NotifPrefs {
    private const val FILE = "notif_prefs"
    private const val REMINDER = "reminder_enabled"
    private const val GOALS = "goals_enabled"
    private const val SEEN = "seen_goals"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun reminderEnabled(ctx: Context) = sp(ctx).getBoolean(REMINDER, false)
    fun goalsEnabled(ctx: Context) = sp(ctx).getBoolean(GOALS, false)
    fun setReminder(ctx: Context, on: Boolean) = sp(ctx).edit().putBoolean(REMINDER, on).apply()
    fun setGoals(ctx: Context, on: Boolean) = sp(ctx).edit().putBoolean(GOALS, on).apply()

    fun seenGoals(ctx: Context): MutableSet<String> =
        sp(ctx).getStringSet(SEEN, emptySet())!!.toMutableSet()
    fun markGoalSeen(ctx: Context, key: String) =
        sp(ctx).edit().putStringSet(SEEN, seenGoals(ctx).apply { add(key) }).apply()
}

/** Canales, IDs y helpers de posteo. Un único punto para crear notificaciones. */
object Notif {
    const val CH_MATCHES = "matches"
    const val CH_GOALS = "goals"
    const val CH_LIVE = "live"
    const val ONGOING_ID = 1001

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_MATCHES, "Recordatorios de partidos", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(CH_GOALS, "Goles", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(CH_LIVE, "Seguimiento en vivo", NotificationManager.IMPORTANCE_LOW))
    }

    private fun smallIcon(ctx: Context) = ctx.applicationInfo.icon

    fun post(ctx: Context, id: Int, channel: String, title: String, text: String, matchId: String? = null) {
        ensureChannels(ctx)
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(smallIcon(ctx))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // Tocar la notificación abre la app directo en el detalle del partido.
            .apply {
                if (matchId != null) setContentIntent(
                    PendingIntent.getActivity(
                        ctx, id,
                        Intent(ctx, MainActivity::class.java)
                            .putExtra("matchId", matchId)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
            }
            .build()
        // ponytail: si falta el permiso POST_NOTIFICATIONS (API 33+) no revienta, simplemente no se muestra.
        if (NotificationManagerCompat.from(ctx).areNotificationsEnabled()) {
            runCatching { NotificationManagerCompat.from(ctx).notify(id, n) }
        }
    }

    /** Notificación fija del servicio en vivo (obligatoria para un foreground service). */
    fun ongoing(ctx: Context, text: String): android.app.Notification {
        ensureChannels(ctx)
        return NotificationCompat.Builder(ctx, CH_LIVE)
            .setSmallIcon(smallIcon(ctx))
            .setContentTitle("Mundial 26 en vivo")
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
