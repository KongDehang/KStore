package com.kk.detain.data

import com.kk.detain.domain.LockKind
import com.kk.detain.domain.LockState
import com.kk.detain.domain.ScheduleCalculator
import kotlinx.coroutines.flow.Flow

class DetainRepository(
    private val appDao: MonitoredAppDao,
    private val windowDao: LockWindowDao,
    private val sessionDao: LockSessionDao,
    private val exemptionDao: ExemptionDao,
    private val attemptDao: LockAttemptDao,
) {
    val monitoredApps: Flow<List<MonitoredApp>> = appDao.observeAll()
    val windows: Flow<List<LockWindow>> = windowDao.observeAll()
    val recentSessions: Flow<List<LockSession>> = sessionDao.observeRecent()
    val exemptions: Flow<List<Exemption>> = exemptionDao.observeAll()

    fun observeWindows(packageName: String): Flow<List<LockWindow>> = windowDao.observeFor(packageName)
    fun observeSessions(packageName: String): Flow<List<LockSession>> = sessionDao.observeFor(packageName)
    fun observeAttemptsSince(since: Long): Flow<Int> = attemptDao.observeCountSince(since)

    suspend fun allApps(): List<MonitoredApp> = appDao.getAll()
    suspend fun findApp(packageName: String): MonitoredApp? = appDao.find(packageName)

    suspend fun addApp(packageName: String, label: String) {
        if (appDao.find(packageName) != null) return
        val accent = (appDao.getAll().size % 4)
        appDao.upsert(
            MonitoredApp(
                packageName = packageName,
                label = label,
                accentIndex = accent,
            ),
        )
    }

    suspend fun removeApp(packageName: String) {
        windowDao.deleteFor(packageName)
        sessionDao.deleteFor(packageName)
        exemptionDao.delete(packageName)
        attemptDao.deleteFor(packageName)
        appDao.delete(packageName)
    }

    suspend fun setEnabled(packageName: String, enabled: Boolean) =
        appDao.setEnabled(packageName, enabled)

    suspend fun setScheduleEnabled(packageName: String, enabled: Boolean) =
        appDao.setScheduleEnabled(packageName, enabled)

    suspend fun setPromptOnOpen(packageName: String, enabled: Boolean) =
        appDao.setPromptOnOpen(packageName, enabled)

    suspend fun setDefaultLockMinutes(packageName: String, minutes: Int) =
        appDao.setDefaultLockMinutes(packageName, minutes)

    suspend fun windowsFor(packageName: String): List<LockWindow> = windowDao.getFor(packageName)

    suspend fun allWindows(): List<LockWindow> = windowDao.getAll()

    suspend fun addWindow(packageName: String, dayOfWeek: Int, startMinute: Int, endMinute: Int): Long =
        windowDao.insert(
            LockWindow(
                packageName = packageName,
                dayOfWeek = dayOfWeek,
                startMinute = startMinute,
                endMinute = endMinute,
            ),
        )

    suspend fun updateWindow(window: LockWindow) = windowDao.update(window)

    suspend fun deleteWindow(window: LockWindow) = windowDao.delete(window)

    suspend fun startTimedLock(packageName: String, minutes: Int, source: String): LockSession {
        val now = System.currentTimeMillis()
        val session = LockSession(
            packageName = packageName,
            source = source,
            startAt = now,
            endAt = now + minutes * 60_000L,
        )
        val id = sessionDao.insert(session)
        return session.copy(id = id)
    }

    suspend fun activeTimedLocks(now: Long = System.currentTimeMillis()): List<LockSession> =
        sessionDao.activeAll(now)

    suspend fun grantExemption(packageName: String, untilAt: Long) =
        exemptionDao.put(Exemption(packageName, untilAt))

    suspend fun exemptionFor(packageName: String, now: Long): Exemption? =
        exemptionDao.get(packageName)?.takeIf { it.untilAt > now }

    suspend fun recordAttempt(packageName: String) =
        attemptDao.insert(LockAttempt(packageName = packageName, timestamp = System.currentTimeMillis()))

    suspend fun attemptCountSince(packageName: String, since: Long): Int =
        attemptDao.countSince(packageName, since)

    /**
     * The single source of truth used by the accessibility service and the guard notification.
     */
    suspend fun lockStateFor(app: MonitoredApp, now: Long = System.currentTimeMillis()): LockState {
        if (!app.enabled) return LockState.Free

        val exemption = exemptionDao.get(app.packageName)
        if (exemption != null && exemption.untilAt > now) return LockState.Free

        val session = sessionDao.activeFor(app.packageName, now)
        if (session != null) {
            return LockState.Locked(session.startAt, session.endAt, LockKind.TIMED)
        }

        val windows = if (app.scheduleEnabled) windowDao.getFor(app.packageName) else emptyList()
        val period = ScheduleCalculator.activePeriod(windows, now)
        if (period != null) {
            return LockState.Locked(period.startAt, period.endAt, LockKind.SCHEDULED)
        }

        if (app.promptOnOpen && windows.isEmpty()) {
            // Give the user a short breather after a supervised session instead of
            // immediately throwing the prompt back in their face.
            val latest = sessionDao.latestFor(app.packageName)
            val inGrace = latest != null && now < latest.endAt + PROMPT_GRACE_MS
            if (!inGrace) return LockState.NeedsPrompt(app.defaultLockMinutes)
        }
        return LockState.Free
    }

    companion object {
        const val PROMPT_GRACE_MS = 10 * 60_000L
    }
}
