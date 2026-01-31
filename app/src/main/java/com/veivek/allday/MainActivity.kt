package com.veivek.allday

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.veivek.allday.service.CallMonitorService
import com.veivek.allday.ui.components.AddTaskDialog
import com.veivek.allday.ui.components.CallEndedDialog
import com.veivek.allday.ui.screens.TaskListScreen
import com.veivek.allday.ui.theme.AllDayTheme
import com.veivek.allday.utils.BatteryOptimizationHelper
import com.veivek.allday.utils.OverlayPermissionHelper

/**
 * Main Activity for TaskSnap MVP
 *
 * This app tests two core features:
 * 1. Call-ended detection → prompt user to create a task (Truecaller-style)
 * 2. Text selection & Share integration → create a task from selected text
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "TaskSnapPrefs"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
    }

    // State for dialogs
    private var showAddTaskDialog = mutableStateOf(false)
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

        Log.d(TAG, "Permissions granted: $granted, denied: $denied")

        if (permissions[Manifest.permission.READ_PHONE_STATE] == true) {
            // Phone permission granted, continue with overlay permission
            checkAndRequestOverlayPermission()
        } else {
            Toast.makeText(
                this,
                "Phone permission is required for call detection",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Overlay permission result launcher
    private val overlayPermissionResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (OverlayPermissionHelper.hasOverlayPermission(this)) {
            Log.d(TAG, "Overlay permission granted")
            onAllPermissionsGranted()
        } else {
            Toast.makeText(
                this,
                "Overlay permission needed for popup after calls",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Set up callback for when service detects call ended
        CallMonitorService.onCallEnded = { phoneNumber, contactName, isIncoming ->
            Log.d(TAG, "Call ended callback received: $phoneNumber, $contactName, $isIncoming")
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
            // First launch - request permissions
            showSetupWizard.value = true
            requestPhonePermissions()
        } else {
            // Already set up - verify service is running
            ensureServiceRunning()
        }

        setContent {
            AllDayTheme {
                val showAddDialog by showAddTaskDialog
                val callEnded by callEndedData
                val showWizard by showSetupWizard

                if (showWizard) {
                    SetupWizardScreen(
                        onComplete = {
                            showSetupWizard.value = false
                        }
                    )
                } else {
                    TaskListScreen(
                        onAddTaskClick = { showAddTaskDialog.value = true }
                    )

                    // Manual add task dialog
                    if (showAddDialog) {
                        AddTaskDialog(
                            onDismiss = { showAddTaskDialog.value = false }
                        )
                    }

                    // Call ended dialog (backup if overlay permission not granted)
                    callEnded?.let { info ->
                        if (!OverlayPermissionHelper.hasOverlayPermission(this)) {
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
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Check permissions if wizard is showing (user returning from settings)
        if (showSetupWizard.value) {
            val hasOverlay = OverlayPermissionHelper.hasOverlayPermission(this)
            val hasBattery = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)

            if (hasOverlay && hasBattery) {
                onAllPermissionsGranted()
            } else if (hasOverlay) {
                // If returned from overlay settings but haven't asked for battery yet/denied
                // We could prompt again or just proceed. For now, let's try to request.
                if (!hasBattery) {
                    // Ideally we don't loop, but let's check if we should trigger it
                }
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == CallMonitorService.ACTION_CALL_ENDED) {
            val phoneNumber = intent.getStringExtra(CallMonitorService.EXTRA_PHONE_NUMBER)
            val contactName = intent.getStringExtra(CallMonitorService.EXTRA_CONTACT_NAME)
            val isIncoming = intent.getBooleanExtra(CallMonitorService.EXTRA_IS_INCOMING, false)

            Log.d(TAG, "Received call ended intent: $phoneNumber, $contactName")
            callEndedData.value = CallEndedInfo(phoneNumber, contactName, isIncoming)
        }
    }

    private fun requestPhonePermissions() {
        val permissions = mutableListOf<String>()

        // Core permission for call state
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
            Log.d(TAG, "All phone permissions already granted")
            checkAndRequestOverlayPermission()
        } else {
            Log.d(TAG, "Requesting permissions: $permissionsNeeded")
            permissionLauncher.launch(permissionsNeeded.toTypedArray())
        }
    }

    private fun checkAndRequestOverlayPermission() {
        if (OverlayPermissionHelper.hasOverlayPermission(this)) {
            Log.d(TAG, "Overlay permission already granted")
            checkAndRequestBatteryOptimization()
        } else {
            Log.d(TAG, "Requesting overlay permission")
            OverlayPermissionHelper.requestOverlayPermission(this)
        }
    }

    private fun checkAndRequestBatteryOptimization() {
        if (BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) {
            Log.d(TAG, "Battery optimization already ignored")
            onAllPermissionsGranted()
        } else {
            Log.d(TAG, "Requesting battery optimization exemption")
            BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(this)
        }
    }

    private fun onAllPermissionsGranted() {
        Log.d(TAG, "All permissions granted")

        // Mark setup as complete
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, true).apply()

        // Start the service
        startCallMonitorService()

        // Hide wizard
        showSetupWizard.value = false

        Toast.makeText(this, "✅ TaskSnap is ready!", Toast.LENGTH_SHORT).show()
    }

    private fun startCallMonitorService() {
        try {
            CallMonitorService.start(this)
            Log.d(TAG, "Call monitor service started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start call monitor service", e)
        }
    }

    private fun ensureServiceRunning() {
        // The service should already be running (started by boot receiver)
        // But we can verify and restart if needed
        startCallMonitorService()
    }

    override fun onDestroy() {
        super.onDestroy()
        CallMonitorService.onCallEnded = null
        // Note: We don't stop the service - it should keep running
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardScreen(onComplete: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("TaskSnap Setup") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "📞",
                style = MaterialTheme.typography.displayLarge
            )

            Text(
                text = "Welcome to TaskSnap",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "To work like Truecaller, TaskSnap needs a few permissions:",
                style = MaterialTheme.typography.bodyLarge
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PermissionItem(
                        icon = "📞",
                        title = "Phone",
                        description = "Detect when calls end"
                    )
                    PermissionItem(
                        icon = "👤",
                        title = "Contacts",
                        description = "Show who you called"
                    )
                    PermissionItem(
                        icon = "🔔",
                        title = "Notifications",
                        description = "Background monitoring"
                    )
                    PermissionItem(
                        icon = "💬",
                        title = "Display over apps",
                        description = "Show popup after calls"
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "The app will start automatically after setup",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PermissionItem(icon: String, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, style = MaterialTheme.typography.headlineMedium)
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}