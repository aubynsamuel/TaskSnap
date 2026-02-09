package com.veivek.taskSnap

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.veivek.taskSnap.data.local.TaskDatabase
import com.veivek.taskSnap.data.repository.TaskRepositoryImpl
import com.veivek.taskSnap.domain.model.Task
import com.veivek.taskSnap.domain.model.TaskSource
import com.veivek.taskSnap.domain.repository.TaskRepository
import com.veivek.taskSnap.presentation.components.EnhancedAddTaskDialog
import com.veivek.taskSnap.ui.theme.TaskSnapTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Activity that handles ACTION_PROCESS_TEXT and ACTION_SEND intents.
 * This is the core of Feature 2: Text Selection & Share Integration
 *
 * Technical Notes:
 * - ACTION_PROCESS_TEXT: Appears in the text selection menu (Android 6+)
 * - ACTION_SEND: Appears in the share sheet
 * - Uses a transparent theme to show as a dialog
 */
class ProcessTextActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ProcessTextActivity"
    }

    private val coroutineScope = CoroutineScope(SupervisorJob())

    val database = TaskDatabase.getInstance(this)
    val repository: TaskRepository = TaskRepositoryImpl(database.taskDao())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val (text, source) = extractTextFromIntent(intent)

        if (text.isNullOrBlank()) {
            Log.w(TAG, "No text received, finishing")
            finish()
            return
        }

        Log.d(TAG, "Received text: $text from source: $source")

        setContent {
            TaskSnapTheme {
                EnhancedAddTaskDialog(
                    prefillTitle = text,
                    onDismiss = { finish() },
                    onSave = { title, description, isUrgent, isImportant ->
                        coroutineScope.launch {
                            repository.addTask(
                                Task(
                                    id = UUID.randomUUID().toString(),
                                    title = title,
                                    description = description,
                                    isUrgent = isUrgent,
                                    isImportant = isImportant,
                                    createdTimestamp = System.currentTimeMillis(),
                                    lastModified = System.currentTimeMillis(),
                                    source = TaskSource.SHARE_INTENT,
                                    relatedContact = null,
                                    assignedTo = null,
                                    isSynced = false,
                                    cloudId = null,
                                    isCompleted = false,
                                    completedTimestamp = null,
                                )
                            )
                            finish()
                        }
                    }
                )
            }
        }
    }

    private fun extractTextFromIntent(intent: Intent): Pair<String?, TaskSource> {
        return when (intent.action) {
            // Text selection menu
            Intent.ACTION_PROCESS_TEXT -> {
                val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
                Pair(text, TaskSource.TEXT_SELECTION)
            }
            // Share sheet
            Intent.ACTION_SEND -> {
                val text = when {
                    intent.type?.startsWith("text/") == true -> {
                        intent.getStringExtra(Intent.EXTRA_TEXT)
                    }

                    else -> null
                }
                Pair(text, TaskSource.SHARE_INTENT)
            }

            else -> Pair(null, TaskSource.MANUAL)
        }
    }
}

