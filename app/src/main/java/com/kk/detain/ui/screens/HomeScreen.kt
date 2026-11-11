package com.kk.detain.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kk.detain.data.LockWindow
import com.kk.detain.data.MonitoredApp
import com.kk.detain.domain.ActiveLock
import com.kk.detain.domain.Tips
import com.kk.detain.domain.hasSchedule
import com.kk.detain.domain.resolveActiveLock
import com.kk.detain.ui.DetainViewModel
import com.kk.detain.ui.PermissionStatus
import com.kk.detain.ui.components.AppIcon
import com.kk.detain.ui.components.EmptyState
import com.kk.detain.ui.components.PageHeader
import com.kk.detain.ui.components.Pill
import com.kk.detain.ui.components.PrimaryButton
import com.kk.detain.ui.components.QuotaHearts
import com.kk.detain.ui.components.SectionHeader
import com.kk.detain.ui.components.SoftCard
import com.kk.detain.ui.formatMinutes
import com.kk.detain.ui.formatMonthDay
import com.kk.detain.ui.formatRemainingShort
import com.kk.detain.ui.formatWeekday
import com.kk.detain.ui.greetingForHour
import com.kk.detain.ui.openAccessibilitySettings
import com.kk.detain.ui.theme.Ink
import com.kk.detain.ui.theme.InkFaint
import com.kk.detain.ui.theme.InkSoft
import com.kk.detain.ui.theme.Mint
import com.kk.detain.ui.theme.MintSoft
import com.kk.detain.ui.theme.Peach
import com.kk.detain.ui.theme.PeachSoft
import com.kk.detain.ui.theme.PeriwinkleDeep
import com.kk.detain.ui.theme.PeriwinkleSoft
import kotlinx.coroutines.delay
import java.time.LocalDateTime

@Composable
fun HomeScreen(
    vm: DetainViewModel,
    onAddApp: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    val windows by vm.windows.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val exemptions by vm.exemptions.collectAsStateWithLifecycle()
    val quota by vm.quota.collectAsStateWithLifecycle()
    val permissions by vm.permissions.collectAsStateWithLifecycle()
    val usage by vm.usageReport.collectAsStateWithLifecycle()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    val activeLocks = apps.mapNotNull { app ->
        resolveActiveLock(app, windows, sessions, exemptions, now)?.let { app to it }
    }
    val tip = remember { Tips.forToday().first() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HomeHeader()
        }
        item {
            HeroCard(
                appCount = apps.size,
                activeCount = activeLocks.size,
                quotaRemaining = quota.remaining,
                usage = usage,
                onOpenStats = onOpenStats,
            )
        }
        item {
            QuotaCard(remaining = quota.remaining)
        }
        if (!permissions.coreReady) {
            item { PermissionNudge(permissions = permissions, onOpenSettings = onOpenSettings) }
        }
        if (activeLocks.isNotEmpty()) {
            item { SectionHeader("正在替你守住") }
            items(activeLocks, key = { "lock-${it.first.packageName}" }) { pair ->
                val app = pair.first
                val lock = pair.second
                ActiveLockCard(app = app, lock = lock, now = now, onClick = { onOpenDetail(app.packageName) })
            }
        }
        item {
            SectionHeader("守护名单") {
                TextButton(onClick = onAddApp) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("添加")
                }
            }
        }
        if (apps.isEmpty()) {
            item {
                EmptyState(
                    icon = { Icon(Icons.Rounded.Spa, contentDescription = null, tint = PeriwinkleDeep) },
                    title = "还没有守护的应用",
                    body = "从最想少刷一会的那个开始。添加后可以设置每周的锁定时段，或者每次打开前提醒你一下。",
                    action = { PrimaryButton("添加第一个应用", onAddApp) },
                )
            }
        } else {
            items(apps, key = { "app-${it.packageName}" }) { app ->
                AppRow(
                    app = app,
                    lock = activeLocks.firstOrNull { it.first.packageName == app.packageName }?.second,
                    windows = windows.filter { it.packageName == app.packageName },
                    usageMinutes = usage?.todayMinutesOf(app.packageName) ?: 0,
                    usageAvailable = permissions.usageAccess,
                    now = now,
                    onClick = { onOpenDetail(app.packageName) },
                )
            }
        }
        item {
            TipCard(title = tip.title, body = tip.body)
        }
    }
}

@Composable
private fun HomeHeader() {
    val timestamp = System.currentTimeMillis()
    val hour = LocalDateTime.now().hour
    PageHeader(
        title = greetingForHour(hour),
        subtitle = "${formatMonthDay(timestamp)} · ${formatWeekday(timestamp)}",
    )
}

@Composable
private fun HeroCard(
    appCount: Int,
    activeCount: Int,
    quotaRemaining: Int,
    usage: com.kk.detain.usage.UsageReport?,
    onOpenStats: () -> Unit,
) {
    val comparison = when {
        usage == null -> "开启使用统计，看看时间都去哪了"
        usage.todayTotal == 0 && usage.yesterdayTotal == 0 -> "今天还没有打开它们，很好"
        usage.yesterdayTotal == 0 -> "昨天几乎没有使用"
        usage.todayChange <= 0 -> "比昨天少 ${formatMinutes(-usage.todayChange)}"
        else -> "比昨天多 ${formatMinutes(usage.todayChange)}"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFEDEFFF), Color(0xFFE8F8F1), Color(0xFFFFF2EB)),
                ),
            ),
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 38.dp, y = (-52).dp)
                .size(150.dp)
                .background(Color.White.copy(alpha = 0.45f), CircleShape),
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 26.dp, y = 34.dp)
                .size(84.dp)
                .background(Color.White.copy(alpha = 0.35f), CircleShape),
        )
        Column(Modifier.padding(22.dp)) {
            Text(
                text = "今天，你在守护的应用上用了",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (usage == null) "—" else formatMinutes(usage.todayTotal),
                style = MaterialTheme.typography.displaySmall,
                color = Ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(text = comparison, style = MaterialTheme.typography.bodySmall, color = InkSoft)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip("守护 $appCount 个")
                InfoChip("锁定 $activeCount 个")
                InfoChip("额度 $quotaRemaining/2")
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onOpenStats)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "看看时间都去哪了",
                    style = MaterialTheme.typography.labelMedium,
                    color = PeriwinkleDeep,
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = PeriwinkleDeep,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(color = Color.White.copy(alpha = 0.72f), shape = RoundedCornerShape(12.dp)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Ink,
        )
    }
}

@Composable
private fun QuotaCard(remaining: Int) {
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PeachSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = Peach,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("本周克制额度", style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "强制解锁还剩 $remaining 次 · 每周一 00:00 重置",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
            QuotaHearts(remaining = remaining, size = 19.dp)
        }
    }
}

@Composable
private fun ActiveLockCard(app: MonitoredApp, lock: ActiveLock, now: Long, onClick: () -> Unit) {
    val remaining = (lock.endAt - now).coerceAtLeast(0L)
    val total = (lock.endAt - lock.startAt).coerceAtLeast(1L)
    val progress = (remaining.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    SoftCard(onClick = onClick, color = Color(0xFFFFF6F1), padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(packageName = app.packageName, size = 42.dp, background = Color.White)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${formatRemainingShort(remaining)} 后自动解锁",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = Peach,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = InkFaint,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(50)),
            color = Peach,
            trackColor = Color.White.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun AppRow(
    app: MonitoredApp,
    lock: ActiveLock?,
    windows: List<LockWindow>,
    usageMinutes: Int,
    usageAvailable: Boolean,
    now: Long,
    onClick: () -> Unit,
) {
    val scheduled = hasSchedule(app, windows)
    val status = when {
        !app.enabled -> HomeStatusSpec("已暂停", Color(0xFFF1F2F8), InkSoft)
        lock != null -> HomeStatusSpec("锁定中", PeachSoft, Color(0xFFB4573A))
        scheduled -> HomeStatusSpec("守护中", MintSoft, Color(0xFF1F7A65))
        app.promptOnOpen -> HomeStatusSpec("提醒模式", PeriwinkleSoft, PeriwinkleDeep)
        else -> null
    }

    val parts = mutableListOf<String>()
    when {
        !app.enabled -> parts += "守护已暂停"
        lock != null -> parts += "${formatRemainingShort(lock.endAt - now)} 后解锁"
        else -> {
            parts += if (usageAvailable) {
                if (usageMinutes > 0) "今日 ${formatMinutes(usageMinutes)}" else "今日还没打开"
            } else {
                "今日用时未知"
            }
            if (scheduled) {
                parts += "每周 ${windows.size} 段计划"
            } else if (app.promptOnOpen) {
                parts += "打开前提醒"
            }
        }
    }

    SoftCard(onClick = onClick, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(
                packageName = app.packageName,
                size = 46.dp,
                background = if (app.enabled) Color(0xFFF7F8FD) else Color(0xFFF2F3F7),
            )
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (app.enabled) Ink else InkSoft,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = parts.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
            if (status != null) {
                Pill(text = status.label, container = status.container, contentColor = status.content)
                Spacer(Modifier.width(4.dp))
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = InkFaint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private data class HomeStatusSpec(val label: String, val container: Color, val content: Color)

@Composable
private fun PermissionNudge(permissions: PermissionStatus, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    SoftCard(color = Color(0xFFFFF8EE)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = Peach,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (!permissions.accessibility) {
                        "拦截未开启：无障碍服务"
                    } else {
                        "使用统计未开启"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (!permissions.accessibility) Color(0xFFB4573A) else Ink,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (!permissions.accessibility) {
                        "没有它，静水无法知道你把哪个应用打开了，锁定页也不会出现。开启后回来，重新添加应用即可。"
                    } else {
                        "开启使用情况访问权限，才能看到每个应用的真实用时；不影响拦截。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        TextButton(
            onClick = {
                if (!permissions.accessibility) {
                    openAccessibilitySettings(context)
                } else {
                    onOpenSettings()
                }
            },
        ) {
            Text(if (!permissions.accessibility) "开启无障碍" else "去设置")
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun TipCard(title: String, body: String) {
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MintSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.TipsAndUpdates,
                    contentDescription = null,
                    tint = Mint,
                    modifier = Modifier.size(17.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text("今日小建议", style = MaterialTheme.typography.titleSmall, color = InkSoft)
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodySmall, color = InkSoft)
    }
}
