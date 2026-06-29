package com.mundial26.app.data.remote.openfootball

import com.mundial26.app.domain.model.EventKind
import com.mundial26.app.domain.model.MatchEvent

fun fixEncoding(s: String): String = try { String(s.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8) } catch (e: Exception) { s }

fun OfMatch.toEvents(): List<MatchEvent> =
    (goals1.map { MatchEvent(it.minute ?: 0, EventKind.GOAL, 'A', fixEncoding(it.name)) } +
     goals2.map { MatchEvent(it.minute ?: 0, EventKind.GOAL, 'B', fixEncoding(it.name)) }).sortedBy { it.min }

fun List<OfMatch>.findEventsFor(day: String, teamAName: String, teamBName: String): List<MatchEvent> =
    firstOrNull { it.date == day && (it.team1.contains(teamAName, true) || teamAName.contains(it.team1, true)) && (it.team2.contains(teamBName, true) || teamBName.contains(it.team2, true)) }?.toEvents() ?: emptyList()
