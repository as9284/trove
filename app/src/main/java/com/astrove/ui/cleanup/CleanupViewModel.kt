package com.astrove.ui.cleanup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrove.data.DupGroup
import com.astrove.data.ScreenshotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CleanupUiState(
    val groups: List<DupGroup> = emptyList(),
    val selected: Set<Long> = emptySet(),
    val loading: Boolean = true,
)

class CleanupViewModel(private val repo: ScreenshotRepository) : ViewModel() {

    private val groups = MutableStateFlow<List<DupGroup>>(emptyList())
    private val selected = MutableStateFlow<Set<Long>>(emptySet())
    private val loading = MutableStateFlow(true)

    val state: StateFlow<CleanupUiState> = combine(groups, selected, loading) { g, s, l ->
        CleanupUiState(groups = g, selected = s, loading = l)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CleanupUiState())

    init { load() }

    fun load() {
        viewModelScope.launch {
            loading.value = true
            val g = repo.dedupGroups()
            groups.value = g
            // Pre-select every shot except the best one to keep in each set.
            selected.value = g.flatMap { grp ->
                grp.items.filter { it.mediaId != grp.keepId }.map { it.mediaId }
            }.toSet()
            loading.value = false
        }
    }

    fun toggle(id: Long) {
        selected.value = selected.value.let { if (id in it) it - id else it + id }
    }

    fun selectedIds(): List<Long> = selected.value.toList()

    fun selectedUris(): List<Uri> = selected.value.map { repo.mediaUri(it) }

    fun onTrashed() {
        viewModelScope.launch {
            repo.markTrashed(selected.value.toList())
            load()
        }
    }
}
