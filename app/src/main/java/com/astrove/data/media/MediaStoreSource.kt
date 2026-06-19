package com.astrove.data.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

/** A screenshot as seen by MediaStore, before indexing. */
data class MediaItem(
    val mediaId: Long,
    val uri: String,
    val displayName: String,
    val dateAdded: Long,
    val dateTaken: Long?,
    val width: Int,
    val height: Int,
    val mime: String,
    val bucket: String,
    val sizeBytes: Long,
)

/**
 * Reads screenshots from shared storage. Matches both `Pictures/Screenshots`
 * (Pixel/most) and `DCIM/Screenshots` (Samsung/Xiaomi), case-insensitive,
 * preferring path over the localized bucket name where available.
 */
class MediaStoreSource(private val context: Context) {

    fun contentUri(mediaId: Long): Uri =
        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId)

    /** Screenshots with DATE_ADDED strictly greater than [sinceSeconds]. */
    fun querySince(sinceSeconds: Long): List<MediaItem> {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val usePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        val projection = buildList {
            add(MediaStore.Images.Media._ID)
            add(MediaStore.Images.Media.DISPLAY_NAME)
            add(MediaStore.Images.Media.DATE_ADDED)
            add(MediaStore.Images.Media.DATE_TAKEN)
            add(MediaStore.Images.Media.WIDTH)
            add(MediaStore.Images.Media.HEIGHT)
            add(MediaStore.Images.Media.MIME_TYPE)
            add(MediaStore.Images.Media.SIZE)
            add(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            if (usePath) add(MediaStore.Images.Media.RELATIVE_PATH)
            else @Suppress("DEPRECATION") add(MediaStore.Images.Media.DATA)
        }.toTypedArray()

        val selection: String
        val args: Array<String>
        if (usePath) {
            selection = "(${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? COLLATE NOCASE " +
                "OR ${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} LIKE ? COLLATE NOCASE) " +
                "AND ${MediaStore.Images.Media.DATE_ADDED} > ?"
            args = arrayOf("%Screenshots%", "%Screenshot%", sinceSeconds.toString())
        } else {
            @Suppress("DEPRECATION")
            selection = "(${MediaStore.Images.Media.DATA} LIKE ? COLLATE NOCASE " +
                "OR ${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} LIKE ? COLLATE NOCASE) " +
                "AND ${MediaStore.Images.Media.DATE_ADDED} > ?"
            args = arrayOf("%/Screenshots/%", "%Screenshot%", sinceSeconds.toString())
        }

        val sort = "${MediaStore.Images.Media.DATE_ADDED} ASC"
        val out = ArrayList<MediaItem>()

        context.contentResolver.query(collection, projection, selection, args, sort)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val takenCol = c.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
            val wCol = c.getColumnIndex(MediaStore.Images.Media.WIDTH)
            val hCol = c.getColumnIndex(MediaStore.Images.Media.HEIGHT)
            val mimeCol = c.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
            val sizeCol = c.getColumnIndex(MediaStore.Images.Media.SIZE)
            val bucketCol = c.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val taken = if (takenCol >= 0 && !c.isNull(takenCol)) c.getLong(takenCol) else null
                out.add(
                    MediaItem(
                        mediaId = id,
                        uri = contentUri(id).toString(),
                        displayName = c.getString(nameCol) ?: "screenshot",
                        dateAdded = c.getLong(addedCol),
                        dateTaken = taken,
                        width = if (wCol >= 0) c.getInt(wCol) else 0,
                        height = if (hCol >= 0) c.getInt(hCol) else 0,
                        mime = if (mimeCol >= 0) c.getString(mimeCol) ?: "image/*" else "image/*",
                        sizeBytes = if (sizeCol >= 0) c.getLong(sizeCol) else 0L,
                        bucket = if (bucketCol >= 0) c.getString(bucketCol) ?: "Screenshots" else "Screenshots",
                    )
                )
            }
        }
        return out
    }
}
