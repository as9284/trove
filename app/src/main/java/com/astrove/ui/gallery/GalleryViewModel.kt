package com.astrove.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrove.data.ScreenshotRepository
import com.astrove.data.ShotCategory
import com.astrove.data.db.CategoryCount
import com.astrove.data.db.ScreenshotEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class GalleryUiState(
    val items: List<ScreenshotEntity> = emptyList(),
    val albums: List<CategoryCount> = emptyList(),
    val category: ShotCategory? = null,
    val loading: Boolean = true,
)

class GalleryViewModel(
    repo: ScreenshotRepository,
    private val category: ShotCategory?,
) : ViewModel() {

    private val itemsFlow =
        if (category == null) repo.observeAll() else repo.observeByCategory(category)

    val state: StateFlow<GalleryUiState> = combine(
        itemsFlow,
        repo.observeCategoryCounts(),
    ) { items, albums ->
        GalleryUiState(items = items, albums = albums, category = category, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GalleryUiState(category = category))
}
