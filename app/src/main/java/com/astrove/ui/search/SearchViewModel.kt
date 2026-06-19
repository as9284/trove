package com.astrove.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrove.data.ScreenshotRepository
import com.astrove.data.ShotCategory
import com.astrove.data.db.ScreenshotEntity
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchResultUi(val shot: ScreenshotEntity, val snippet: String)

data class SearchUiState(
    val query: String = "",
    val tokens: List<String> = emptyList(),
    val results: List<SearchResultUi> = emptyList(),
    val category: ShotCategory? = null,
    val availableCategories: List<ShotCategory> = emptyList(),
    val searching: Boolean = false,
    val recent: List<String> = emptyList(),
)

@OptIn(FlowPreview::class)
class SearchViewModel(private val repo: ScreenshotRepository) : ViewModel() {

    private val queryFlow = MutableStateFlow("")
    private val categoryFlow = MutableStateFlow<ShotCategory?>(null)
    private val rawResults = MutableStateFlow<List<com.astrove.data.db.SearchRow>>(emptyList())
    private val searchingFlow = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            queryFlow.debounce(220).collectLatest { q ->
                if (q.isBlank()) {
                    rawResults.value = emptyList()
                    searchingFlow.value = false
                } else {
                    searchingFlow.value = true
                    rawResults.value = repo.searchPreview(q)
                    searchingFlow.value = false
                }
            }
        }
    }

    val state: StateFlow<SearchUiState> = combine(
        queryFlow,
        rawResults,
        categoryFlow,
        searchingFlow,
        repo.recentSearches,
    ) { q, raw, cat, searching, recent ->
        val tokens = tokenize(q)
        val available = raw.map { it.shot.category }.distinct()
        val results = raw
            .filter { cat == null || it.shot.category == cat }
            .map { SearchResultUi(it.shot, buildSnippet(it.ocrText, tokens)) }
        SearchUiState(q, tokens, results, cat, available, searching, recent)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun setQuery(q: String) { queryFlow.value = q }

    fun setCategory(c: ShotCategory) {
        categoryFlow.value = if (categoryFlow.value == c) null else c
    }

    fun recordSearch() {
        val q = queryFlow.value
        if (q.isNotBlank()) viewModelScope.launch { repo.recordSearch(q) }
    }
}

private fun tokenize(q: String): List<String> =
    q.lowercase().split(Regex("""[^\p{L}\p{N}]+""")).filter { it.length >= 2 }

private fun buildSnippet(text: String, tokens: List<String>, window: Int = 150): String {
    if (text.isBlank()) return ""
    val lower = text.lowercase()
    val firstHit = tokens.mapNotNull { t -> lower.indexOf(t).takeIf { it >= 0 } }.minOrNull() ?: 0
    val start = (firstHit - 36).coerceAtLeast(0)
    val end = (start + window).coerceAtMost(text.length)
    var snippet = text.substring(start, end).replace('\n', ' ').replace(Regex("""\s+"""), " ").trim()
    if (start > 0) snippet = "…$snippet"
    if (end < text.length) snippet = "$snippet…"
    return snippet
}
