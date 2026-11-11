package com.kk.detain.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.kk.detain.DetainApp
import com.kk.detain.data.DetainRepository
import com.kk.detain.domain.LockState
import com.kk.detain.domain.ScheduleCalculator
import com.kk.detain.ui.formatRemainingShort
import com.kk.detain.ui.formatTimeOfDay
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

    override fun onCreate() {
        super.onCreate()
        repository = DetainApp.container(this).repository
        startForeground(
            Notifications.GUARD_NOTIFICATION_ID,
            Notifications.guardNotification(this, "静水正在守护", "把时间还给生活"),
        )
        loop = scope.launch {
            while (isActive) {
                runCatching { refresh() }
                delay(20_000L)
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
        val (title, text) = buildStatus()
        val notification = Notifications.guardNotification(this, title, text)
        withContext(Dispatchers.Main) {
            val manager = getSystemService(NotificationManager::class.java) ?: return@withContext
            manager.notify(Notifications.GUARD_NOTIFICATION_ID, notification)
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
        fun start(context: Context) {
            val intent = Intent(context, GuardService::class.java)
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GuardService::class.java))
        }
    }
}
