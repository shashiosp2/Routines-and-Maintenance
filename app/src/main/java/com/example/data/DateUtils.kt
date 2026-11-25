package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object DateUtils {
    private const val DATE_PATTERN = "yyyy-MM-dd"
    private const val MILLIS_PER_DAY = 86_400_000L

    private fun dateFormat(): SimpleDateFormat =
        SimpleDateFormat(DATE_PATTERN, Locale.US).apply {
            isLenient = false
        }

    fun todayDateString(nowMillis: Long = System.currentTimeMillis()): String {
        return dateFormat().format(nowMillis)
    }

    fun parseDateMillis(dateStr: String): Long? {
        val trimmed = dateStr.trim()
        if (trimmed.isEmpty()) return null
        return try {
            val parsed = dateFormat().parse(trimmed) ?: return null
            val cal = Calendar.getInstance().apply {
                time = parsed
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        } catch (_: Exception) {
            null
        }
    }

    fun startOfDayMillis(dateStr: String): Long? {
        val parsed = parseDateMillis(dateStr) ?: return null
        val cal = Calendar.getInstance().apply {
            timeInMillis = parsed
        }
        // Convert local Y/M/D to UTC midnight so DST transitions never shift day diffs
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        }
        return utc.timeInMillis
    }

    fun daysBetween(fromDateStr: String, toDateStr: String): Int {
        val fromUtc = startOfDayMillis(fromDateStr) ?: return 999
        val toUtc = startOfDayMillis(toDateStr) ?: return 999
        return ((toUtc - fromUtc) / MILLIS_PER_DAY).toInt()
    }

    fun addDays(baseDateStr: String, days: Int): String {
        val baseMillis = parseDateMillis(baseDateStr) ?: System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = baseMillis
            add(Calendar.DAY_OF_YEAR, days)
        }
        return dateFormat().format(cal.time)
    }

    fun millisToDateString(utcMillis: Long): String {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        val local = Calendar.getInstance().apply {
            clear()
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
        }
        return dateFormat().format(local.time)
    }

    /**
     * Matches Obsidian routines.ts dayMap:
     * Calendar.SUNDAY (1) -> col 9
     * Calendar.MONDAY (2) -> col 3
     * ...
     * Calendar.SATURDAY (7) -> col 8
     */
    fun currentWeekdayColumnIndex(nowMillis: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> 9
            Calendar.MONDAY -> 3
            Calendar.TUESDAY -> 4
            Calendar.WEDNESDAY -> 5
            Calendar.THURSDAY -> 6
            Calendar.FRIDAY -> 7
            Calendar.SATURDAY -> 8
            else -> 3
        }
    }

    fun currentWeekdayName(nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(cal.time)
    }

    fun shortDayNameForColumn(colIndex: Int): String = when (colIndex) {
        3 -> "Mon"
        4 -> "Tue"
        5 -> "Wed"
        6 -> "Thu"
        7 -> "Fri"
        8 -> "Sat"
        9 -> "Sun"
        else -> ""
    }

    fun formatDurationMmSs(seconds: Int): String {
        val absSec = abs(seconds)
        val mins = absSec / 60
        val secs = absSec % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}
