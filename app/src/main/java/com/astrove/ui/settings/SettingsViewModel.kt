package com.astrove.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrove.data.ScreenshotRepository
import com.astrove.data.prefs.ThemeMode
import com.astrove.ui.theme.applyNightMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val total: Int = 0,
    val read: Int = 0,
    val pending: Int = 0,
    val chargingOnly: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

class SettingsViewModel(private val repo: ScreenshotRepository) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        repo.observeCount(),
        repo.observeOcrPending(),
        repo.chargingOnly,
        repo.themeMode,
    ) { total, pending, chargingOnly, themeMode ->
        SettingsUiState(
            total = total,
            read = (total - pending).coerceAtLeast(0),
            pending = pending,
            chargingOnly = chargingOnly,
            themeMode = themeMode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setChargingOnly(enabled: Boolean) {
        viewModelScope.launch { repo.setChargingOnly(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        applyNightMode(mode)
        viewModelScope.launch { repo.setThemeMode(mode) }
    }
}
