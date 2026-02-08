package com.aicoach.fitness.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aicoach.fitness.R
import com.aicoach.fitness.domain.model.Exercise
import com.aicoach.fitness.domain.model.WorkoutDay
import com.aicoach.fitness.ui.components.*
import com.aicoach.fitness.ui.theme.*
import com.aicoach.fitness.ui.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    onStartWorkout: (dayNumber: Int, exerciseId: String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadWorkoutPlan()
    }

    GradientBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.dashboard_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    uiState.isLoading -> {
                        LoadingOverlay(
                            message = "Loading your plan...",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    uiState.workoutPlan == null -> {
                        EmptyPlanView(
                            onGeneratePlan = { viewModel.generateNewPlan() },
                            isGenerating = uiState.isGenerating
                        )
                    }
                    else -> {
                        WorkoutPlanContent(
                            days = uiState.workoutPlan!!.days,
                            completedExercises = uiState.completedExercises,
                            selectedDay = uiState.selectedDay,
                            onDaySelected = { viewModel.selectDay(it) },
                            onExerciseComplete = { day, exercise, completed ->
                                viewModel.markExerciseComplete(day, exercise, completed)
                            },
                            onStartExercise = { day, exercise ->
                                onStartWorkout(day, exercise)
                            },
                            voiceState = voiceState,
                            onVoiceCommand = { viewModel.toggleVoiceListening() }
                        )
                    }
                }

                // Voice Command Indicator
                AnimatedVisibility(
                    visible = voiceState is com.aicoach.fitness.utils.VoiceAssistant.VoiceState.Listening,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Cyan400.copy(alpha = 0.9f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.say_task_complete),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPlanView(
    onGeneratePlan: () -> Unit,
    isGenerating: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            tint = Slate500,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Workout Plan Yet",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Generate your personalized 7-day workout plan",
            style = MaterialTheme.typography.bodyLarge,
            color = Slate400,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        AIButton(
            text = "Generate Plan",
            onClick = onGeneratePlan,
            isLoading = isGenerating,
            icon = {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        )
    }
}

@Composable
private fun WorkoutPlanContent(
    days: List<WorkoutDay>,
    completedExercises: Map<String, Boolean>,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    onExerciseComplete: (Int, String, Boolean) -> Unit,
    onStartExercise: (Int, String) -> Unit,
    voiceState: com.aicoach.fitness.utils.VoiceAssistant.VoiceState,
    onVoiceCommand: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Day Selector
        DaySelector(
            days = days,
            selectedDay = selectedDay,
            onDaySelected = onDaySelected
        )

        // Exercises List
        val currentDay = days.find { it.dayNumber == selectedDay }
        currentDay?.let { day ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(day.exercises) { exercise ->
                    val isCompleted = completedExercises["${day.dayNumber}_${exercise.id}"] == true
                    ExerciseCard(
                        exercise = exercise,
                        isCompleted = isCompleted,
                        onComplete = { completed ->
                            onExerciseComplete(day.dayNumber, exercise.id, completed)
                        },
                        onStart = {
                            onStartExercise(day.dayNumber, exercise.id)
                        }
                    )
                }

                // Voice Command Hint
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Slate800)
                            .clickable { onVoiceCommand() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (voiceState is com.aicoach.fitness.utils.VoiceAssistant.VoiceState.Listening)
                                Icons.Default.Mic else Icons.Default.MicNone,
                            contentDescription = "Voice",
                            tint = if (voiceState is com.aicoach.fitness.utils.VoiceAssistant.VoiceState.Listening)
                                Cyan400 else Slate400
                        )
                        Text(
                            text = stringResource(R.string.say_task_complete),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate400
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DaySelector(
    days: List<WorkoutDay>,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { day ->
            val isSelected = day.dayNumber == selectedDay
            val completedCount = day.exercises.count { false } // Simplified
            val isCompleted = completedCount == day.exercises.size && day.exercises.isNotEmpty()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            isSelected -> ElectricBlue500
                            isCompleted -> SuccessGreen.copy(alpha = 0.3f)
                            else -> Slate800
                        }
                    )
                    .clickable { onDaySelected(day.dayNumber) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.day_format, day.dayNumber),
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            isSelected -> Color.White
                            isCompleted -> SuccessGreen
                            else -> Slate400
                        },
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = SuccessGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: Exercise,
    isCompleted: Boolean,
    onComplete: (Boolean) -> Unit,
    onStart: () -> Unit
) {
    AICard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Checkbox
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCompleted) SuccessGreen else Slate600)
                        .clickable { onComplete(!isCompleted) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Exercise Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isCompleted) SuccessGreen else Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = exercise.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        maxLines = 2
                    )
                }

                // Start Button
                IconButton(
                    onClick = onStart,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElectricBlue500)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExerciseStat(
                    icon = Icons.Default.Repeat,
                    value = "${exercise.sets} sets",
                    label = "Sets"
                )
                ExerciseStat(
                    icon = Icons.Default.FitnessCenter,
                    value = "${exercise.reps} reps",
                    label = "Reps"
                )
                if (exercise.targetMuscleGroups.isNotEmpty()) {
                    Text(
                        text = exercise.targetMuscleGroups.take(2).joinToString(", "),
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricBlue400,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElectricBlue500.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Slate400,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = Slate300
        )
    }
}
