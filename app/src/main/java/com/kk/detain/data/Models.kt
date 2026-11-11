package com.kk.detain.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An app the user chose to keep an eye on.
 */
@Entity(tableName = "monitored_apps")
data class MonitoredApp(
    @PrimaryKey val packageName: String,
    val label: String,
    val enabled: Boolean = true,
    val scheduleEnabled: Boolean = false,
    val promptOnOpen: Boolean = true,
    val defaultLockMinutes: Int = 30,
    val addedAt: Long = System.currentTimeMillis(),
    val accentIndex: Int = 0,
)

/**
 * A recurring lock window. [dayOfWeek] follows java.time: Monday = 1 ... Sunday = 7.
 * [startMinute] / [endMinute] are minutes from midnight. When end <= start the window
 * crosses midnight (end == start means the whole day).
 */
@Entity(tableName = "lock_windows", indices = [Index("packageName")])
data class LockWindow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
)

/**
 * A one-off supervised lock that unlocks itself when [endAt] is reached.
 */
@Entity(tableName = "lock_sessions", indices = [Index("packageName")])
data class LockSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val source: String,
    val startAt: Long,
    val endAt: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * A temporary "free pass" for an app. Created when the user spends a weekly forced-unlock
 * credit or chooses to use an app freely for a while.
 */
@Entity(tableName = "exemptions")
data class Exemption(
    @PrimaryKey val packageName: String,
    val untilAt: Long,
)

/**
 * Every time the lock screen had to step in. Used for the "you tried N times" nudge.
 */
@Entity(tableName = "lock_attempts", indices = [Index("timestamp")])
data class LockAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis(),
)

object LockSource {
    const val PROMPT = "prompt"
    const val MANUAL = "manual"
}
