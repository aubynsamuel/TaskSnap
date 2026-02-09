package com.veivek.taskSnap.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.veivek.taskSnap.data.local.TaskDatabase
import com.veivek.taskSnap.data.repository.TaskRepositoryImpl
import com.veivek.taskSnap.domain.repository.TaskRepository
import com.veivek.taskSnap.presentation.screens.EisenhowerMatrixScreen
import com.veivek.taskSnap.presentation.screens.QuadrantDetailScreen
import com.veivek.taskSnap.presentation.viewmodel.TaskViewModel

/**
 * Main navigation component for TaskSnap.
 * Uses Navigation 3 library with type-safe routes.
 */
@Composable
fun Navigation(
    database: TaskDatabase,
) {
    // Initialize repository
    val repository: TaskRepository = TaskRepositoryImpl(database.taskDao())

    // Create ViewModel (in production, use proper DI)
    val viewModel = viewModel { TaskViewModel(repository) }

    // Create navigation backstack starting at Eisenhower Matrix
    val backStack = retain { NavBackStack<NavKey>(AppRoutes.EisenhowerMatrix) }

    NavDisplay(
        onBack = { backStack.removeLastOrNull() },
        backStack = backStack,
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        entryProvider = entryProvider {
            entry<AppRoutes.EisenhowerMatrix> {
                EisenhowerMatrixScreen(
                    backStack = backStack,
                    viewModel = viewModel
                )
            }

            entry<AppRoutes.QuadrantDetail> {
                QuadrantDetailScreen(
                    quadrantNumber = it.quadrant,
                    backStack = backStack,
                    viewModel = viewModel,
                    repository = repository
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
