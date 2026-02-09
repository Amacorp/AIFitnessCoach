package com.aicoach.fitness.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background  // ← اضافه شد
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aicoach.fitness.R
import com.aicoach.fitness.ui.components.*
import com.aicoach.fitness.ui.theme.*
import com.aicoach.fitness.ui.viewmodel.OnboardingViewModel
import com.aicoach.fitness.utils.VoiceAssistant

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startOnboarding()
    }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onOnboardingComplete()
        }
    }

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(0.3f))

                // App Logo
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(ElectricBlue500.copy(alpha = 0.3f), ElectricBlue600.copy(alpha = 0.1f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = "AI Fitness Coach",
                        tint = ElectricBlue400,
                        modifier = Modifier.size(60.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Title
                Text(
                    text = "AI Fitness Coach",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your personal AI-powered trainer",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Slate400,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Voice Interaction Area
                AICard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Voice Indicator
                        VoiceIndicator(
                            isListening = voiceState is VoiceAssistant.VoiceState.Listening,
                            isSpeaking = voiceState is VoiceAssistant.VoiceState.Speaking
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Status Text
                        Text(
                            text = when (voiceState) {
                                is VoiceAssistant.VoiceState.Listening -> stringResource(R.string.listening)
                                is VoiceAssistant.VoiceState.Speaking -> stringResource(R.string.speaking)
                                is VoiceAssistant.VoiceState.Processing -> "Processing..."
                                else -> stringResource(R.string.tap_to_speak)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = when (voiceState) {
                                is VoiceAssistant.VoiceState.Listening -> Cyan400
                                is VoiceAssistant.VoiceState.Speaking -> ElectricBlue400
                                else -> Slate400
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Recognized Text Display
                        AnimatedVisibility(visible = uiState.recognizedText.isNotEmpty()) {
                            Column {
                                Text(
                                    text = "\"${uiState.recognizedText}\"",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Slate300,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        // Confirmation Display
                        AnimatedVisibility(visible = uiState.confirmationText.isNotEmpty()) {
                            Text(
                                text = uiState.confirmationText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SuccessGreen,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Manual Input Option
                if (voiceState is VoiceAssistant.VoiceState.Error) {
                    AISecondaryButton(
                        text = "Type Instead",
                        onClick = { viewModel.showManualInput() }
                    )
                }

                Spacer(modifier = Modifier.weight(0.5f))
            }

            // Loading Overlay
            AnimatedVisibility(
                visible = uiState.isGeneratingPlan,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LoadingOverlay(
                    message = stringResource(R.string.generating_plan),
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Manual Input Dialog
            if (uiState.showManualInput) {
                ManualInputDialog(
                    onDismiss = { viewModel.hideManualInput() },
                    onSubmit = { goal, injuries ->
                        viewModel.submitManualInput(goal, injuries)
                    }
                )
            }
        }
    }
}

@Composable
private fun ManualInputDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var goal by remember { mutableStateOf("") }
    var injuries by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate800,
        title = {
            Text(
                text = "Enter Your Information",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AITextField(
                    value = goal,
                    onValueChange = { goal = it },
                    label = "Fitness Goal",
                    placeholder = "e.g., Build muscle, Lose weight"
                )
                AITextField(
                    value = injuries,
                    onValueChange = { injuries = it },
                    label = "Injuries (if any)",
                    placeholder = "e.g., Knee pain, Back issues"
                )
            }
        },
        confirmButton = {
            AIButton(
                text = "Submit",
                onClick = { onSubmit(goal, injuries) },
                enabled = goal.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate400)
            }
        }
    )
}