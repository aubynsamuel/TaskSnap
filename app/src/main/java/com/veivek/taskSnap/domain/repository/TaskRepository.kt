package com.veivek.taskSnap.domain.repository

import com.veivek.taskSnap.domain.model.Quadrant
import com.veivek.taskSnap.domain.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Task operations.
 * Defines the contract for task data access.
 */
interface TaskRepository {

    // Create
    suspend fun addTask(task: Task): Result<String>

    // Read
    suspend fun getTaskById(taskId: String): Result<Task?>
    fun observeTaskById(taskId: String): Flow<Task?>
    fun observeAllActiveTasks(): Flow<List<Task>>
    fun observeCompletedTasks(): Flow<List<Task>>
    fun observeTasksByQuadrant(quadrant: Quadrant): Flow<List<Task>>
    fun searchTasks(query: String): Flow<List<Task>>
    fun observeQuadrantTaskCount(quadrant: Quadrant): Flow<Int>
    fun observeQuadrantTasks(quadrant: Quadrant, limit: Int): Flow<List<Task>>

    // Update
    suspend fun updateTask(task: Task): Result<Unit>
    suspend fun markTaskCompleted(taskId: String, isCompleted: Boolean): Result<Unit>
    suspend fun updateTaskPriority(
        taskId: String,
        isUrgent: Boolean,
        isImportant: Boolean,
    ): Result<Unit>

    // Delete
    suspend fun deleteTask(taskId: String): Result<Unit>
    suspend fun deleteAllCompletedTasks(): Result<Unit>

    // Sync (placeholder for future cloud sync)
    suspend fun syncTasks(): Result<Unit>
}
