package com.mundial26.app.data.net

import com.mundial26.app.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

val AppJson: Json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
private val jsonMedia = "application/json".toMediaType()

private fun okHttp(headers: Map<String, String> = emptyMap()) = OkHttpClient.Builder()
    .addInterceptor(Interceptor { chain ->
        val b = chain.request().newBuilder()
        headers.forEach { (k, v) -> b.header(k, v) }
        chain.proceed(b.build())
    }).build()

fun footballDataRetrofit(): Retrofit = Retrofit.Builder()
    .baseUrl("https://api.football-data.org/v4/")
    .client(okHttp(mapOf("X-Auth-Token" to BuildConfig.FOOTBALL_DATA_TOKEN)))
    .addConverterFactory(AppJson.asConverterFactory(jsonMedia)).build()

fun openFootballRetrofit(): Retrofit = Retrofit.Builder()
    .baseUrl("https://raw.githubusercontent.com/")
    .client(okHttp())
    .addConverterFactory(AppJson.asConverterFactory(jsonMedia)).build()
