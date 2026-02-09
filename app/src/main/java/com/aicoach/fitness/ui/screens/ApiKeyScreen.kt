package com.aicoach.fitness.ui.screens

import androidx.compose.foundation.background  // ← اضافه شد
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key  // ← اضافه شد
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aicoach.fitness.R
import com.aicoach.fitness.ui.components.*
import com.aicoach.fitness.ui.theme.*
import com.aicoach.fitness.ui.viewmodel.ApiKeyViewModel

@Composable
fun ApiKeyScreen(
    onApiKeySaved: () -> Unit,
    onSkip: (() -> Unit)? = null,
    viewModel: ApiKeyViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var apiKey by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onApiKeySaved()
        }
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(ElectricBlue500.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = "API Key",
                    tint = ElectricBlue400,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = stringResource(R.string.api_key_title),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = stringResource(R.string.api_key_description),
                style = MaterialTheme.typography.bodyLarge,
                color = Slate400,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // API Key Input
            AITextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    viewModel.clearError()
                },
                label = "API Key",
                placeholder = stringResource(R.string.api_key_hint),
                isError = uiState.error != null,
                supportingText = uiState.error ?: "Get your key from openrouter.ai",
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide" else "Show",
                            tint = Slate400
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            AIButton(
                text = stringResource(R.string.save_key),
                onClick = { viewModel.saveApiKey(apiKey) },
                enabled = apiKey.isNotBlank() && !uiState.isLoading,
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Skip Button
            if (onSkip != null) {
                AISecondaryButton(
                    text = stringResource(R.string.skip_for_now),
                    onClick = onSkip
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Security Note
            Text(
                text = "Your API key is encrypted and stored locally on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500,
                textAlign = TextAlign.Center
            )
        }
    }
}