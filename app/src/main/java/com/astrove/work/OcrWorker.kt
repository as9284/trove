package com.astrove.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.astrove.TroveApp

/**
 * Charging-only background OCR (used when the setting is enabled). Resumable: a
 * killed run just leaves rows PENDING, and per-row status prevents double work.
 */
class OcrWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as TroveApp).container
        return try {
            drainOcr(container.repository, container.ocrEngine) { !isStopped }
            Result.success()
        } catch (e: Throwable) {
            Result.retry()
        }
    }
}
