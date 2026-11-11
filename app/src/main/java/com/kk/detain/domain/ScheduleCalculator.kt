package com.kk.detain.domain

import com.kk.detain.data.Exemption
import com.kk.detain.data.LockSession
import com.kk.detain.data.LockWindow
import com.kk.detain.data.MonitoredApp
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class LockPeriod(val startAt: Long, val endAt: Long)

enum class LockKind { TIMED, SCHEDULED }

sealed interface LockState {
    data class Locked(val startAt: Long, val endAt: Long, val kind: LockKind) : LockState
    data class NeedsPrompt(val suggestedMinutes: Int) : LockState
    data object Free : LockState
}

data class ActiveLock(val startAt: Long, val endAt: Long, val kind: LockKind)

object ScheduleCalculator {
    const val MINUTES_PER_DAY = 24 * 60

    fun windowDurationMinutes(window: LockWindow): Int {
        if (window.startMinute == window.endMinute) return MINUTES_PER_DAY
        return ((window.endMinute - window.startMinute) + MINUTES_PER_DAY) % MINUTES_PER_DAY
    }

    private fun windowRange(window: LockWindow, date: LocalDate, zone: ZoneId): LockPeriod {
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val start = dayStart + window.startMinute * 60_000L
        val end = start + windowDurationMinutes(window) * 60_000L
        return LockPeriod(start, end)
    }

    fun activePeriod(
        windows: List<LockWindow>,
        now: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): LockPeriod? {
        if (windows.isEmpty()) return null
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        var active: LockPeriod? = null
        // A window that started yesterday may still be running after midnight.
        for (offset in -1..0) {
            val date = today.plusDays(offset.toLong())
            val dayOfWeek = date.dayOfWeek.value
            for (window in windows) {
                if (window.dayOfWeek != dayOfWeek) continue
                val range = windowRange(window, date, zone)
                if (now >= range.startAt && now < range.endAt) {
                    val current = active
                    active = if (current == null) {
                        range
                    } else {
                        LockPeriod(
                            startAt = minOf(current.startAt, range.startAt),
                            endAt = maxOf(current.endAt, range.endAt),
                        )
                    }
                }
            }
        }
        return active
    }

    fun nextStart(
        windows: List<LockWindow>,
        now: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        if (windows.isEmpty()) return null
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        var best: Long? = null
        for (offset in 0..7) {
            val date = today.plusDays(offset.toLong())
            val dayOfWeek = date.dayOfWeek.value
            for (window in windows) {
                if (window.dayOfWeek != dayOfWeek) continue
                val start = windowRange(window, date, zone).startAt
                if (start > now) {
                    val current = best
                    if (current == null || start < current) best = start
                }
            }
        }
        return best
    }

    fun startOfDay(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
            .atStartOfDay(zone).toInstant().toEpochMilli()

    fun startOfWeek(
        epochMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun nextWeekReset(
        epochMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return date.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            .atStartOfDay(zone).toInstant().toEpochMilli()
    }
}

/**
 * Resolves the live lock state of an app from data already loaded in the UI.
 */
fun resolveActiveLock(
    app: MonitoredApp,
    windows: List<LockWindow>,
    sessions: List<LockSession>,
    exemptions: List<Exemption>,
    now: Long,
): ActiveLock? {
    if (!app.enabled) return null
    if (exemptions.any { it.packageName == app.packageName && it.untilAt > now }) return null

    val session = sessions.firstOrNull {
        it.packageName == app.packageName && it.startAt <= now && it.endAt > now
    }
    if (session != null) return ActiveLock(session.startAt, session.endAt, LockKind.TIMED)

    if (app.scheduleEnabled) {
        val appWindows = windows.filter { it.packageName == app.packageName }
        val period = ScheduleCalculator.activePeriod(appWindows, now)
        if (period != null) return ActiveLock(period.startAt, period.endAt, LockKind.SCHEDULED)
    }
    return null
}

fun hasSchedule(app: MonitoredApp, windows: List<LockWindow>): Boolean =
    app.scheduleEnabled && windows.any { it.packageName == app.packageName }
