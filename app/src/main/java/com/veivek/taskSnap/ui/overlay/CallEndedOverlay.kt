package com.veivek.taskSnap.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
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

    private val coroutineScope = CoroutineScope(SupervisorJob())
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

                val database = TaskDatabase.getInstance(context)
                val repository: TaskRepository = TaskRepositoryImpl(database.taskDao())

                setContent {
                    TaskSnapTheme {
                        EnhancedAddTaskDialog(
                            onDismiss = { dismiss() },
                            prefillTitle = "Follow up: $displayName",
                            prefillDescription = "",
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
                                            source = TaskSource.CALL_ENDED,
                                            relatedContact = contactName,
                                            assignedTo = null,
                                            isSynced = false,
                                            cloudId = null,
                                            isCompleted = false,
                                            completedTimestamp = null,
                                        )
                                    )
                                    dismiss()
                                }
                            },
                        )
                    }
                }
            }

            // Set up window parameters
            val params = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                type =
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
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
        ViewModelStoreOwner {
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
