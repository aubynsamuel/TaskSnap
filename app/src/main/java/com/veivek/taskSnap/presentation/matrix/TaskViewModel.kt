package com.veivek.taskSnap.presentation.matrix

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veivek.taskSnap.domain.model.Quadrant
import com.veivek.taskSnap.domain.model.Task
import com.veivek.taskSnap.domain.model.TaskSource
import com.veivek.taskSnap.domain.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for Eisenhower Matrix screen.
 * Manages task state and operations.
 */
class TaskViewModel(
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

    // All active tasks
    val allTasks = repository.observeAllActiveTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state
    private val _uiState = MutableStateFlow<MatrixUiState>(MatrixUiState.Success)
    val uiState: StateFlow<MatrixUiState> = _uiState.asStateFlow()

    /**
     * Create a new task.
     */
    fun createTask(
        title: String,
        description: String = "",
        isUrgent: Boolean,
        isImportant: Boolean,
        source: TaskSource = TaskSource.MANUAL,
        relatedContact: String? = null,
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
                relatedContact = relatedContact,
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
                    _uiState.value = MatrixUiState.Error(error.message ?: "Failed to create task")
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
                    _uiState.value = MatrixUiState.Error(error.message ?: "Failed to complete task")
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
                    _uiState.value = MatrixUiState.Error(error.message ?: "Failed to delete task")
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
                    _uiState.value = MatrixUiState.Error(error.message ?: "Failed to update task")
                }
        }
    }

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
