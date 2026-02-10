package com.veivek.taskSnap.presentation.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.veivek.taskSnap.domain.model.Quadrant
import com.veivek.taskSnap.presentation.components.EnhancedAddTaskDialog
import com.veivek.taskSnap.presentation.theme.Q1Primary
import com.veivek.taskSnap.presentation.theme.Q2Primary
import com.veivek.taskSnap.presentation.theme.Q3Primary
import com.veivek.taskSnap.presentation.theme.Q4Primary
import com.veivek.taskSnap.presentation.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    backStack: NavBackStack<NavKey>,
    viewModel: TaskViewModel,
) {
    val task by viewModel.getTaskById(taskId).collectAsState(initial = null)
    val context = LocalContext.current

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (task == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentTask = task!!

    // Edit Dialog
    if (showEditDialog) {
        EnhancedAddTaskDialog(
            onDismiss = { showEditDialog = false },
            onSave = { title, description, isUrgent, isImportant ->
                val updatedTask = currentTask.copy(
                    title = title,
                    description = description,
                    isUrgent = isUrgent,
                    isImportant = isImportant,
                    lastModified = System.currentTimeMillis()
                )
                viewModel.updateTask(updatedTask)
                showEditDialog = false
            },
            prefillTitle = currentTask.title,
            prefillDescription = currentTask.description,
            prefillIsUrgent = currentTask.isUrgent,
            prefillIsImportant = currentTask.isImportant,
            dialogTitle = "Edit Task",
            confirmButtonText = "Save Changes"
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Task?") },
            text = { Text("Are you sure you want to delete this task? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTask(currentTask.id)
                        backStack.removeLastOrNull() // Go back after deletion
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val quadrant = currentTask.getQuadrant()
    val (primaryColor, emoji) = when (quadrant) {
        Quadrant.Q1_DO_FIRST -> Q1Primary to "🔥 Do First"
        Quadrant.Q2_SCHEDULE -> Q2Primary to "📅 Schedule"
        Quadrant.Q3_DELEGATE -> Q3Primary to "👥 Delegate"
        Quadrant.Q4_DELETE -> Q4Primary to "🗑️ Delete"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Details") },
                navigationIcon = {
                    IconButton(onClick = { backStack.removeLastOrNull() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Quadrant Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(primaryColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.labelLarge,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (currentTask.isCompleted) {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "✅ Completed",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Title
            Text(
                text = currentTask.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // Description
            if (currentTask.description.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = currentTask.description,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }

            // Contact Info
            if (!currentTask.relatedContactName.isNullOrBlank() || !currentTask.relatedContactPhoneNumber.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Contact Info",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!currentTask.relatedContactName.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Name: ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentTask.relatedContactName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        if (!currentTask.relatedContactPhoneNumber.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Phone,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = currentTask.relatedContactPhoneNumber,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data =
                                                "tel:${currentTask.relatedContactPhoneNumber}".toUri()
                                        }
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Call")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Call")
                                }
                            }
                        }
                    }
                }
            }

            // Metadata
            val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
            val dateString = dateFormat.format(Date(currentTask.createdTimestamp))

            Column {
                Text(
                    text = "Created",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer Actions
            if (!currentTask.isCompleted) {
                Button(
                    onClick = {
                        viewModel.completeTask(currentTask.id)
                        backStack.removeLastOrNull()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Completed")
                }
            }
        }
    }
}
