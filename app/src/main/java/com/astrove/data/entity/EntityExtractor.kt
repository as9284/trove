package com.astrove.data.entity

enum class EntityType { LINK, EMAIL, PHONE, CODE, TRACKING }

data class DetectedEntity(
    val type: EntityType,
    /** Raw matched value (used for copy / share). */
    val value: String,
)

/**
 * Pulls actionable entities out of OCR text with on-device regexes only.
 * Order is stable and duplicates are removed.
 */
object EntityExtractor {

    private val email = Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""")
    private val link = Regex("""\b(?:https?://|www\.)[^\s]+|\b[a-z0-9.\-]+\.(?:com|org|net|io|app|dev|co)(?:/[^\s]*)?""", RegexOption.IGNORE_CASE)
    private val phone = Regex("""(?<!\d)(?:\+?\d{1,3}[\s.\-]?)?(?:\(\d{2,4}\)[\s.\-]?)?\d{3}[\s.\-]?\d{3,4}(?:[\s.\-]?\d{2,4})?(?!\d)""")
    private val ups = Regex("""\b1Z[0-9A-Z]{16}\b""")
    private val fedex = Regex("""\b\d{12}(?:\d{3})?\b""")
    private val usps = Regex("""\b9\d{15,21}\b""")
    private val codeContext = Regex("""(?:code|otp|verification|passcode|pin)\D{0,20}(\d{4,8})""", RegexOption.IGNORE_CASE)

    fun extract(text: String): List<DetectedEntity> {
        if (text.isBlank()) return emptyList()
        val seen = LinkedHashSet<String>()
        val out = ArrayList<DetectedEntity>()

        fun add(type: EntityType, value: String) {
            val v = value.trim().trimEnd('.', ',', ')', ']')
            if (v.length < 3) return
            val key = "${type.name}:${v.lowercase()}"
            if (seen.add(key)) out.add(DetectedEntity(type, v))
        }

        codeContext.findAll(text).forEach { add(EntityType.CODE, it.groupValues[1]) }
        email.findAll(text).forEach { add(EntityType.EMAIL, it.value) }
        link.findAll(text).forEach { m ->
            // Don't double-count an email's domain as a link.
            if (!m.value.contains('@')) add(EntityType.LINK, m.value)
        }
        ups.findAll(text).forEach { add(EntityType.TRACKING, it.value) }
        usps.findAll(text).forEach { add(EntityType.TRACKING, it.value) }
        fedex.findAll(text).forEach { add(EntityType.TRACKING, it.value) }
        phone.findAll(text).forEach { m ->
            val digits = m.value.count { it.isDigit() }
            if (digits in 7..15) add(EntityType.PHONE, m.value.trim())
        }
        return out
    }
}
