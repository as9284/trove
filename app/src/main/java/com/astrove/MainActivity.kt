package com.astrove

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.astrove.ui.nav.TroveAppContent
import com.astrove.ui.permissions.PermissionGate
import com.astrove.ui.theme.TroveTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // AppCompat's day/night mode drives the configuration, so this reflects
            // the in-app choice as well as the system setting.
            val dark = isSystemInDarkTheme()
            val view = LocalView.current
            LaunchedEffect(dark) {
                val window = (view.context as Activity).window
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
            TroveTheme(darkTheme = dark) {
                PermissionGate {
                    TroveAppContent()
                }
            }
        }
    }
}
