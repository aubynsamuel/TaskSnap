package com.veivek.taskSnap.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.veivek.taskSnap.presentation.screens.EisenhowerMatrixScreen
import com.veivek.taskSnap.presentation.screens.QuadrantDetailScreen
import com.veivek.taskSnap.presentation.viewmodel.TaskViewModel

/**
 * Main navigation component for TaskSnap.
 * Uses Navigation 3 library with type-safe routes.
 */
@Composable
fun Navigation() {
    val backStack = retain { NavBackStack<NavKey>(AppRoutes.EisenhowerMatrix) }

    NavDisplay(
        onBack = { backStack.removeLastOrNull() },
        backStack = backStack,
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        entryProvider = entryProvider {
            entry<AppRoutes.EisenhowerMatrix> {
                val viewModel: TaskViewModel = hiltViewModel()
                EisenhowerMatrixScreen(
                    backStack = backStack,
                    viewModel = viewModel
                )
            }

            entry<AppRoutes.QuadrantDetail> {
                val viewModel: TaskViewModel = hiltViewModel()
                QuadrantDetailScreen(
                    quadrantNumber = it.quadrant,
                    backStack = backStack,
                    viewModel = viewModel
                )
            }

            entry<AppRoutes.TaskDetail> {
                // TODO: Implement task detail screen
                androidx.compose.material3.Text("Task Detail: ${it.taskId}")
            }

            entry<AppRoutes.CreateTask> {
                // TODO: Implement standalone create task screen if needed
                androidx.compose.material3.Text("Create Task")
            }

            entry<AppRoutes.Settings> {
                // TODO: Implement settings screen
                androidx.compose.material3.Text("Settings")
            }
        }
    )
}
