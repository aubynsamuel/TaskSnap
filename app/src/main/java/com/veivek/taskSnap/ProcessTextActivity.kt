package com.veivek.taskSnap

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.veivek.taskSnap.data.TaskRepository
import com.veivek.taskSnap.data.TaskSource
import com.veivek.taskSnap.ui.theme.AllDayTheme

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
            AllDayTheme {
                QuickTaskDialog(
                    initialText = text,
                    source = source,
                    onDismiss = { finish() },
                    onSave = { title, description ->
                        TaskRepository.addTask(
                            title = title,
                            description = description,
                            source = source
                        )
                        finish()
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

@Composable
fun QuickTaskDialog(
    initialText: String,
    source: TaskSource,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String) -> Unit
) {
    var title by remember { mutableStateOf(initialText.take(100)) } // Limit title length
    var description by remember { 
        mutableStateOf(if (initialText.length > 100) initialText else "") 
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = when (source) {
                        TaskSource.TEXT_SELECTION -> "📝 Create Task from Selection"
                        TaskSource.SHARE_INTENT -> "📤 Create Task from Share"
                        else -> "➕ New Task"
                    },
                    style = MaterialTheme.typography.titleLarge
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    maxLines = 5
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(title, description) },
                        enabled = title.isNotBlank()
                    ) {
                        Text("Save Task")
                    }
                }
            }
        }
    }
}
