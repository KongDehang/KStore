package com.kk.detain.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.kk.detain.data.MonitoredApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

data class DayUsage(val date: LocalDate, val minutes: Int)

data class AppUsage(
    val packageName: String,
    val label: String,
    val todayMinutes: Int,
    val weekMinutes: Int,
)

data class UsageReport(
    val days: List<DayUsage> = emptyList(),
    val apps: List<AppUsage> = emptyList(),
    val todayTotal: Int = 0,
    val yesterdayTotal: Int = 0,
) {
    val weekTotal: Int get() = days.sumOf { it.minutes }
    val todayChange: Int get() = todayTotal - yesterdayTotal
    fun todayMinutesOf(packageName: String): Int =
        apps.firstOrNull { it.packageName == packageName }?.todayMinutes ?: 0
}

class UsageRepository(private val context: Context) {

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            } else {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            }
        } catch (_: Exception) {
            return false
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    suspend fun todayMinutesFor(packageName: String): Int = withContext(Dispatchers.IO) {
        if (!hasUsageAccess()) return@withContext 0
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return@withContext 0
        val zone = ZoneId.systemDefault()
        val date = LocalDate.now(zone)
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val stats = runCatching {
            usageStatsManager.queryAndAggregateUsageStats(start, end)
        }.getOrNull()
        val foreground = stats?.get(packageName)?.totalTimeInForeground ?: 0L
        (foreground / 60_000L).toInt().coerceAtLeast(0)
    }

    suspend fun report(apps: List<MonitoredApp>, days: Int = 7): UsageReport =
        withContext(Dispatchers.IO) {
            if (apps.isEmpty() || !hasUsageAccess()) return@withContext UsageReport()

            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                    ?: return@withContext UsageReport()
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val packages = apps.map { it.packageName }.toSet()
            val dayUsages = mutableListOf<DayUsage>()
            val weekMinutes = mutableMapOf<String, Int>()
            var todayPerApp: Map<String, Int> = emptyMap()
            var todayTotal = 0
            var yesterdayTotal = 0

            for (offset in (days - 1) downTo 0) {
                val date = today.minusDays(offset.toLong())
                val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val stats = runCatching {
                    usageStatsManager.queryAndAggregateUsageStats(start, end)
                }.getOrNull().orEmpty()

                val perApp = mutableMapOf<String, Int>()
                stats.forEach { (packageName, usage) ->
                    if (packageName !in packages) return@forEach
                    val minutes = (usage.totalTimeInForeground / 60_000L).toInt().coerceAtLeast(0)
                    if (minutes > 0) perApp[packageName] = minutes
                }
                val dayTotal = perApp.values.sum()
                dayUsages += DayUsage(date = date, minutes = dayTotal)
                if (offset == 0) {
                    todayPerApp = perApp
                    todayTotal = dayTotal
                }
                if (offset == 1) yesterdayTotal = dayTotal
                perApp.forEach { (packageName, minutes) ->
                    weekMinutes[packageName] = (weekMinutes[packageName] ?: 0) + minutes
                }
            }

            val appUsages = apps.map { app ->
                AppUsage(
                    packageName = app.packageName,
                    label = app.label,
                    todayMinutes = todayPerApp[app.packageName] ?: 0,
                    weekMinutes = weekMinutes[app.packageName] ?: 0,
                )
            }.sortedByDescending { it.weekMinutes }

            UsageReport(
                days = dayUsages,
                apps = appUsages,
                todayTotal = todayTotal,
                yesterdayTotal = yesterdayTotal,
            )
        }
}
