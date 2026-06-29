package com.mundial26.app.data.remote.openfootball

import kotlinx.serialization.Serializable

@Serializable data class OfGoal(val name: String = "", val minute: Int? = null)
@Serializable data class OfMatch(val date: String = "", val team1: String = "", val team2: String = "", val goals1: List<OfGoal> = emptyList(), val goals2: List<OfGoal> = emptyList())
@Serializable data class OfWorldCup(val name: String = "", val matches: List<OfMatch> = emptyList())
