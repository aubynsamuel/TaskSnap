package com.veivek.taskSnap.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.veivek.taskSnap.domain.model.Quadrant
import com.veivek.taskSnap.domain.model.Task
import com.veivek.taskSnap.presentation.utils.containerColor
import com.veivek.taskSnap.presentation.utils.icon
import com.veivek.taskSnap.presentation.utils.primaryColor

@Composable
fun QuadrantCard(
    quadrant: Quadrant,
    taskCount: Int,
    previewTasks: List<Task>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onTaskClick: (Task) -> Unit = {},
) {
    val primaryColor = quadrant.primaryColor
    val containerColor = quadrant.containerColor
    val icon = quadrant.icon

    Card(
        modifier = modifier
            .fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = Color.Black.copy(alpha = 0.8f),
        )
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = quadrant.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )

                Text(
                    text = quadrant.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = primaryColor.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Task Previews
                if (previewTasks.isNotEmpty()) {
                    previewTasks.forEach { task ->
                        Text(
                            text = "• ${task.title}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { onTaskClick(task) }
                                .padding(vertical = 4.dp)
                        )
                    }

                    if (taskCount > previewTasks.size) {
                        Text(
                            text = "+ ${taskCount - previewTasks.size} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = "No tasks",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Task count badge
            AnimatedVisibility(
                visible = taskCount > 0,
                enter = scaleIn(spring(stiffness = Spring.StiffnessHigh)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.align(Alignment.End),
                    shape = CircleShape,
                    color = primaryColor,
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = taskCount.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
