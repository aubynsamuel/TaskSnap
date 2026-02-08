package com.veivek.taskSnap.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.veivek.taskSnap.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Task operations.
 * Uses Flow for reactive updates to UI.
 */
@Dao
interface TaskDao {

    // ========== CREATE ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    // ========== READ ==========

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeTaskById(taskId: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY createdTimestamp DESC")
    fun observeAllActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY completedTimestamp DESC")
    fun observeCompletedTasks(): Flow<List<TaskEntity>>

    // Quadrant-specific queries
    @Query(
        """
        SELECT * FROM tasks 
        WHERE isUrgent = 1 AND isImportant = 1 AND isCompleted = 0 
        ORDER BY createdTimestamp DESC
    """
    )
    fun observeQ1Tasks(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks 
        WHERE isUrgent = 0 AND isImportant = 1 AND isCompleted = 0 
        ORDER BY createdTimestamp DESC
    """
    )
    fun observeQ2Tasks(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks 
        WHERE isUrgent = 1 AND isImportant = 0 AND isCompleted = 0 
        ORDER BY createdTimestamp DESC
    """
    )
    fun observeQ3Tasks(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks 
        WHERE isUrgent = 0 AND isImportant = 0 AND isCompleted = 0 
        ORDER BY createdTimestamp DESC
    """
    )
    fun observeQ4Tasks(): Flow<List<TaskEntity>>

    // Search
    @Query(
        """
        SELECT * FROM tasks 
        WHERE isCompleted = 0 
        AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        ORDER BY createdTimestamp DESC
    """
    )
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    // Sync queries
    @Query("SELECT * FROM tasks WHERE isSynced = 0")
    suspend fun getUnsyncedTasks(): List<TaskEntity>

    // ========== UPDATE ==========

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedTimestamp = :timestamp WHERE id = :taskId")
    suspend fun markTaskCompleted(taskId: String, isCompleted: Boolean, timestamp: Long?)

    @Query("UPDATE tasks SET isUrgent = :isUrgent, isImportant = :isImportant, lastModified = :timestamp WHERE id = :taskId")
    suspend fun updateTaskPriority(
        taskId: String,
        isUrgent: Boolean,
        isImportant: Boolean,
        timestamp: Long,
    )

    @Query("UPDATE tasks SET isSynced = :isSynced WHERE id = :taskId")
    suspend fun markTaskSynced(taskId: String, isSynced: Boolean)

    // ========== DELETE ==========

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun deleteAllCompletedTasks()

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    // ========== STATS ==========

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    fun observeActiveTaskCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE isUrgent = :isUrgent AND isImportant = :isImportant AND isCompleted = 0")
    fun observeQuadrantTaskCount(isUrgent: Boolean, isImportant: Boolean): Flow<Int>
}
