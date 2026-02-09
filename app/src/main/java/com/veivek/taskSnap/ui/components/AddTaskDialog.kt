package com.veivek.taskSnap.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.veivek.taskSnap.data.local.TaskDatabase
import com.veivek.taskSnap.data.repository.TaskRepositoryImpl
import com.veivek.taskSnap.domain.model.Task
import com.veivek.taskSnap.domain.model.TaskSource
import com.veivek.taskSnap.domain.repository.TaskRepository
import com.veivek.taskSnap.presentation.components.EnhancedAddTaskDialog
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Dialog shown after a call ends.
 * Pre-fills with contact information.
 */
@Composable
fun CallEndedDialog(
    phoneNumber: String?,
    contactName: String?,
    isIncoming: Boolean,
    onDismiss: () -> Unit,
) {
    val callType = if (isIncoming) "incoming" else "outgoing"
    val displayName = contactName ?: phoneNumber ?: "Unknown"
    val suggestedTitle = "Follow up: $displayName"
    val suggestedDescription = "After $callType call with $displayName"
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val database = TaskDatabase.getInstance(context)
    val repository: TaskRepository = TaskRepositoryImpl(database.taskDao())

    EnhancedAddTaskDialog(
        onDismiss = onDismiss,
        prefillTitle = suggestedTitle,
        prefillDescription = suggestedDescription,
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
                onDismiss()
            }
        },
    )
}
