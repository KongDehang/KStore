package com.kk.detain.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Brings the guard notification back after a reboot or an app update; the accessibility
 * service itself is rebound by the system as long as it stays enabled in settings.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            GuardService.start(context)
        }
    }
}
