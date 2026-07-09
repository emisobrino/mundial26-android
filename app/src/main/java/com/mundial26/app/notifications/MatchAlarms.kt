package com.mundial26.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.mundial26.app.MundialApplication
import com.mundial26.app.domain.model.MatchStatus
import com.mundial26.app.domain.model.Nations

private const val ACTION_REMINDER = "com.mundial26.app.REMINDER"
private const val ACTION_GOALS_START = "com.mundial26.app.GOALS_START"
private const val SP_FILE = "notif_prefs"
private const val KEY_SCHEDULED = "scheduled_keys"
private const val REMINDER_LEAD_MS = 10 * 60_000L

private fun kickoffMs(iso: String?): Long? =
    iso?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }

private fun pendingIntent(ctx: Context, key: String, action: String, create: Boolean, extras: Bundle? = null): PendingIntent? {
    val intent = Intent(ctx, AlarmReceiver::class.java).setAction(action)
    extras?.let { intent.putExtras(it) }
    var flags = PendingIntent.FLAG_IMMUTABLE
    flags = flags or if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
    return PendingIntent.getBroadcast(ctx, key.hashCode(), intent, flags)
}

/**
 * Reprograma todas las alarmas de partidos según los switches. Cancela las anteriores primero.
 * Se llama al abrir la app y al cambiar un switch. ponytail: sin BroadcastReceiver de BOOT — tras
 * reiniciar el teléfono las alarmas se pierden hasta reabrir la app; alcanza para uso personal.
 */
suspend fun syncMatchAlarms(ctx: Context) {
    val app = ctx.applicationContext
    val am = app.getSystemService(AlarmManager::class.java)
    val sp = app.getSharedPreferences(SP_FILE, Context.MODE_PRIVATE)

    // Cancelar lo programado anteriormente.
    sp.getStringSet(KEY_SCHEDULED, emptySet())!!.forEach { key ->
        val action = if (key.startsWith("rem:")) ACTION_REMINDER else ACTION_GOALS_START
        pendingIntent(app, key, action, create = false)?.let { am.cancel(it); it.cancel() }
    }

    val reminderOn = NotifPrefs.reminderEnabled(app)
    val goalsOn = NotifPrefs.goalsEnabled(app)
    if (!reminderOn && !goalsOn) {
        sp.edit().putStringSet(KEY_SCHEDULED, emptySet()).apply()
        return
    }

    val repo = (app as MundialApplication).container.matchRepository
    val now = System.currentTimeMillis()
    val scheduled = mutableSetOf<String>()

    repo.resultsAndUpcoming()
        .filter { it.status == MatchStatus.UPCOMING }
        .forEach { m ->
            val ko = kickoffMs(m.kickoff) ?: return@forEach
            if (reminderOn && ko - REMINDER_LEAD_MS > now) {
                val extras = Bundle().apply {
                    putInt("notifId", ("rem:" + m.id).hashCode())
                    putString("title", "Empieza en 10 min")
                    putString("text", "${Nations.name(m.teamA)} vs ${Nations.name(m.teamB)} · ${m.time ?: ""}")
                }
                pendingIntent(app, "rem:${m.id}", ACTION_REMINDER, create = true, extras)
                    ?.let { setExact(am, ko - REMINDER_LEAD_MS, it); scheduled.add("rem:${m.id}") }
            }
            if (goalsOn && ko > now) {
                pendingIntent(app, "gol:${m.id}", ACTION_GOALS_START, create = true)
                    ?.let { setExact(am, ko, it); scheduled.add("gol:${m.id}") }
            }
        }

    sp.edit().putStringSet(KEY_SCHEDULED, scheduled).apply()

    // Si ya hay partidos en vivo ahora mismo, arrancar el seguimiento sin esperar al KO.
    if (goalsOn && repo.liveMatches().isNotEmpty()) LiveGoalsService.start(app)
}

private fun setExact(am: AlarmManager, atMs: Long, pi: PendingIntent) {
    val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    runCatching {
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REMINDER -> Notif.post(
                ctx,
                id = intent.getIntExtra("notifId", 2000),
                channel = Notif.CH_MATCHES,
                title = intent.getStringExtra("title") ?: "Partido próximo",
                text = intent.getStringExtra("text") ?: ""
            )
            ACTION_GOALS_START -> if (NotifPrefs.goalsEnabled(ctx)) LiveGoalsService.start(ctx)
        }
    }
}
