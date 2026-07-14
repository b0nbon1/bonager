package com.bonvic.bonager.data

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Dates {
    private val utc = TimeZone.getTimeZone("UTC")
    private val storagePatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
    )
    private val localPatterns = listOf(
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd",
    )

    fun nowIso(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = utc
    }.format(Date())

    fun toDateKey(date: Date = Date()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)

    fun toMonthKey(date: Date = Date()): String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(date)

    fun parse(value: String?): Date? {
        val clean = value?.trim().orEmpty()
        if (clean.isEmpty()) return null

        for (pattern in storagePatterns) {
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                    timeZone = utc
                }.parse(clean)
            }.getOrNull()?.let { return it }
        }
        for (pattern in localPatterns) {
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(clean)
            }.getOrNull()?.let { return it }
        }
        return null
    }

    fun inputToStorage(value: String): String? = parse(value)?.let { date ->
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = utc
        }.format(date)
    }

    fun toLocalInput(value: String?): String = parse(value)?.let {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(it)
    }.orEmpty()

    fun formatShortDate(value: String?): String = parse(value)?.let {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(it)
    } ?: (value ?: "No date")

    fun formatDateTime(value: String?): String = parse(value)?.let {
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(it)
    } ?: "No reminder"

    fun isToday(value: String?): Boolean = parse(value)?.let { toDateKey(it) == toDateKey() } ?: false

    fun quickReminder(hours: Int): String = Calendar.getInstance().apply {
        add(Calendar.HOUR_OF_DAY, hours)
    }.time.let { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(it) }

    fun addDays(dateKey: String, days: Int): String {
        val date = parse(dateKey) ?: Date()
        return Calendar.getInstance().apply {
            time = date
            add(Calendar.DAY_OF_MONTH, days)
        }.time.let(::toDateKey)
    }
}

fun formatMoney(value: Double): String = runCatching {
    NumberFormat.getCurrencyInstance().apply {
        maximumFractionDigits = if (value % 1.0 == 0.0) 0 else 2
    }.format(value)
}.getOrElse { "$" + String.format(Locale.US, "%.2f", value) }

fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours == 0 -> "${mins}m"
        mins == 0 -> "${hours}h"
        else -> "${hours}h ${mins}m"
    }
}
