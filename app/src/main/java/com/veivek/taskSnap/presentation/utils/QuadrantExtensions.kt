package com.veivek.taskSnap.presentation.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

/**
 * Extension properties for Quadrant to centralize UI presentation logic.
 * Keeps the domain model pure while allowing easy access to Icons and Colors.
 */

val Quadrant.icon: ImageVector
    get() = when (this) {
        Quadrant.Q1_DO_FIRST -> Icons.Default.Whatshot
        Quadrant.Q2_SCHEDULE -> Icons.Default.Event
        Quadrant.Q3_DELEGATE -> Icons.Default.Groups
        Quadrant.Q4_DELETE -> Icons.Default.Delete
    }

val Quadrant.primaryColor: Color
    get() = when (this) {
        Quadrant.Q1_DO_FIRST -> Q1Primary
        Quadrant.Q2_SCHEDULE -> Q2Primary
        Quadrant.Q3_DELEGATE -> Q3Primary
        Quadrant.Q4_DELETE -> Q4Primary
    }

val Quadrant.secondaryColor: Color
    get() = when (this) {
        Quadrant.Q1_DO_FIRST -> Q1Secondary
        Quadrant.Q2_SCHEDULE -> Q2Secondary
        Quadrant.Q3_DELEGATE -> Q3Secondary
        Quadrant.Q4_DELETE -> Q4Secondary
    }

val Quadrant.containerColor: Color
    get() = when (this) {
        Quadrant.Q1_DO_FIRST -> Q1Container
        Quadrant.Q2_SCHEDULE -> Q2Container
        Quadrant.Q3_DELEGATE -> Q3Container
        Quadrant.Q4_DELETE -> Q4Container
    }
