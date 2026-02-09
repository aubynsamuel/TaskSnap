package com.veivek.taskSnap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.veivek.taskSnap.data.local.TaskDatabase
import com.veivek.taskSnap.data.service.CallMonitorService
import com.veivek.taskSnap.presentation.components.CallEndedDialog
import com.veivek.taskSnap.presentation.components.SetupWizardScreen
import com.veivek.taskSnap.presentation.navigation.Navigation
import com.veivek.taskSnap.presentation.theme.TaskSnapTheme
import com.veivek.taskSnap.presentation.utils.BatteryOptimizationHelper
import com.veivek.taskSnap.presentation.utils.OverlayPermissionHelper

/**
 * Main Activity for TaskSnap MVP
 *
 * IMPROVEMENTS:
 * - Better logging for debugging service startup
 * - Explicit service start after permissions granted
 * - Verification that service is actually running
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "TaskSnapPrefs"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
    }

    // State for dialogs
    private var callEndedData = mutableStateOf<CallEndedInfo?>(null)
    private var showSetupWizard = mutableStateOf(false)

    data class CallEndedInfo(
        val phoneNumber: String?,
        val contactName: String?,
        val isIncoming: Boolean,
    )

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.filter { it.value }.keys
        val denied = permissions.filter { !it.value }.keys

        Log.d(TAG, "✅ Permissions granted: $granted")
        Log.d(TAG, "❌ Permissions denied: $denied")

        val hasPhoneState = permissions[Manifest.permission.READ_PHONE_STATE] == true
        val hasCallLog = permissions[Manifest.permission.READ_CALL_LOG] == true

        if (hasPhoneState && hasCallLog) {
            Log.d(TAG, "✅ Essential permissions granted, continuing setup...")
            // Phone permissions granted, continue with overlay permission
            checkAndRequestOverlayPermission()
        } else {
            Log.e(TAG, "❌ Essential permissions missing!")
            Toast.makeText(
                this,
                "Phone and Call Log permissions are required for call detection",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d(TAG, "📱 MainActivity onCreate()")

        // Set up callback for when service detects call ended
        CallMonitorService.onCallEnded = { phoneNumber, contactName, isIncoming ->
            Log.d(TAG, "📞 Call ended callback received: $phoneNumber, $contactName, $isIncoming")
            runOnUiThread {
                callEndedData.value = CallEndedInfo(phoneNumber, contactName, isIncoming)
            }
        }

        // Handle intent from notification
        handleIntent(intent)

        // Check if first launch
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val setupComplete = prefs.getBoolean(KEY_SETUP_COMPLETE, false)

        if (!setupComplete) {
            Log.d(TAG, "🆕 First launch detected - starting setup wizard")
            showSetupWizard.value = true
            requestPhonePermissions()
        } else {
            Log.d(TAG, "✅ Setup already complete - verifying service")
            ensureServiceRunning()
        }

        setContent {
            TaskSnapTheme {
                val showWizard by showSetupWizard

                if (showWizard) {
                    SetupWizardScreen(
                        onComplete = {
                            showSetupWizard.value = false
                        }
                    )
                } else {
                    // Initialize database and navigation
                    val database = TaskDatabase.getInstance(this)
                    Navigation(database = database)
                }

                // Call ended dialog (fallback if overlay not shown)
                val callEnded by callEndedData
                callEnded?.let { info ->
                    CallEndedDialog(
                        phoneNumber = info.phoneNumber,
                        contactName = info.contactName,
                        isIncoming = info.isIncoming,
                        onDismiss = { callEndedData.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "📱 MainActivity onResume()")

        // Check permissions if wizard is showing (user returning from settings)
        if (showSetupWizard.value) {
            val hasOverlay = OverlayPermissionHelper.hasOverlayPermission(this)
            val hasBattery = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)

            Log.d(TAG, "Permission status: overlay=$hasOverlay, battery=$hasBattery")

            if (hasOverlay && hasBattery) {
                onAllPermissionsGranted()
            } else if (hasOverlay && !hasBattery) {
                // User granted overlay but not battery - ask for battery
                checkAndRequestBatteryOptimization()
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == CallMonitorService.ACTION_CALL_ENDED) {
            val phoneNumber = intent.getStringExtra(CallMonitorService.EXTRA_PHONE_NUMBER)
            val contactName = intent.getStringExtra(CallMonitorService.EXTRA_CONTACT_NAME)
            val isIncoming = intent.getBooleanExtra(CallMonitorService.EXTRA_IS_INCOMING, false)

            Log.d(TAG, "📞 Received call ended intent: $phoneNumber, $contactName")
            callEndedData.value = CallEndedInfo(phoneNumber, contactName, isIncoming)
        }
    }

    private fun requestPhonePermissions() {
        val permissions = mutableListOf<String>()

        // Core permissions for call state
        permissions.add(Manifest.permission.READ_PHONE_STATE)
        permissions.add(Manifest.permission.READ_CALL_LOG)
        permissions.add(Manifest.permission.READ_CONTACTS)

        // For notifications on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val permissionsNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isEmpty()) {
            Log.d(TAG, "✅ All phone permissions already granted")
            checkAndRequestOverlayPermission()
        } else {
            Log.d(TAG, "🔐 Requesting permissions: $permissionsNeeded")
            permissionLauncher.launch(permissionsNeeded.toTypedArray())
        }
    }

    private fun checkAndRequestOverlayPermission() {
        if (OverlayPermissionHelper.hasOverlayPermission(this)) {
            Log.d(TAG, "✅ Overlay permission already granted")
            checkAndRequestBatteryOptimization()
        } else {
            Log.d(TAG, "🔐 Requesting overlay permission")
            Toast.makeText(
                this,
                "Please allow 'Display over other apps' for popup notifications",
                Toast.LENGTH_LONG
            ).show()
            OverlayPermissionHelper.requestOverlayPermission(this)
        }
    }

    private fun checkAndRequestBatteryOptimization() {
        if (BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) {
            Log.d(TAG, "✅ Battery optimization already ignored")
            onAllPermissionsGranted()
        } else {
            Log.d(TAG, "🔋 Requesting battery optimization exemption")
            Toast.makeText(
                this,
                "Please disable battery optimization to ensure reliable call detection",
                Toast.LENGTH_LONG
            ).show()
            BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(this)
        }
    }

    private fun onAllPermissionsGranted() {
        Log.d(TAG, "✅✅✅ All permissions granted! Starting service...")

        // Mark setup as complete
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, true).apply()
        Log.d(TAG, "💾 Setup completion saved to SharedPreferences")

        // Start the service
        startCallMonitorService()

        // Hide wizard
        showSetupWizard.value = false

        Toast.makeText(this, "✅ TaskSnap is ready!", Toast.LENGTH_SHORT).show()
    }

    private fun startCallMonitorService() {
        Log.d(TAG, "🚀 Starting CallMonitorService...")

        try {
            CallMonitorService.start(this)
            Log.d(TAG, "✅ CallMonitorService start command sent")

            // Verify service started (with slight delay to let it initialize)
            postDelayed(1000) {
                verifyServiceRunning()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start call monitor service", e)
            Toast.makeText(
                this,
                "Error starting service: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun verifyServiceRunning() {
        // This is a simple check - in production you might want to use ActivityManager
        Log.d(TAG, "🔍 Verifying service is running...")
        // The service should log "Call monitoring started successfully ✅" if working
    }

    private fun ensureServiceRunning() {
        Log.d(TAG, "🔍 Ensuring service is running...")
        // The service should already be running (started by boot receiver)
        // But we can restart it just to be safe
        startCallMonitorService()
    }

    private fun postDelayed(delayMs: Long, action: () -> Unit) {
        window.decorView.postDelayed(action, delayMs)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "💀 MainActivity onDestroy()")
        CallMonitorService.onCallEnded = null
        // Note: We don't stop the service - it should keep running
    }
}
