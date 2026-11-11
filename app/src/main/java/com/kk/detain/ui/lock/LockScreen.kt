package com.kk.detain.ui.lock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kk.detain.DetainApp
import com.kk.detain.domain.Encouragements
import com.kk.detain.domain.LockKind
import com.kk.detain.ui.components.AppIcon
import com.kk.detain.ui.components.PrimaryButton
import com.kk.detain.ui.components.SoftCard
import com.kk.detain.ui.formatCountdown
import com.kk.detain.ui.formatMinutes
import com.kk.detain.ui.formatMinutesShort
import com.kk.detain.ui.formatTimeOfDay
import com.kk.detain.ui.theme.Ink
import com.kk.detain.ui.theme.InkFaint
import com.kk.detain.ui.theme.InkSoft
import com.kk.detain.ui.theme.Mint
import com.kk.detain.ui.theme.MintSoft
import com.kk.detain.ui.theme.Peach
import com.kk.detain.ui.theme.PeachSoft
import com.kk.detain.ui.theme.Periwinkle
import com.kk.detain.ui.theme.PeriwinkleDeep
import com.kk.detain.ui.theme.PeriwinkleSoft
import com.kk.detain.ui.theme.SandSoft
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun LockScreen(
    model: LockModel,
    quotaRemaining: Int,
    bubblesEnabled: Boolean,
    nudgeTick: Int,
    onStartSupervision: (Int) -> Unit,
    onSkipPrompt: () -> Unit,
    onForceUnlock: () -> Unit,
    onGoHome: () -> Unit,
    onLockExpired: () -> Unit,
) {
    when (model.mode) {
        LockMode.PROMPT -> PromptContent(
            model = model,
            onStartSupervision = onStartSupervision,
            onSkipPrompt = onSkipPrompt,
        )
        LockMode.LOCKED -> LockedContent(
            model = model,
            quotaRemaining = quotaRemaining,
            bubblesEnabled = bubblesEnabled,
            nudgeTick = nudgeTick,
            onForceUnlock = onForceUnlock,
            onGoHome = onGoHome,
            onLockExpired = onLockExpired,
        )
    }
}

@Composable
private fun PromptContent(
    model: LockModel,
    onStartSupervision: (Int) -> Unit,
    onSkipPrompt: () -> Unit,
) {
    val durations = remember { listOf(15, 30, 45, 60, 90, 120) }
    val initialIndex = remember(model.suggestedMinutes) {
        durations.indexOf(model.suggestedMinutes).takeIf { it >= 0 } ?: 1
    }
    var index by remember { mutableIntStateOf(initialIndex) }
    var starting by remember { mutableStateOf(false) }
    val minutes = durations[index]

    val context = LocalContext.current
    var todayMinutes by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(model.packageName) {
        todayMinutes = DetainApp.container(context).usage.todayMinutesFor(model.packageName)
    }
    val promptQuote = remember(model.packageName) { Encouragements.random() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopPill("静水 · 打开前提醒")
        Spacer(Modifier.weight(0.55f))
        AppIcon(model.packageName, size = 76.dp, background = Color.White)
        Spacer(Modifier.height(18.dp))
        Text(
            text = "打开「${model.label}」前，先停一下",
            style = MaterialTheme.typography.headlineSmall,
            color = Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "它还没有定时计划。给这次使用一个边界，到点静水会替你关上。",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        val usage = todayMinutes
        if (usage != null && usage > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "今天它已经拿走了你 ${formatMinutes(usage)}。",
                style = MaterialTheme.typography.bodySmall,
                color = Peach,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(26.dp))
        SoftCard(
            color = Color.White.copy(alpha = 0.88f),
            padding = PaddingValues(20.dp),
        ) {
            Text("这次专注", style = MaterialTheme.typography.bodySmall, color = InkSoft)
            Spacer(Modifier.height(4.dp))
            Text(formatMinutes(minutes), style = MaterialTheme.typography.displaySmall, color = Ink)
            Spacer(Modifier.height(10.dp))
            Slider(
                value = index.toFloat(),
                onValueChange = { index = it.roundToInt().coerceIn(0, durations.lastIndex) },
                valueRange = 0f..(durations.lastIndex).toFloat(),
                steps = durations.size - 2,
                colors = SliderDefaults.colors(
                    thumbColor = Periwinkle,
                    activeTrackColor = Periwinkle,
                    inactiveTrackColor = PeriwinkleSoft,
                    activeTickColor = Color.White.copy(alpha = 0.6f),
                    inactiveTickColor = Periwinkle.copy(alpha = 0.25f),
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("15 分钟", style = MaterialTheme.typography.labelSmall, color = InkFaint)
                Text("2 小时", style = MaterialTheme.typography.labelSmall, color = InkFaint)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "滑动选择这次锁定的长度，到时间会自动解开。",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = promptQuote,
            style = MaterialTheme.typography.bodySmall,
            color = InkFaint,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        PrimaryButton(
            text = if (starting) "正在开始…" else "开始守护 ${formatMinutes(minutes)}",
            onClick = {
                if (!starting) {
                    starting = true
                    onStartSupervision(minutes)
                }
            },
            enabled = !starting,
        )
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onSkipPrompt, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "先自由使用（30 分钟内不再提醒）",
                style = MaterialTheme.typography.labelMedium,
                color = InkSoft,
            )
        }
        Text(
            text = "你随时可以在守护名单里为它设置定时计划。",
            style = MaterialTheme.typography.labelSmall,
            color = InkFaint,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LockedContent(
    model: LockModel,
    quotaRemaining: Int,
    bubblesEnabled: Boolean,
    nudgeTick: Int,
    onForceUnlock: () -> Unit,
    onGoHome: () -> Unit,
    onLockExpired: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(500L)
        }
    }
    val remaining = (model.endAt - now).coerceAtLeast(0L)
    val total = (model.endAt - model.startAt).coerceAtLeast(1L)
    val lockedMinutes = ((now - model.startAt) / 60_000L).coerceAtLeast(0L).toInt()
    val quote = remember(model.packageName) { Encouragements.random() }
    var forceDialogVisible by remember { mutableStateOf(false) }
    var expiredHandled by remember { mutableStateOf(false) }

    LaunchedEffect(remaining) {
        if (remaining <= 0L && !expiredHandled) {
            expiredHandled = true
            delay(1_400L)
            onLockExpired()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF1F4FF), Color(0xFFFDF7F2), Color(0xFFEFFAF5)),
                ),
            ),
    ) {
        val bubbles = remember { mutableStateListOf<BubbleUi>() }
        var bubbleId by remember { mutableLongStateOf(0L) }
        val palette = listOf(PeriwinkleSoft, MintSoft, PeachSoft, SandSoft)

        fun addBubble() {
            // One calm thought at a time, placed in the breathing room above the actions.
            if (bubbles.isNotEmpty()) return
            val text = Encouragements.random(excluding = bubbles.lastOrNull()?.text)
            val maxX = (maxWidth - 264.dp).coerceAtLeast(0.dp)
            val x = 12.dp + maxX * Random.nextFloat()
            val y = maxHeight * (0.715f + Random.nextFloat() * 0.02f)
            bubbles += BubbleUi(
                id = bubbleId++,
                text = text,
                x = x,
                y = y,
                color = palette[Random.nextInt(palette.size)],
            )
        }

        LaunchedEffect(model.attemptCount, bubblesEnabled) {
            if (!bubblesEnabled) return@LaunchedEffect
            while (true) {
                delay(if (model.attemptCount >= 2) 8_000L else 15_000L)
                addBubble()
            }
        }
        LaunchedEffect(model.attemptCount) {
            if (bubblesEnabled && model.attemptCount >= 2) {
                repeat(2) {
                    addBubble()
                    delay(900L)
                }
            }
        }
        LaunchedEffect(nudgeTick) {
            if (bubblesEnabled && nudgeTick > 0) {
                bubbles.clear()
                addBubble()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopPill("静水 · 守护中")
            Spacer(Modifier.weight(0.35f))
            Box(contentAlignment = Alignment.Center) {
                val transition = rememberInfiniteTransition(label = "halo")
                val haloScale by transition.animateFloat(
                    initialValue = 0.94f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3_000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "haloScale",
                )
                Box(
                    Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            scaleX = haloScale
                            scaleY = haloScale
                            alpha = 0.65f
                        }
                        .background(Color.White, CircleShape),
                )
                AppIcon(model.packageName, size = 78.dp, background = Color.White)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = model.label,
                style = MaterialTheme.typography.headlineMedium,
                color = Ink,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    model.kind == LockKind.SCHEDULED -> "已为你暂停 · 这个时段留给别的事"
                    lockedMinutes > 0 -> "已为你暂停 · 你坚持了 ${formatMinutes(lockedMinutes)}"
                    else -> "已为你暂停"
                },
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
            Spacer(Modifier.height(20.dp))
            CountdownRing(remainingMs = remaining, totalMs = total)
            Spacer(Modifier.height(14.dp))
            Surface(
                color = Color.White.copy(alpha = 0.72f),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Spa,
                        contentDescription = null,
                        tint = Mint,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(quote, style = MaterialTheme.typography.bodySmall, color = InkSoft)
                }
            }
            if (model.attemptCount >= 2) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "第 ${model.attemptCount} 次想打开它，你都忍住了。",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkFaint,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.weight(1f))
            PrimaryButton(text = "回到桌面", onClick = onGoHome)
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = { forceDialogVisible = true },
                enabled = quotaRemaining > 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (quotaRemaining > 0) {
                        "强制解锁 · 本周还剩 $quotaRemaining 次"
                    } else {
                        "本周强制解锁机会已用完"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (quotaRemaining > 0) InkSoft else InkFaint,
                )
            }
            Text(
                text = "除了留给意外的那两次机会，只能等它自己解开。",
                style = MaterialTheme.typography.labelSmall,
                color = InkFaint,
                textAlign = TextAlign.Center,
            )
        }

        bubbles.forEach { bubble ->
            Box(
                modifier = Modifier
                    .offset(x = bubble.x, y = bubble.y),
            ) {
                EncouragementBubble(
                    text = bubble.text,
                    color = bubble.color,
                    onGone = { bubbles.removeAll { it.id == bubble.id } },
                )
            }
        }
    }

    if (forceDialogVisible) {
        ForceUnlockDialog(
            remaining = quotaRemaining,
            unlockAt = model.endAt,
            onDismiss = { forceDialogVisible = false },
            onConfirm = {
                forceDialogVisible = false
                onForceUnlock()
            },
        )
    }
}

@Composable
private fun TopPill(text: String) {
    Surface(color = Color.White.copy(alpha = 0.68f), shape = CircleShape) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelSmall,
            color = InkSoft,
        )
    }
}

@Composable
private fun CountdownRing(remainingMs: Long, totalMs: Long) {
    val progress = if (totalMs > 0L) {
        (remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "ringProgress",
    )
    Box(modifier = Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = Color.White.copy(alpha = 0.85f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (animatedProgress > 0.002f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Mint, Periwinkle, Color(0xFFAAB6FF), Mint),
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatCountdown(remainingMs),
                style = MaterialTheme.typography.displaySmall,
                color = Ink,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (remainingMs <= 0L) "已解锁" else "后自动解锁",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
        }
    }
}

@Composable
private fun EncouragementBubble(text: String, color: Color, onGone: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        delay(6_500L)
        visible = false
        delay(500L)
        onGone()
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(650)) + scaleIn(
            initialScale = 0.86f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        ),
        exit = fadeOut(tween(420)) + scaleOut(targetScale = 1.05f, animationSpec = tween(420)),
    ) {
        Surface(
            color = color.copy(alpha = 0.94f),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp),
            shadowElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier
                    .width(248.dp)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    Modifier
                        .padding(top = 5.dp)
                        .size(6.dp)
                        .background(PeriwinkleDeep.copy(alpha = 0.6f), CircleShape),
                )
                Spacer(Modifier.width(8.dp))
                Text(text, style = MaterialTheme.typography.bodySmall, color = Ink)
            }
        }
    }
}

@Composable
private fun ForceUnlockDialog(
    remaining: Int,
    unlockAt: Long,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var seconds by remember { mutableIntStateOf(5) }
    LaunchedEffect(Unit) {
        while (seconds > 0) {
            delay(1_000L)
            seconds--
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "先深呼吸 5 秒",
                style = MaterialTheme.typography.titleLarge,
                color = Ink,
            )
        },
        text = {
            Column {
                Text(
                    text = "这会用掉 1 次本周强制解锁机会（还剩 $remaining 次）。解锁后，应用会一直开放到 ${formatTimeOfDay(unlockAt)}。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "很想打开它的时候，往往也是最不需要它的时候。",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = seconds <= 0) {
                Text(
                    text = if (seconds <= 0) "确认解锁" else "${seconds} 秒后可确认",
                    color = if (seconds <= 0) Peach else InkFaint,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("再坚持一下", color = PeriwinkleDeep) }
        },
    )
}

private data class BubbleUi(
    val id: Long,
    val text: String,
    val x: androidx.compose.ui.unit.Dp,
    val y: androidx.compose.ui.unit.Dp,
    val color: Color,
)
