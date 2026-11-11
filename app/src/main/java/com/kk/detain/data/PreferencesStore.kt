package com.kk.detain.data

import android.content.Context
import android.content.SharedPreferences
import com.kk.detain.domain.ScheduleCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.WeekFields

const val WEEKLY_UNLOCK_ALLOWANCE = 2

data class QuotaState(
    val used: Int,
    val weekKey: String,
) {
    val remaining: Int get() = (WEEKLY_UNLOCK_ALLOWANCE - used).coerceAtLeast(0)
    val resetAt: Long get() = ScheduleCalculator.nextWeekReset()
}

class PreferencesStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _quota = MutableStateFlow(loadQuota())
    val quota: StateFlow<QuotaState> = _quota.asStateFlow()

    private val _bubblesEnabled = MutableStateFlow(prefs.getBoolean(KEY_BUBBLES, true))
    val bubblesEnabled: StateFlow<Boolean> = _bubblesEnabled.asStateFlow()

    fun refreshQuota() {
        _quota.value = loadQuota()
    }

    @Synchronized
    fun consumeQuota(): Boolean {
        val current = loadQuota()
        if (current.remaining <= 0) {
            _quota.value = current
            return false
        }
        val next = QuotaState(current.used + 1, current.weekKey)
        prefs.edit()
            .putString(KEY_WEEK, next.weekKey)
            .putInt(KEY_USED, next.used)
            .apply()
        _quota.value = next
        return true
    }

    fun setBubblesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BUBBLES, enabled).apply()
        _bubblesEnabled.value = enabled
    }

    private fun loadQuota(): QuotaState {
        val weekKey = currentWeekKey()
        val storedWeek = prefs.getString(KEY_WEEK, null)
        if (storedWeek != weekKey) {
            prefs.edit().putString(KEY_WEEK, weekKey).putInt(KEY_USED, 0).apply()
            return QuotaState(used = 0, weekKey = weekKey)
        }
        val used = prefs.getInt(KEY_USED, 0).coerceIn(0, WEEKLY_UNLOCK_ALLOWANCE)
        return QuotaState(used = used, weekKey = weekKey)
    }

    private fun currentWeekKey(now: Long = System.currentTimeMillis()): String {
        val date = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
        val fields = WeekFields.ISO
        return "${date.get(fields.weekBasedYear())}-W${date.get(fields.weekOfWeekBasedYear())}"
    }

    companion object {
        const val WEEKLY_ALLOWANCE = WEEKLY_UNLOCK_ALLOWANCE
        private const val PREFS_NAME = "detain_prefs"
        private const val KEY_WEEK = "quota_week"
        private const val KEY_USED = "quota_used"
        private const val KEY_BUBBLES = "bubbles_enabled"
    }
}
