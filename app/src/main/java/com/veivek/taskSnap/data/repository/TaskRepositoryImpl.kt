package com.veivek.taskSnap.data.repository

import android.util.Log
import com.veivek.taskSnap.data.local.dao.TaskDao
import com.veivek.taskSnap.data.local.entity.TaskEntity
import com.veivek.taskSnap.domain.model.Quadrant
import com.veivek.taskSnap.domain.model.Task
import com.veivek.taskSnap.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of TaskRepository using Room database.
 * Provides offline-first data access with placeholder for cloud sync.
 */
class TaskRepositoryImpl(
    private val taskDao: TaskDao,
) : TaskRepository {

    companion object {
        private const val TAG = "TaskRepositoryImpl"
    }

    override suspend fun addTask(task: Task): Result<String> {
        return try {
            val entity = TaskEntity.fromDomain(task)
            taskDao.insertTask(entity)
            Log.d(TAG, "Task added successfully: ${task.id}")
            Result.success(task.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding task", e)
            Result.failure(e)
        }
    }

    override suspend fun getTaskById(taskId: String): Result<Task?> {
        return try {
            val entity = taskDao.getTaskById(taskId)
            Result.success(entity?.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Error getting task by id", e)
            Result.failure(e)
        }
    }

    override fun observeTaskById(taskId: String): Flow<Task?> {
        return taskDao.observeTaskById(taskId).map { it?.toDomain() }
    }

    override fun observeAllActiveTasks(): Flow<List<Task>> {
        return taskDao.observeAllActiveTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeCompletedTasks(): Flow<List<Task>> {
        return taskDao.observeCompletedTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeTasksByQuadrant(quadrant: Quadrant): Flow<List<Task>> {
        val flow = when (quadrant) {
            Quadrant.Q1_DO_FIRST -> taskDao.observeQ1Tasks()
            Quadrant.Q2_SCHEDULE -> taskDao.observeQ2Tasks()
            Quadrant.Q3_DELEGATE -> taskDao.observeQ3Tasks()
            Quadrant.Q4_DELETE -> taskDao.observeQ4Tasks()
        }
        return flow.map { entities -> entities.map { it.toDomain() } }
    }

    override fun searchTasks(query: String): Flow<List<Task>> {
        return taskDao.searchTasks(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeQuadrantTaskCount(quadrant: Quadrant): Flow<Int> {
        return when (quadrant) {
            Quadrant.Q1_DO_FIRST -> taskDao.observeQuadrantTaskCount(
                isUrgent = true,
                isImportant = true
            )

            Quadrant.Q2_SCHEDULE -> taskDao.observeQuadrantTaskCount(
                isUrgent = false,
                isImportant = true
            )

            Quadrant.Q3_DELEGATE -> taskDao.observeQuadrantTaskCount(
                isUrgent = true,
                isImportant = false
            )

            Quadrant.Q4_DELETE -> taskDao.observeQuadrantTaskCount(
                isUrgent = false,
                isImportant = false
            )
        }
    }

    override suspend fun updateTask(task: Task): Result<Unit> {
        return try {
            val entity = TaskEntity.fromDomain(task)
            taskDao.updateTask(entity)
            Log.d(TAG, "Task updated successfully: ${task.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task", e)
            Result.failure(e)
        }
    }

    override suspend fun markTaskCompleted(taskId: String, isCompleted: Boolean): Result<Unit> {
        return try {
            val timestamp = if (isCompleted) System.currentTimeMillis() else null
            taskDao.markTaskCompleted(taskId, isCompleted, timestamp)
            Log.d(TAG, "Task marked as ${if (isCompleted) "completed" else "active"}: $taskId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking task completed", e)
            Result.failure(e)
        }
    }

    override suspend fun updateTaskPriority(
        taskId: String,
        isUrgent: Boolean,
        isImportant: Boolean,
    ): Result<Unit> {
        return try {
            val timestamp = System.currentTimeMillis()
            taskDao.updateTaskPriority(taskId, isUrgent, isImportant, timestamp)
            Log.d(TAG, "Task priority updated: $taskId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task priority", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            taskDao.deleteTaskById(taskId)
            Log.d(TAG, "Task deleted: $taskId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteAllCompletedTasks(): Result<Unit> {
        return try {
            taskDao.deleteAllCompletedTasks()
            Log.d(TAG, "All completed tasks deleted")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting completed tasks", e)
            Result.failure(e)
        }
    }

    override suspend fun syncTasks(): Result<Unit> {
        // Placeholder for future cloud sync implementation
        Log.d(TAG, "Sync not yet implemented - offline only mode")
        return Result.success(Unit)
    }
}
