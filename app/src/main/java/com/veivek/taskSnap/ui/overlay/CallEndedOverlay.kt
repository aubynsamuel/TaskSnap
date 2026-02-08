package com.veivek.taskSnap.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.veivek.taskSnap.data.TaskRepository
import com.veivek.taskSnap.data.TaskSource
import com.veivek.taskSnap.ui.theme.TaskSnapTheme

/**
 * Floating overlay window that appears after a call ends.
 * This is the Truecaller-style popup that appears on top of everything.
 */
class CallEndedOverlay(private val context: Context) {

    companion object {
        private const val TAG = "CallEndedOverlay"
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null
    private var isShowing = false

    /**
     * Show the overlay with call information.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun show(phoneNumber: String?, contactName: String?, isIncoming: Boolean) {
        if (isShowing) {
            Log.w(TAG, "Overlay already showing, dismissing first")
            dismiss()
        }

        try {
            val displayName = contactName ?: phoneNumber ?: "Unknown"
            val callType = if (isIncoming) "incoming" else "outgoing"

            // Create the Compose view
            val view = ComposeView(context).apply {
                // Set up lifecycle for Compose
                val lifecycleOwner = MyLifecycleOwner()
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(lifecycleOwner)
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)

                setContent {
                    TaskSnapTheme {
                        CallEndedOverlayContent(
                            displayName = displayName,
                            phoneNumber = phoneNumber,
                            callType = callType,
                            onSave = { title, description ->
                                TaskRepository.addTask(
                                    title = title,
                                    description = description,
                                    source = TaskSource.CALL_ENDED
                                )
                                dismiss()
                            },
                            onDismiss = {
                                dismiss()
                            }
                        )
                    }
                }
            }

            // Set up window parameters
            val params = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.CENTER
            }

            windowManager.addView(view, params)
            overlayView = view
            isShowing = true

            Log.d(TAG, "Overlay shown for $displayName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
        }
    }

    /**
     * Dismiss the overlay.
     */
    fun dismiss() {
        try {
            overlayView?.let {
                windowManager.removeView(it)
                overlayView = null
                isShowing = false
                Log.d(TAG, "Overlay dismissed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dismiss overlay", e)
        }
    }

    /**
     * Simple lifecycle owner for Compose in overlay.
     */
    private class MyLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner,
        androidx.lifecycle.ViewModelStoreOwner {
        private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController: SavedStateRegistryController =
            SavedStateRegistryController.create(this)
        private val _viewModelStore = ViewModelStore()

        init {
            // Restore needs to happen before setting state to CREATED/RESUMED
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateRegistryController.savedStateRegistry

        override val viewModelStore: ViewModelStore
            get() = _viewModelStore
    }
}

@Composable
fun CallEndedOverlayContent(
    displayName: String,
    phoneNumber: String?,
    callType: String,
    onSave: (title: String, description: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember {
        mutableStateOf("Follow up: $displayName")
    }
    var description by remember {
        mutableStateOf("After $callType call with $displayName")
    }

    // Semi-transparent background with card in center
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = "📞 Create task after call?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                // Call info
                Text(
                    text = "$callType call with $displayName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (phoneNumber != null && phoneNumber != displayName) {
                    Text(
                        text = phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Divider()

                // Task title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Task description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    maxLines = 3
                )

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Dismiss")
                    }
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
