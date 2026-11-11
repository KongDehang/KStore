package com.kk.detain.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kk.detain.data.DetainRepository
import com.kk.detain.data.MonitoredApp
import com.kk.detain.domain.LockKind
import com.kk.detain.domain.LockState
import com.kk.detain.ui.lock.LockActivity
import com.kk.detain.ui.lock.LockMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Decides what should happen when another app comes to the foreground.
 */
class LockCoordinator(
    private val context: Context,
    private val repository: DetainRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val recentlyHandled = mutableMapOf<String, Long>()

    fun onForeground(packageName: String) {
        if (packageName == context.packageName) return
        if (LockActivity.isShowing) return

        val now = System.currentTimeMillis()
        val last = recentlyHandled[packageName]
        if (last != null && now - last < 900L) return
        recentlyHandled[packageName] = now

        scope.launch {
            val app = repository.findApp(packageName) ?: return@launch
            GuardDiagnostics.noteForeground(packageName)
            when (val state = repository.lockStateFor(app, now)) {
                is LockState.Locked -> {
                    GuardDiagnostics.recordEvent(packageName, app.label, "弹出锁定页")
                    presentLock(app, state, now)
                }
                is LockState.NeedsPrompt -> {
                    GuardDiagnostics.recordEvent(packageName, app.label, "弹出提醒页")
                    presentPrompt(app, state)
                }
                LockState.Free -> {
                    GuardDiagnostics.recordEvent(packageName, app.label, "放行")
                }
            }
        }
    }

    private suspend fun presentLock(app: MonitoredApp, state: LockState.Locked, now: Long) {
        repository.recordAttempt(app.packageName)
        val attempts = repository.attemptCountSince(app.packageName, now - 10 * 60_000L)
        launchLockScreen(
            app = app,
            mode = LockMode.LOCKED,
            startAt = state.startAt,
            endAt = state.endAt,
            suggestedMinutes = app.defaultLockMinutes,
            attempts = attempts,
            kind = state.kind,
        )
    }

    private fun presentPrompt(app: MonitoredApp, state: LockState.NeedsPrompt) {
        launchLockScreen(
            app = app,
            mode = LockMode.PROMPT,
            startAt = 0L,
            endAt = 0L,
            suggestedMinutes = state.suggestedMinutes,
            attempts = 0,
        )
    }

    private fun launchLockScreen(
        app: MonitoredApp,
        mode: LockMode,
        startAt: Long,
        endAt: Long,
        suggestedMinutes: Int,
        attempts: Int,
        kind: LockKind = LockKind.TIMED,
    ) {
        val intent = Intent(context, LockActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP,
            )
            putExtra(LockActivity.EXTRA_PACKAGE, app.packageName)
            putExtra(LockActivity.EXTRA_LABEL, app.label)
            putExtra(LockActivity.EXTRA_MODE, mode.name)
            putExtra(LockActivity.EXTRA_START_AT, startAt)
            putExtra(LockActivity.EXTRA_END_AT, endAt)
            putExtra(LockActivity.EXTRA_SUGGESTED_MINUTES, suggestedMinutes)
            putExtra(LockActivity.EXTRA_ATTEMPTS, attempts)
            putExtra(LockActivity.EXTRA_KIND, kind.name)
        }
        val launchedAt = System.currentTimeMillis()
        // Prefer the accessibility service context: some ROMs apply the background
        // activity launch exemption more reliably when the call comes from the service.
        val starter = DetainAccessibilityService.instance ?: context
        runCatching { starter.startActivity(intent) }
            .onFailure { error ->
                Log.w(GuardDiagnostics.TAG, "startActivity failed for ${app.packageName}", error)
                GuardDiagnostics.setLaunchBlocked(true)
                GuardDiagnostics.recordEvent(
                    app.packageName,
                    app.label,
                    "启动锁定页失败：${error.javaClass.simpleName}",
                )
                // If the system refuses the background start, at least take the user home.
                DetainAccessibilityService.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            }

        scope.launch {
            delay(1_500L)
            val shown = GuardDiagnostics.lastLockShownAt.value >= launchedAt
            if (!shown) {
                Log.w(
                    GuardDiagnostics.TAG,
                    "lock page was not visible after launch; background pop-up may be blocked",
                )
                GuardDiagnostics.setLaunchBlocked(true)
                GuardDiagnostics.recordEvent(app.packageName, app.label, "锁定页未显示")
            }
        }
    }
}
