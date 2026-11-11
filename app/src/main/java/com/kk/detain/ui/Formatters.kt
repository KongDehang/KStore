package com.kk.detain.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
private val weekdayShort = listOf("一", "二", "三", "四", "五", "六", "日")

fun weekdayName(dayOfWeek: Int): String = weekdays.getOrElse(dayOfWeek - 1) { "周一" }

fun weekdayShortName(dayOfWeek: Int): String = weekdayShort.getOrElse(dayOfWeek - 1) { "一" }

fun formatMinutes(minutes: Int): String = when {
    minutes <= 0 -> "0 分钟"
    minutes < 60 -> "$minutes 分钟"
    minutes % 60 == 0 -> "${minutes / 60} 小时"
    else -> "${minutes / 60} 小时 ${minutes % 60} 分"
}

fun formatMinutesShort(minutes: Int): String = when {
    minutes <= 0 -> "0 分"
    minutes < 60 -> "$minutes 分"
    minutes % 60 == 0 -> "${minutes / 60} 小时"
    else -> "${minutes / 60} 小时 ${minutes % 60} 分"
}

/** 01:23 or 1:02:03 for the live countdown. */
fun formatCountdown(remainingMs: Long): String {
    val totalSeconds = (remainingMs / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun formatClock(minuteOfDay: Int): String {
    val normalized = ((minuteOfDay % 1440) + 1440) % 1440
    return String.format(Locale.US, "%02d:%02d", normalized / 60, normalized % 60)
}

fun formatTimeOfDay(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(epochMillis).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))

fun formatTimeWithSeconds(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(epochMillis).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm:ss"))

fun formatMonthDay(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(epochMillis).atZone(zone).format(DateTimeFormatter.ofPattern("M月d日"))

fun formatWeekday(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    weekdayName(Instant.ofEpochMilli(epochMillis).atZone(zone).dayOfWeek.value)

fun formatDateTimeShort(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(epochMillis).atZone(zone).format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))

fun formatRemainingShort(remainingMs: Long): String {
    val totalMinutes = ((remainingMs / 60_000) + if (remainingMs % 60_000 > 0) 1 else 0).toInt()
    return when {
        totalMinutes <= 1 -> "不到 1 分钟"
        totalMinutes < 60 -> "$totalMinutes 分钟"
        else -> "${totalMinutes / 60} 小时 ${totalMinutes % 60} 分"
    }
}

fun scheduleWindowLabel(startMinute: Int, endMinute: Int): String =
    if (startMinute == endMinute) "全天" else "${formatClock(startMinute)} – ${formatClock(endMinute)}"

fun greetingForHour(hour: Int): String = when (hour) {
    in 5..10 -> "早上好"
    in 11..13 -> "午安"
    in 14..18 -> "下午好"
    in 19..22 -> "晚上好"
    else -> "夜深了"
}
