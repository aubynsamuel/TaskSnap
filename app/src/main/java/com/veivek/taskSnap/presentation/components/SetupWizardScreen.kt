package com.veivek.taskSnap.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardScreen(onComplete: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("TaskSnap Setup") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "📞",
                style = MaterialTheme.typography.displayLarge
            )

            Text(
                text = "Welcome to TaskSnap",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "To work like Truecaller, TaskSnap needs a few permissions:",
                style = MaterialTheme.typography.bodyLarge
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PermissionItem(
                        icon = "📞",
                        title = "Phone",
                        description = "Detect when calls end"
                    )
                    PermissionItem(
                        icon = "👤",
                        title = "Contacts",
                        description = "Show who you called"
                    )
                    PermissionItem(
                        icon = "🔔",
                        title = "Notifications",
                        description = "Background monitoring"
                    )
                    PermissionItem(
                        icon = "💬",
                        title = "Display over apps",
                        description = "Show popup after calls"
                    )
                    PermissionItem(
                        icon = "🔋",
                        title = "Battery optimization",
                        description = "Reliable background operation"
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "The app will start automatically after setup",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
