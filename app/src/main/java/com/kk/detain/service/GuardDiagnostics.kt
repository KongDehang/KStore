package com.kk.detain.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GuardEvent(
    val packageName: String,
    val label: String?,
    val time: Long,
    val outcome: String,
)

data class ForegroundSignal(
    val packageName: String,
    val time: Long,
)

/**
 * Small, in-memory trace of the guard chain so the user can see where blocking stops:
 * service connected -> foreground app seen -> lock page launched.
 */
object GuardDiagnostics {
    const val TAG = "DetainGuard"
    private const val MAX_EVENTS = 12

    private val _serviceConnected = MutableStateFlow(false)
    val serviceConnected: StateFlow<Boolean> = _serviceConnected.asStateFlow()

    private val _lastForeground = MutableStateFlow<ForegroundSignal?>(null)
    val lastForeground: StateFlow<ForegroundSignal?> = _lastForeground.asStateFlow()

    private val _events = MutableStateFlow<List<GuardEvent>>(emptyList())
    val events: StateFlow<List<GuardEvent>> = _events.asStateFlow()

    private val _launchBlocked = MutableStateFlow(false)
    val launchBlocked: StateFlow<Boolean> = _launchBlocked.asStateFlow()

    private val _lastLockShownAt = MutableStateFlow(0L)
    val lastLockShownAt: StateFlow<Long> = _lastLockShownAt.asStateFlow()

    fun setServiceConnected(connected: Boolean) {
        _serviceConnected.value = connected
    }

    fun noteLockShown() {
        _lastLockShownAt.value = System.currentTimeMillis()
        _launchBlocked.value = false
    }

    fun noteForeground(packageName: String) {
        _lastForeground.value = ForegroundSignal(
            packageName = packageName,
            time = System.currentTimeMillis(),
        )
    }

    fun recordEvent(packageName: String, label: String?, outcome: String) {
        val event = GuardEvent(
            packageName = packageName,
            label = label,
            time = System.currentTimeMillis(),
            outcome = outcome,
        )
        _events.value = (listOf(event) + _events.value).take(MAX_EVENTS)
    }

    fun setLaunchBlocked(blocked: Boolean) {
        _launchBlocked.value = blocked
    }
}
