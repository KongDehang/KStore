package com.kk.detain.domain

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

data class InstalledApp(
    val packageName: String,
    val label: String,
)

object InstalledAppsLoader {

    suspend fun load(context: Context): List<InstalledApp> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = runCatching { packageManager.queryIntentActivities(intent, 0) }.getOrNull().orEmpty()
        resolved.asSequence()
            .mapNotNull { it.activityInfo?.packageName }
            .filter { it != context.packageName }
            .distinct()
            .mapNotNull { packageName ->
                runCatching {
                    val label = packageManager
                        .getApplicationLabel(packageManager.getApplicationInfo(packageName, 0))
                        .toString()
                    InstalledApp(packageName = packageName, label = label)
                }.getOrNull()
            }
            .sortedWith(compareBy(Collator.getInstance(Locale.SIMPLIFIED_CHINESE)) { it.label })
            .toList()
    }
    
    suspend fun labelOf(context: Context, packageName: String): String = withContext(Dispatchers.IO) {
        runCatching {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(packageName)
    }
}
