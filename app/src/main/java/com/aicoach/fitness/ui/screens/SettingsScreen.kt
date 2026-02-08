package com.aicoach.fitness.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aicoach.fitness.R
import com.aicoach.fitness.ui.components.*
import com.aicoach.fitness.ui.theme.*
import com.aicoach.fitness.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearKeyDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    GradientBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.settings),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Slate300
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Slate900.copy(alpha = 0.8f)
                    )
                )
            },
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // API Key Status
                AICard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = if (uiState.hasApiKey) SuccessGreen else ErrorRed,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "OpenRouter API Key",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (uiState.hasApiKey) "Configured" else "Not configured",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (uiState.hasApiKey) SuccessGreen else ErrorRed
                                    )
                                }
                            }
                        }

                        if (uiState.hasApiKey) {
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = { showClearKeyDialog = true },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = stringResource(R.string.clear_api_key),
                                    color = ErrorRed
                                )
                            }
                        }
                    }
                }

                // Voice Settings
                AICard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = ElectricBlue400,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.voice_commands),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Say \"Task complete\" to mark exercises done",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate400
                                    )
                                }
                            }
                        }
                    }
                }

                // User Profile
                if (uiState.userGoal.isNotEmpty()) {
                    AICard {
                        Column {
                            Text(
                                text = "Your Profile",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ProfileItem(
                                icon = Icons.Default.FitnessCenter,
                                label = "Fitness Goal",
                                value = uiState.userGoal
                            )
                            if (uiState.userInjuries.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                ProfileItem(
                                    icon = Icons.Default.Healing,
                                    label = "Injuries/Limitations",
                                    value = uiState.userInjuries
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Danger Zone
                AICard {
                    Column {
                        Text(
                            text = "Danger Zone",
                            style = MaterialTheme.typography.titleMedium,
                            color = ErrorRed
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AISecondaryButton(
                            text = "Clear All Data",
                            onClick = { showClearDataDialog = true }
                        )
                    }
                }
            }
        }
    }

    // Clear API Key Dialog
    if (showClearKeyDialog) {
        AlertDialog(
            onDismissRequest = { showClearKeyDialog = false },
            containerColor = Slate800,
            title = { Text("Clear API Key?", color = Color.White) },
            text = {
                Text(
                    "This will remove your OpenRouter API key. AI features will be disabled until you add a new key.",
                    color = Slate300
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearApiKey()
                        showClearKeyDialog = false
                    }
                ) {
                    Text("Clear", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearKeyDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            }
        )
    }

    // Clear All Data Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            containerColor = Slate800,
            title = { Text("Clear All Data?", color = Color.White) },
            text = {
                Text(
                    "This will permanently delete your workout plan, progress, and settings. This action cannot be undone.",
                    color = Slate300
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataDialog = false
                    }
                ) {
                    Text("Clear All", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            }
        )
    }
}

@Composable
private fun ProfileItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Slate400,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Slate500
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = Slate300
            )
        }
    }
}
