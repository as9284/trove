package com.astrove.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrove.data.ScreenshotRepository
import com.astrove.data.db.ScreenshotEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val totalCount: Int = 0,
    val ocrPending: Int = 0,
    val resurfacing: List<ScreenshotEntity> = emptyList(),
    val pinned: List<ScreenshotEntity> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val loading: Boolean = true,
)

class HomeViewModel(private val repo: ScreenshotRepository) : ViewModel() {

    private val resurfacing = MutableStateFlow<List<ScreenshotEntity>>(emptyList())

    val state: StateFlow<HomeUiState> = combine(
        repo.observeCount(),
        repo.observeOcrPending(),
        repo.recentSearches,
        repo.observePinned(),
        resurfacing,
    ) { count, pending, recent, pinned, resurf ->
        HomeUiState(
            totalCount = count,
            ocrPending = pending,
            resurfacing = resurf,
            pinned = pinned,
            recentSearches = recent,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { resurfacing.value = repo.resurfacing() }
    }
}
