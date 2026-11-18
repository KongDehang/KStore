package com.kk.detain.data

import android.content.Context
import android.content.SharedPreferences
import com.kk.detain.domain.ScheduleCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val MONTHLY_UNLOCK_ALLOWANCE = 2

data class QuotaState(
    val used: Int,
    val monthKey: String,
) {
    val remaining: Int get() = (MONTHLY_UNLOCK_ALLOWANCE - used).coerceAtLeast(0)
    val resetAt: Long get() = ScheduleCalculator.nextMonthReset()
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
        val next = QuotaState(current.used + 1, current.monthKey)
        prefs.edit()
            .putString(KEY_MONTH, next.monthKey)
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
        val monthKey = ScheduleCalculator.monthKey()
        val storedMonth = prefs.getString(KEY_MONTH, null)
        if (storedMonth != monthKey) {
            prefs.edit()
                .putString(KEY_MONTH, monthKey)
                .putInt(KEY_USED, 0)
                .remove(KEY_LEGACY_WEEK)
                .apply()
            return QuotaState(used = 0, monthKey = monthKey)
        }
        val used = prefs.getInt(KEY_USED, 0).coerceIn(0, MONTHLY_UNLOCK_ALLOWANCE)
        return QuotaState(used = used, monthKey = monthKey)
    }

    companion object {
        const val MONTHLY_ALLOWANCE = MONTHLY_UNLOCK_ALLOWANCE
        private const val PREFS_NAME = "detain_prefs"
        private const val KEY_MONTH = "quota_month"
        private const val KEY_LEGACY_WEEK = "quota_week"
        private const val KEY_USED = "quota_used"
        private const val KEY_BUBBLES = "bubbles_enabled"
    }
}
