package com.veivek.taskSnap.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for TaskSnap.
 * Using Navigation 3 library with sealed class pattern.
 */
sealed class AppRoutes : NavKey {

    @Serializable
    object EisenhowerMatrix : NavKey

    @Serializable
    data class QuadrantDetail(
        val quadrant: Int, // 1=Q1, 2=Q2, 3=Q3, 4=Q4
    ) : NavKey

    @Serializable
    data class TaskDetail(
        val taskId: String,
    ) : NavKey

    @Serializable
    data class CreateTask(
        val prefillTitle: String? = null,
        val prefillDescription: String? = null,
        val source: String? = null, // CALL_ENDED, TEXT_SELECTION, etc.
        val relatedContact: String? = null,
    ) : NavKey

    @Serializable
    object Settings : NavKey

    @Serializable
    object CompletedTasks : NavKey
}
