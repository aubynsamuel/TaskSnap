package com.veivek.taskSnap.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.veivek.taskSnap.domain.model.Quadrant
import com.veivek.taskSnap.presentation.components.SwipeableTaskCard
import com.veivek.taskSnap.presentation.navigation.AppRoutes
import com.veivek.taskSnap.presentation.navigation.navigate
import com.veivek.taskSnap.presentation.navigation.popOrStay
import com.veivek.taskSnap.presentation.theme.Q1Primary
import com.veivek.taskSnap.presentation.theme.Q2Primary
import com.veivek.taskSnap.presentation.theme.Q3Primary
import com.veivek.taskSnap.presentation.theme.Q4Primary
import com.veivek.taskSnap.presentation.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedTasksScreen(
    backStack: NavBackStack<NavKey>,
    viewModel: TaskViewModel,
) {
    val completedTasks by viewModel.completedTasks.collectAsState()
    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }

    // Helper to get color
    fun getQuadrantColor(quadrant: Quadrant): Color {
        return when (quadrant) {
            Quadrant.Q1_DO_FIRST -> Q1Primary
            Quadrant.Q2_SCHEDULE -> Q2Primary
            Quadrant.Q3_DELEGATE -> Q3Primary
            Quadrant.Q4_DELETE -> Q4Primary
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Completed Tasks") },
                navigationIcon = {
                    IconButton(onClick = { backStack.popOrStay() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (completedTasks.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllConfirmDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete All",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (completedTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "No completed tasks",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tasks you complete will show up here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(completedTasks, key = { it.id }) { task ->
                    // Re-using SwipeableTaskCard but "Complete" action becomes "Restore"
                    SwipeableTaskCard(
                        task = task,
                        onComplete = { viewModel.restoreTask(task.id) }, // Right swipe restores
                        onDelete = { viewModel.deleteTask(task.id) },    // Left swipe deletes
                        quadrantColor = getQuadrantColor(task.getQuadrant()),
                        onClick = { backStack.navigate(AppRoutes.TaskDetail(task.id)) },
                        startToEndLabel = "Restore",
                        startToEndIcon = Icons.Default.Refresh,
                        startToEndColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showDeleteAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmDialog = false },
            title = { Text("Delete All Completed Tasks?") },
            text = { Text("This will permanently remove all completed tasks. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllCompletedTasks()
                        showDeleteAllConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
