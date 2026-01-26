package com.veivek.allday

import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.veivek.allday.receiver.CallReceiver
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

    // Call receiver for detecting phone state changes
    private val callReceiver = CallReceiver()
    private var isReceiverRegistered = false

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
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            Log.d(TAG, "All permissions granted")
            registerCallReceiver()
        } else {
            Log.w(TAG, "Some permissions denied: $permissions")
            Toast.makeText(
                this,
                "Phone permissions needed for call detection feature",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Set up call ended callback
        CallReceiver.onCallEnded = { phoneNumber, contactName, isIncoming ->
            Log.d(TAG, "Call ended callback: $phoneNumber, $contactName, $isIncoming")
            runOnUiThread {
                callEndedData.value = CallEndedInfo(phoneNumber, contactName, isIncoming)
            }
        }

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

    private fun requestPhonePermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_PHONE_STATE
        )

        // READ_CALL_LOG is needed to get phone numbers on Android 9+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.READ_CALL_LOG)
        }

        // Optional: for resolving contact names
        permissions.add(Manifest.permission.READ_CONTACTS)

        val permissionsNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isEmpty()) {
            Log.d(TAG, "All permissions already granted")
            registerCallReceiver()
        } else {
            Log.d(TAG, "Requesting permissions: $permissionsNeeded")
            permissionLauncher.launch(permissionsNeeded.toTypedArray())
        }
    }

    private fun registerCallReceiver() {
        if (isReceiverRegistered) return

        try {
            val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(callReceiver, filter, RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(callReceiver, filter)
            }
            isReceiverRegistered = true
            Log.d(TAG, "Call receiver registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register call receiver", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(callReceiver)
                isReceiverRegistered = false
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver", e)
            }
        }
        CallReceiver.onCallEnded = null
    }
}