package com.veivek.taskSnap.presentation.quadrant

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.veivek.taskSnap.domain.model.Quadrant
import com.veivek.taskSnap.domain.model.Task
import com.veivek.taskSnap.domain.model.TaskSource
import com.veivek.taskSnap.domain.repository.TaskRepository
import com.veivek.taskSnap.presentation.components.EnhancedAddTaskDialog
import com.veivek.taskSnap.presentation.matrix.TaskViewModel
import com.veivek.taskSnap.presentation.navigation.popOrStay
import com.veivek.taskSnap.presentation.theme.ErrorLight
import com.veivek.taskSnap.presentation.theme.Q1Container
import com.veivek.taskSnap.presentation.theme.Q1Primary
import com.veivek.taskSnap.presentation.theme.Q1Secondary
import com.veivek.taskSnap.presentation.theme.Q2Container
import com.veivek.taskSnap.presentation.theme.Q2Primary
import com.veivek.taskSnap.presentation.theme.Q2Secondary
import com.veivek.taskSnap.presentation.theme.Q3Container
import com.veivek.taskSnap.presentation.theme.Q3Primary
import com.veivek.taskSnap.presentation.theme.Q3Secondary
import com.veivek.taskSnap.presentation.theme.Q4Container
import com.veivek.taskSnap.presentation.theme.Q4Primary
import com.veivek.taskSnap.presentation.theme.Q4Secondary
import com.veivek.taskSnap.presentation.theme.SuccessLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    repository: TaskRepository,
) {
    val quadrant = Quadrant.fromNumber(quadrantNumber)
    val tasks by repository.observeTasksByQuadrant(quadrant).collectAsState(initial = emptyList())

    var showAddTaskDialog by remember { mutableStateOf(false) }

    val (primaryColor, secondaryColor, containerColor) = when (quadrant) {
        Quadrant.Q1_DO_FIRST -> Triple(Q1Primary, Q1Secondary, Q1Container)
        Quadrant.Q2_SCHEDULE -> Triple(Q2Primary, Q2Secondary, Q2Container)
        Quadrant.Q3_DELEGATE -> Triple(Q3Primary, Q3Secondary, Q3Container)
        Quadrant.Q4_DELETE -> Triple(Q4Primary, Q4Secondary, Q4Container)
    }

    val emoji = when (quadrant) {
        Quadrant.Q1_DO_FIRST -> "🔥"
        Quadrant.Q2_SCHEDULE -> "📅"
        Quadrant.Q3_DELEGATE -> "👥"
        Quadrant.Q4_DELETE -> "🗑️"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = emoji, style = MaterialTheme.typography.headlineSmall)
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
                emoji = emoji,
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
                        quadrantColor = primaryColor
                    )
                }
            }
        }
    }

    // Add Task Dialog with pre-filled priority
    if (showAddTaskDialog) {
        EnhancedAddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, description, isUrgent, isImportant ->
                viewModel.createTask(
                    title = title,
                    description = description,
                    isUrgent = isUrgent,
                    isImportant = isImportant
                )
                showAddTaskDialog = false
            }
        )
    }
}

/**
 * Empty state for quadrant with no tasks.
 */
@Composable
fun EmptyQuadrantState(
    quadrant: Quadrant,
    emoji: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "No tasks yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap the + button to add a task to ${quadrant.displayName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * Task card with swipe-to-complete (right) and swipe-to-delete (left).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTaskCard(
    task: Task,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    quadrantColor: Color,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swipe left to delete
                    onDelete()
                    true
                }

                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swipe right to complete
                    onComplete()
                    true
                }

                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> SuccessLight
                    SwipeToDismissBoxValue.EndToStart -> ErrorLight
                    else -> Color.Transparent
                },
                label = "swipe_bg",
                animationSpec = spring(stiffness = Spring.StiffnessHigh)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp),
                contentAlignment = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
            ) {
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Complete",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Complete",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    SwipeToDismissBoxValue.EndToStart -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Delete",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    ) {
        TaskCard(task = task, quadrantColor = quadrantColor)
    }
}

/**
 * Individual task card with beautiful design.
 */
@Composable
fun TaskCard(
    task: Task,
    quadrantColor: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title and source badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                SourceBadge(source = task.source, color = quadrantColor)
            }

            // Description
            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Related contact (for call-based tasks)
            if (task.relatedContact != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "👤", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = task.relatedContact,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Timestamp
            Text(
                text = formatTimestamp(task.createdTimestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Source badge showing where the task came from.
 */
@Composable
fun SourceBadge(source: TaskSource, color: Color) {
    val (emoji, label) = when (source) {
        TaskSource.MANUAL -> "✏️" to "Manual"
        TaskSource.CALL_ENDED -> "📞" to "Call"
        TaskSource.TEXT_SELECTION -> "📝" to "Text"
        TaskSource.SHARE_INTENT -> "📤" to "Share"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = "$emoji $label",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
