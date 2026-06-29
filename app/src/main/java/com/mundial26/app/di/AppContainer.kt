package com.mundial26.app.di

import android.content.Context
import com.mundial26.app.data.local.db.MundialDatabase
import com.mundial26.app.data.net.footballDataRetrofit
import com.mundial26.app.data.net.openFootballRetrofit
import com.mundial26.app.data.remote.footballdata.FootballDataApi
import com.mundial26.app.data.remote.news.NewsRssSource
import com.mundial26.app.data.remote.openfootball.OpenFootballApi
import com.mundial26.app.data.repository.DefaultMatchRepository
import com.mundial26.app.data.repository.DefaultNewsRepository
import com.mundial26.app.data.repository.DefaultTeamRepository
import com.mundial26.app.data.sync.SyncManager
import com.mundial26.app.domain.repository.MatchRepository
import com.mundial26.app.domain.repository.NewsRepository
import com.mundial26.app.domain.repository.TeamRepository

interface AppContainer {
    val matchRepository: MatchRepository
    val teamRepository: TeamRepository
    val newsRepository: NewsRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val db = MundialDatabase.build(context.applicationContext)
    private val fdApi = footballDataRetrofit().create(FootballDataApi::class.java)
    private val ofApi = openFootballRetrofit().create(OpenFootballApi::class.java)
    private val sync = SyncManager(fdApi, db.teamDao(), db.playerDao(), db.standingDao(), db.matchDao(), db.syncMetaDao())

    override val teamRepository: TeamRepository by lazy { DefaultTeamRepository(sync, db.teamDao(), db.playerDao(), db.standingDao()) }
    override val matchRepository: MatchRepository by lazy { DefaultMatchRepository(sync, db.matchDao(), ofApi) }
    override val newsRepository: NewsRepository by lazy { DefaultNewsRepository(NewsRssSource()) }
}
