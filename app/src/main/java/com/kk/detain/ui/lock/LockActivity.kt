package com.kk.detain.ui.lock

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.kk.detain.DetainApp
import com.kk.detain.data.LockSource
import com.kk.detain.domain.LockKind
import com.kk.detain.service.GuardDiagnostics
import com.kk.detain.ui.theme.DetainTheme
import com.kk.detain.ui.theme.Mist
import kotlinx.coroutines.launch

class LockActivity : ComponentActivity() {

    private val container by lazy { DetainApp.container(this) }

    private var model by mutableStateOf<LockModel?>(null)
    private var nudgeTick by mutableIntStateOf(0)
    private var quotaRemaining by mutableIntStateOf(2)
    private var bubblesEnabled by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        container.preferences.refreshQuota()
        quotaRemaining = container.preferences.quota.value.remaining
        bubblesEnabled = container.preferences.bubblesEnabled.value

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (model?.mode == LockMode.PROMPT) {
                        goHome()
                    } else {
                        // Back is not an escape hatch. It just earns a gentle reminder.
                        nudgeTick++
                    }
                }
            },
        )

        handleIntent(intent)
        if (model == null) {
            finish()
            return
        }
        setContent {
            DetainTheme {
                val current = model
                if (current == null) {
                    Box(Modifier.fillMaxSize().background(Mist))
                } else {
                    LockScreen(
                        model = current,
                        quotaRemaining = quotaRemaining,
                        bubblesEnabled = bubblesEnabled,
                        nudgeTick = nudgeTick,
                        onStartSupervision = ::startSupervision,
                        onSkipPrompt = ::skipPrompt,
                        onForceUnlock = ::forceUnlock,
                        onGoHome = ::goHome,
                        onLockExpired = ::finishAfterUnlock,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        isShowing = true
        GuardDiagnostics.noteLockShown()
        container.preferences.refreshQuota()
        quotaRemaining = container.preferences.quota.value.remaining
        bubblesEnabled = container.preferences.bubblesEnabled.value
        val current = model
        if (current != null && current.mode == LockMode.LOCKED &&
            current.endAt <= System.currentTimeMillis()
        ) {
            finish()
        }
    }

    override fun onPause() {
        isShowing = false
        super.onPause()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val packageName = intent.getStringExtra(EXTRA_PACKAGE) ?: run {
            finish()
            return
        }
        val mode = runCatching {
            LockMode.valueOf(intent.getStringExtra(EXTRA_MODE) ?: LockMode.LOCKED.name)
        }.getOrDefault(LockMode.LOCKED)
        val endAt = intent.getLongExtra(EXTRA_END_AT, 0L)
        val startAt = intent.getLongExtra(EXTRA_START_AT, 0L)
        val suggested = intent.getIntExtra(EXTRA_SUGGESTED_MINUTES, 30).coerceIn(5, 180)
        val label = intent.getStringExtra(EXTRA_LABEL) ?: packageName
        val attempts = intent.getIntExtra(EXTRA_ATTEMPTS, 0)
        val kind = runCatching {
            LockKind.valueOf(intent.getStringExtra(EXTRA_KIND) ?: LockKind.TIMED.name)
        }.getOrDefault(LockKind.TIMED)

        if (mode == LockMode.LOCKED && endAt <= System.currentTimeMillis()) {
            finish()
            return
        }
        model = LockModel(
            packageName = packageName,
            label = label,
            mode = mode,
            startAt = startAt,
            endAt = endAt,
            suggestedMinutes = suggested,
            attemptCount = attempts,
            kind = kind,
        )
    }

    private fun startSupervision(minutes: Int) {
        val current = model ?: return
        lifecycleScope.launch {
            val session = container.repository.startTimedLock(
                packageName = current.packageName,
                minutes = minutes,
                source = LockSource.PROMPT,
            )
            model = current.copy(
                mode = LockMode.LOCKED,
                startAt = session.startAt,
                endAt = session.endAt,
            )
            container.preferences.refreshQuota()
            quotaRemaining = container.preferences.quota.value.remaining
        }
    }

    private fun skipPrompt() {
        val current = model ?: return
        lifecycleScope.launch {
            container.repository.grantExemption(
                current.packageName,
                System.currentTimeMillis() + 30 * 60_000L,
            )
            finish()
        }
    }

    private fun forceUnlock() {
        val current = model ?: return
        lifecycleScope.launch {
            val granted = container.preferences.consumeQuota()
            if (granted) {
                container.repository.grantExemption(current.packageName, current.endAt)
                Toast.makeText(this@LockActivity, "已解锁。休息不是逃跑。", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                quotaRemaining = 0
                Toast.makeText(this@LockActivity, "本周的强制解锁机会已经用完了。", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(home) }
        finish()
    }

    private fun finishAfterUnlock() {
        Toast.makeText(this, "锁已经自动解开了，去使用吧。", Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_START_AT = "extra_start_at"
        const val EXTRA_END_AT = "extra_end_at"
        const val EXTRA_SUGGESTED_MINUTES = "extra_suggested_minutes"
        const val EXTRA_ATTEMPTS = "extra_attempts"
        const val EXTRA_KIND = "extra_kind"

        @Volatile
        var isShowing: Boolean = false
            private set
    }
}

enum class LockMode { LOCKED, PROMPT }

data class LockModel(
    val packageName: String,
    val label: String,
    val mode: LockMode,
    val startAt: Long,
    val endAt: Long,
    val suggestedMinutes: Int,
    val attemptCount: Int,
    val kind: LockKind = LockKind.TIMED,
)
