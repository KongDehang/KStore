package com.kk.detain.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kk.detain.domain.Tips
import com.kk.detain.ui.DetainViewModel
import com.kk.detain.ui.components.AppIcon
import com.kk.detain.ui.components.PageHeader
import com.kk.detain.ui.components.Pill
import com.kk.detain.ui.components.PrimaryButton
import com.kk.detain.ui.components.SectionHeader
import com.kk.detain.ui.components.SoftCard
import com.kk.detain.ui.formatMinutes
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
import com.kk.detain.ui.weekdayShortName
import com.kk.detain.usage.UsageReport

@Composable
fun StatsScreen(vm: DetainViewModel, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val usage by vm.usageReport.collectAsStateWithLifecycle()
    val permissions by vm.permissions.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val quota by vm.quota.collectAsStateWithLifecycle()
    val attempts by vm.weeklyAttempts.collectAsStateWithLifecycle()
    val tips = remember { Tips.forToday() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PageHeader(
                title = "使用统计",
                subtitle = "看见时间，才好把它收回来。",
            )
        }
        if (!permissions.usageAccess) {
            item {
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
                                imageVector = Icons.Rounded.QueryStats,
                                contentDescription = null,
                                tint = Peach,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("还没有读取使用情况的权限", style = MaterialTheme.typography.titleMedium, color = Ink)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "开启后，这里会显示每个守护应用的真实用时。",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(
                        text = "去开启使用情况访问",
                        onClick = { openUsageAccessSettings(context) },
                    )
                }
            }
        }
        item {
            SummaryCard(usage = usage, appCount = apps.size)
        }
        item {
            WeekChartCard(usage = usage, usageAvailable = permissions.usageAccess)
        }
        item {
            SectionHeader("用在哪里")
        }
        item {
            AppUsageCard(usage = usage, usageAvailable = permissions.usageAccess)
        }
        item {
            AttemptsCard(attempts = attempts, forcedUnlocks = quota.used, onOpenSettings = onOpenSettings)
        }
        item {
            SectionHeader("给自己的一些小办法")
        }
        items(tips.size) { index ->
            val tip = tips[index]
            SoftCard {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MintSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Mint,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(tip.title, style = MaterialTheme.typography.titleSmall, color = Ink)
                        Spacer(Modifier.height(3.dp))
                        Text(tip.body, style = MaterialTheme.typography.bodySmall, color = InkSoft)
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.TipsAndUpdates,
                    contentDescription = null,
                    tint = InkFaint,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "数据只保存在你的手机上。",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkFaint,
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(usage: UsageReport?, appCount: Int) {
    val comparison = when {
        usage == null -> "—"
        usage.todayTotal == 0 && usage.yesterdayTotal == 0 -> "暂无"
        usage.todayChange == 0 -> "持平"
        usage.todayChange < 0 -> "少 ${formatMinutes(-usage.todayChange)}"
        else -> "多 ${formatMinutes(usage.todayChange)}"
    }
    SoftCard {
        Text("最近 7 天，在守护应用上", style = MaterialTheme.typography.bodySmall, color = InkSoft)
        Spacer(Modifier.height(6.dp))
        Text(
            text = usage?.let { formatMinutes(it.weekTotal) } ?: "—",
            style = MaterialTheme.typography.displaySmall,
            color = Ink,
        )
        Spacer(Modifier.height(16.dp))
        Row {
            StatColumn(label = "今日", value = usage?.let { formatMinutes(it.todayTotal) } ?: "—")
            StatColumn(label = "对比昨天", value = comparison)
            StatColumn(label = "守护应用", value = "$appCount 个")
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.StatColumn(label: String, value: String) {
    Column(Modifier.weight(1f).padding(end = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = InkFaint)
        Spacer(Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = Ink, maxLines = 1)
    }
}

@Composable
private fun WeekChartCard(usage: UsageReport?, usageAvailable: Boolean) {
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("最近 7 天", style = MaterialTheme.typography.titleMedium, color = Ink)
            Spacer(Modifier.weight(1f))
            val peak = usage?.days?.maxOfOrNull { it.minutes } ?: 0
            if (peak > 0) {
                Pill("峰值 ${formatMinutes(peak)}", PeriwinkleSoft, PeriwinkleDeep)
            }
        }
        Spacer(Modifier.height(18.dp))
        when {
            !usageAvailable -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "开启使用情况访问后，这里会显示最近 7 天的走势。",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
            }
            usage == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Periwinkle, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                }
            }
            usage.days.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "还没有可统计的使用记录。",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
            }
            else -> {
                val hasData = usage.days.any { it.minutes > 0 }
                if (!hasData) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "这一周还没有记录。\n当你真正用起来，这里会出现时间的形状。",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                } else {
                    val maxMinutes = (usage.days.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        usage.days.forEachIndexed { index, day ->
                            val fraction = (day.minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f)
                            val animated by animateFloatAsState(
                                targetValue = fraction,
                                animationSpec = tween(650),
                                label = "bar$index",
                            )
                            val isToday = index == usage.days.lastIndex
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(112.dp)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.BottomCenter,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(16.dp)
                                            .height((108.dp * animated).coerceAtLeast(4.dp))
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    day.minutes == 0 -> SolidColor(OutlineFaint)
                                                    isToday -> Brush.verticalGradient(listOf(Periwinkle, Mint))
                                                    else -> Brush.verticalGradient(
                                                        listOf(
                                                            Periwinkle.copy(alpha = 0.45f),
                                                            Mint.copy(alpha = 0.35f),
                                                        ),
                                                    )
                                                },
                                            ),
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = weekdayShortName(day.date.dayOfWeek.value),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isToday) PeriwinkleDeep else InkFaint,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUsageCard(usage: UsageReport?, usageAvailable: Boolean) {
    val rows = usage?.apps?.filter { it.weekMinutes > 0 }.orEmpty()
    if (rows.isEmpty()) {
        SoftCard {
            Text(
                text = when {
                    !usageAvailable -> "开启使用情况访问后，这里会出现每个应用的真实用时。"
                    usage == null -> "正在读取使用记录…"
                    else -> "最近 7 天还没有可统计的使用记录，保持下去。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
        }
        return
    }
    val maxMinutes = rows.maxOf { it.weekMinutes }.coerceAtLeast(1)
    SoftCard {
        rows.forEachIndexed { index, item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(packageName = item.packageName, size = 34.dp, background = Color(0xFFF7F8FD))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatMinutes(item.weekMinutes),
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSoft,
                )
            }
            Spacer(Modifier.height(7.dp))
            LinearProgressIndicator(
                progress = { (item.weekMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (index == 0) Periwinkle else Periwinkle.copy(alpha = 0.55f),
                trackColor = OutlineFaint,
            )
            if (index < rows.lastIndex) Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun AttemptsCard(attempts: Int, forcedUnlocks: Int, onOpenSettings: () -> Unit) {
    SoftCard(color = Color(0xFFFFF6F1)) {
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
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (attempts > 0) "本周拦下了 $attempts 次冲动" else "本周还没有被打断",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (attempts > 0) {
                        "其中用了 $forcedUnlocks 次强制解锁机会。"
                    } else {
                        "继续把注意力留给真正想做的事。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onOpenSettings) {
            Text("调整守护设置", style = MaterialTheme.typography.labelMedium, color = Peach)
        }
    }
}
