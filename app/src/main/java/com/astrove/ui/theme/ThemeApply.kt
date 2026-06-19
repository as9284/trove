package com.astrove.ui.theme

import androidx.appcompat.app.AppCompatDelegate
import com.astrove.data.prefs.ThemeMode

/**
 * Pushes the chosen theme into AppCompat's day/night machinery. On API 31+ this
 * also tells the system the app's night mode, so the launch (splash) window
 * background matches even when the choice overrides the system setting.
 */
fun applyNightMode(mode: ThemeMode) {
    AppCompatDelegate.setDefaultNightMode(
        when (mode) {
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        },
    )
}
