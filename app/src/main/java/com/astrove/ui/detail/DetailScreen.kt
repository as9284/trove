package com.astrove.ui.detail

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.astrove.data.OcrStatus
import com.astrove.data.entity.DetectedEntity
import com.astrove.data.entity.EntityType
import com.astrove.ui.theme.TroveMotion
import com.astrove.ui.troveViewModel
import com.astrove.ui.util.formatAge
import com.astrove.ui.util.formatBytes

@Composable
fun DetailRoute(id: Long, onBack: () -> Unit) {
    val vm = troveViewModel(key = "detail-$id") { DetailViewModel(it.repository, id) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.onTrashed(onBack)
    }

    DetailScreen(
        state = state,
        onBack = onBack,
        onTogglePin = { vm.setPinned(it) },
        onRetry = vm::retry,
        onCopyText = { copy(context, it) },
        onCopyEntity = { copy(context, it) },
        onShare = { shareImage(context, vm.mediaUri().toString(), state.shot?.mime ?: "image/*") },
        onDelete = {
            val uri = vm.mediaUri()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pi = MediaStore.createTrashRequest(context.contentResolver, listOf(uri), true)
                trashLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
            } else {
                runCatching { context.contentResolver.delete(uri, null, null) }
                vm.onTrashed(onBack)
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailScreen(
    state: DetailUiState,
    onBack: () -> Unit,
    onTogglePin: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onCopyText: (String) -> Unit,
    onCopyEntity: (String) -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val shot = state.shot
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { onTogglePin(!(shot?.pinned ?: false)) }) {
                AnimatedContent(
                    targetState = shot?.pinned == true,
                    transitionSpec = {
                        (scaleIn(TroveMotion.spec()) + fadeIn()) togetherWith
                            (scaleOut(TroveMotion.spec()) + fadeOut())
                    },
                    label = "pin",
                ) { pinned ->
                    Icon(
                        if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin",
                        tint = if (pinned) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, contentDescription = "Share")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Move to Trash")
            }
        }

        if (shot != null) {
            var viewerOpen by remember { mutableStateOf(false) }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(shot.uri).crossfade(true).build(),
                contentDescription = "Tap to view full screen",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .padding(horizontal = 20.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { viewerOpen = true },
                contentScale = ContentScale.Fit,
            )
            if (viewerOpen) {
                ZoomableImageViewer(uri = shot.uri, onDismiss = { viewerOpen = false })
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Text in this screenshot",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                        modifier = Modifier.weight(1f),
                    )
                    if (shot.ocrStatus == OcrStatus.DONE && !state.text.isNullOrBlank()) {
                        TextButton(onClick = { onCopyText(state.text) }) {
                            Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy")
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OcrTextBlock(status = shot.ocrStatus, text = state.text, onRetry = onRetry)

                if (state.entities.isNotEmpty()) {
                    Spacer(Modifier.height(28.dp))
                    Text("Detected", style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp))
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.entities.forEach { entity ->
                            EntityChip(entity, onClick = { onCopyEntity(entity.value) })
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
                Text(
                    meta(shot.category.label, shot.dateAdded, shot.width, shot.height, shot.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/** Full-screen image with pinch-to-zoom, pan, and double-tap to zoom. */
@Composable
private fun ZoomableImageViewer(uri: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().background(Color.Black),
        ) {
            val maxX = constraints.maxWidth.toFloat()
            val maxY = constraints.maxHeight.toFloat()
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            fun clampOffset() {
                val boundX = (maxX * (scale - 1f)) / 2f
                val boundY = (maxY * (scale - 1f)) / 2f
                offset = Offset(
                    offset.x.coerceIn(-boundX, boundX),
                    offset.y.coerceIn(-boundY, boundY),
                )
            }

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(uri).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset += pan
                            clampOffset()
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { if (scale <= 1f) onDismiss() },
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            },
                        )
                    },
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

@Composable
private fun OcrTextBlock(status: OcrStatus, text: String?, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            when {
                status == OcrStatus.DONE && !text.isNullOrBlank() -> SelectionContainer {
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                }
                status == OcrStatus.DONE -> Text(
                    "No text found in this screenshot.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                status == OcrStatus.PENDING -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("Reading text…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                status == OcrStatus.UNAVAILABLE -> Text(
                    "This image is unavailable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Column {
                    Text(
                        "Couldn’t read the text.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onRetry, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Text("Try again")
                    }
                }
            }
        }
    }
}

@Composable
private fun EntityChip(entity: DetectedEntity, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(entity.type.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
            Text(entity.value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }
    }
}

private val EntityType.icon: ImageVector
    get() = when (this) {
        EntityType.LINK -> Icons.Outlined.Link
        EntityType.EMAIL -> Icons.Outlined.Mail
        EntityType.PHONE -> Icons.Outlined.Phone
        EntityType.CODE -> Icons.Outlined.Key
        EntityType.TRACKING -> Icons.Outlined.LocalShipping
    }

private fun meta(category: String, dateAddedSec: Long, w: Int, h: Int, size: Long): String {
    val dims = if (w > 0 && h > 0) " · ${w}×${h}" else ""
    val bytes = if (size > 0) " · ${formatBytes(size)}" else ""
    return "$category · ${formatAge(dateAddedSec)}$dims$bytes"
}

private fun copy(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Trove", text))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

private fun shareImage(context: Context, uri: String, mime: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(uri))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share screenshot"))
}
