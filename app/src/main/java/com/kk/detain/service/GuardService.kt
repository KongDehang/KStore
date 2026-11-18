package com.kk.detain.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.kk.detain.DetainApp
import com.kk.detain.data.DetainRepository
import com.kk.detain.domain.LockState
import com.kk.detain.domain.ScheduleCalculator
import com.kk.detain.ui.formatRemainingShort
import com.kk.detain.ui.formatTimeOfDay
import com.kk.detain.usage.UsageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Keeps a quiet, always-current status notification around so the lock is visible
 * even when the app itself is closed.
 */
class GuardService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null
    private lateinit var repository: DetainRepository
    private lateinit var usage: UsageRepository
    private lateinit var coordinator: LockCoordinator
    private val startedAt = System.currentTimeMillis()
    private var disconnectedChecks = 0
    private var connectionLostNotified = false

    override fun onCreate() {
        super.onCreate()
        val container = DetainApp.container(this)
        repository = container.repository
        usage = container.usage
        coordinator = container.coordinator
        startForeground(
            Notifications.GUARD_NOTIFICATION_ID,
            Notifications.guardNotification(this, "静水正在守护", "把时间还给生活"),
        )
        loop = scope.launch {
            var tick = 0
            while (isActive) {
                runCatching {
                    pollForeground()
                    // The status text is not worth a wake-up every 10 s.
                    if (tick % 2 == 0) refresh()
                }
                tick++
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch { runCatching { refresh() } }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        loop?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun refresh() {
        if (checkConnection()) return
        val (title, text) = buildStatus()
        val notification = Notifications.guardNotification(this, title, text)
        notify(Notifications.GUARD_NOTIFICATION_ID, notification)
    }

    /**
     * Independent foreground check. Accessibility events are the fast path, but a service
     * can stay bound while events silently stop arriving; usage stats keep the guard alive.
     */
    private suspend fun pollForeground() {
        val power = getSystemService(PowerManager::class.java)
        if (power != null && !power.isInteractive) return
        val packageName = usage.foregroundPackage() ?: return
        coordinator.onForegroundPolled(packageName)
    }

    /**
     * An enabled accessibility service that never reports itself connected means the
     * system stopped delivering events: the guard is silently dead until it is toggled.
     */
    private suspend fun checkConnection(): Boolean {
        val connected = GuardDiagnostics.serviceConnected.value
        val enabled = isAccessibilityEnabled(this)
        val warmingUp = System.currentTimeMillis() - startedAt < CONNECTION_GRACE_MS

        if (!enabled || connected || warmingUp) {
            disconnectedChecks = 0
            clearConnectionNotice()
            return false
        }

        disconnectedChecks++
        if (disconnectedChecks < DISCONNECTED_CHECKS) return false
        if (!connectionLostNotified) {
            connectionLostNotified = true
            notify(Notifications.CONNECTION_LOST_ID, Notifications.connectionLostNotification(this))
        }
        notify(
            Notifications.GUARD_NOTIFICATION_ID,
            Notifications.guardNotification(
                this,
                "静水暂停了",
                "无障碍服务已断开，点按重新开启后守护才会继续。",
            ),
        )
        return true
    }

    private suspend fun clearConnectionNotice() {
        if (!connectionLostNotified) return
        connectionLostNotified = false
        withContext(Dispatchers.Main) {
            getSystemService(NotificationManager::class.java)
                ?.cancel(Notifications.CONNECTION_LOST_ID)
        }
    }

    private suspend fun notify(id: Int, notification: android.app.Notification) {
        withContext(Dispatchers.Main) {
            val manager = getSystemService(NotificationManager::class.java) ?: return@withContext
            manager.notify(id, notification)
        }
    }

    private suspend fun buildStatus(): Pair<String, String> {
        val apps = repository.allApps().filter { it.enabled }
        val now = System.currentTimeMillis()
        if (apps.isEmpty()) {
            return "静水在等你" to "还没有守护应用，点按添加一个吧"
        }
        for (app in apps) {
            val state = repository.lockStateFor(app, now)
            if (state is LockState.Locked) {
                return "「${app.label}」锁定中" to
                    "还剩 ${formatRemainingShort(state.endAt - now)} · 到点自动解锁"
            }
        }
        val enabledPackages = apps.map { it.packageName }.toSet()
        val windows = repository.allWindows()
            .filter { it.packageName in enabledPackages && apps.first { app -> app.packageName == it.packageName }.scheduleEnabled }
        val next = ScheduleCalculator.nextStart(windows, now)
        if (next != null) {
            return "静水正在守护 ${apps.size} 个应用" to "下一次锁定 ${formatTimeOfDay(next)}"
        }
        return "静水正在守护 ${apps.size} 个应用" to "把时间还给生活"
    }

    companion object {
        /** 10 s foreground poll; the status notification refreshes every other tick. */
        private const val POLL_INTERVAL_MS = 10_000L

        /** 20 s per check, so three misses mean roughly a minute without a connection. */
        private const val DISCONNECTED_CHECKS = 3

        /** Give the accessibility service time to bind after a process start. */
        private const val CONNECTION_GRACE_MS = 90_000L

        fun start(context: Context) {
            val intent = Intent(context, GuardService::class.java)
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GuardService::class.java))
        }
    }
}
