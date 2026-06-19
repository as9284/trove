package com.astrove.data

/** OCR lifecycle for a screenshot row. */
enum class OcrStatus {
    PENDING,      // indexed, not yet OCR'd
    DONE,         // text extracted and stored
    FAILED,       // OCR attempted and errored (retryable)
    UNAVAILABLE,  // image could not be opened/decoded (cloud-only, stale)
}
