package com.veivek.taskSnap.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.veivek.taskSnap.domain.model.Task
import com.veivek.taskSnap.domain.model.TaskSource
import java.util.UUID

/**
 * Room entity for Task storage.
 * Indexed on isCompleted and quadrant fields for efficient queries.
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["isCompleted"]),
        Index(value = ["isUrgent", "isImportant"]),
        Index(value = ["createdTimestamp"])
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val title: String,
    val description: String,
    val isUrgent: Boolean,
    val isImportant: Boolean,
    val createdTimestamp: Long,
    val lastModified: Long,
    val source: String, // Stored as string for Room compatibility
    val relatedContact: String?,
    val assignedTo: String?,
    val isSynced: Boolean,
    val cloudId: String?,
    val isCompleted: Boolean,
    val completedTimestamp: Long?,
) {
    /**
     * Convert entity to domain model.
     */
    fun toDomain(): Task {
        return Task(
            id = id,
            title = title,
            description = description,
            isUrgent = isUrgent,
            isImportant = isImportant,
            createdTimestamp = createdTimestamp,
            lastModified = lastModified,
            source = TaskSource.valueOf(source),
            relatedContact = relatedContact,
            assignedTo = assignedTo,
            isSynced = isSynced,
            cloudId = cloudId,
            isCompleted = isCompleted,
            completedTimestamp = completedTimestamp
        )
    }

    companion object {
        /**
         * Convert domain model to entity.
         */
        fun fromDomain(task: Task): TaskEntity {
            return TaskEntity(
                id = task.id,
                title = task.title,
                description = task.description,
                isUrgent = task.isUrgent,
                isImportant = task.isImportant,
                createdTimestamp = task.createdTimestamp,
                lastModified = task.lastModified,
                source = task.source.name,
                relatedContact = task.relatedContact,
                assignedTo = task.assignedTo,
                isSynced = task.isSynced,
                cloudId = task.cloudId,
                isCompleted = task.isCompleted,
                completedTimestamp = task.completedTimestamp
            )
        }
    }
}
