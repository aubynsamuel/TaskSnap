package com.veivek.allday.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory repository for tasks.
 * For MVP testing - no Room database needed.
 */
object TaskRepository {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    fun addTask(task: Task) {
        _tasks.value = _tasks.value + task
    }

    fun addTask(title: String, description: String = "", source: TaskSource = TaskSource.MANUAL) {
        addTask(Task(title = title, description = description, source = source))
    }

    fun removeTask(taskId: String) {
        _tasks.value = _tasks.value.filter { it.id != taskId }
    }

    fun clearAll() {
        _tasks.value = emptyList()
    }
}
