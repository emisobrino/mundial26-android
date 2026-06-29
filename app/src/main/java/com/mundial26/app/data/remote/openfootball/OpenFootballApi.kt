package com.mundial26.app.data.remote.openfootball

import retrofit2.http.GET

interface OpenFootballApi {
    @GET("openfootball/worldcup.json/master/2026/worldcup.json")
    suspend fun worldCup(): OfWorldCup
}
