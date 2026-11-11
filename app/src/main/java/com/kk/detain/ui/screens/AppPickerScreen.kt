package com.kk.detain.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kk.detain.ui.DetainViewModel
import com.kk.detain.ui.components.AppIcon
import com.kk.detain.ui.components.BackHeader
import com.kk.detain.ui.components.Pill
import com.kk.detain.ui.components.SoftCard
import com.kk.detain.ui.theme.Ink
import com.kk.detain.ui.theme.InkFaint
import com.kk.detain.ui.theme.InkSoft
import com.kk.detain.ui.theme.Mint
import com.kk.detain.ui.theme.MintSoft
import com.kk.detain.ui.theme.Periwinkle
import com.kk.detain.ui.theme.PeriwinkleDeep

@Composable
fun AppPickerScreen(
    vm: DetainViewModel,
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    val installed by vm.installedApps.collectAsStateWithLifecycle()
    val loading by vm.installedAppsLoading.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    val monitoredPackages = remember(apps) { apps.map { it.packageName }.toSet() }
    val filtered = remember(installed, query) {
        if (query.isBlank()) {
            installed
        } else {
            installed.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(Unit) { vm.loadInstalledApps() }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        BackHeader(title = "添加守护应用", onBack = onBack)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "选择你希望能少用一会的应用",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
        )
        Spacer(Modifier.height(14.dp))
        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("搜索应用", style = MaterialTheme.typography.bodyMedium, color = InkFaint)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = InkFaint,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "清空",
                            tint = InkFaint,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Periwinkle,
                focusedTextColor = Ink,
                unfocusedTextColor = Ink,
            ),
        )
        Spacer(Modifier.height(14.dp))
        when {
            loading -> {
                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Periwinkle, strokeWidth = 2.5.dp)
                }
            }
            filtered.isEmpty() -> {
                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (installed.isEmpty()) "没有找到可以添加的应用" else "没有匹配的应用",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtered, key = { it.packageName }) { item ->
                        val monitored = item.packageName in monitoredPackages
                        SoftCard(
                            onClick = {
                                if (!monitored) vm.addApp(item.packageName, item.label)
                                onOpenDetail(item.packageName)
                            },
                            padding = PaddingValues(12.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppIcon(item.packageName, size = 44.dp, background = Color(0xFFF7F8FD))
                                Spacer(Modifier.width(13.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Ink,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    )
                                }
                                if (monitored) {
                                    Pill("已添加", MintSoft, Mint)
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Mint,
                                        modifier = Modifier.size(18.dp),
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "添加",
                                        tint = PeriwinkleDeep,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
