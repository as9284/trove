package com.astrove.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astrove.data.ShotCategory
import com.astrove.ui.components.ScreenshotThumb
import com.astrove.ui.troveViewModel
import com.astrove.ui.util.formatAge

@Composable
fun SearchRoute(initialQuery: String, onOpenShot: (Long) -> Unit) {
    val vm = troveViewModel { SearchViewModel(it.repository) }
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            vm.setQuery(initialQuery)
            vm.recordSearch()
        }
    }
    val state by vm.state.collectAsStateWithLifecycle()
    SearchScreen(
        state = state,
        onQueryChange = vm::setQuery,
        onSubmit = vm::recordSearch,
        onCategory = vm::setCategory,
        onOpenShot = onOpenShot,
    )
}

@Composable
private fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCategory: (ShotCategory) -> Unit,
    onOpenShot: (Long) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        if (state.query.isEmpty()) focusRequester.requestFocus()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                .focusRequester(focusRequester),
            placeholder = { Text("Search your screenshots") },
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit(); keyboard?.hide() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
        )

        if (state.availableCategories.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterPill("All", state.category == null) { if (state.category != null) onCategory(state.category) }
                state.availableCategories.forEach { cat ->
                    FilterPill(cat.label, state.category == cat) { onCategory(cat) }
                }
            }
        }

        when {
            state.query.isBlank() -> RecentList(state.recent, onQueryChange)
            state.results.isEmpty() && !state.searching ->
                EmptyMessage("Nothing matches “${state.query}” yet.")
            else -> ResultList(state, onOpenShot)
        }
    }
}

@Composable
private fun ResultList(state: SearchUiState, onOpenShot: (Long) -> Unit) {
    val bg = MaterialTheme.colorScheme.primary
    val fg = MaterialTheme.colorScheme.onPrimary
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        items(state.results, key = { it.shot.mediaId }) { result ->
            Column(modifier = Modifier.animateItem()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenShot(result.shot.mediaId) }
                        .padding(vertical = 12.dp),
                ) {
                    ScreenshotThumb(
                        uri = result.shot.uri,
                        modifier = Modifier.size(52.dp).clip(MaterialTheme.shapes.small),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            highlight(result.snippet, state.tokens, bg, fg),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "${result.shot.category.label} · ${formatAge(result.shot.dateAdded)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun RecentList(recent: List<String>, onPick: (String) -> Unit) {
    if (recent.isEmpty()) {
        EmptyMessage("Type a word you remember and Trove will find the screenshot.")
        return
    }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            "Recent",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
        )
        Spacer(Modifier.height(8.dp))
        recent.forEach { q ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onPick(q) }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text(q, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

private fun highlight(snippet: String, tokens: List<String>, bg: Color, fg: Color): AnnotatedString {
    if (tokens.isEmpty() || snippet.isEmpty()) return AnnotatedString(snippet)
    val lower = snippet.lowercase()
    val ranges = ArrayList<IntRange>()
    for (t in tokens) {
        var i = lower.indexOf(t)
        while (i >= 0) {
            ranges.add(i until i + t.length)
            i = lower.indexOf(t, i + t.length)
        }
    }
    if (ranges.isEmpty()) return AnnotatedString(snippet)
    return buildAnnotatedString {
        append(snippet)
        val style = SpanStyle(background = bg, color = fg)
        ranges.forEach { addStyle(style, it.first, it.last + 1) }
    }
}
