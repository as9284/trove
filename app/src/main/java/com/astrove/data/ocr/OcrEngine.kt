package com.astrove.data.ocr

import android.net.Uri

/** Result of an OCR attempt. */
sealed interface OcrResult {
    data class Success(val text: String) : OcrResult
    /** Decoded but recognition failed (retryable). */
    data object Failed : OcrResult
    /** Image could not be opened/decoded (cloud-only, stale row). */
    data object Unavailable : OcrResult
}

/** On-device OCR. Implementations must be safe to call sequentially from a worker. */
interface OcrEngine {
    suspend fun recognize(uri: Uri): OcrResult
    fun close()
}
