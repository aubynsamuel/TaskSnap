package com.veivek.allday.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * BroadcastReceiver to detect phone call state changes.
 * This is the core of Feature 1: Call-Ended Detection
 * 
 * Technical Notes:
 * - Works on Android 10-14+ with proper permissions
 * - Requires READ_PHONE_STATE permission
 * - READ_CALL_LOG is needed to get the phone number
 * - Some OEMs may restrict this functionality
 */
class CallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallReceiver"
        
        // Callback to notify when a call ends
        var onCallEnded: ((phoneNumber: String?, contactName: String?, isIncoming: Boolean) -> Unit)? = null
        
        // Track previous state to detect transitions
        private var previousState = TelephonyManager.CALL_STATE_IDLE
        private var isIncoming = false
        private var lastPhoneNumber: String? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }

        val stateString = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        Log.d(TAG, "Phone state changed: $stateString, number: ${phoneNumber ?: "unknown"}")

        val currentState = when (stateString) {
            TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
            TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
            TelephonyManager.EXTRA_STATE_IDLE -> TelephonyManager.CALL_STATE_IDLE
            else -> TelephonyManager.CALL_STATE_IDLE
        }

        // Store the phone number when we first see it
        if (phoneNumber != null) {
            lastPhoneNumber = phoneNumber
        }

        // Detect call end: transition from OFFHOOK (in-call) to IDLE
        if (previousState == TelephonyManager.CALL_STATE_OFFHOOK && 
            currentState == TelephonyManager.CALL_STATE_IDLE) {
            
            Log.d(TAG, "Call ended! Number: $lastPhoneNumber, Incoming: $isIncoming")
            
            // Try to get contact name
            val contactName = lastPhoneNumber?.let { 
                getContactName(context, it) 
            }
            
            // Notify callback
            onCallEnded?.invoke(lastPhoneNumber, contactName, isIncoming)
            
            // Reset tracking
            lastPhoneNumber = null
        }

        // Track if call is incoming (ringing state means incoming)
        if (currentState == TelephonyManager.CALL_STATE_RINGING) {
            isIncoming = true
        } else if (previousState == TelephonyManager.CALL_STATE_IDLE && 
                   currentState == TelephonyManager.CALL_STATE_OFFHOOK) {
            // Went directly from idle to offhook = outgoing call
            isIncoming = false
        }

        previousState = currentState
    }

    /**
     * Attempts to resolve a phone number to a contact name.
     * Requires READ_CONTACTS permission.
     */
    private fun getContactName(context: Context, phoneNumber: String): String? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) 
            != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        return try {
            val uri = android.net.Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(phoneNumber)
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving contact name", e)
            null
        }
    }
}
