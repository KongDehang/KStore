package com.kk.detain.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MonitoredApp::class,
        LockWindow::class,
        LockSession::class,
        Exemption::class,
        LockAttempt::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class DetainDatabase : RoomDatabase() {
    abstract fun monitoredAppDao(): MonitoredAppDao
    abstract fun lockWindowDao(): LockWindowDao
    abstract fun lockSessionDao(): LockSessionDao
    abstract fun exemptionDao(): ExemptionDao
    abstract fun lockAttemptDao(): LockAttemptDao

    companion object {
        @Volatile
        private var instance: DetainDatabase? = null

        fun get(context: Context): DetainDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DetainDatabase::class.java,
                    "detain.db",
                ).build().also { instance = it }
            }
    }
}
