package com.kk.detain.ui

import android.app.Application
import android.content.ComponentName
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kk.detain.DetainApp
import com.kk.detain.data.LockSource
import com.kk.detain.data.LockWindow
import com.kk.detain.data.MonitoredApp
import com.kk.detain.domain.InstalledApp
import com.kk.detain.domain.InstalledAppsLoader
import com.kk.detain.domain.ScheduleCalculator
import com.kk.detain.service.DetainAccessibilityService
import com.kk.detain.usage.UsageReport
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PermissionStatus(
    val usageAccess: Boolean = false,
    val accessibility: Boolean = false,
    val notifications: Boolean = false,
) {
    val coreReady: Boolean get() = usageAccess && accessibility
}

class DetainViewModel(application: Application) : AndroidViewModel(application) {

    private val container = DetainApp.container(application)
    private val repository = container.repository
    private val preferences = container.preferences

    val apps: StateFlow<List<MonitoredApp>> = repository.monitoredApps.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val windows: StateFlow<List<LockWindow>> = repository.windows.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val sessions = repository.recentSessions.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val exemptions = repository.exemptions.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val quota = preferences.quota
    val bubblesEnabled = preferences.bubblesEnabled

    val weeklyAttempts = repository
        .observeAttemptsSince(ScheduleCalculator.startOfWeek(System.currentTimeMillis()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _permissions = MutableStateFlow(PermissionStatus())
    val permissions: StateFlow<PermissionStatus> = _permissions.asStateFlow()

    private val _usageReport = MutableStateFlow<UsageReport?>(null)
    val usageReport: StateFlow<UsageReport?> = _usageReport.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _installedAppsLoading = MutableStateFlow(false)
    val installedAppsLoading: StateFlow<Boolean> = _installedAppsLoading.asStateFlow()

    private var usageJob: Job? = null

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        val context = getApplication<Application>()
        val expected = ComponentName(context, DetainAccessibilityService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        val accessibility = enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
        _permissions.value = PermissionStatus(
            usageAccess = container.usage.hasUsageAccess(),
            accessibility = accessibility,
            notifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        )
        preferences.refreshQuota()
    }

    fun refreshUsage() {
        usageJob?.cancel()
        usageJob = viewModelScope.launch {
            _usageReport.value = container.usage.report(repository.allApps())
        }
    }

    fun loadInstalledApps(force: Boolean = false) {
        if (_installedAppsLoading.value) return
        if (!force && _installedApps.value.isNotEmpty()) return
        viewModelScope.launch {
            _installedAppsLoading.value = true
            try {
                _installedApps.value = InstalledAppsLoader.load(getApplication())
            } finally {
                _installedAppsLoading.value = false
            }
        }
    }

    fun addApp(packageName: String, label: String) = viewModelScope.launch {
        repository.addApp(packageName, label)
    }

    fun removeApp(packageName: String) = viewModelScope.launch {
        repository.removeApp(packageName)
    }

    fun setEnabled(packageName: String, enabled: Boolean) = viewModelScope.launch {
        repository.setEnabled(packageName, enabled)
    }

    fun setScheduleEnabled(packageName: String, enabled: Boolean) = viewModelScope.launch {
        repository.setScheduleEnabled(packageName, enabled)
    }

    fun setPromptOnOpen(packageName: String, enabled: Boolean) = viewModelScope.launch {
        repository.setPromptOnOpen(packageName, enabled)
    }

    fun setDefaultLockMinutes(packageName: String, minutes: Int) = viewModelScope.launch {
        repository.setDefaultLockMinutes(packageName, minutes)
    }

    fun addWindows(packageName: String, days: Set<Int>, startMinute: Int, endMinute: Int) =
        viewModelScope.launch {
            days.forEach { day ->
                repository.addWindow(packageName, day, startMinute, endMinute)
            }
            repository.setScheduleEnabled(packageName, true)
        }

    fun replaceWindow(window: LockWindow, days: Set<Int>, startMinute: Int, endMinute: Int) =
        viewModelScope.launch {
            repository.deleteWindow(window)
            days.forEach { day ->
                repository.addWindow(window.packageName, day, startMinute, endMinute)
            }
        }

    fun deleteWindow(window: LockWindow) = viewModelScope.launch {
        repository.deleteWindow(window)
    }

    fun startTimedLock(
        packageName: String,
        minutes: Int,
        source: String = LockSource.MANUAL,
        onStarted: (Long) -> Unit = {},
    ) = viewModelScope.launch {
        val session = repository.startTimedLock(packageName, minutes, source)
        onStarted(session.endAt)
    }

    fun forceUnlock(
        packageName: String,
        untilAt: Long,
        onResult: (Boolean) -> Unit = {},
    ) = viewModelScope.launch {
        val granted = preferences.consumeQuota()
        if (granted) repository.grantExemption(packageName, untilAt)
        onResult(granted)
    }

    fun setBubblesEnabled(enabled: Boolean) {
        preferences.setBubblesEnabled(enabled)
    }

    fun weeklyResetAt(): Long = ScheduleCalculator.nextWeekReset()
}
