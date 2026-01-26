package com.veivek.allday.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.veivek.allday.data.TaskRepository
import com.veivek.allday.data.TaskSource

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    initialTitle: String = "",
    initialDescription: String = "",
    source: TaskSource = TaskSource.MANUAL,
    dialogTitle: String = "➕ New Task"
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = dialogTitle,
                    style = MaterialTheme.typography.titleLarge
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    maxLines = 5
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                TaskRepository.addTask(
                                    title = title,
                                    description = description,
                                    source = source
                                )
                                onDismiss()
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text("Save Task")
                    }
                }
            }
        }
    }
}

/**
 * Dialog shown after a call ends.
 * Pre-fills with contact information.
 */
@Composable
fun CallEndedDialog(
    phoneNumber: String?,
    contactName: String?,
    isIncoming: Boolean,
    onDismiss: () -> Unit
) {
    val callType = if (isIncoming) "incoming" else "outgoing"
    val displayName = contactName ?: phoneNumber ?: "Unknown"
    val suggestedTitle = "Follow up: $displayName"
    val suggestedDescription = "After $callType call with $displayName"

    AddTaskDialog(
        onDismiss = onDismiss,
        initialTitle = suggestedTitle,
        initialDescription = suggestedDescription,
        source = TaskSource.CALL_ENDED,
        dialogTitle = "📞 Task after call with $displayName"
    )
}
