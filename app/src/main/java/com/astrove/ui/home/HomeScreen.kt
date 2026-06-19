package com.astrove.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astrove.R
import com.astrove.data.db.ScreenshotEntity
import com.astrove.ui.components.ScreenshotThumb
import com.astrove.ui.components.TroveMark
import com.astrove.ui.troveViewModel
import com.astrove.ui.util.formatAge
import com.astrove.ui.util.formatCount

@Composable
fun HomeRoute(
    onOpenSearch: () -> Unit,
    onSearchQuery: (String) -> Unit,
    onOpenShot: (Long) -> Unit,
    onOpenGallery: () -> Unit,
    onOpenCleanup: () -> Unit,
) {
    val vm = troveViewModel { HomeViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    HomeScreen(state, onOpenSearch, onSearchQuery, onOpenShot, onOpenGallery, onOpenCleanup)
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenSearch: () -> Unit,
    onSearchQuery: (String) -> Unit,
    onOpenShot: (Long) -> Unit,
    onOpenGallery: () -> Unit,
    onOpenCleanup: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TroveMark(size = 30.dp)
            Spacer(Modifier.width(10.dp))
            Text("Trove", style = MaterialTheme.typography.displaySmall)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(26.dp))
        SearchEntry(onClick = onOpenSearch)

        if (state.ocrPending > 0 && state.totalCount > 0) {
            Spacer(Modifier.height(16.dp))
            IndexingBanner(done = state.totalCount - state.ocrPending, total = state.totalCount)
        }

        if (state.pinned.isNotEmpty()) {
            SectionHeader("Pinned")
            Shelf(items = state.pinned, onOpenShot = onOpenShot, showAge = false)
        }

        if (state.resurfacing.isNotEmpty()) {
            SectionHeader("On this day")
            Shelf(items = state.resurfacing, onOpenShot = onOpenShot, showAge = true)
        }

        if (state.recentSearches.isNotEmpty()) {
            SectionHeader("Recent searches")
            RecentSearches(state.recentSearches, onSearchQuery)
        }

        Spacer(Modifier.height(28.dp))
        ActionRow(
            icon = { Icon(Icons.Outlined.GridView, null, Modifier.size(20.dp)) },
            label = "Browse all ${formatCount(state.totalCount)} screenshots",
            onClick = onOpenGallery,
        )
        Spacer(Modifier.height(4.dp))
        ActionRow(
            icon = { Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(20.dp)) },
            label = "Review duplicates",
            onClick = onOpenCleanup,
        )

        if (!state.loading && state.totalCount == 0 && state.ocrPending == 0) {
            Spacer(Modifier.height(32.dp))
            Text(
                text = "No screenshots yet. New ones show up here on their own.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Spacer(Modifier.height(28.dp))
    Text(text, style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp))
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun SearchEntry(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "Search the words in your screenshots",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IndexingBanner(done: Int, total: Int) {
    val fraction = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
    Column {
        Text(
            "Reading your screenshots… ${formatCount(done)} of ${formatCount(total)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Search keeps getting better as this finishes. Leave Trove open and it'll keep going.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Shelf(items: List<ScreenshotEntity>, onOpenShot: (Long) -> Unit, showAge: Boolean) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.mediaId }) { shot ->
            Column(modifier = Modifier.width(96.dp)) {
                ScreenshotThumb(
                    uri = shot.uri,
                    modifier = Modifier
                        .width(96.dp)
                        .height(120.dp)
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onOpenShot(shot.mediaId) },
                )
                if (showAge) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        formatAge(shot.dateAdded),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentSearches(items: List<String>, onClick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { query ->
            Surface(
                onClick = { onClick(query) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Outlined.History, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(query, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ActionRow(icon: @Composable () -> Unit, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
}
