package com.mundial26.app.di
import com.mundial26.app.data.mock.*
import com.mundial26.app.domain.repository.*

interface AppContainer {
    val matchRepository: MatchRepository
    val teamRepository: TeamRepository
    val newsRepository: NewsRepository
}

class DefaultAppContainer : AppContainer {
    // v1: implementaciones mock. En v3 se sustituyen por las de data/remote.
    override val matchRepository: MatchRepository by lazy { FakeMatchRepository() }
    override val teamRepository: TeamRepository by lazy { FakeTeamRepository() }
    override val newsRepository: NewsRepository by lazy { FakeNewsRepository() }
}
