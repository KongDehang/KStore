package com.kk.detain.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kk.detain.data.LockWindow
import com.kk.detain.data.MonitoredApp
import com.kk.detain.domain.ActiveLock
import com.kk.detain.domain.ScheduleCalculator
import com.kk.detain.domain.hasSchedule
import com.kk.detain.domain.resolveActiveLock
import com.kk.detain.ui.DetainViewModel
import com.kk.detain.ui.components.AppIcon
import com.kk.detain.ui.components.BackHeader
import com.kk.detain.ui.components.ConfirmDialog
import com.kk.detain.ui.components.DurationPickerDialog
import com.kk.detain.ui.components.Pill
import com.kk.detain.ui.components.PrimaryButton
import com.kk.detain.ui.components.SoftCard
import com.kk.detain.ui.components.WindowEditorDialog
import com.kk.detain.ui.formatMinutes
import com.kk.detain.ui.formatMinutesShort
import com.kk.detain.ui.formatMonthDay
import com.kk.detain.ui.formatRemainingShort
import com.kk.detain.ui.formatTimeOfDay
import com.kk.detain.ui.scheduleWindowLabel
import com.kk.detain.ui.theme.Ink
import com.kk.detain.ui.theme.InkFaint
import com.kk.detain.ui.theme.InkSoft
import com.kk.detain.ui.theme.Mint
import com.kk.detain.ui.theme.MintSoft
import com.kk.detain.ui.theme.Mist
import com.kk.detain.ui.theme.OutlineFaint
import com.kk.detain.ui.theme.Peach
import com.kk.detain.ui.theme.PeachSoft
import com.kk.detain.ui.theme.Periwinkle
import com.kk.detain.ui.theme.PeriwinkleDeep
import com.kk.detain.ui.theme.PeriwinkleSoft
import com.kk.detain.ui.theme.Rose
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId

@Composable
fun AppDetailScreen(
    vm: DetainViewModel,
    packageName: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val apps by vm.apps.collectAsStateWithLifecycle()
    val allWindows by vm.windows.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val exemptions by vm.exemptions.collectAsStateWithLifecycle()
    val quota by vm.quota.collectAsStateWithLifecycle()
    val usage by vm.usageReport.collectAsStateWithLifecycle()
    val permissions by vm.permissions.collectAsStateWithLifecycle()

    val app = apps.firstOrNull { it.packageName == packageName }
    var everLoaded by remember { mutableStateOf(false) }
    if (app != null) everLoaded = true
    LaunchedEffect(app, everLoaded) {
        if (app == null && everLoaded) onBack()
    }

    if (app == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (!everLoaded) CircularProgressIndicator(color = Periwinkle, strokeWidth = 2.5.dp)
        }
        return
    }

    val windows = remember(allWindows, packageName) {
        allWindows.filter { it.packageName == packageName }
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val lock = resolveActiveLock(app, allWindows, sessions, exemptions, now)
    val locked = lock != null
    val nextStart = remember(windows, app.scheduleEnabled, now / 60_000L) {
        if (app.scheduleEnabled) ScheduleCalculator.nextStart(windows, now) else null
    }

    var showDurationPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showForceUnlockConfirm by remember { mutableStateOf(false) }
    var editingWindow by remember { mutableStateOf<LockWindow?>(null) }
    var addWindowDay by remember { mutableStateOf<Int?>(null) }
    val defaultStart = remember(addWindowDay) { defaultWindowStart() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            BackHeader(
                title = app.label,
                onBack = onBack,
                trailing = {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        enabled = !locked,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "移除",
                            tint = if (locked) InkFaint else InkSoft,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )
        }
        item {
            OverviewCard(
                app = app,
                lock = lock,
                windows = windows,
                nextStart = nextStart,
                todayMinutes = usage?.todayMinutesOf(packageName) ?: 0,
                weekMinutes = usage?.apps?.firstOrNull { it.packageName == packageName }?.weekMinutes ?: 0,
                usageAvailable = permissions.usageAccess,
                now = now,
                editable = !locked,
                onToggleEnabled = { vm.setEnabled(packageName, it) },
            )
        }
        if (lock != null) {
            item {
                ActiveLockDetailCard(
                    lock = lock,
                    quotaRemaining = quota.remaining,
                    now = now,
                    onForceUnlock = { showForceUnlockConfirm = true },
                )
            }
        }
        item {
            ScheduleCard(
                app = app,
                windows = windows,
                locked = locked,
                onToggle = { vm.setScheduleEnabled(packageName, it) },
                onAdd = { day -> addWindowDay = day },
                onEdit = { window -> editingWindow = window },
            )
        }
        item {
            PromptCard(
                app = app,
                onToggle = { vm.setPromptOnOpen(packageName, it) },
                onDefaultMinutes = { minutes -> vm.setDefaultLockMinutes(packageName, minutes) },
            )
        }
        item {
            QuickLockCard(onStart = { showDurationPicker = true })
        }
        item {
            TextButton(
                onClick = { showDeleteConfirm = true },
                enabled = !locked,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (locked) "锁定期间不能移除这个应用" else "不再守护这个应用",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (locked) InkFaint else Rose,
                )
            }
        }
    }

    if (showDurationPicker) {
        DurationPickerDialog(
            title = "锁定多久？",
            description = "到时间会自动解开。中途想要提前结束，需要使用本周的强制解锁机会。",
            initialMinutes = app.defaultLockMinutes,
            confirmLabel = { minutes -> "锁定 ${formatMinutesShort(minutes)}" },
            onDismiss = { showDurationPicker = false },
            onConfirm = { minutes ->
                showDurationPicker = false
                vm.startTimedLock(packageName, minutes)
                Toast.makeText(context, "已开始锁定 ${formatMinutesShort(minutes)}", Toast.LENGTH_SHORT).show()
            },
        )
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "不再守护「${app.label}」？",
            message = "所有定时计划、锁定记录和提醒都会一起删除，这个操作无法撤销。",
            confirmLabel = "移除",
            destructive = true,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                vm.removeApp(packageName)
                onBack()
            },
        )
    }

    if (showForceUnlockConfirm && lock != null) {
        ConfirmDialog(
            title = "确定要强制解锁吗？",
            message = "会消耗 1 次本周强制解锁机会（还剩 ${quota.remaining} 次），应用会一直开放到 ${formatTimeOfDay(lock.endAt)}。",
            confirmLabel = "解锁",
            onDismiss = { showForceUnlockConfirm = false },
            onConfirm = {
                showForceUnlockConfirm = false
                vm.forceUnlock(packageName, lock.endAt) { granted ->
                    val message = if (granted) {
                        "已经解开，好好使用这段时间。"
                    } else {
                        "本周的强制解锁机会已经用完了。"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    val editing = editingWindow
    if (editing != null) {
        WindowEditorDialog(
            initialDays = setOf(editing.dayOfWeek),
            initialStartMinute = editing.startMinute,
            initialEndMinute = editing.endMinute,
            isEditing = true,
            onDismiss = { editingWindow = null },
            onSave = { days, start, end ->
                vm.replaceWindow(editing, days, start, end)
                editingWindow = null
            },
            onDelete = {
                vm.deleteWindow(editing)
                editingWindow = null
            },
        )
    }

    val day = addWindowDay
    if (day != null) {
        WindowEditorDialog(
            initialDays = setOf(day),
            initialStartMinute = defaultStart,
            initialEndMinute = (defaultStart + 60) % (24 * 60),
            isEditing = false,
            onDismiss = { addWindowDay = null },
            onSave = { days, start, end ->
                vm.addWindows(packageName, days, start, end)
                addWindowDay = null
            },
        )
    }
}

@Composable
private fun OverviewCard(
    app: MonitoredApp,
    lock: ActiveLock?,
    windows: List<LockWindow>,
    nextStart: Long?,
    todayMinutes: Int,
    weekMinutes: Int,
    usageAvailable: Boolean,
    now: Long,
    editable: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
) {
    val scheduled = hasSchedule(app, windows)
    val status = when {
        !app.enabled -> DetailStatusSpec("已暂停", Color(0xFFF1F2F8), InkSoft)
        lock != null -> DetailStatusSpec("锁定中", PeachSoft, Color(0xFFB4573A))
        scheduled -> DetailStatusSpec("定时守护", MintSoft, Color(0xFF1F7A65))
        app.promptOnOpen -> DetailStatusSpec("提醒模式", PeriwinkleSoft, PeriwinkleDeep)
        else -> DetailStatusSpec("仅记录", Color(0xFFF1F2F8), InkSoft)
    }
    val usageLine = when {
        !usageAvailable -> "开启使用统计后可以查看真实用时"
        todayMinutes > 0 -> "今日 ${formatMinutes(todayMinutes)} · 近 7 天 ${formatMinutes(weekMinutes)}"
        else -> "今天还没有打开过它"
    }
    val planLine = when {
        lock != null -> "${formatRemainingShort(lock.endAt - now)} 后自动解锁"
        nextStart != null -> "下一次锁定 ${formatMonthDay(nextStart)} ${formatTimeOfDay(nextStart)}"
        scheduled -> "已设置 ${windows.size} 段每周计划"
        else -> "还没有定时计划"
    }

    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(packageName = app.packageName, size = 58.dp, background = Mist)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Ink,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(usageLine, style = MaterialTheme.typography.bodySmall, color = InkSoft)
            }
            Pill(text = status.label, container = status.container, contentColor = status.content)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = planLine,
            style = MaterialTheme.typography.bodySmall,
            color = if (lock != null) Peach else InkSoft,
        )
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = OutlineFaint)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text("启用守护", style = MaterialTheme.typography.titleSmall, color = Ink)
                Text(
                    text = if (editable) {
                        "关闭后不再拦截，也不会提醒。"
                    } else {
                        "锁定结束后才能修改这项设置。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (editable) InkFaint else Peach,
                )
            }
            Switch(
                checked = app.enabled,
                onCheckedChange = onToggleEnabled,
                enabled = editable,
                colors = softSwitchColors(),
            )
        }
    }
}

@Composable
private fun ActiveLockDetailCard(
    lock: ActiveLock,
    quotaRemaining: Int,
    now: Long,
    onForceUnlock: () -> Unit,
) {
    SoftCard(color = Color(0xFFFFF6F1)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = Peach,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("正在锁定中", style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(
                    text = "还剩 ${formatRemainingShort(lock.endAt - now)} · 到点自动解锁",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "本周额度 $quotaRemaining/${com.kk.detain.data.PreferencesStore.WEEKLY_ALLOWANCE}",
                style = MaterialTheme.typography.labelSmall,
                color = InkFaint,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onForceUnlock, enabled = quotaRemaining > 0) {
                Text(
                    text = if (quotaRemaining > 0) "强制解锁" else "额度已用完",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (quotaRemaining > 0) Peach else InkFaint,
                )
            }
        }
    }
}

@Composable
private fun ScheduleCard(
    app: MonitoredApp,
    windows: List<LockWindow>,
    locked: Boolean,
    onToggle: (Boolean) -> Unit,
    onAdd: (Int) -> Unit,
    onEdit: (LockWindow) -> Unit,
) {
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text("每周定时计划", style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "到点自动锁上，到点自动解开。",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
            Switch(
                checked = app.scheduleEnabled,
                onCheckedChange = onToggle,
                enabled = !locked,
                colors = softSwitchColors(),
            )
        }
        if (locked) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "锁定期间暂时不能修改计划。等它自己解开，或使用一次强制解锁机会。",
                style = MaterialTheme.typography.bodySmall,
                color = Peach,
            )
        }
        if (app.scheduleEnabled) {
            Spacer(Modifier.height(12.dp))
            for (day in 1..7) {
                val dayWindows = windows.filter { it.dayOfWeek == day }.sortedBy { it.startMinute }
                DayScheduleRow(
                    day = day,
                    windows = dayWindows,
                    enabled = !locked,
                    onAdd = { onAdd(day) },
                    onEdit = onEdit,
                )
                if (day < 7) {
                    HorizontalDivider(color = OutlineFaint)
                }
            }
            if (windows.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "还没有时段，点右侧的 ＋ 添加第一段。",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                )
            }
        } else {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "开启后可以按周设置锁定时段。没有定时计划时，静水会在每次打开前提醒你。",
                style = MaterialTheme.typography.bodySmall,
                color = InkFaint,
            )
        }
    }
}

@Composable
private fun DayScheduleRow(
    day: Int,
    windows: List<LockWindow>,
    enabled: Boolean,
    onAdd: () -> Unit,
    onEdit: (LockWindow) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = com.kk.detain.ui.weekdayName(day),
            modifier = Modifier.width(46.dp),
            style = MaterialTheme.typography.titleSmall,
            color = Ink,
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (windows.isEmpty()) {
                Text("未设置", style = MaterialTheme.typography.bodySmall, color = InkFaint)
            } else {
                windows.forEach { window ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (enabled) PeriwinkleSoft else Color(0xFFF1F2F8),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = enabled) { onEdit(window) },
                    ) {
                        Text(
                            text = scheduleWindowLabel(window.startMinute, window.endMinute),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (enabled) PeriwinkleDeep else InkFaint,
                        )
                    }
                }
            }
        }
        IconButton(onClick = onAdd, enabled = enabled, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "添加时段",
                tint = if (enabled) PeriwinkleDeep else InkFaint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun PromptCard(
    app: MonitoredApp,
    onToggle: (Boolean) -> Unit,
    onDefaultMinutes: (Int) -> Unit,
) {
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text("打开前提醒", style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "没有定时计划时，打开应用前先停一下，选一个这次的专注时长。",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
            Switch(
                checked = app.promptOnOpen,
                onCheckedChange = onToggle,
                colors = softSwitchColors(),
            )
        }
        if (app.promptOnOpen) {
            Spacer(Modifier.height(14.dp))
            Text("默认专注时长", style = MaterialTheme.typography.labelSmall, color = InkFaint)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (minutes in listOf(15, 30, 45, 60)) {
                    val selected = app.defaultLockMinutes == minutes
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) Periwinkle else OutlineFaint,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDefaultMinutes(minutes) },
                    ) {
                        Text(
                            text = formatMinutesShort(minutes),
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White else InkSoft,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickLockCard(onStart: () -> Unit) {
    SoftCard(color = Color(0xFFF6F8FF)) {
        Text("立即锁一段", style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(3.dp))
        Text(
            text = "给自己留一段不被打扰的时间，到点自动解锁。",
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
        )
        Spacer(Modifier.height(14.dp))
        PrimaryButton(text = "开始锁定", onClick = onStart)
    }
}

@Composable
private fun softSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = Periwinkle,
    checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = Color.White,
    uncheckedTrackColor = Color(0xFFE9EBF3),
    uncheckedBorderColor = Color.Transparent,
)

private data class DetailStatusSpec(val label: String, val container: Color, val content: Color)

private fun defaultWindowStart(now: Long = System.currentTimeMillis()): Int {
    val time = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalTime()
    val minutes = time.hour * 60 + time.minute
    val rounded = ((minutes + 29) / 30) * 30
    return if (rounded >= 24 * 60) 0 else rounded
}
