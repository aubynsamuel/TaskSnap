package com.veivek.allday.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.veivek.allday.service.CallMonitorService

/**
 * Boot Receiver - Automatically starts call monitoring service when device boots.
 *
 * This enables Truecaller-style behavior where the app doesn't need to be manually
 * opened to start working. Once permissions are granted on first launch, the service
 * will automatically start after every reboot.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed - starting CallMonitorService")

            // Start the call monitoring service automatically
            try {
                CallMonitorService.start(context)
                Log.d(TAG, "CallMonitorService started successfully after boot")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start CallMonitorService after boot", e)
            }
        }
    }
}
