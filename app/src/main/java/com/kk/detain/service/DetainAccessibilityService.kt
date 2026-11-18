package com.kk.detain.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.kk.detain.DetainApp

class DetainAccessibilityService : AccessibilityService() {

    private var screenReceiver: BroadcastReceiver? = null

    @Volatile
    private var lastPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        GuardDiagnostics.setServiceConnected(true)
        Log.i(GuardDiagnostics.TAG, "Accessibility service connected")
        runCatching {
            DetainApp.container(this).preferences.refreshQuota()
            registerScreenReceiver()
        }.onFailure { error ->
            Log.w(GuardDiagnostics.TAG, "service setup failed", error)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Never let an exception escape: an uncaught one crashes the process and the
        // system then stops delivering events until the service is toggled again.
        try {
            if (event == null) return
            if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
            val packageName = event.packageName?.toString() ?: return
            lastPackage = packageName
            Log.d(GuardDiagnostics.TAG, "window -> $packageName")
            DetainApp.container(this).coordinator.onForeground(packageName)
        } catch (error: Throwable) {
            Log.w(GuardDiagnostics.TAG, "failed to handle accessibility event", error)
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        release()
        Log.i(GuardDiagnostics.TAG, "Accessibility service unbound")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun registerScreenReceiver() {
        if (screenReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                runCatching {
                    val coordinator = DetainApp.container(this@DetainAccessibilityService).coordinator
                    when (intent?.action) {
                        Intent.ACTION_SCREEN_OFF -> coordinator.onScreenOff()
                        // Waking up does not emit a window-state event, so re-check the
                        // app that was in front before the screen went dark.
                        Intent.ACTION_USER_PRESENT -> coordinator.onScreenUnlocked(lastPackage)
                    }
                }.onFailure { error ->
                    Log.w(GuardDiagnostics.TAG, "screen state handling failed", error)
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(
            this,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        screenReceiver = receiver
    }

    private fun release() {
        if (instance === this) instance = null
        GuardDiagnostics.setServiceConnected(false)
        screenReceiver?.let { receiver ->
            runCatching { unregisterReceiver(receiver) }
        }
        screenReceiver = null
        lastPackage = null
        runCatching { DetainApp.container(this).coordinator.onScreenOff() }
    }

    companion object {
        @Volatile
        var instance: DetainAccessibilityService? = null
            private set
    }
}
