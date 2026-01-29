package com.veivek.allday.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.provider.ContactsContract
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.veivek.allday.MainActivity
import com.veivek.allday.R

/**
 * Foreground Service for reliable call state monitoring.
 * 
 * This is the recommended approach for Android 10+ because:
 * - BroadcastReceivers in the background are unreliable
 * - Foreground services have higher priority
 * - Works even when app is not in foreground
 */
class CallMonitorService : Service() {

    companion object {
        private const val TAG = "CallMonitorService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "call_monitor_channel"

        // Actions
        const val ACTION_START = "com.veivek.allday.action.START_MONITORING"
        const val ACTION_STOP = "com.veivek.allday.action.STOP_MONITORING"
        const val ACTION_CALL_ENDED = "com.veivek.allday.action.CALL_ENDED"

        // Extras
        const val EXTRA_PHONE_NUMBER = "phone_number"
        const val EXTRA_CONTACT_NAME = "contact_name"
        const val EXTRA_IS_INCOMING = "is_incoming"

        // Callback for in-app notification
        var onCallEnded: ((phoneNumber: String?, contactName: String?, isIncoming: Boolean) -> Unit)? = null

        fun start(context: Context) {
            val intent = Intent(context, CallMonitorService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CallMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyCallback: TelephonyCallback? = null

    // State tracking
    private var previousState = TelephonyManager.CALL_STATE_IDLE
    private var isIncoming = false
    private var lastPhoneNumber: String? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")

        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startMonitoring()
            }
            ACTION_STOP -> {
                stopMonitoring()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors calls to create follow-up tasks"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TaskSnap")
            .setContentText("Monitoring calls for task creation")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun startMonitoring() {
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ uses TelephonyCallback
            startModernMonitoring()
        } else {
            // Older versions use PhoneStateListener
            startLegacyMonitoring()
        }

        Log.d(TAG, "Call monitoring started")
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startModernMonitoring() {
        telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                handleCallStateChange(state)
            }
        }

        try {
            telephonyManager?.registerTelephonyCallback(
                mainExecutor,
                telephonyCallback!!
            )
            Log.d(TAG, "Modern TelephonyCallback registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register TelephonyCallback", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun startLegacyMonitoring() {
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                if (phoneNumber != null) {
                    lastPhoneNumber = phoneNumber
                }
                handleCallStateChange(state)
            }
        }

        try {
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
            Log.d(TAG, "Legacy PhoneStateListener registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register PhoneStateListener", e)
        }
    }

    private fun handleCallStateChange(state: Int) {
        Log.d(TAG, "Call state changed: $state (previous: $previousState)")

        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                isIncoming = true
                Log.d(TAG, "Phone ringing - incoming call")
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (previousState == TelephonyManager.CALL_STATE_IDLE) {
                    isIncoming = false
                    Log.d(TAG, "Call started - outgoing call")
                } else {
                    Log.d(TAG, "Call answered")
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (previousState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    Log.d(TAG, "Call ended! Triggering task creation")
                    handleCallEnded()
                }
            }
        }

        previousState = state
    }

    private fun handleCallEnded() {
        // Try to get the last call info from call log
        val callInfo = getLastCallInfo()
        val phoneNumber = callInfo?.first ?: lastPhoneNumber
        val contactName = phoneNumber?.let { getContactName(it) }

        Log.d(TAG, "Call ended - Number: $phoneNumber, Contact: $contactName, Incoming: $isIncoming")

        // Notify via callback (for when app is visible)
        onCallEnded?.invoke(phoneNumber, contactName, isIncoming)

        // Also send a notification for when app is in background
        showCallEndedNotification(phoneNumber, contactName, isIncoming)

        // Reset
        lastPhoneNumber = null
    }

    private fun getLastCallInfo(): Pair<String?, Long>? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "No READ_CALL_LOG permission")
            return null
        }

        return try {
            val cursor = contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                arrayOf(
                    android.provider.CallLog.Calls.NUMBER,
                    android.provider.CallLog.Calls.DATE
                ),
                null, null,
                "${android.provider.CallLog.Calls.DATE} DESC"
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val number = it.getString(0)
                    val date = it.getLong(1)
                    Pair(number, date)
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading call log", e)
            null
        }
    }

    private fun getContactName(phoneNumber: String): String? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        return try {
            val uri = android.net.Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(phoneNumber)
            )
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting contact name", e)
            null
        }
    }

    private fun showCallEndedNotification(phoneNumber: String?, contactName: String?, isIncoming: Boolean) {
        val displayName = contactName ?: phoneNumber ?: "Unknown"
        val callType = if (isIncoming) "incoming" else "outgoing"

        // Create intent to open app with call info
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_CALL_ENDED
            putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
            putExtra(EXTRA_CONTACT_NAME, contactName)
            putExtra(EXTRA_IS_INCOMING, isIncoming)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("📞 Create task after call?")
            .setContentText("$callType call with $displayName ended")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun stopMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let {
                telephonyManager?.unregisterTelephonyCallback(it)
            }
        } else {
            @Suppress("DEPRECATION")
            phoneStateListener?.let {
                telephonyManager?.listen(it, PhoneStateListener.LISTEN_NONE)
            }
        }
        Log.d(TAG, "Call monitoring stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        Log.d(TAG, "Service destroyed")
    }
}
