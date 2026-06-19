package com.astrove.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * On-device OCR with PaddleOCR PP-OCRv5 models on ONNX Runtime: a DBNet detector
 * finds text regions, then a CRNN recognizer reads each one. Screenshot text is
 * upright, so detection boxes are treated as axis-aligned, which keeps the whole
 * pipeline in plain Kotlin.
 */
class OnnxOcrEngine(private val context: Context) : OcrEngine {

    private val mutex = Mutex()
    private var env: OrtEnvironment? = null
    private var detSession: OrtSession? = null
    private var recSession: OrtSession? = null
    private var dict: List<String> = emptyList()
    private var initFailed = false

    private fun ensureReady(): Boolean {
        if (detSession != null && recSession != null) return true
        if (initFailed) return false
        return try {
            val e = OrtEnvironment.getEnvironment()
            val opts = OrtSession.SessionOptions()
            detSession = e.createSession(context.assets.open("ocr/det.onnx").use { it.readBytes() }, opts)
            recSession = e.createSession(context.assets.open("ocr/rec.onnx").use { it.readBytes() }, opts)
            dict = context.assets.open("ocr/keys.txt").bufferedReader().use { it.readLines() }
            env = e
            true
        } catch (t: Throwable) {
            initFailed = true
            false
        }
    }

    override suspend fun recognize(uri: Uri): OcrResult = withContext(Dispatchers.Default) {
        val bitmap = decode(uri) ?: return@withContext OcrResult.Unavailable
        try {
            mutex.withLock {
                if (!ensureReady()) return@withLock OcrResult.Failed
                val boxes = detect(bitmap)
                if (boxes.isEmpty()) return@withLock OcrResult.Success("")
                val lines = readLines(bitmap, boxes)
                OcrResult.Success(lines.joinToString("\n").trim())
            }
        } catch (t: Throwable) {
            OcrResult.Failed
        } finally {
            bitmap.recycle()
        }
    }

    override fun close() {
        detSession?.close(); detSession = null
        recSession?.close(); recSession = null
        env = null
    }

    // ---- Detection -------------------------------------------------------

    private data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int)

    private fun detect(src: Bitmap): List<Box> {
        val env = env ?: return emptyList()
        val det = detSession ?: return emptyList()
        val w = src.width
        val h = src.height
        val scale = if (max(w, h) > DET_MAX_SIDE) DET_MAX_SIDE.toFloat() / max(w, h) else 1f
        val rw = max(32, (w * scale / 32f).roundToInt() * 32)
        val rh = max(32, (h * scale / 32f).roundToInt() * 32)
        val resized = Bitmap.createScaledBitmap(src, rw, rh, true)

        val input = FloatArray(3 * rh * rw)
        val px = IntArray(rw * rh)
        resized.getPixels(px, 0, rw, 0, 0, rw, rh)
        if (resized != src) resized.recycle()
        val plane = rh * rw
        for (i in 0 until plane) {
            val p = px[i]
            val r = (p shr 16 and 0xFF)
            val g = (p shr 8 and 0xFF)
            val b = (p and 0xFF)
            input[i] = ((b / 255f) - 0.485f) / 0.229f          // channel 0 = B
            input[plane + i] = ((g / 255f) - 0.456f) / 0.224f  // channel 1 = G
            input[2 * plane + i] = ((r / 255f) - 0.406f) / 0.225f // channel 2 = R
        }

        val shape = longArrayOf(1, 3, rh.toLong(), rw.toLong())
        val prob = FloatArray(plane)
        OnnxTensor.createTensor(env, FloatBuffer.wrap(input), shape).use { tensor ->
            det.run(mapOf(det.inputNames.first() to tensor)).use { result ->
                val out = result[0] as OnnxTensor
                val buf = out.floatBuffer
                buf.get(prob)
            }
        }
        // DBNet output is a sigmoid probability map; guard in case of raw logits.
        if (prob.isNotEmpty() && prob.max() > 1.5f) {
            for (i in prob.indices) prob[i] = 1f / (1f + Math.exp(-prob[i].toDouble()).toFloat())
        }

        val boxes = extractBoxes(prob, rw, rh)
        // Map detection-space boxes back to the source bitmap.
        val sx = w.toFloat() / rw
        val sy = h.toFloat() / rh
        return boxes.map {
            Box(
                left = (it.left * sx).toInt().coerceIn(0, w - 1),
                top = (it.top * sy).toInt().coerceIn(0, h - 1),
                right = (it.right * sx).roundToInt().coerceIn(1, w),
                bottom = (it.bottom * sy).roundToInt().coerceIn(1, h),
            )
        }.filter { it.right - it.left >= 4 && it.bottom - it.top >= 4 }
    }

    /** Threshold the probability map and turn connected blobs into unclipped boxes. */
    private fun extractBoxes(prob: FloatArray, w: Int, h: Int): List<Box> {
        val visited = BooleanArray(w * h)
        val queue = IntArray(w * h)
        val result = ArrayList<Box>()
        for (start in 0 until w * h) {
            if (visited[start] || prob[start] < DET_THRESH) continue
            var head = 0; var tail = 0
            queue[tail++] = start
            visited[start] = true
            var minX = w; var minY = h; var maxX = 0; var maxY = 0
            var sum = 0f; var count = 0
            while (head < tail) {
                val idx = queue[head++]
                val x = idx % w; val y = idx / w
                if (x < minX) minX = x; if (x > maxX) maxX = x
                if (y < minY) minY = y; if (y > maxY) maxY = y
                sum += prob[idx]; count++
                var dy = -1
                while (dy <= 1) {
                    var dx = -1
                    while (dx <= 1) {
                        val nx = x + dx; val ny = y + dy
                        if (nx in 0 until w && ny in 0 until h) {
                            val nIdx = ny * w + nx
                            if (!visited[nIdx] && prob[nIdx] >= DET_THRESH) {
                                visited[nIdx] = true
                                queue[tail++] = nIdx
                            }
                        }
                        dx++
                    }
                    dy++
                }
            }
            val bw = maxX - minX + 1
            val bh = maxY - minY + 1
            if (bw < 3 || bh < 3) continue
            if (count == 0 || sum / count < BOX_THRESH) continue
            // Unclip: grow the box outward like PaddleOCR's Vatti offset (ratio 1.5).
            val dist = (bw * bh * UNCLIP) / (2f * (bw + bh))
            val d = dist.roundToInt()
            result.add(
                Box(
                    left = (minX - d).coerceAtLeast(0),
                    top = (minY - d).coerceAtLeast(0),
                    right = (maxX + d).coerceAtMost(w - 1),
                    bottom = (maxY + d).coerceAtMost(h - 1),
                ),
            )
        }
        return result
    }

    // ---- Recognition -----------------------------------------------------

    /** Groups boxes into reading order and recognizes each, returning text lines. */
    private fun readLines(src: Bitmap, boxes: List<Box>): List<String> {
        val sorted = boxes.sortedBy { it.top }
        val lines = ArrayList<MutableList<Box>>()
        for (box in sorted) {
            val line = lines.lastOrNull()
            val centerY = (box.top + box.bottom) / 2
            if (line != null) {
                val ref = line.first()
                val overlap = min(box.bottom, ref.bottom) - max(box.top, ref.top)
                if (overlap > 0 && overlap > 0.3f * (box.bottom - box.top) && centerY in ref.top..ref.bottom + (ref.bottom - ref.top)) {
                    line.add(box); continue
                }
            }
            lines.add(mutableListOf(box))
        }
        return lines.mapNotNull { line ->
            val words = line.sortedBy { it.left }.mapNotNull { recognize(src, it) }.filter { it.isNotBlank() }
            if (words.isEmpty()) null else words.joinToString(" ")
        }
    }

    private fun recognize(src: Bitmap, box: Box): String? {
        val env = env ?: return null
        val rec = recSession ?: return null
        val bw = box.right - box.left
        val bh = box.bottom - box.top
        if (bw < 2 || bh < 2) return null
        val crop = Bitmap.createBitmap(src, box.left, box.top, bw, bh)
        val tw = (REC_HEIGHT * bw.toFloat() / bh).roundToInt().coerceIn(16, REC_MAX_WIDTH)
        val scaled = Bitmap.createScaledBitmap(crop, tw, REC_HEIGHT, true)
        if (crop != scaled) crop.recycle()

        val plane = REC_HEIGHT * tw
        val input = FloatArray(3 * plane)
        val px = IntArray(plane)
        scaled.getPixels(px, 0, tw, 0, 0, tw, REC_HEIGHT)
        scaled.recycle()
        for (i in 0 until plane) {
            val p = px[i]
            val r = (p shr 16 and 0xFF)
            val g = (p shr 8 and 0xFF)
            val b = (p and 0xFF)
            input[i] = ((b / 255f) - 0.5f) / 0.5f
            input[plane + i] = ((g / 255f) - 0.5f) / 0.5f
            input[2 * plane + i] = ((r / 255f) - 0.5f) / 0.5f
        }

        val shape = longArrayOf(1, 3, REC_HEIGHT.toLong(), tw.toLong())
        OnnxTensor.createTensor(env, FloatBuffer.wrap(input), shape).use { tensor ->
            rec.run(mapOf(rec.inputNames.first() to tensor)).use { result ->
                val out = result[0] as OnnxTensor
                val info = out.info as TensorInfo
                val s = info.shape
                val steps = s[1].toInt()
                val classes = s[2].toInt()
                val buf = out.floatBuffer
                val data = FloatArray(steps * classes)
                buf.get(data)
                val decoded = ctcDecode(data, steps, classes)
                val text = decoded.text.trim()
                // Drop weak reads (icons/glyphs the detector mistook for text) and
                // fragments with no letter or digit (lone symbols and emoji).
                return when {
                    text.isEmpty() -> null
                    decoded.confidence < REC_DROP_SCORE -> null
                    text.none { it.isLetterOrDigit() } -> null
                    else -> text
                }
            }
        }
    }

    private class Decoded(val text: String, val confidence: Float)

    /**
     * Greedy CTC decode with a mean per-character confidence. Index 0 is blank;
     * 1..dict map to the dictionary; anything past it falls back to a space. The
     * rec model may emit probabilities (rows sum to ~1) or raw logits, so the
     * confidence path softmaxes only when the output looks like logits.
     */
    private fun ctcDecode(data: FloatArray, steps: Int, classes: Int): Decoded {
        var row0 = 0f
        for (c in 0 until classes) row0 += data[c]
        val isLogits = row0 < 0.5f || row0 > 1.5f

        val sb = StringBuilder()
        var prev = -1
        var confSum = 0f
        var confCount = 0
        for (t in 0 until steps) {
            val base = t * classes
            var best = 0
            var bestV = data[base]
            for (c in 1 until classes) {
                val v = data[base + c]
                if (v > bestV) { bestV = v; best = c }
            }
            if (best != 0 && best != prev) {
                sb.append(if (best - 1 < dict.size) dict[best - 1] else " ")
                confSum += if (isLogits) {
                    var sum = 0f
                    for (c in 0 until classes) sum += Math.exp((data[base + c] - bestV).toDouble()).toFloat()
                    if (sum > 0f) 1f / sum else 0f
                } else {
                    bestV
                }
                confCount++
            }
            prev = best
        }
        val conf = if (confCount > 0) confSum / confCount else 0f
        return Decoded(sb.toString(), conf)
    }

    // ---- Image decode ----------------------------------------------------

    private fun decode(uri: Uri): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val longEdge = max(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (longEdge / sample > SRC_MAX_SIDE * 2) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null
        val longest = max(decoded.width, decoded.height)
        if (longest <= SRC_MAX_SIDE) {
            decoded
        } else {
            val s = SRC_MAX_SIDE.toFloat() / longest
            val scaled = Bitmap.createScaledBitmap(
                decoded, (decoded.width * s).toInt(), (decoded.height * s).toInt(), true,
            )
            if (scaled != decoded) decoded.recycle()
            scaled
        }
    } catch (t: Throwable) {
        null
    }

    private companion object {
        const val DET_MAX_SIDE = 960
        const val SRC_MAX_SIDE = 2560
        const val REC_HEIGHT = 48
        const val REC_MAX_WIDTH = 3200
        const val DET_THRESH = 0.3f
        const val BOX_THRESH = 0.6f
        const val UNCLIP = 1.5f
        // Min mean per-character recognition confidence to keep a read. Matches
        // PaddleOCR's default drop_score; filters icons/glyphs read as junk text.
        const val REC_DROP_SCORE = 0.5f
    }
}
