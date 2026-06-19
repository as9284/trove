package com.astrove.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey
import com.astrove.data.OcrStatus
import com.astrove.data.ShotCategory

@Entity(tableName = "screenshots")
data class ScreenshotEntity(
    @PrimaryKey val mediaId: Long,
    val uri: String,
    val displayName: String,
    /** MediaStore DATE_ADDED, in epoch seconds. */
    val dateAdded: Long,
    /** DATE_TAKEN in epoch millis, when present. */
    val dateTaken: Long?,
    val width: Int,
    val height: Int,
    val mime: String,
    val bucket: String,
    val sizeBytes: Long,
    val category: ShotCategory = ShotCategory.OTHER,
    val ocrStatus: OcrStatus = OcrStatus.PENDING,
    /** 8x8 average-hash for near-duplicate detection; 0 = not computed. */
    val imageHash: Long = 0L,
    val pinned: Boolean = false,
    val trashed: Boolean = false,
)

/** Full-text index backing search + snippets. rowid == mediaId. */
@Fts4
@Entity(tableName = "screenshot_text")
data class ScreenshotTextFts(
    @PrimaryKey @ColumnInfo(name = "rowid") val mediaId: Long,
    val text: String,
)
