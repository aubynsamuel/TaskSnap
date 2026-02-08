package com.veivek.taskSnap.domain.model

/**
 * Domain model for Task.
 * This is separate from the database entity to maintain clean architecture.
 */
data class Task(
    val id: String,
    val title: String,
    val description: String,
    val isUrgent: Boolean,
    val isImportant: Boolean,
    val createdTimestamp: Long,
    val lastModified: Long,
    val source: TaskSource,
    val relatedContact: String?,
    val assignedTo: String?,
    val isSynced: Boolean,
    val cloudId: String?,
    val isCompleted: Boolean,
    val completedTimestamp: Long?,
) {
    /**
     * Get the quadrant this task belongs to based on Eisenhower Matrix.
     * Q1 = Urgent & Important (Do First)
     * Q2 = Important & Not Urgent (Schedule)
     * Q3 = Urgent & Not Important (Delegate)
     * Q4 = Not Urgent & Not Important (Delete)
     */
    fun getQuadrant(): Quadrant {
        return when {
            isUrgent && isImportant -> Quadrant.Q1_DO_FIRST
            !isUrgent && isImportant -> Quadrant.Q2_SCHEDULE
            isUrgent && !isImportant -> Quadrant.Q3_DELEGATE
            else -> Quadrant.Q4_DELETE
        }
    }

    /**
     * Check if task needs sync to cloud.
     */
    fun needsSync(): Boolean {
        return !isSynced && cloudId == null
    }
}

enum class TaskSource {
    MANUAL,         // Created manually in-app
    CALL_ENDED,     // Created after a phone call ended
    TEXT_SELECTION, // Created via text selection menu
    SHARE_INTENT    // Created via share sheet
}

enum class Quadrant(val displayName: String, val description: String) {
    Q1_DO_FIRST("Do First", "Urgent & Important"),
    Q2_SCHEDULE("Schedule", "Important, Not Urgent"),
    Q3_DELEGATE("Delegate", "Urgent, Not Important"),
    Q4_DELETE("Delete", "Not Urgent, Not Important");

    companion object {
        fun fromNumber(number: Int): Quadrant {
            return when (number) {
                1 -> Q1_DO_FIRST
                2 -> Q2_SCHEDULE
                3 -> Q3_DELEGATE
                4 -> Q4_DELETE
                else -> throw IllegalArgumentException("Invalid quadrant number: $number")
            }
        }
    }

    fun toNumber(): Int {
        return when (this) {
            Q1_DO_FIRST -> 1
            Q2_SCHEDULE -> 2
            Q3_DELEGATE -> 3
            Q4_DELETE -> 4
        }
    }
}
