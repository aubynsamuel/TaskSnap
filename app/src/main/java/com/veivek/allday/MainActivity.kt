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
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.veivek.allday.service.CallMonitorService
import com.veivek.allday.ui.components.AddTaskDialog
import com.veivek.allday.ui.components.CallEndedDialog
import com.veivek.allday.ui.screens.TaskListScreen
import com.veivek.allday.ui.theme.AllDayTheme

/**
 * Main Activity for TaskSnap MVP
 * 
 * This app tests two core features:
 * 1. Call-ended detection → prompt user to create a task
 * 2. Text selection & Share integration → create a task from selected text
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // State for dialogs
    private var showAddTaskDialog = mutableStateOf(false)
    private var callEndedData = mutableStateOf<CallEndedInfo?>(null)

    data class CallEndedInfo(
        val phoneNumber: String?,
        val contactName: String?,
        val isIncoming: Boolean
    )

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.filter { it.value }.keys
        val denied = permissions.filter { !it.value }.keys

        Log.d(TAG, "Permissions granted: $granted, denied: $denied")

        if (permissions[Manifest.permission.READ_PHONE_STATE] == true) {
            // Start the foreground service for call monitoring
            startCallMonitorService()
        } else {
            Toast.makeText(
                this,
                "Phone permission needed for call detection feature",
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

        // Request permissions for call detection
        requestPhonePermissions()

        setContent {
            AllDayTheme {
                val showAddDialog by showAddTaskDialog
                val callEnded by callEndedData

                TaskListScreen(
                    onAddTaskClick = { showAddTaskDialog.value = true }
                )

                // Manual add task dialog
                if (showAddDialog) {
                    AddTaskDialog(
                        onDismiss = { showAddTaskDialog.value = false }
                    )
                }

                // Call ended dialog
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

        // READ_CALL_LOG is needed to get phone numbers on Android 9+
        permissions.add(Manifest.permission.READ_CALL_LOG)

        // For resolving contact names
        permissions.add(Manifest.permission.READ_CONTACTS)

        // For foreground service notification on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val permissionsNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isEmpty()) {
            Log.d(TAG, "All permissions already granted")
            startCallMonitorService()
        } else {
            Log.d(TAG, "Requesting permissions: $permissionsNeeded")
            permissionLauncher.launch(permissionsNeeded.toTypedArray())
        }
    }

    private fun startCallMonitorService() {
        try {
            CallMonitorService.start(this)
            Log.d(TAG, "Call monitor service started")
            Toast.makeText(this, "📞 Call monitoring active", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start call monitor service", e)
            Toast.makeText(this, "Failed to start call monitoring", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CallMonitorService.onCallEnded = null
        // Note: We don't stop the service here so it continues monitoring
        // User can manually stop it or it will stop when app is force-stopped
    }
}