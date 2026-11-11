package com.kk.detain.domain

import com.kk.detain.data.Exemption
import com.kk.detain.data.LockSession
import com.kk.detain.data.LockWindow
import com.kk.detain.data.MonitoredApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ScheduleCalculatorTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(year, month, day, hour, minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    private fun window(day: Int, start: Int, end: Int) = LockWindow(
        id = day.toLong(),
        packageName = "com.example.app",
        dayOfWeek = day,
        startMinute = start,
        endMinute = end,
    )

    // 2024-06-10 is a Monday.
    private val mondayMorning = window(day = 1, start = 10 * 60, end = 12 * 60)

    @Test
    fun windowIsActiveInsideItsRange() {
        val period = ScheduleCalculator.activePeriod(
            windows = listOf(mondayMorning),
            now = at(2024, 6, 10, 11),
            zone = zone,
        )
        assertEquals(at(2024, 6, 10, 10), period?.startAt)
        assertEquals(at(2024, 6, 10, 12), period?.endAt)
    }

    @Test
    fun windowIsInactiveOutsideItsRange() {
        assertNull(
            ScheduleCalculator.activePeriod(
                windows = listOf(mondayMorning),
                now = at(2024, 6, 10, 9, 59),
                zone = zone,
            ),
        )
        assertNull(
            ScheduleCalculator.activePeriod(
                windows = listOf(mondayMorning),
                now = at(2024, 6, 10, 12),
                zone = zone,
            ),
        )
    }

    @Test
    fun crossMidnightWindowStaysActiveAfterMidnight() {
        val overnight = window(day = 1, start = 22 * 60, end = 2 * 60)
        val period = ScheduleCalculator.activePeriod(
            windows = listOf(overnight),
            now = at(2024, 6, 11, 1),
            zone = zone,
        )
        assertEquals(at(2024, 6, 11, 2), period?.endAt)
        assertNull(
            ScheduleCalculator.activePeriod(
                windows = listOf(overnight),
                now = at(2024, 6, 11, 2),
                zone = zone,
            ),
        )
    }

    @Test
    fun equalStartAndEndMeansWholeDay() {
        val allDay = window(day = 1, start = 0, end = 0)
        val period = ScheduleCalculator.activePeriod(
            windows = listOf(allDay),
            now = at(2024, 6, 10, 23, 59),
            zone = zone,
        )
        assertEquals(at(2024, 6, 11, 0), period?.endAt)
    }

    @Test
    fun overlappingWindowsExtendTheLockUntilTheLastEnd() {
        val first = window(day = 1, start = 22 * 60, end = 23 * 60)
        val second = window(day = 1, start = 22 * 60 + 30, end = 23 * 60 + 30)
        val period = ScheduleCalculator.activePeriod(
            windows = listOf(first, second),
            now = at(2024, 6, 10, 22, 40),
            zone = zone,
        )
        assertEquals(at(2024, 6, 10, 23, 30), period?.endAt)
    }

    @Test
    fun nextStartFindsTheClosestUpcomingWindow() {
        val monday = window(day = 1, start = 10 * 60, end = 12 * 60)
        val wednesday = window(day = 3, start = 9 * 60, end = 10 * 60)
        val next = ScheduleCalculator.nextStart(
            windows = listOf(monday, wednesday),
            now = at(2024, 6, 10, 12, 30),
            zone = zone,
        )
        assertEquals(at(2024, 6, 12, 9), next)
    }

    @Test
    fun nextStartCanRollIntoTheFollowingWeek() {
        val next = ScheduleCalculator.nextStart(
            windows = listOf(mondayMorning),
            now = at(2024, 6, 12, 12),
            zone = zone,
        )
        assertEquals(at(2024, 6, 17, 10), next)
    }

    @Test
    fun nextWeekResetIsTheFollowMondayMidnight() {
        val reset = ScheduleCalculator.nextWeekReset(at(2024, 6, 12, 15), zone)
        assertEquals(at(2024, 6, 17, 0), reset)
    }

    @Test
    fun timedSessionLocksTheApp() {
        val now = at(2024, 6, 10, 11)
        val app = MonitoredApp(packageName = "com.example.app", label = "示例")
        val session = LockSession(
            packageName = app.packageName,
            source = "prompt",
            startAt = now - 60_000L,
            endAt = now + 30 * 60_000L,
        )
        val lock = resolveActiveLock(app, emptyList(), listOf(session), emptyList(), now)
        assertEquals(LockKind.TIMED, lock?.kind)
        assertEquals(session.endAt, lock?.endAt)
    }

    @Test
    fun exemptionSuppressesEveryLock() {
        val now = at(2024, 6, 10, 11)
        val app = MonitoredApp(packageName = "com.example.app", label = "示例")
        val session = LockSession(
            packageName = app.packageName,
            source = "prompt",
            startAt = now - 60_000L,
            endAt = now + 30 * 60_000L,
        )
        val exemption = Exemption(packageName = app.packageName, untilAt = now + 10 * 60_000L)
        assertNull(resolveActiveLock(app, emptyList(), listOf(session), listOf(exemption), now))
        assertTrue(
            resolveActiveLock(
                app,
                emptyList(),
                listOf(session),
                listOf(exemption),
                now + 11 * 60_000L,
            ) != null,
        )
    }

    @Test
    fun disabledAppNeverLocks() {
        val now = at(2024, 6, 10, 11)
        val app = MonitoredApp(packageName = "com.example.app", label = "示例", enabled = false)
        val session = LockSession(
            packageName = app.packageName,
            source = "prompt",
            startAt = now - 60_000L,
            endAt = now + 30 * 60_000L,
        )
        assertNull(resolveActiveLock(app, emptyList(), listOf(session), emptyList(), now))
    }
}
