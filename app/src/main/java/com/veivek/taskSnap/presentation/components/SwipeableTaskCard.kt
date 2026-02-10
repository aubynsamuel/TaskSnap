package com.veivek.taskSnap.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veivek.taskSnap.domain.model.Task
import com.veivek.taskSnap.presentation.theme.ErrorLight
import com.veivek.taskSnap.presentation.theme.SuccessLight

/**
 * Task card with swipe-to-complete (right) and swipe-to-delete (left).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTaskCard(
    task: Task,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    quadrantColor: Color,
    onClick: () -> Unit = {},
    startToEndLabel: String = "Complete",
    startToEndIcon: ImageVector = Icons.Default.Check,
    startToEndColor: Color = SuccessLight,
    endToStartLabel: String = "Delete",
    endToStartIcon: ImageVector = Icons.Default.Delete,
    endToStartColor: Color = ErrorLight,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swipe left to delete
                    onDelete()
                    true
                }

                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swipe right to complete
                    onComplete()
                    true
                }

                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            // Use targetValue OR direction (if dragging) to determine color/content
            val isStartToEnd = direction == SwipeToDismissBoxValue.StartToEnd
            val isEndToStart = direction == SwipeToDismissBoxValue.EndToStart

            val targetColor = when {
                isStartToEnd -> startToEndColor
                isEndToStart -> endToStartColor
                else -> Color.Transparent
            }

            val color by animateColorAsState(
                targetValue = targetColor,
                label = "swipe_bg",
                animationSpec = spring(stiffness = Spring.StiffnessHigh)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp),
                contentAlignment = when {
                    isStartToEnd -> Alignment.CenterStart
                    isEndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
            ) {
                when {
                    isStartToEnd -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                startToEndIcon,
                                contentDescription = startToEndLabel,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = startToEndLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    isEndToStart -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = endToStartLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                endToStartIcon,
                                contentDescription = endToStartLabel,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    ) {
        TaskCard(
            task = task,
            quadrantColor = quadrantColor,
            onClick = onClick
        )
    }
}