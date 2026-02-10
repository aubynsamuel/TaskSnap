package com.veivek.taskSnap.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veivek.taskSnap.domain.model.TaskSource

/**
 * Source badge showing where the task came from.
 */
@Composable
fun SourceBadge(source: TaskSource, color: Color) {
    val (emoji, label) = when (source) {
        TaskSource.MANUAL -> "✏️" to "Manual"
        TaskSource.CALL_ENDED -> "📞" to "Call"
        TaskSource.TEXT_SELECTION -> "📝" to "Text"
        TaskSource.SHARE_INTENT -> "📤" to "Share"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = "$emoji $label",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontWeight = FontWeight.Medium
        )
    }
}