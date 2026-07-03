package com.mundial26.app.data.remote.footballdata
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FootballDataApi {
    @GET("competitions/{c}/matches") suspend fun matches(@Path("c") c: String = "WC"): FdMatchesResponse
    @GET("competitions/{c}/standings") suspend fun standings(@Path("c") c: String = "WC"): FdStandingsResponse
    @GET("competitions/{c}/teams") suspend fun teams(@Path("c") c: String = "WC"): FdTeamsResponse
    // limit=100 para actualizar goles/asistencias de TODOS los que convirtieron (no solo el top).
    @GET("competitions/{c}/scorers") suspend fun scorers(@Path("c") c: String = "WC", @Query("limit") limit: Int = 100): FdScorersResponse
    // Perfil de persona: club actual y dorsal.
    @GET("persons/{id}") suspend fun person(@Path("id") id: Long): FdPerson
}
