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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Decides what should happen when another app comes to the foreground.
 *
 * Opening an app is only one moment of a session, so the coordinator also keeps a light
 * heartbeat running while a guarded app stays in front. Without it, a lock window that
 * starts in the middle of a long session would only be noticed after switching apps.
 */
class LockCoordinator(
    private val context: Context,
    private val repository: DetainRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val recentlyHandled = mutableMapOf<String, Long>()

    @Volatile
    private var watchedPackage: String? = null

    private var heartbeat: Job? = null

    /** Bumped on every real foreground switch so stale delayed checks can bail out. */
    private var watchToken = 0

    /** Last state we acted on, so the heartbeat only reacts to real transitions. */
    private var lastStateKey: String? = null

    fun onForeground(packageName: String) {
        if (packageName == context.packageName) {
            // Our own window came to the front. It is either the guard page (keep watching
            // the app underneath, the heartbeat just pauses) or the main app (session over).
            val token = watchToken
            scope.launch {
                delay(LOCK_PAGE_SETTLE_MS)
                if (token == watchToken && !LockActivity.isShowing) stopHeartbeat()
            }
            return
        }
        if (packageName in OVERLAY_PACKAGES) {
            // Notification shade, permission dialogs and the like float above the app
            // without a window event when they close, so keep the current watch.
            return
        }
        val now = System.currentTimeMillis()
        val last = recentlyHandled[packageName]
        if (last != null && now - last < EVENT_DEDUPE_MS) return
        recentlyHandled[packageName] = now
        recentlyHandled.entries.removeAll { now - it.value > EVENT_DEDUPE_MS * 4 }

        lastStateKey = null
        watch(packageName)
    }

    /** The screen came back on with the same app still in front, so no window event fires. */
    fun onScreenUnlocked(packageName: String?) {
        if (packageName == null || packageName == context.packageName) return
        lastStateKey = null
        watch(packageName)
    }

    fun onScreenOff() {
        stopHeartbeat()
    }

    private fun watch(packageName: String) {
        watchedPackage = packageName
        watchToken++
        evaluate(packageName, fromHeartbeat = false)
        if (heartbeat?.isActive != true) {
            heartbeat = scope.launch {
                while (isActive) {
                    delay(HEARTBEAT_MS)
                    val current = watchedPackage ?: break
                    if (LockActivity.isShowing) continue
                    evaluate(current, fromHeartbeat = true)
                }
            }
        }
    }

    private fun stopHeartbeat() {
        watchedPackage = null
        lastStateKey = null
        heartbeat?.cancel()
        heartbeat = null
    }

    private fun evaluate(packageName: String, fromHeartbeat: Boolean) {
        scope.launch {
            runCatching {
                val app = repository.findApp(packageName)
                if (app == null) {
                    if (watchedPackage == packageName) stopHeartbeat()
                    return@runCatching
                }
                val now = System.currentTimeMillis()
                val state = repository.lockStateFor(app, now)
                val stateKey = when (state) {
                    is LockState.Locked -> "locked:${state.kind}:${state.startAt}"
                    is LockState.NeedsPrompt -> "prompt"
                    LockState.Free -> "free"
                }
                if (fromHeartbeat && stateKey == lastStateKey) return@runCatching
                if (state !is LockState.Free && LockActivity.isShowing) {
                    // The guard page is already in front; do not stack another launch on it.
                    lastStateKey = stateKey
                    return@runCatching
                }
                lastStateKey = stateKey
                if (!fromHeartbeat) GuardDiagnostics.noteForeground(packageName)

                when (state) {
                    is LockState.Locked -> {
                        GuardDiagnostics.recordEvent(packageName, app.label, "弹出锁定页")
                        presentLock(app, state, now)
                    }
                    is LockState.NeedsPrompt -> {
                        GuardDiagnostics.recordEvent(packageName, app.label, "弹出提醒页")
                        presentPrompt(app, state)
                    }
                    LockState.Free -> {
                        if (!fromHeartbeat) {
                            GuardDiagnostics.recordEvent(packageName, app.label, "放行")
                        }
                    }
                }
            }.onFailure { error ->
                Log.w(GuardDiagnostics.TAG, "guard decision failed for $packageName", error)
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

    companion object {
        /** How often the app in front is re-checked while it stays open. */
        private const val HEARTBEAT_MS = 10_000L
        private const val EVENT_DEDUPE_MS = 900L
        private const val LOCK_PAGE_SETTLE_MS = 1_500L

        private val OVERLAY_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
        )
    }
}
