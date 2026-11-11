package com.kk.detain.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kk.detain.ui.screens.AppDetailScreen
import com.kk.detain.ui.screens.AppPickerScreen
import com.kk.detain.ui.screens.HomeScreen
import com.kk.detain.ui.screens.SettingsScreen
import com.kk.detain.ui.screens.StatsScreen
import com.kk.detain.ui.theme.Ink
import com.kk.detain.ui.theme.InkSoft
import com.kk.detain.ui.theme.PeriwinkleDeep
import com.kk.detain.ui.theme.PeriwinkleSoft

enum class RootTab(val label: String, val icon: ImageVector) {
    Home("守护", Icons.Rounded.Home),
    Stats("统计", Icons.Rounded.BarChart),
    Settings("设置", Icons.Rounded.Settings),
}

@Composable
fun AppRoot(vm: DetainViewModel) {
    val context = LocalContext.current
    val permissions by vm.permissions.collectAsStateWithLifecycle()
    var tabName by rememberSaveable { mutableStateOf(RootTab.Home.name) }
    var detailPackage by rememberSaveable { mutableStateOf<String?>(null) }
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    var showAccessibilityGate by rememberSaveable { mutableStateOf(false) }
    var pendingAdd by rememberSaveable { mutableStateOf(false) }
    val tab = RootTab.valueOf(tabName)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        vm.refreshPermissions()
        vm.refreshUsage()
    }

    LaunchedEffect(tab) {
        if (tab == RootTab.Stats) vm.refreshUsage()
    }

    LaunchedEffect(permissions.accessibility, pendingAdd) {
        if (pendingAdd && permissions.accessibility) {
            pendingAdd = false
            vm.loadInstalledApps()
            pickerOpen = true
        }
    }

    val showBottomBar = detailPackage == null && !pickerOpen

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                DetainBottomBar(selected = tab, onSelect = { tabName = it.name })
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                detailPackage != null -> AppDetailScreen(
                    vm = vm,
                    packageName = detailPackage.orEmpty(),
                    onBack = { detailPackage = null },
                )

                pickerOpen -> AppPickerScreen(
                    vm = vm,
                    onBack = { pickerOpen = false },
                    onOpenDetail = { packageName ->
                        pickerOpen = false
                        detailPackage = packageName
                    },
                )

                tab == RootTab.Home -> HomeScreen(
                    vm = vm,
                    onAddApp = {
                        if (permissions.accessibility) {
                            vm.loadInstalledApps()
                            pickerOpen = true
                        } else {
                            showAccessibilityGate = true
                        }
                    },
                    onOpenDetail = { detailPackage = it },
                    onOpenStats = { tabName = RootTab.Stats.name },
                    onOpenSettings = { tabName = RootTab.Settings.name },
                )

                tab == RootTab.Stats -> StatsScreen(
                    vm = vm,
                    onOpenSettings = { tabName = RootTab.Settings.name },
                )

                else -> SettingsScreen(vm = vm)
            }
        }
    }

    BackHandler(enabled = detailPackage != null || pickerOpen) {
        if (detailPackage != null) {
            detailPackage = null
        } else {
            pickerOpen = false
        }
    }

    if (showAccessibilityGate) {
        AlertDialog(
            onDismissRequest = { showAccessibilityGate = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "先开启无障碍服务",
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                )
            },
            text = {
                Text(
                    text = "没有它，静水无法知道你把哪个应用打开了，锁定页也就不会出现。开启后回到静水，会继续带你添加应用。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAccessibilityGate = false
                        pendingAdd = true
                        openAccessibilitySettings(context)
                    },
                ) {
                    Text("去开启", color = PeriwinkleDeep)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAccessibilityGate = false
                        vm.loadInstalledApps()
                        pickerOpen = true
                    },
                ) {
                    Text("仍要添加", color = InkSoft)
                }
            },
        )
    }
}

@Composable
private fun DetainBottomBar(selected: RootTab, onSelect: (RootTab) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                RootTab.values().forEach { item ->
                    val active = item == selected
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (active) PeriwinkleSoft else Color.Transparent)
                            .clickable { onSelect(item) }
                            .padding(vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (active) PeriwinkleDeep else InkSoft,
                            modifier = Modifier.size(21.dp),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) PeriwinkleDeep else InkSoft,
                        )
                    }
                }
            }
        }
    }
}
