package com.astrove.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.astrove.TroveApp

/** Reconciles MediaStore → Room (metadata + near-dup hash), resumable via the watermark. */
class IndexWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repo = (applicationContext as TroveApp).container.repository
        return try {
            repo.reconcile()
            Result.success()
        } catch (e: Throwable) {
            Result.retry()
        }
    }
}
