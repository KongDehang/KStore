package com.kk.detain.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kk.detain.ui.formatClock
import com.kk.detain.ui.formatMinutes
import com.kk.detain.ui.formatMinutesShort
import com.kk.detain.ui.theme.Ink
import com.kk.detain.ui.theme.InkFaint
import com.kk.detain.ui.theme.InkSoft
import com.kk.detain.ui.theme.OutlineFaint
import com.kk.detain.ui.theme.Periwinkle
import com.kk.detain.ui.theme.PeriwinkleDeep
import com.kk.detain.ui.theme.PeriwinkleSoft
import com.kk.detain.ui.theme.Rose
import kotlin.math.roundToInt

@Composable
fun DurationPickerDialog(
    title: String,
    description: String? = null,
    initialMinutes: Int,
    confirmLabel: (Int) -> String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val durations = remember { listOf(15, 30, 45, 60, 90, 120) }
    var index by remember {
        mutableIntStateOf(durations.indexOf(initialMinutes).takeIf { it >= 0 } ?: 1)
    }
    val minutes = durations[index]
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp),
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = Ink) },
        text = {
            Column {
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodySmall, color = InkSoft)
                    Spacer(Modifier.height(14.dp))
                }
                Text(formatMinutes(minutes), style = MaterialTheme.typography.displaySmall, color = Ink)
                Spacer(Modifier.height(6.dp))
                Slider(
                    value = index.toFloat(),
                    onValueChange = { index = it.roundToInt().coerceIn(0, durations.lastIndex) },
                    valueRange = 0f..(durations.lastIndex).toFloat(),
                    steps = durations.size - 2,
                    colors = SliderDefaults.colors(
                        thumbColor = Periwinkle,
                        activeTrackColor = Periwinkle,
                        inactiveTrackColor = PeriwinkleSoft,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("15 分钟", style = MaterialTheme.typography.labelSmall, color = InkFaint)
                    Text("2 小时", style = MaterialTheme.typography.labelSmall, color = InkFaint)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(minutes) }) {
                Text(confirmLabel(minutes), color = PeriwinkleDeep)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = InkSoft) }
        },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp),
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = Ink) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, color = InkSoft) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) Rose else PeriwinkleDeep)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = InkSoft) }
        },
    )
}

@Composable
fun WindowEditorDialog(
    initialDays: Set<Int>,
    initialStartMinute: Int,
    initialEndMinute: Int,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: (Set<Int>, Int, Int) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var selectedDays by remember { mutableStateOf(initialDays) }
    var startMinute by remember { mutableIntStateOf(initialStartMinute) }
    var endMinute by remember { mutableIntStateOf(initialEndMinute) }
    var pickerTarget by remember { mutableStateOf<Int?>(null) }

    val duration = if (startMinute == endMinute) {
        24 * 60
    } else {
        ((endMinute - startMinute) + 24 * 60) % (24 * 60)
    }
    val crossesMidnight = endMinute < startMinute
    val summary = buildString {
        append("每周 ${selectedDays.size} 天 · 每次 ${formatMinutesShort(duration)}")
        if (crossesMidnight) append(" · 跨夜")
        if (startMinute == endMinute) append(" · 全天")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .widthIn(max = 420.dp),
        ) {
            Column(Modifier.padding(22.dp)) {
                Text(
                    text = if (isEditing) "编辑锁定时段" else "添加锁定时段",
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "选择重复的日期与时段",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    for (day in 1..7) {
                        DayChip(
                            label = com.kk.detain.ui.weekdayShortName(day),
                            selected = day in selectedDays,
                            onClick = {
                                selectedDays = if (day in selectedDays) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimeField(
                        label = "开始",
                        minute = startMinute,
                        modifier = Modifier.weight(1f),
                        onClick = { pickerTarget = 0 },
                    )
                    TimeField(
                        label = "结束",
                        minute = endMinute,
                        modifier = Modifier.weight(1f),
                        onClick = { pickerTarget = 1 },
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(summary, style = MaterialTheme.typography.labelSmall, color = InkFaint)
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Text("删除", color = Rose)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("取消", color = InkSoft) }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = { onSave(selectedDays, startMinute, endMinute) },
                        enabled = selectedDays.isNotEmpty(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Periwinkle),
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }

    val target = pickerTarget
    if (target != null) {
        TimeInputDialog(
            initialMinute = if (target == 0) startMinute else endMinute,
            onDismiss = { pickerTarget = null },
            onConfirm = { value ->
                if (target == 0) startMinute = value else endMinute = value
                pickerTarget = null
            },
        )
    }
}

@Composable
private fun DayChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (selected) Periwinkle else OutlineFaint)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else InkSoft,
        )
    }
}

@Composable
private fun TimeField(
    label: String,
    minute: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = OutlineFaint,
        modifier = modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = InkFaint)
            Spacer(Modifier.height(2.dp))
            Text(formatClock(minute), style = MaterialTheme.typography.titleLarge, color = Ink)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeInputDialog(
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialMinute / 60,
        initialMinute = initialMinute % 60,
        is24Hour = true,
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color.White) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "选择时间",
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                TimeInput(state = state)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = InkSoft) }
                    TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                        Text("确定", color = PeriwinkleDeep)
                    }
                }
            }
        }
    }
}
