package com.astrove.data.media

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri

/**
 * 64-bit difference-hash (dHash). Encodes horizontal gradients, which is far more
 * discriminative than an average-hash on high-key (mostly-white) text screenshots.
 */
object ImageHasher {

    fun hash(resolver: ContentResolver, uri: Uri): Long {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val longEdge = maxOf(bounds.outWidth, bounds.outHeight)
            val opts = BitmapFactory.Options().apply {
                inSampleSize = if (longEdge > 72) Integer.highestOneBit(longEdge / 36) else 1
            }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return 0L

            // 9x8 so each row yields 8 left>right comparisons → 64 bits.
            val small = Bitmap.createScaledBitmap(decoded, 9, 8, true)
            if (small != decoded) decoded.recycle()
            val px = IntArray(72)
            small.getPixels(px, 0, 9, 0, 0, 9, 8)
            small.recycle()

            fun gray(i: Int): Int {
                val c = px[i]
                return (Color.red(c) * 0.299 + Color.green(c) * 0.587 + Color.blue(c) * 0.114).toInt()
            }

            var h = 0L
            var bit = 0
            for (row in 0 until 8) {
                for (col in 0 until 8) {
                    val left = gray(row * 9 + col)
                    val right = gray(row * 9 + col + 1)
                    if (left > right) h = h or (1L shl bit)
                    bit++
                }
            }
            if (h == 0L) 1L else h
        } catch (e: Throwable) {
            0L
        }
    }

    fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)
}
