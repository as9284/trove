package com.astrove.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrove.data.OcrStatus
import com.astrove.data.ScreenshotRepository
import com.astrove.data.db.ScreenshotEntity
import com.astrove.data.entity.DetectedEntity
import com.astrove.data.entity.EntityExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val shot: ScreenshotEntity? = null,
    val text: String? = null,
    val entities: List<DetectedEntity> = emptyList(),
)

class DetailViewModel(
    private val repo: ScreenshotRepository,
    private val id: Long,
) : ViewModel() {

    private val textFlow = MutableStateFlow<String?>(null)
    private val entitiesFlow = MutableStateFlow<List<DetectedEntity>>(emptyList())

    val state: StateFlow<DetailUiState> = combine(
        repo.observe(id),
        textFlow,
        entitiesFlow,
    ) { shot, text, entities ->
        DetailUiState(shot = shot, text = text, entities = entities)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    init {
        loadText()
        viewModelScope.launch {
            repo.observe(id).collect { shot ->
                if (shot?.ocrStatus == OcrStatus.DONE && textFlow.value.isNullOrBlank()) loadText()
            }
        }
    }

    private fun loadText() {
        viewModelScope.launch {
            val t = repo.textFor(id)
            textFlow.value = t
            entitiesFlow.value = t?.let { EntityExtractor.extract(it) } ?: emptyList()
        }
    }

    fun setPinned(pinned: Boolean) {
        viewModelScope.launch { repo.setPinned(id, pinned) }
    }

    fun retry() {
        viewModelScope.launch { repo.retryOcr(id) }
    }

    /** Marks this shot trashed locally after the system trash request succeeds. */
    fun onTrashed(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.markTrashed(listOf(id))
            onDone()
        }
    }

    fun mediaUri() = repo.mediaUri(id)
}
