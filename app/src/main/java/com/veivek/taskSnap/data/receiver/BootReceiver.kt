package com.veivek.taskSnap.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.veivek.taskSnap.data.service.CallMonitorService

/**
 * Boot Receiver - Automatically starts call monitoring service when device boots.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed - starting CallMonitorService")

            try {
                CallMonitorService.start(context)
                Log.d(TAG, "CallMonitorService started successfully after boot")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start CallMonitorService after boot", e)
            }
        }
    }
}
