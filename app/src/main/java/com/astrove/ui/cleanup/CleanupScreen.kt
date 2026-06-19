package com.astrove.ui.cleanup

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.astrove.data.DupGroup
import com.astrove.ui.theme.TroveMotion
import com.astrove.ui.troveViewModel
import com.astrove.ui.util.formatBytes

@Composable
fun CleanupRoute(onOpenShot: (Long) -> Unit) {
    val vm = troveViewModel { CleanupViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.onTrashed()
    }

    CleanupScreen(
        state = state,
        onToggle = vm::toggle,
        onOpenShot = onOpenShot,
        onTrash = {
            val uris = vm.selectedUris()
            if (uris.isNotEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val pi = MediaStore.createTrashRequest(context.contentResolver, uris, true)
                    trashLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                } else {
                    uris.forEach { uri ->
                        runCatching { context.contentResolver.delete(uri, null, null) }
                    }
                    vm.onTrashed()
                }
            }
        },
    )
}

@Composable
private fun CleanupScreen(
    state: CleanupUiState,
    onToggle: (Long) -> Unit,
    onOpenShot: (Long) -> Unit,
    onTrash: () -> Unit,
) {
    val sizeById = state.groups.flatMap { it.items }.associate { it.mediaId to it.sizeBytes }
    val reclaim = state.selected.sumOf { sizeById[it] ?: 0L }
    val nearDupes = state.groups.sumOf { it.items.size - 1 }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Clean up",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp),
        )

        if (state.groups.isEmpty() && !state.loading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Nothing to clean up. No duplicate screenshots here.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(formatBytes(reclaim), style = MaterialTheme.typography.displaySmall)
                Text(
                    "ready to free · ${state.groups.size} duplicate sets · $nearDupes near-duplicates",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.groups, key = { it.keepId }) { group ->
                    DupGroupCard(group, state.selected, onToggle, onOpenShot, Modifier.animateItem())
                }
            }

            val count = state.selected.size
            Column(modifier = Modifier.padding(20.dp)) {
                Button(
                    onClick = onTrash,
                    enabled = count > 0,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (count > 0) "Move $count to Trash" else "Nothing selected")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "You can get these back from your system Trash.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun DupGroupCard(
    group: DupGroup,
    selected: Set<Long>,
    onToggle: (Long) -> Unit,
    onOpenShot: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            "${group.items.size} shots · ${formatBytes(group.reclaimBytes)} reclaimable",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            group.items.take(4).forEach { shot ->
                val isKeep = shot.mediaId == group.keepId
                Box(modifier = Modifier.weight(1f).height(96.dp)) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(shot.uri).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { if (isKeep) onOpenShot(shot.mediaId) else onToggle(shot.mediaId) },
                        contentScale = ContentScale.Crop,
                    )
                    if (isKeep) {
                        Text(
                            "Keep",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .padding(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    } else {
                        SelectDot(
                            checked = shot.mediaId in selected,
                            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectDot(checked: Boolean, modifier: Modifier = Modifier) {
    val fill by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        animationSpec = TroveMotion.spec(),
        label = "dotFill",
    )
    val stroke by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = TroveMotion.spec(),
        label = "dotStroke",
    )
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.5.dp, stroke, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = scaleIn(TroveMotion.spec()) + fadeIn(),
            exit = scaleOut(TroveMotion.spec()) + fadeOut(),
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
