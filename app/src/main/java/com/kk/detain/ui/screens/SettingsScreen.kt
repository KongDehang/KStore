package com.kk.detain.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kk.detain.BuildConfig
import com.kk.detain.service.ForegroundSignal
import com.kk.detain.service.GuardDiagnostics
import com.kk.detain.service.GuardEvent
import com.kk.detain.ui.DetainViewModel
import com.kk.detain.ui.PermissionStatus
import com.kk.detain.ui.components.PageHeader
import com.kk.detain.ui.components.Pill
import com.kk.detain.ui.components.QuotaHearts
import com.kk.detain.ui.components.SectionHeader
import com.kk.detain.ui.components.SoftCard
import com.kk.detain.ui.formatTimeWithSeconds
import com.kk.detain.ui.openAccessibilitySettings
import com.kk.detain.ui.openAppDetailsSettings
import com.kk.detain.ui.openNotificationSettings
import com.kk.detain.ui.openUsageAccessSettings
import com.kk.detain.ui.theme.Ink
import com.kk.detain.ui.theme.InkFaint
import com.kk.detain.ui.theme.InkSoft
import com.kk.detain.ui.theme.Mint
import com.kk.detain.ui.theme.MintSoft
import com.kk.detain.ui.theme.OutlineFaint
import com.kk.detain.ui.theme.Peach
import com.kk.detain.ui.theme.PeachSoft
import com.kk.detain.ui.theme.Periwinkle
import com.kk.detain.ui.theme.PeriwinkleDeep
import com.kk.detain.ui.theme.PeriwinkleSoft
import com.kk.detain.ui.theme.SandSoft

@Composable
fun SettingsScreen(vm: DetainViewModel) {
    val context = LocalContext.current
    val permissions by vm.permissions.collectAsStateWithLifecycle()
    val quota by vm.quota.collectAsStateWithLifecycle()
    val bubblesEnabled by vm.bubblesEnabled.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val serviceConnected by GuardDiagnostics.serviceConnected.collectAsStateWithLifecycle()
    val lastForeground by GuardDiagnostics.lastForeground.collectAsStateWithLifecycle()
    val guardEvents by GuardDiagnostics.events.collectAsStateWithLifecycle()
    val launchBlocked by GuardDiagnostics.launchBlocked.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PageHeader(title = "设置", subtitle = "让守护按你的方式发生。")
        }
        item { SectionHeader("守护能力") }
        item {
            SoftCard(padding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)) {
                PermissionRow(
                    icon = Icons.Rounded.Accessibility,
                    title = "无障碍服务",
                    description = "在已守护的应用被打开时，让静水及时出现。",
                    granted = permissions.accessibility,
                    onRequest = { openAccessibilitySettings(context) },
                )
                HorizontalDivider(color = OutlineFaint)
                PermissionRow(
                    icon = Icons.Rounded.QueryStats,
                    title = "使用情况访问",
                    description = "读取每个守护应用的真实使用时长。",
                    granted = permissions.usageAccess,
                    onRequest = { openUsageAccessSettings(context) },
                )
                HorizontalDivider(color = OutlineFaint)
                PermissionRow(
                    icon = Icons.Rounded.NotificationsNone,
                    title = "通知",
                    description = "在通知栏显示锁定状态与倒计时。",
                    granted = permissions.notifications,
                    onRequest = { openNotificationSettings(context) },
                )
            }
        }
        item {
            Text(
                text = "静水不读取屏幕内容，也不会上传任何数据。",
                style = MaterialTheme.typography.labelSmall,
                color = InkFaint,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
        item { SectionHeader("运行诊断") }
        item {
            DiagnosticsCard(
                permissions = permissions,
                serviceConnected = serviceConnected,
                lastForeground = lastForeground,
                latestEvent = guardEvents.firstOrNull(),
                launchBlocked = launchBlocked,
                hasApps = apps.any { it.enabled },
                onOpenAppSettings = { openAppDetailsSettings(context) },
                onTest = {
                    if (!permissions.accessibility) {
                        Toast.makeText(context, "先开启无障碍服务，才能测试拦截。", Toast.LENGTH_SHORT).show()
                    } else {
                        val target = apps.firstOrNull { it.enabled }
                        val intent = target?.let {
                            context.packageManager.getLaunchIntentForPackage(it.packageName)
                        }
                        if (intent != null) {
                            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } else {
                            Toast.makeText(context, "还没有可测试的守护应用。", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onCopy = {
                    val text = buildDiagnosticsText(
                        permissions = permissions,
                        serviceConnected = serviceConnected,
                        lastForeground = lastForeground,
                        latestEvent = guardEvents.firstOrNull(),
                        launchBlocked = launchBlocked,
                    )
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(ClipData.newPlainText("静水诊断", text))
                    Toast.makeText(context, "诊断信息已复制，可以直接粘贴发给我。", Toast.LENGTH_SHORT).show()
                },
            )
        }
        item { SectionHeader("克制额度") }
        item {
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
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = Peach,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("本周强制解锁", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "每周 2 次 · 周一 00:00 重置",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                    }
                    QuotaHearts(remaining = quota.remaining, size = 19.dp)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .background(SandSoft)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "剩余 ${quota.remaining} 次",
                        style = MaterialTheme.typography.labelMedium,
                        color = Ink,
                    )
                    Text(
                        text = "已用 ${quota.used} 次",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSoft,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "把它留给真正的意外，而不是一时的冲动。",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                )
            }
        }
        item { SectionHeader("提醒方式") }
        item {
            SoftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PeriwinkleSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Spa,
                            contentDescription = null,
                            tint = PeriwinkleDeep,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("心痒时的鸡汤气泡", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "多次尝试打开被锁定的应用时，屏幕上会飘出一些提醒。",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                    }
                    Switch(
                        checked = bubblesEnabled,
                        onCheckedChange = { vm.setBubblesEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Periwinkle,
                            checkedBorderColor = Color.Transparent,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFE9EBF3),
                            uncheckedBorderColor = Color.Transparent,
                        ),
                    )
                }
            }
        }
        item { SectionHeader("怎么用") }
        item {
            SoftCard {
                StepRow(number = "1", text = "添加一个最想少刷一会的应用。")
                StepRow(number = "2", text = "为它设置每周的锁定时段，或者只在打开前提醒。")
                StepRow(number = "3", text = "到时间自动解开。想提前结束，只能用每周那两次机会。")
            }
        }
        item { SectionHeader("关于") }
        item {
            SoftCard(color = Color(0xFFF8F9FE)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Spa,
                            contentDescription = null,
                            tint = Mint,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("静水", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Text(
                            text = "把时间还给生活 · v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                    }
                    Pill("本机存储", MintSoft, Mint)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "所有守护计划、锁定记录和使用统计都只保存在这台手机上。",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                )
            }
        }
    }
}

@Composable
private fun DiagnosticsCard(
    permissions: PermissionStatus,
    serviceConnected: Boolean,
    lastForeground: ForegroundSignal?,
    latestEvent: GuardEvent?,
    launchBlocked: Boolean,
    hasApps: Boolean,
    onOpenAppSettings: () -> Unit,
    onTest: () -> Unit,
    onCopy: () -> Unit,
) {
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PeriwinkleSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.BugReport,
                    contentDescription = null,
                    tint = PeriwinkleDeep,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text("运行诊断", style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "看看拦截链路卡在哪一步",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
            Pill(
                text = if (serviceConnected) "运行中" else "未连接",
                container = if (serviceConnected) MintSoft else PeachSoft,
                contentColor = if (serviceConnected) Mint else Peach,
            )
        }
        Spacer(Modifier.height(12.dp))
        DiagnosticRow(
            label = "无障碍授权",
            value = if (permissions.accessibility) "已开启" else "未开启",
            ok = permissions.accessibility,
        )
        DiagnosticRow(
            label = "最近检测到的守护应用",
            value = lastForeground?.let { "${formatTimeWithSeconds(it.time)} · ${it.packageName}" }
                ?: "还没有事件",
            ok = lastForeground != null,
        )
        DiagnosticRow(
            label = "最近一次拦截",
            value = latestEvent?.let {
                "${formatTimeWithSeconds(it.time)} · ${it.label ?: it.packageName} · ${it.outcome}"
            } ?: "还没有记录",
            ok = latestEvent != null,
        )
        if (permissions.accessibility && !serviceConnected) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "无障碍已授权但服务未连接：请到系统设置里把「静水 · 应用守护」关闭再打开，或重启手机后重试。",
                style = MaterialTheme.typography.bodySmall,
                color = Peach,
            )
        }
        if (launchBlocked) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "锁定页没有显示出来。请到系统设置里允许静水「后台弹出界面 / 显示在其他应用上层 / 自启动」，然后再试一次。",
                style = MaterialTheme.typography.bodySmall,
                color = Peach,
            )
        }
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = OutlineFaint)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onTest, enabled = hasApps) {
                Text("测试拦截", style = MaterialTheme.typography.labelMedium)
            }
            TextButton(onClick = onOpenAppSettings) {
                Text("应用权限设置", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCopy) {
                Text(
                    text = "复制诊断",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSoft,
                )
            }
        }
        Text(
            text = "测试会打开第一个守护应用；若出现提醒或锁定页，说明拦截链路正常。",
            style = MaterialTheme.typography.labelSmall,
            color = InkFaint,
        )
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String, ok: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = InkFaint,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1.7f),
            style = MaterialTheme.typography.labelSmall,
            color = if (ok) Ink else InkSoft,
            textAlign = TextAlign.End,
        )
    }
}

private fun buildDiagnosticsText(
    permissions: PermissionStatus,
    serviceConnected: Boolean,
    lastForeground: ForegroundSignal?,
    latestEvent: GuardEvent?,
    launchBlocked: Boolean,
): String = buildString {
    appendLine("静水 · 运行诊断")
    appendLine(
        "设备：${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    )
    appendLine("无障碍授权：${permissions.accessibility}")
    appendLine("服务连接：$serviceConnected")
    appendLine("使用统计授权：${permissions.usageAccess}")
    appendLine("通知授权：${permissions.notifications}")
    appendLine(
        "最近守护应用：" + (
            lastForeground?.let { "${it.packageName} @ ${formatTimeWithSeconds(it.time)}" } ?: "无"
        ),
    )
    appendLine(
        "最近拦截：" + (
            latestEvent?.let {
                "${it.label ?: it.packageName} · ${it.outcome} @ ${formatTimeWithSeconds(it.time)}"
            } ?: "无"
        ),
    )
    appendLine("锁定页未显示：$launchBlocked")
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (granted) MintSoft else OutlineFaint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (granted) Mint else InkSoft,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Ink)
            Spacer(Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = InkFaint)
        }
        if (granted) {
            Pill("已开启", MintSoft, Mint)
        } else {
            TextButton(onClick = onRequest) {
                Text(
                    text = "去开启",
                    style = MaterialTheme.typography.labelMedium,
                    color = PeriwinkleDeep,
                )
            }
        }
    }
}

@Composable
private fun StepRow(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(PeriwinkleSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                color = PeriwinkleDeep,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
            modifier = Modifier.weight(1f),
        )
    }
}
