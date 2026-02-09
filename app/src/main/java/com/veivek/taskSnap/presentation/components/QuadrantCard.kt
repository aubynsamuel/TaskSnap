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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.veivek.taskSnap.domain.model.Quadrant
import com.veivek.taskSnap.presentation.theme.Q1Container
import com.veivek.taskSnap.presentation.theme.Q1Primary
import com.veivek.taskSnap.presentation.theme.Q1Secondary
import com.veivek.taskSnap.presentation.theme.Q2Container
import com.veivek.taskSnap.presentation.theme.Q2Primary
import com.veivek.taskSnap.presentation.theme.Q2Secondary
import com.veivek.taskSnap.presentation.theme.Q3Container
import com.veivek.taskSnap.presentation.theme.Q3Primary
import com.veivek.taskSnap.presentation.theme.Q3Secondary
import com.veivek.taskSnap.presentation.theme.Q4Container
import com.veivek.taskSnap.presentation.theme.Q4Primary
import com.veivek.taskSnap.presentation.theme.Q4Secondary

@Composable
fun QuadrantCard(
    quadrant: Quadrant,
    taskCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val (primaryColor, _, containerColor) = when (quadrant) {
        Quadrant.Q1_DO_FIRST -> Triple(Q1Primary, Q1Secondary, Q1Container)
        Quadrant.Q2_SCHEDULE -> Triple(Q2Primary, Q2Secondary, Q2Container)
        Quadrant.Q3_DELEGATE -> Triple(Q3Primary, Q3Secondary, Q3Container)
        Quadrant.Q4_DELETE -> Triple(Q4Primary, Q4Secondary, Q4Container)
    }

    val emoji = when (quadrant) {
        Quadrant.Q1_DO_FIRST -> "🔥"
        Quadrant.Q2_SCHEDULE -> "📅"
        Quadrant.Q3_DELEGATE -> "👥"
        Quadrant.Q4_DELETE -> "🗑️"
    }

    Card(
        modifier = modifier
            .fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor.compositeOver(primaryColor)
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
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.displaySmall
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
                    color = primaryColor.copy(alpha = 0.7f)
                )
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
