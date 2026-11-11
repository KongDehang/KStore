package com.kk.detain.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.kk.detain.DetainApp

class DetainAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        GuardDiagnostics.setServiceConnected(true)
        Log.i(GuardDiagnostics.TAG, "Accessibility service connected")
        DetainApp.container(this).preferences.refreshQuota()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return
        DetainApp.container(this).coordinator.onForeground(packageName)
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance === this) instance = null
        GuardDiagnostics.setServiceConnected(false)
        Log.i(GuardDiagnostics.TAG, "Accessibility service unbound")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        GuardDiagnostics.setServiceConnected(false)
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: DetainAccessibilityService? = null
            private set
    }
}
