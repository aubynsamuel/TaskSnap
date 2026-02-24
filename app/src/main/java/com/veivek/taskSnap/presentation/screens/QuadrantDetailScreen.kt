package com.veivek.taskSnap.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.veivek.taskSnap.domain.model.Quadrant
import com.veivek.taskSnap.presentation.components.EmptyQuadrantState
import com.veivek.taskSnap.presentation.components.EnhancedAddTaskDialog
import com.veivek.taskSnap.presentation.components.SwipeableTaskCard
import com.veivek.taskSnap.presentation.navigation.AppRoutes
import com.veivek.taskSnap.presentation.navigation.navigate
import com.veivek.taskSnap.presentation.navigation.popOrStay
import com.veivek.taskSnap.presentation.theme.ErrorLight
import com.veivek.taskSnap.presentation.theme.SuccessLight
import com.veivek.taskSnap.presentation.utils.icon
import com.veivek.taskSnap.presentation.utils.primaryColor
import com.veivek.taskSnap.presentation.viewmodel.TaskViewModel

/**
 * Quadrant Detail Screen - Shows all tasks in a specific quadrant.
 * Features swipe-to-complete and swipe-to-delete with beautiful animations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuadrantDetailScreen(
    quadrantNumber: Int,
    backStack: NavBackStack<NavKey>,
    viewModel: TaskViewModel,
) {
    val quadrant = Quadrant.fromNumber(quadrantNumber)
    val tasks by viewModel.tasksByQuadrant.collectAsState()
    var showAddTaskDialog by remember { mutableStateOf(false) }

    val primaryColor = quadrant.primaryColor
    val icon = quadrant.icon

    LaunchedEffect(Unit) {
        viewModel.observeTasksByQuadrant(quadrant)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.removeObserver() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Column {
                            Text(
                                text = quadrant.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = quadrant.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { backStack.popOrStay() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = primaryColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        if (tasks.isEmpty()) {
            EmptyQuadrantState(
                quadrant = quadrant,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    SwipeableTaskCard(
                        task = task,
                        onComplete = { viewModel.completeTask(task.id) },
                        onDelete = { viewModel.deleteTask(task.id) },
                        quadrantColor = primaryColor,
                        onClick = { backStack.navigate(AppRoutes.TaskDetail(task.id)) },
                        startToEndLabel = "Complete",
                        startToEndIcon = Icons.Default.Check,
                        startToEndColor = SuccessLight,
                        endToStartLabel = "Delete",
                        endToStartIcon = Icons.Default.Delete,
                        endToStartColor = ErrorLight
                    )
                }
            }
        }
    }

    // Add Task Dialog with pre-filled priority
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

