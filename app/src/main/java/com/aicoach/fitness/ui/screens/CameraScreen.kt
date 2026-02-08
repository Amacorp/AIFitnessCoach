package com.aicoach.fitness.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.aicoach.fitness.R
import com.aicoach.fitness.domain.model.BodyLandmark
import com.aicoach.fitness.domain.model.PoseData
import com.aicoach.fitness.ui.components.FeedbackBubble
import com.aicoach.fitness.ui.theme.*
import com.aicoach.fitness.ui.viewmodel.CameraViewModel
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun CameraScreen(
    exerciseName: String,
    onNavigateBack: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    val cameraPermission = Manifest.permission.CAMERA
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, cameraPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(Unit) {
        viewModel.setExerciseName(exerciseName)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.shutdown()
        }
    }

    if (!hasPermission) {
        PermissionRequestScreen(
            permission = cameraPermission,
            rationale = stringResource(R.string.camera_permission_rationale),
            onPermissionGranted = { hasPermission = true }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        CameraPreview(
            onImageAnalyzed = { imageProxy ->
                viewModel.analyzeFrame(imageProxy)
            }
        )

        // Pose Overlay
        uiState.poseData?.let { poseData ->
            PoseOverlay(
                poseData = poseData,
                modifier = Modifier.fillMaxSize()
            )
        }

        // UI Overlay
        CameraOverlay(
            exerciseName = exerciseName,
            timer = uiState.timer,
            repCount = uiState.repCount,
            feedback = uiState.feedback,
            isFeedbackPositive = uiState.isFeedbackPositive,
            isAnalyzing = uiState.isAnalyzing,
            onNavigateBack = onNavigateBack,
            onToggleRecording = { viewModel.toggleRecording() },
            isRecording = uiState.isRecording
        )
    }
}

@Composable
private fun CameraPreview(
    onImageAnalyzed: (ImageProxy) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also {
                    it.setAnalyzer(executor) { imageProxy ->
                        onImageAnalyzed(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProviderFuture.get().unbindAll()
                cameraProviderFuture.get().bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun PoseOverlay(
    poseData: PoseData,
    modifier: Modifier = Modifier
) {
    val landmarks = poseData.landmarks

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Define connections for skeletal wireframe
        val connections = listOf(
            // Torso
            Pair("left_shoulder", "right_shoulder"),
            Pair("left_shoulder", "left_hip"),
            Pair("right_shoulder", "right_hip"),
            Pair("left_hip", "right_hip"),
            // Left arm
            Pair("left_shoulder", "left_elbow"),
            Pair("left_elbow", "left_wrist"),
            // Right arm
            Pair("right_shoulder", "right_elbow"),
            Pair("right_elbow", "right_wrist"),
            // Left leg
            Pair("left_hip", "left_knee"),
            Pair("left_knee", "left_ankle"),
            // Right leg
            Pair("right_hip", "right_knee"),
            Pair("right_knee", "right_ankle"),
            // Head connections
            Pair("left_shoulder", "left_ear"),
            Pair("right_shoulder", "right_ear"),
            Pair("left_ear", "nose"),
            Pair("right_ear", "nose")
        )

        val landmarkMap = landmarks.associateBy { it.name }

        // Draw connections
        connections.forEach { (startName, endName) ->
            val start = landmarkMap[startName]
            val end = landmarkMap[endName]

            if (start != null && end != null && start.visibility > 0.5f && end.visibility > 0.5f) {
                drawLine(
                    color = Cyan400.copy(alpha = 0.8f),
                    start = Offset(start.x * width, start.y * height),
                    end = Offset(end.x * width, end.y * height),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Draw landmarks
        landmarks.forEach { landmark ->
            if (landmark.visibility > 0.5f) {
                val center = Offset(landmark.x * width, landmark.y * height)

                // Outer glow
                drawCircle(
                    color = ElectricBlue500.copy(alpha = 0.3f),
                    radius = 12f,
                    center = center
                )

                // Inner point
                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = center
                )

                // Key joints are larger
                val keyJoints = listOf("left_shoulder", "right_shoulder", "left_hip", "right_hip", "left_knee", "right_knee")
                if (landmark.name in keyJoints) {
                    drawCircle(
                        color = Cyan400,
                        radius = 8f,
                        center = center,
                        style = Stroke(width = 2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraOverlay(
    exerciseName: String,
    timer: Int,
    repCount: Int,
    feedback: String,
    isFeedbackPositive: Boolean,
    isAnalyzing: Boolean,
    onNavigateBack: () -> Unit,
    onToggleRecording: () -> Unit,
    isRecording: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Exercise Name
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Recording Indicator
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isRecording) ErrorRed.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) ErrorRed else Color.Gray)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(
                value = repCount.toString(),
                label = stringResource(R.string.reps),
                icon = Icons.Default.FitnessCenter
            )
            StatCard(
                value = formatTime(timer),
                label = stringResource(R.string.timer),
                icon = Icons.Default.Timer
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Feedback
        AnimatedVisibility(
            visible = feedback.isNotEmpty(),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            FeedbackBubble(
                feedback = feedback,
                isPositive = isFeedbackPositive,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Analyzing Indicator
        AnimatedVisibility(visible = isAnalyzing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = ElectricBlue400,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.analyzing_pose),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ElectricBlue400
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Toggle Recording Button
            IconButton(
                onClick = onToggleRecording,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) ErrorRed else SuccessGreen)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isRecording) "Stop" else "Start",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = ElectricBlue400,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Slate400
            )
        }
    }
}

@Composable
private fun PermissionRequestScreen(
    permission: String,
    rationale: String,
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        }
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                tint = ElectricBlue400,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.permission_required),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = rationale,
                style = MaterialTheme.typography.bodyLarge,
                color = Slate400,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            AIButton(
                text = stringResource(R.string.grant_permission),
                onClick = { launcher.launch(permission) }
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
