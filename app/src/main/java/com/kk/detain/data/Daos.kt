package com.kk.detain.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoredAppDao {
    @Query("SELECT * FROM monitored_apps ORDER BY addedAt ASC")
    fun observeAll(): Flow<List<MonitoredApp>>

    @Query("SELECT * FROM monitored_apps ORDER BY addedAt ASC")
    suspend fun getAll(): List<MonitoredApp>

    @Query("SELECT * FROM monitored_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun find(packageName: String): MonitoredApp?

    @Query("SELECT * FROM monitored_apps WHERE packageName = :packageName LIMIT 1")
    fun observe(packageName: String): Flow<MonitoredApp?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: MonitoredApp)

    @Query("DELETE FROM monitored_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("UPDATE monitored_apps SET enabled = :enabled WHERE packageName = :packageName")
    suspend fun setEnabled(packageName: String, enabled: Boolean)

    @Query("UPDATE monitored_apps SET scheduleEnabled = :enabled WHERE packageName = :packageName")
    suspend fun setScheduleEnabled(packageName: String, enabled: Boolean)

    @Query("UPDATE monitored_apps SET promptOnOpen = :enabled WHERE packageName = :packageName")
    suspend fun setPromptOnOpen(packageName: String, enabled: Boolean)

    @Query("UPDATE monitored_apps SET defaultLockMinutes = :minutes WHERE packageName = :packageName")
    suspend fun setDefaultLockMinutes(packageName: String, minutes: Int)

    @Query("UPDATE monitored_apps SET accentIndex = :index WHERE packageName = :packageName")
    suspend fun setAccentIndex(packageName: String, index: Int)
}

@Dao
interface LockWindowDao {
    @Query("SELECT * FROM lock_windows ORDER BY dayOfWeek ASC, startMinute ASC")
    fun observeAll(): Flow<List<LockWindow>>

    @Query("SELECT * FROM lock_windows WHERE packageName = :packageName ORDER BY dayOfWeek ASC, startMinute ASC")
    fun observeFor(packageName: String): Flow<List<LockWindow>>

    @Query("SELECT * FROM lock_windows WHERE packageName = :packageName ORDER BY dayOfWeek ASC, startMinute ASC")
    suspend fun getFor(packageName: String): List<LockWindow>

    @Query("SELECT * FROM lock_windows")
    suspend fun getAll(): List<LockWindow>

    @Insert
    suspend fun insert(window: LockWindow): Long

    @Update
    suspend fun update(window: LockWindow)

    @Delete
    suspend fun delete(window: LockWindow)

    @Query("DELETE FROM lock_windows WHERE packageName = :packageName")
    suspend fun deleteFor(packageName: String)
}

@Dao
interface LockSessionDao {
    @Query(
        "SELECT * FROM lock_sessions WHERE packageName = :packageName AND startAt <= :now AND endAt > :now " +
            "ORDER BY endAt DESC LIMIT 1",
    )
    suspend fun activeFor(packageName: String, now: Long): LockSession?

    @Query("SELECT * FROM lock_sessions WHERE startAt <= :now AND endAt > :now ORDER BY endAt ASC")
    suspend fun activeAll(now: Long): List<LockSession>

    @Query("SELECT * FROM lock_sessions ORDER BY createdAt DESC LIMIT 60")
    fun observeRecent(): Flow<List<LockSession>>

    @Query("SELECT * FROM lock_sessions WHERE packageName = :packageName ORDER BY createdAt DESC LIMIT 30")
    fun observeFor(packageName: String): Flow<List<LockSession>>

    @Query("SELECT * FROM lock_sessions WHERE packageName = :packageName ORDER BY endAt DESC LIMIT 1")
    suspend fun latestFor(packageName: String): LockSession?

    @Insert
    suspend fun insert(session: LockSession): Long

    @Query("DELETE FROM lock_sessions WHERE packageName = :packageName")
    suspend fun deleteFor(packageName: String)
}

@Dao
interface ExemptionDao {
    @Query("SELECT * FROM exemptions WHERE packageName = :packageName LIMIT 1")
    suspend fun get(packageName: String): Exemption?

    @Query("SELECT * FROM exemptions")
    fun observeAll(): Flow<List<Exemption>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(exemption: Exemption)

    @Query("DELETE FROM exemptions WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}

@Dao
interface LockAttemptDao {
    @Insert
    suspend fun insert(attempt: LockAttempt)

    @Query("SELECT COUNT(*) FROM lock_attempts WHERE packageName = :packageName AND timestamp > :since")
    suspend fun countSince(packageName: String, since: Long): Int

    @Query("SELECT COUNT(*) FROM lock_attempts WHERE timestamp > :since")
    fun observeCountSince(since: Long): Flow<Int>

    @Query("DELETE FROM lock_attempts WHERE packageName = :packageName")
    suspend fun deleteFor(packageName: String)
}
