package com.veivek.taskSnap.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.veivek.taskSnap.domain.model.Quadrant
import com.veivek.taskSnap.presentation.components.EnhancedAddTaskDialog
import com.veivek.taskSnap.presentation.navigation.AppRoutes
import com.veivek.taskSnap.presentation.navigation.navigate
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
                    Text(
                        text = "📊 Organize by Priority",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
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
                        modifier = Modifier.weight(1f),
                        onClick = { backStack.navigate(AppRoutes.QuadrantDetail(1)) }
                    )
                    QuadrantCard(
                        quadrant = Quadrant.Q2_SCHEDULE,
                        taskCount = q2Count,
                        modifier = Modifier.weight(1f),
                        onClick = { backStack.navigate(AppRoutes.QuadrantDetail(2)) }
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
                        modifier = Modifier.weight(1f),
                        onClick = { backStack.navigate(AppRoutes.QuadrantDetail(3)) }
                    )
                    QuadrantCard(
                        quadrant = Quadrant.Q4_DELETE,
                        taskCount = q4Count,
                        modifier = Modifier.weight(1f),
                        onClick = { backStack.navigate(AppRoutes.QuadrantDetail(4)) }
                    )
                }
            }
        }
    }

    // Add Task Dialog
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

@Composable
fun QuadrantCard(
    quadrant: Quadrant,
    taskCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val (primaryColor, _, containerColor) = when (quadrant) {
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

    Card(
        modifier = modifier
            .fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor.compositeOver(primaryColor)
        )
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = quadrant.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Text(
                    text = quadrant.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = primaryColor.copy(alpha = 0.7f)
                )
            }

            // Task count badge
            AnimatedVisibility(
                visible = taskCount > 0,
                enter = scaleIn(spring(stiffness = Spring.StiffnessHigh)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.align(Alignment.End),
                    shape = CircleShape,
                    color = primaryColor,
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = taskCount.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

