package com.veivek.taskSnap.taskActivities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.veivek.taskSnap.domain.model.Task
import com.veivek.taskSnap.domain.model.TaskSource
import com.veivek.taskSnap.domain.repository.TaskRepository
import com.veivek.taskSnap.presentation.components.EnhancedAddTaskDialog
import com.veivek.taskSnap.presentation.theme.TaskSnapTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Floating overlay window that appears after a call ends.
 */
@AndroidEntryPoint
class CallEndedOverlayActivity : ComponentActivity() {

    companion object {
        const val TAG = "CallEndedOverlayActivity"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_CONTACT_NAME = "extra_contact_name"
        const val EXTRA_IS_INCOMING = "extra_is_incoming"
    }

    @Inject
    lateinit var repository: TaskRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: CallEndedOverlayActivity started")

        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER)
        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME)
        val isIncoming = intent.getBooleanExtra(EXTRA_IS_INCOMING, false)

        val displayName = contactName ?: phoneNumber ?: "Unknown"

        setContent {
            TaskSnapTheme {
                EnhancedAddTaskDialog(
                    onDismiss = { finish() },
                    prefillTitle = "Follow up: $displayName",
                    prefillDescription = "",
                    onSave = { title, description, isUrgent, isImportant ->
                        lifecycleScope.launch {
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
                    }
                )
            }
        }
    }
}