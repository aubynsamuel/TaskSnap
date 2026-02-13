package com.veivek.taskSnap.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.veivek.taskSnap.domain.model.Quadrant
import com.veivek.taskSnap.presentation.components.EnhancedAddTaskDialog
import com.veivek.taskSnap.presentation.components.QuadrantCard
import com.veivek.taskSnap.presentation.navigation.AppRoutes
import com.veivek.taskSnap.presentation.navigation.navigate
import com.veivek.taskSnap.presentation.viewmodel.MatrixUiState
import com.veivek.taskSnap.presentation.viewmodel.TaskViewModel

/**
 * Eisenhower Matrix Screen - Main screen showing 4-quadrant grid.
 * Beautiful, modern design with vibrant colors and smooth animations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EisenhowerMatrixScreen(
    backStack: NavBackStack<NavKey>,
    viewModel: TaskViewModel,
) {
    val q1Count by viewModel.q1Count.collectAsState()
    val q2Count by viewModel.q2Count.collectAsState()
    val q3Count by viewModel.q3Count.collectAsState()
    val q4Count by viewModel.q4Count.collectAsState()

    val q1Preview by viewModel.q1PreviewTasks.collectAsState()
    val q2Preview by viewModel.q2PreviewTasks.collectAsState()
    val q3Preview by viewModel.q3PreviewTasks.collectAsState()
    val q4Preview by viewModel.q4PreviewTasks.collectAsState()

    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    // Handle error states
    LaunchedEffect(uiState) {
        if (uiState is MatrixUiState.Error) {
            snackbarHostState.showSnackbar((uiState as MatrixUiState.Error).message)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "TaskSnap",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Eisenhower Matrix",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { backStack.navigate(AppRoutes.CompletedTasks) }) {
                        Icon(Icons.Default.List, contentDescription = "Completed Tasks")
                    }
                    IconButton(onClick = { backStack.navigate(AppRoutes.Settings) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Organize by Priority",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "Tap a quadrant to view tasks. Use + to create new tasks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // 2x2 Grid of Quadrants
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Row 1: Q1 and Q2
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    QuadrantCard(
                        quadrant = Quadrant.Q1_DO_FIRST,
                        taskCount = q1Count,
                        previewTasks = q1Preview,
                        modifier = Modifier.weight(1f),
                        onClick = { backStack.navigate(AppRoutes.QuadrantDetail(1)) },
                        onTaskClick = { task -> backStack.navigate(AppRoutes.TaskDetail(task.id)) }
                    )
                    QuadrantCard(
                        quadrant = Quadrant.Q2_SCHEDULE,
                        taskCount = q2Count,
                        previewTasks = q2Preview,
                        modifier = Modifier.weight(1f),
                        onClick = { backStack.navigate(AppRoutes.QuadrantDetail(2)) },
                        onTaskClick = { task -> backStack.navigate(AppRoutes.TaskDetail(task.id)) }
                    )
                }

                // Row 2: Q3 and Q4
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    QuadrantCard(
                        quadrant = Quadrant.Q3_DELEGATE,
                        taskCount = q3Count,
                        previewTasks = q3Preview,
                        modifier = Modifier.weight(1f),
                        onClick = { backStack.navigate(AppRoutes.QuadrantDetail(3)) },
                        onTaskClick = { task -> backStack.navigate(AppRoutes.TaskDetail(task.id)) }
                    )
                    QuadrantCard(
                        quadrant = Quadrant.Q4_DELETE,
                        taskCount = q4Count,
                        previewTasks = q4Preview,
                        modifier = Modifier.weight(1f),
                        onClick = { backStack.navigate(AppRoutes.QuadrantDetail(4)) },
                        onTaskClick = { task -> backStack.navigate(AppRoutes.TaskDetail(task.id)) }
                    )
                }
            }
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        EnhancedAddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, description, isUrgent, isImportant, reminderTime ->
                viewModel.createTask(
                    title = title,
                    description = description,
                    isUrgent = isUrgent,
                    isImportant = isImportant,
                    scheduledReminderTime = reminderTime
                )
                showAddTaskDialog = false
            }
        )
    }
}
