package com.veivek.taskSnap.taskActivities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.veivek.taskSnap.data.local.TaskDatabase
import com.veivek.taskSnap.data.repository.TaskRepositoryImpl
import com.veivek.taskSnap.domain.model.Task
import com.veivek.taskSnap.domain.model.TaskSource
import com.veivek.taskSnap.domain.repository.TaskRepository
import com.veivek.taskSnap.presentation.components.EnhancedAddTaskDialog
import com.veivek.taskSnap.presentation.theme.TaskSnapTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Floating overlay window that appears after a call ends.
 * This is the Truecaller-style popup that appears on top of everything.
 */
class CallEndedOverlayActivity : ComponentActivity() {

    companion object {
        const val TAG = "CallEndedOverlayActivity"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_CONTACT_NAME = "extra_contact_name"
        const val EXTRA_IS_INCOMING = "extra_is_incoming"
    }

    private val coroutineScope = CoroutineScope(SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: CallEndedOverlayActivity started")

        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER)
        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME)
        val isIncoming = intent.getBooleanExtra(EXTRA_IS_INCOMING, false)

        val displayName = contactName ?: phoneNumber ?: "Unknown"

        val database = TaskDatabase.Companion.getInstance(applicationContext)
        val repository: TaskRepository = TaskRepositoryImpl(database.taskDao())

        setContent {
            TaskSnapTheme {
                EnhancedAddTaskDialog(
                    onDismiss = { finish() },
                    prefillTitle = "Follow up: $displayName",
                    prefillDescription = "",
                    onSave = { title, description, isUrgent, isImportant ->
                        coroutineScope.launch {
                            repository.addTask(
                                Task(
                                    id = UUID.randomUUID().toString(),
                                    title = title,
                                    description = description,
                                    isUrgent = isUrgent,
                                    isImportant = isImportant,
                                    createdTimestamp = System.currentTimeMillis(),
                                    lastModified = System.currentTimeMillis(),
                                    source = TaskSource.CALL_ENDED,
                                    relatedContact = contactName,
                                    assignedTo = null,
                                    isSynced = false,
                                    cloudId = null,
                                    isCompleted = false,
                                    completedTimestamp = null,
                                )
                            )
                            finish()
                        }
                    },
                )
            }
        }
    }
}