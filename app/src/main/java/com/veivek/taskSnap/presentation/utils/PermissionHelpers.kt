package com.veivek.taskSnap.presentation.utils

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri

/**
 * Helper for managing overlay (draw over other apps) permissions.
 * Required for showing floating windows on top of other apps.
 */
object OverlayPermissionHelper {

    /**
     * Check if the app has permission to draw overlays.
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Open system settings to allow user to grant overlay permission.
     */
    fun requestOverlayPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context.packageName}".toUri()
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

/**
 * Helper for managing battery optimization exemptions.
 * Helps ensure the service stays running in the background.
 */
object BatteryOptimizationHelper {

    /**
     * Check if the app is exempt from battery optimizations.
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager =
            context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Request battery optimization exemption.
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to general battery optimization settings
            val fallbackIntent =
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            context.startActivity(fallbackIntent)
        }
    }
}
