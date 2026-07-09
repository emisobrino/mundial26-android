package com.mundial26.app.notifications

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.mundial26.app.MundialApplication
import com.mundial26.app.domain.model.EventKind
import com.mundial26.app.domain.model.Match
import com.mundial26.app.domain.model.MatchEvent
import com.mundial26.app.domain.model.Nations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano: mientras hay partidos en vivo, sondea cada 30 s y avisa cada gol nuevo
 * (jugador, país, minuto). Se auto-detiene cuando ya no hay partidos en vivo.
 */
class LiveGoalsService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var running = false

    companion object {
        fun start(ctx: Context) =
            ContextCompat.startForegroundService(ctx, Intent(ctx, LiveGoalsService::class.java))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this, Notif.ONGOING_ID, Notif.ongoing(this, "Siguiendo los goles en vivo"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
        if (!running) { running = true; loop() }
        return START_STICKY
    }

    private fun loop() = scope.launch {
        val repo = (application as MundialApplication).container.matchRepository
        var emptyStreak = 0
        var firstPass = true   // el primer barrido siembra los goles ya ocurridos sin notificar (evita ráfaga al arrancar)
        while (isActive) {
            if (!NotifPrefs.goalsEnabled(applicationContext)) break
            val live = runCatching { repo.liveMatches() }.getOrDefault(emptyList())
            if (live.isEmpty()) {
                // ~4 min de gracia: cubre el retraso de datos justo tras el KO antes de rendirse.
                if (++emptyStreak >= 8) break
            } else {
                emptyStreak = 0
                val seen = NotifPrefs.seenGoals(applicationContext)
                for (m in live) {
                    val full = runCatching { repo.match(m.id) }.getOrNull() ?: m
                    full.events.filter { it.kind == EventKind.GOAL }.forEach { ev ->
                        val key = "${full.id}:${ev.min}:${ev.player}"
                        if (key !in seen) {
                            seen.add(key)
                            NotifPrefs.markGoalSeen(applicationContext, key)
                            if (!firstPass) postGoal(full, ev)
                        }
                    }
                }
                firstPass = false
            }
            delay(30_000)
        }
        stopSelf()
    }

    private fun postGoal(m: Match, ev: MatchEvent) {
        val country = if (ev.side == 'A') m.teamA else m.teamB
        Notif.post(
            this, id = "${m.id}:${ev.min}:${ev.player}".hashCode(), channel = Notif.CH_GOALS,
            title = "⚽ ¡Gol de ${Nations.name(country)}!",
            text = "${ev.player} · ${ev.min}'  (${m.teamA} ${m.scoreA ?: 0}-${m.scoreB ?: 0} ${m.teamB})"
        )
    }

    override fun onDestroy() {
        scope.cancel(); running = false
        super.onDestroy()
    }
}
