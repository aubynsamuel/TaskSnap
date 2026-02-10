package com.veivek.taskSnap.taskActivities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.veivek.taskSnap.domain.model.Task
import com.veivek.taskSnap.domain.model.TaskSource
import com.veivek.taskSnap.domain.repository.TaskRepository
import com.veivek.taskSnap.presentation.components.EnhancedAddTaskDialog
import com.veivek.taskSnap.presentation.theme.TaskSnapTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject


/**
 * Activity that handles ACTION_PROCESS_TEXT and ACTION_SEND intents.
 * This is the core of Feature 2: Text Selection & Share Integration
 *
 * Technical Notes:
 * - ACTION_PROCESS_TEXT: Appears in the text selection menu (Android 6+)
 * - ACTION_SEND: Appears in the share sheet
 * - Uses a transparent theme to show as a dialog
 */
@AndroidEntryPoint
class ProcessTextActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ProcessTextActivity"
    }

    @Inject
    lateinit var repository: TaskRepository

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
                    prefillTitle = "",
                    prefillDescription = text,
                    onDismiss = { finish() },
                    onSave = { title, description, isUrgent, isImportant ->
                        lifecycleScope.launch {
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
                                    relatedContactName = null,
                                    relatedContactPhoneNumber = null,
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