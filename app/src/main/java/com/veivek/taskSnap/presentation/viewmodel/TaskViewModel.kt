package com.veivek.taskSnap.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veivek.taskSnap.domain.model.Quadrant
import com.veivek.taskSnap.domain.model.Task
import com.veivek.taskSnap.domain.model.TaskSource
import com.veivek.taskSnap.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for Eisenhower Matrix screen.
 * Manages task state and operations.
 */
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository,
) : ViewModel() {

    // Task counts for each quadrant
    val q1Count = repository.observeQuadrantTaskCount(Quadrant.Q1_DO_FIRST)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val q2Count = repository.observeQuadrantTaskCount(Quadrant.Q2_SCHEDULE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val q3Count = repository.observeQuadrantTaskCount(Quadrant.Q3_DELEGATE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val q4Count = repository.observeQuadrantTaskCount(Quadrant.Q4_DELETE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Task previews for each quadrant (Top 3)
    val q1PreviewTasks = repository.observeQuadrantTasks(Quadrant.Q1_DO_FIRST, 3)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val q2PreviewTasks = repository.observeQuadrantTasks(Quadrant.Q2_SCHEDULE, 3)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val q3PreviewTasks = repository.observeQuadrantTasks(Quadrant.Q3_DELEGATE, 3)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val q4PreviewTasks = repository.observeQuadrantTasks(Quadrant.Q4_DELETE, 3)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All active tasks
    val allTasks = repository.observeAllActiveTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Completed tasks
    val completedTasks = repository.observeCompletedTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state
    private val _uiState = MutableStateFlow<MatrixUiState>(MatrixUiState.Success)
    val uiState: StateFlow<MatrixUiState> = _uiState.asStateFlow()

    val currentObservingQuadrant = MutableStateFlow<Quadrant?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasksByQuadrant = currentObservingQuadrant.flatMapLatest { quadrant ->
        if (quadrant != null) {
            repository.observeTasksByQuadrant(quadrant)
        } else {
            emptyFlow()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun observeTasksByQuadrant(quadrant: Quadrant) {
        currentObservingQuadrant.value = quadrant
    }

    fun removeObserver() {
        currentObservingQuadrant.value = null
    }

    /**
     * Create a new task.
     */
    fun createTask(
        title: String,
        description: String = "",
        isUrgent: Boolean,
        isImportant: Boolean,
        source: TaskSource = TaskSource.MANUAL,
        relatedContactName: String? = null,
        relatedContactPhoneNumber: String? = null,
    ) {
        viewModelScope.launch {
            _uiState.value = MatrixUiState.Loading

            val task = Task(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                isUrgent = isUrgent,
                isImportant = isImportant,
                createdTimestamp = System.currentTimeMillis(),
                lastModified = System.currentTimeMillis(),
                source = source,
                relatedContactName = relatedContactName,
                relatedContactPhoneNumber = relatedContactPhoneNumber,
                assignedTo = null,
                isSynced = false,
                cloudId = null,
                isCompleted = false,
                completedTimestamp = null
            )

            repository.addTask(task)
                .onSuccess {
                    _uiState.value = MatrixUiState.Success
                }
                .onFailure { error ->
                    _uiState.value =
                        MatrixUiState.Error(error.message ?: "Failed to create task")
                }
        }
    }

    /**
     * Mark task as completed.
     */
    fun completeTask(taskId: String) {
        viewModelScope.launch {
            repository.markTaskCompleted(taskId, true)
                .onFailure { error ->
                    _uiState.value =
                        MatrixUiState.Error(error.message ?: "Failed to complete task")
                }
        }
    }

    /**
     * Restore a completed task.
     */
    fun restoreTask(taskId: String) {
        viewModelScope.launch {
            repository.markTaskCompleted(taskId, false)
                .onFailure { error ->
                    _uiState.value =
                        MatrixUiState.Error(error.message ?: "Failed to restore task")
                }
        }
    }

    /**
     * Delete a task.
     */
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
                .onFailure { error ->
                    _uiState.value =
                        MatrixUiState.Error(error.message ?: "Failed to delete task")
                }
        }
    }

    /**
     * Delete all completed tasks.
     */
    fun deleteAllCompletedTasks() {
        viewModelScope.launch {
            repository.deleteAllCompletedTasks()
                .onFailure { error ->
                    _uiState.value =
                        MatrixUiState.Error(error.message ?: "Failed to delete completed tasks")
                }
        }
    }

    /**
     * Update task priority (move between quadrants).
     */
    fun updateTaskPriority(taskId: String, isUrgent: Boolean, isImportant: Boolean) {
        viewModelScope.launch {
            repository.updateTaskPriority(taskId, isUrgent, isImportant)
                .onFailure { error ->
                    _uiState.value =
                        MatrixUiState.Error(error.message ?: "Failed to update task")
                }
        }
    }

    /**
     * Update an entire task.
     */
    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
                .onFailure { error ->
                    _uiState.value =
                        MatrixUiState.Error(error.message ?: "Failed to update task")
                }
        }
    }

    /**
     * Get a task by ID.
     */
    fun getTaskById(taskId: String) = repository.observeTaskById(taskId)

    /**
     * Clear error state.
     */
    fun clearError() {
        _uiState.value = MatrixUiState.Success
    }
}

sealed class MatrixUiState {
    object Success : MatrixUiState()
    object Loading : MatrixUiState()
    data class Error(val message: String) : MatrixUiState()
}
