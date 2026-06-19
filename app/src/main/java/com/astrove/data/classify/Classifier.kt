package com.astrove.data.classify

import com.astrove.data.ShotCategory
import com.astrove.data.entity.EntityExtractor
import com.astrove.data.entity.EntityType

/** Text-driven auto-categorization. Runs after OCR; deliberately conservative. */
object Classifier {

    private val receiptWords = listOf(
        "total", "subtotal", "tax", "amount", "receipt", "invoice", "qty",
        "change due", "cash", "visa", "mastercard", "balance due", "order #",
    )
    private val timeStamp = Regex("""\b\d{1,2}:\d{2}\s?(?:am|pm)?\b""", RegexOption.IGNORE_CASE)

    fun classify(text: String): ShotCategory {
        val lower = text.lowercase()
        if (lower.isBlank()) return ShotCategory.OTHER

        val hasDigits = text.any { it.isDigit() }
        val receiptHits = receiptWords.count { lower.contains(it) }
        if (hasDigits && receiptHits >= 2 && lower.contains('$') || (hasDigits && receiptHits >= 3)) {
            return ShotCategory.RECEIPT
        }

        val lines = text.lines().filter { it.isNotBlank() }
        val timeHits = timeStamp.findAll(text).count()
        val shortLines = lines.count { it.trim().length in 1..40 }
        if (lines.size >= 6 && timeHits >= 2 && shortLines >= lines.size / 2) {
            return ShotCategory.CHAT
        }

        val entities = EntityExtractor.extract(text)
        val wordCount = lower.split(Regex("""\s+""")).count { it.isNotBlank() }
        if (wordCount <= 12 && entities.any { it.type == EntityType.LINK }) {
            return ShotCategory.LINK
        }

        if (wordCount >= 60) return ShotCategory.DOC

        return ShotCategory.OTHER
    }
}
