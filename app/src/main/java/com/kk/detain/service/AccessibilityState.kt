package com.kk.detain.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

/**
 * Whether the user has switched our accessibility service on in system settings.
 * A service can be enabled here and still be disconnected, so callers that care about
 * the live connection should also look at [GuardDiagnostics.serviceConnected].
 */
fun isAccessibilityEnabled(context: Context): Boolean {
    val expected = ComponentName(context, DetainAccessibilityService::class.java).flattenToString()
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ).orEmpty()
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}
