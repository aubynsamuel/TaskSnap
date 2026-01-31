package com.veivek.allday.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.veivek.allday.ui.overlay.CallEndedOverlay
import com.veivek.allday.utils.OverlayPermissionHelper

/**
 * Foreground Service for reliable call state monitoring.
 *
 * IMPORTANT CHANGES FOR ANDROID 9+:
 * - Registers PHONE_STATE broadcast receiver dynamically (not just manifest)
 * - Uses both TelephonyCallback (Android 12+) and PhoneStateListener (older)
 * - Keeps service running to ensure calls are always detected
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
        var onCallEnded: ((phoneNumber: String?, contactName: String?, isIncoming: Boolean) -> Unit)? =
            null

        fun start(context: Context) {
            val intent = Intent(context, CallMonitorService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "✅ Service start requested")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to start service", e)
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
    private var phoneStateReceiver: BroadcastReceiver? = null

    // State tracking
    private var previousState = TelephonyManager.CALL_STATE_IDLE
    private var isIncoming = false
    private var lastPhoneNumber: String? = null
    private var isMonitoring = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "📱 Service onCreate()")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "📱 onStartCommand: action=${intent?.action}, flags=$flags, startId=$startId")

        when (intent?.action) {
            ACTION_START -> {
                if (!isMonitoring) {
                    Log.d(TAG, "🚀 Starting foreground service and call monitoring...")
                    startForeground(NOTIFICATION_ID, createNotification())
                    startMonitoring()
                } else {
                    Log.d(TAG, "ℹ️ Service already monitoring, ignoring duplicate start")
                }
            }

            ACTION_STOP -> {
                Log.d(TAG, "🛑 Stopping service...")
                stopMonitoring()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            null -> {
                // Service restarted by system after being killed
                Log.d(TAG, "🔄 Service restarted by system, starting monitoring...")
                startForeground(NOTIFICATION_ID, createNotification())
                startMonitoring()
            }
        }

        // START_STICKY ensures service restarts if killed by system
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
            Log.d(TAG, "✅ Notification channel created")
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
            .setContentTitle("TaskSnap Active")
            .setContentText("Monitoring calls for task creation")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun startMonitoring() {
        if (isMonitoring) {
            Log.w(TAG, "⚠️ Already monitoring, skipping duplicate registration")
            return
        }

        Log.d(TAG, "🎯 Starting call monitoring...")

        // Check permissions first
        if (!hasRequiredPermissions()) {
            Log.e(TAG, "❌ Missing required permissions for call monitoring")
            return
        }

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        // Strategy 1: Register dynamic broadcast receiver (works on Android 9+)
        registerDynamicPhoneStateReceiver()

        // Strategy 2: Use TelephonyCallback (Android 12+) or PhoneStateListener (older)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startModernMonitoring()
        } else {
            startLegacyMonitoring()
        }

        isMonitoring = true
        Log.d(TAG, "✅ Call monitoring started successfully")
    }

    /**
     * Register PHONE_STATE broadcast receiver dynamically.
     * This is CRITICAL for Android 9+ where manifest-declared receivers don't receive these broadcasts.
     */
    private fun registerDynamicPhoneStateReceiver() {
        try {
            phoneStateReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                        Log.d(TAG, "📞 Dynamic receiver: state=$state, number=${number?.take(3)}***")

                        // Store incoming number
                        if (number != null) {
                            lastPhoneNumber = number
                        }
                    }
                }
            }

            val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(phoneStateReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(phoneStateReceiver, filter)
            }

            Log.d(TAG, "✅ Dynamic PHONE_STATE receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to register dynamic receiver", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startModernMonitoring() {
        telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                Log.d(TAG, "📱 TelephonyCallback: state=$state (${getStateName(state)})")
                handleCallStateChange(state)
            }
        }

        try {
            telephonyManager?.registerTelephonyCallback(
                mainExecutor,
                telephonyCallback!!
            )
            Log.d(TAG, "✅ Modern TelephonyCallback registered (Android 12+)")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SecurityException registering TelephonyCallback - check permissions", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to register TelephonyCallback", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun startLegacyMonitoring() {
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                Log.d(
                    TAG,
                    "📱 PhoneStateListener: state=$state (${getStateName(state)}), number=${
                        phoneNumber?.take(3)
                    }***"
                )

                if (phoneNumber != null && phoneNumber.isNotEmpty()) {
                    lastPhoneNumber = phoneNumber
                    Log.d(TAG, "📝 Stored phone number: ${phoneNumber.take(3)}***")
                }

                handleCallStateChange(state)
            }
        }

        try {
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
            Log.d(TAG, "✅ Legacy PhoneStateListener registered (Android < 12)")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SecurityException registering PhoneStateListener - check permissions", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to register PhoneStateListener", e)
        }
    }

    private fun handleCallStateChange(state: Int) {
        val stateName = getStateName(state)
        val prevStateName = getStateName(previousState)

        Log.d(TAG, "🔄 State transition: $prevStateName → $stateName")

        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                isIncoming = true
                Log.d(TAG, "📞 RINGING → Incoming call detected")
            }

            TelephonyManager.CALL_STATE_OFFHOOK -> {
                when (previousState) {
                    TelephonyManager.CALL_STATE_IDLE -> {
                        isIncoming = false
                        Log.d(TAG, "📞 IDLE→OFFHOOK → Outgoing call started")
                    }

                    TelephonyManager.CALL_STATE_RINGING -> {
                        Log.d(TAG, "📞 RINGING→OFFHOOK → Incoming call answered")
                    }

                    else -> {
                        Log.d(TAG, "📞 OFFHOOK → Call in progress")
                    }
                }
            }

            TelephonyManager.CALL_STATE_IDLE -> {
                when (previousState) {
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        Log.d(TAG, "📞 OFFHOOK→IDLE → CALL ENDED! Triggering task creation")
                        handleCallEnded()
                    }

                    TelephonyManager.CALL_STATE_RINGING -> {
                        Log.d(TAG, "📞 RINGING→IDLE → Missed call (not answered)")
                    }

                    else -> {
                        Log.d(TAG, "📞 IDLE → No active call")
                    }
                }
            }
        }

        previousState = state
    }

    private fun handleCallEnded() {
        Log.d(TAG, "🎯 Processing call end event...")

        // Try to get the last call info from call log
        val callInfo = getLastCallInfo()
        val phoneNumber = callInfo?.first ?: lastPhoneNumber
        val contactName = phoneNumber?.let { getContactName(it) }

        Log.d(
            TAG, """
            📊 Call Details:
            - Number: ${phoneNumber?.take(3)}***
            - Contact: $contactName
            - Direction: ${if (isIncoming) "incoming" else "outgoing"}
        """.trimIndent()
        )

        // Notify via callback (for when app is visible)
        onCallEnded?.invoke(phoneNumber, contactName, isIncoming)

        // Show overlay window (Truecaller-style popup)
        if (OverlayPermissionHelper.hasOverlayPermission(this)) {
            try {
                val overlay = CallEndedOverlay(applicationContext)
                overlay.show(phoneNumber, contactName, isIncoming)
                Log.d(TAG, "✅ Overlay window shown")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to show overlay, falling back to notification", e)
                showCallEndedNotification(phoneNumber, contactName, isIncoming)
            }
        } else {
            Log.w(TAG, "⚠️ No overlay permission, showing notification instead")
            showCallEndedNotification(phoneNumber, contactName, isIncoming)
        }

        // Reset
        lastPhoneNumber = null
    }

    private fun hasRequiredPermissions(): Boolean {
        val hasPhoneState = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val hasCallLog = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "Permission check: PHONE_STATE=$hasPhoneState, CALL_LOG=$hasCallLog")

        return hasPhoneState
    }

    private fun getLastCallInfo(): Pair<String?, Long>? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "⚠️ No READ_CALL_LOG permission")
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
                    Log.d(TAG, "📋 Last call from log: ${number?.take(3)}***")
                    Pair(number, date)
                } else {
                    Log.w(TAG, "⚠️ No calls in call log")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reading call log", e)
            null
        }
    }

    private fun getContactName(phoneNumber: String): String? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "⚠️ No READ_CONTACTS permission")
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
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0)
                    Log.d(TAG, "👤 Contact found: $name")
                    name
                } else {
                    Log.d(TAG, "👤 No contact found for ${phoneNumber.take(3)}***")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting contact name", e)
            null
        }
    }

    private fun showCallEndedNotification(
        phoneNumber: String?,
        contactName: String?,
        isIncoming: Boolean,
    ) {
        val displayName = contactName ?: phoneNumber ?: "Unknown"
        val callType = if (isIncoming) "incoming" else "outgoing"

        Log.d(TAG, "🔔 Showing notification for $callType call with $displayName")

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
        Log.d(TAG, "🛑 Stopping call monitoring...")

        // Unregister dynamic receiver
        try {
            phoneStateReceiver?.let {
                unregisterReceiver(it)
                phoneStateReceiver = null
                Log.d(TAG, "✅ Dynamic receiver unregistered")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error unregistering dynamic receiver", e)
        }

        // Unregister telephony listeners
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let {
                telephonyManager?.unregisterTelephonyCallback(it)
                Log.d(TAG, "✅ TelephonyCallback unregistered")
            }
        } else {
            @Suppress("DEPRECATION")
            phoneStateListener?.let {
                telephonyManager?.listen(it, PhoneStateListener.LISTEN_NONE)
                Log.d(TAG, "✅ PhoneStateListener unregistered")
            }
        }

        isMonitoring = false
        Log.d(TAG, "✅ Call monitoring stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "💀 Service onDestroy()")
        stopMonitoring()
    }

    private fun getStateName(state: Int): String = when (state) {
        TelephonyManager.CALL_STATE_IDLE -> "IDLE"
        TelephonyManager.CALL_STATE_RINGING -> "RINGING"
        TelephonyManager.CALL_STATE_OFFHOOK -> "OFFHOOK"
        else -> "UNKNOWN($state)"
    }
}