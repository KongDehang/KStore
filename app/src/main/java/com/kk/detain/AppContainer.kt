package com.kk.detain

import android.content.Context
import com.kk.detain.data.DetainDatabase
import com.kk.detain.data.DetainRepository
import com.kk.detain.data.PreferencesStore
import com.kk.detain.service.LockCoordinator
import com.kk.detain.usage.UsageRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = DetainDatabase.get(appContext)

    val repository = DetainRepository(
        appDao = database.monitoredAppDao(),
        windowDao = database.lockWindowDao(),
        sessionDao = database.lockSessionDao(),
        exemptionDao = database.exemptionDao(),
        attemptDao = database.lockAttemptDao(),
    )

    val preferences = PreferencesStore(appContext)
    val usage = UsageRepository(appContext)
    val coordinator = LockCoordinator(appContext, repository)
}
