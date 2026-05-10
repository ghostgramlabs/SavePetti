package com.ghostgramlabs.pettibox.data.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormat {
    private val thisYear = SimpleDateFormat("MMM d", Locale.getDefault())
    private val older = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    fun relative(ts: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - ts
        return when {
            diff < 60_000 -> "just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            diff < 604_800_000 -> "${diff / 86_400_000}d ago"
            else -> {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }
                val curYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                if (cal.get(java.util.Calendar.YEAR) == curYear) thisYear.format(Date(ts))
                else older.format(Date(ts))
            }
        }
    }
}
