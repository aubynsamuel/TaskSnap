package com.veivek.taskSnap.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.veivek.taskSnap.service.CallMonitorService

/**
 * BroadcastReceiver to detect phone call state changes and ensure service is running.
 *
 * CRITICAL NOTES:
 * - On Android 9+ (API 28+), PHONE_STATE broadcasts are severely restricted from manifest
 * - This receiver mainly serves as a backup/trigger to start the service
 * - The actual call monitoring happens in CallMonitorService using TelephonyCallback/PhoneStateListener
 *
 * How it works:
 * 1. When a call is detected (any state change), this receiver fires
 * 2. It immediately starts CallMonitorService if not already running
 * 3. CallMonitorService then handles all call state tracking
 */
class CallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallReceiver"
        private const val PREFS_NAME = "TaskSnapPrefs"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            Log.d(TAG, "Received non-phone-state intent: ${intent.action}")
            return
        }

        // Check if setup was completed (permissions granted)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val setupComplete = prefs.getBoolean(KEY_SETUP_COMPLETE, false)

        if (!setupComplete) {
            Log.w(TAG, "Setup not complete, ignoring call event")
            return
        }

        val stateString = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        Log.d(
            TAG,
            "📞 Phone state broadcast received: state=$stateString, number=${phoneNumber?.take(3)}***"
        )

        // Start the monitoring service if it's not already running
        // The service will handle the actual call state tracking
        try {
            CallMonitorService.start(context)
            Log.d(TAG, "✅ CallMonitorService start triggered from broadcast")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start CallMonitorService from broadcast", e)
        }

        // Note: We don't track call states here anymore - the service does that
        // This receiver's only job is to ensure the service is running when a call happens
    }
}