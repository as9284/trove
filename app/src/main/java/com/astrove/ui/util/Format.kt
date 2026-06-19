package com.astrove.ui.util

import java.time.Instant
import java.time.Period
import java.time.ZoneId
import kotlin.math.roundToInt

fun formatCount(n: Int): String = "%,d".format(n)

fun formatAge(dateAddedSec: Long, nowMs: Long = System.currentTimeMillis()): String {
    val zone = ZoneId.systemDefault()
    val then = Instant.ofEpochSecond(dateAddedSec).atZone(zone).toLocalDate()
    val now = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
    val p = Period.between(then, now)
    return when {
        p.years >= 1 -> if (p.years == 1) "1 year ago" else "${p.years} years ago"
        p.months >= 1 -> if (p.months == 1) "1 month ago" else "${p.months} months ago"
        p.days >= 2 -> "${p.days} days ago"
        p.days == 1 -> "yesterday"
        else -> "today"
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${kb.roundToInt()} KB"
    val mb = kb / 1024.0
    if (mb < 1024) return "${"%.1f".format(mb)} MB"
    return "${"%.1f".format(mb / 1024.0)} GB"
}
