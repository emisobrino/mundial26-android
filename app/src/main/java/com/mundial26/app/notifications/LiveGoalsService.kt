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

    // Marcador visto por partido (vida del servicio): detecta el gol por el cambio de score,
    // que llega antes que la timeline con el goleador.
    private val lastScores = mutableMapOf<String, Pair<Int, Int>>()

    private fun loop() = scope.launch {
        val repo = (application as MundialApplication).container.matchRepository
        var emptyStreak = 0
        var firstPass = true   // el primer barrido siembra el estado actual sin notificar (evita ráfaga al arrancar)
        while (isActive) {
            if (!NotifPrefs.goalsEnabled(applicationContext)) break
            val live = runCatching { repo.liveMatches() }.getOrDefault(emptyList())
            if (live.isEmpty()) {
                // ~4 min de gracia: cubre el retraso de datos justo tras el KO antes de rendirse.
                if (++emptyStreak >= 16) break
            } else {
                emptyStreak = 0
                val seen = NotifPrefs.seenGoals(applicationContext)
                for (m in live) {
                    // 1) Aviso inmediato por cambio de marcador (aún sin goleador).
                    val score = (m.scoreA ?: 0) to (m.scoreB ?: 0)
                    val prev = lastScores[m.id]
                    // Gol anulado (VAR) o corrección del feed: retirar el aviso del gol fantasma.
                    if (prev != null && score.first + score.second < prev.first + prev.second) {
                        androidx.core.app.NotificationManagerCompat.from(this@LiveGoalsService)
                            .cancel(goalNotifId(m.id, prev.first + prev.second))
                    }
                    if (prev != null && !firstPass && score.first + score.second > prev.first + prev.second) {
                        val country = if (score.first > prev.first) m.teamA else m.teamB
                        Notif.post(
                            this@LiveGoalsService,
                            id = goalNotifId(m.id, score.first + score.second),
                            channel = Notif.CH_GOALS,
                            title = "⚽ ¡Gol de ${Nations.name(country)}!",
                            text = "${m.teamA} ${score.first} - ${score.second} ${m.teamB}",
                            matchId = m.id
                        )
                    }
                    lastScores[m.id] = score
                    // 2) Con la timeline: misma notificación actualizada con goleador y minuto.
                    val full = runCatching { repo.match(m.id) }.getOrNull() ?: m
                    val goals = full.events.filter { it.kind == EventKind.GOAL }
                    goals.forEachIndexed { i, ev ->
                        val key = "${full.id}:${ev.min}:${ev.player}"
                        if (key !in seen) {
                            seen.add(key)
                            NotifPrefs.markGoalSeen(applicationContext, key)
                            if (!firstPass) postGoal(full, ev, goalIndex = i + 1)
                        }
                    }
                }
                firstPass = false
            }
            delay(15_000)
        }
        stopSelf()
    }

    // ponytail: el id por (partido, nº de gol) hace que el aviso provisional del marcador y el de
    // la timeline sean LA MISMA notificación (la 2ª reemplaza a la 1ª). Si dos goles casi
    // simultáneos llegan desordenados podría cruzarse el reemplazo; aceptable.
    private fun goalNotifId(matchId: String, goalNumber: Int) = "$matchId:goal:$goalNumber".hashCode()

    private fun postGoal(m: Match, ev: MatchEvent, goalIndex: Int) {
        val country = if (ev.side == 'A') m.teamA else m.teamB
        Notif.post(
            this, id = goalNotifId(m.id, goalIndex), channel = Notif.CH_GOALS,
            title = "⚽ ¡Gol de ${Nations.name(country)}!",
            text = "${ev.player} · ${ev.min}'  (${m.teamA} ${m.scoreA ?: 0}-${m.scoreB ?: 0} ${m.teamB})",
            matchId = m.id
        )
    }

    override fun onDestroy() {
        scope.cancel(); running = false
        super.onDestroy()
    }
}
