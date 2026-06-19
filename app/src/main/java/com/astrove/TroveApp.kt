package com.astrove

import android.app.Application
import com.astrove.data.prefs.readThemeModeSync
import com.astrove.di.AppContainer
import com.astrove.ui.theme.applyNightMode

class TroveApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Apply the saved theme before any activity inflates, so day/night (and the
        // splash window background) are correct from the first frame of this process.
        applyNightMode(readThemeModeSync(this))
        container = AppContainer(this)
        container.startOcrScheduler()
    }
}
