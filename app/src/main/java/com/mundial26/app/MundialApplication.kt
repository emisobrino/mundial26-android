package com.mundial26.app
import android.app.Application
import com.mundial26.app.di.AppContainer
import com.mundial26.app.di.DefaultAppContainer

class MundialApplication : Application() {
    lateinit var container: AppContainer
    override fun onCreate() { super.onCreate(); container = DefaultAppContainer(this) }
}
