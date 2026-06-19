package com.astrove.work

import android.net.Uri
import com.astrove.data.OcrStatus
import com.astrove.data.ScreenshotRepository
import com.astrove.data.ocr.OcrEngine
import com.astrove.data.ocr.OcrResult

const val OCR_BATCH = 6

/**
 * Drains PENDING rows (newest-first, set by the DAO) through the engine until
 * empty or [shouldContinue] turns false. Shared by the foreground scheduler and
 * the charging-only worker so both behave identically and stay resumable.
 */
suspend fun drainOcr(
    repo: ScreenshotRepository,
    engine: OcrEngine,
    shouldContinue: () -> Boolean,
) {
    while (shouldContinue()) {
        val batch = repo.pendingForOcr(OCR_BATCH)
        if (batch.isEmpty()) return
        for (shot in batch) {
            if (!shouldContinue()) return
            when (val result = engine.recognize(Uri.parse(shot.uri))) {
                is OcrResult.Success -> repo.saveOcrResult(shot.mediaId, result.text)
                OcrResult.Failed -> repo.setOcrStatus(shot.mediaId, OcrStatus.FAILED)
                OcrResult.Unavailable -> repo.setOcrStatus(shot.mediaId, OcrStatus.UNAVAILABLE)
            }
        }
    }
}
