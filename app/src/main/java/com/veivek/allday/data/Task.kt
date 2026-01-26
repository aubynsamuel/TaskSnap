package com.veivek.allday.data

import java.util.UUID

/**
 * Simple Task data model for the MVP.
 * Uses in-memory storage for simplicity.
 */
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val source: TaskSource = TaskSource.MANUAL,
    val createdAt: Long = System.currentTimeMillis()
)

enum class TaskSource {
    MANUAL,         // Created manually in-app
    CALL_ENDED,     // Created after a phone call ended
    TEXT_SELECTION, // Created via text selection menu
    SHARE_INTENT    // Created via share sheet
}
