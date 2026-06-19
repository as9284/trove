package com.astrove.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.astrove.data.ShotCategory

val ShotCategory.icon: ImageVector
    get() = when (this) {
        ShotCategory.RECEIPT -> Icons.AutoMirrored.Outlined.ReceiptLong
        ShotCategory.CHAT -> Icons.Outlined.ChatBubbleOutline
        ShotCategory.DOC -> Icons.Outlined.Description
        ShotCategory.LINK -> Icons.Outlined.Link
        ShotCategory.QR -> Icons.Outlined.QrCode2
        ShotCategory.TICKET -> Icons.Outlined.ConfirmationNumber
        ShotCategory.MAP -> Icons.Outlined.Place
        ShotCategory.OTHER -> Icons.Outlined.Image
    }

/** A screenshot thumbnail loaded with Coil over a paper-dim well. */
@Composable
fun ScreenshotThumb(uri: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
            .data(uri)
            .crossfade(true)
            .build(),
        contentDescription = null,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentScale = ContentScale.Crop,
    )
}

/** Fallback icon well for categories without an image (used in album chips). */
@Composable
fun CategoryWell(category: ShotCategory, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = category.icon,
            contentDescription = category.label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
    }
}
