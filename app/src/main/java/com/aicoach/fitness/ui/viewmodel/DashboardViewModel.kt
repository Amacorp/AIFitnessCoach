package com.aicoach.fitness.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aicoach.fitness.data.repository.FitnessRepository
import com.aicoach.fitness.domain.model.Result
import com.aicoach.fitness.domain.model.WorkoutPlan
import com.aicoach.fitness.utils.VoiceAssistant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val workoutPlan: WorkoutPlan? = null,
    val selectedDay: Int = 1,
    val completedExercises: Map<String, Boolean> = emptyMap(),
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: FitnessRepository,
    private val voiceAssistant: VoiceAssistant
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _voiceState = MutableStateFlow<VoiceAssistant.VoiceState>(VoiceAssistant.VoiceState.Idle)
    val voiceState: StateFlow<VoiceAssistant.VoiceState> = _voiceState.asStateFlow()

    init {
        viewModelScope.launch {
            voiceAssistant.voiceState.collect { state ->
                _voiceState.value = state
            }
        }
    }

    fun loadWorkoutPlan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val plan = repository.getWorkoutPlan()
            if (plan != null) {
                // Load completion status for each exercise
                val completionMap = mutableMapOf<String, Boolean>()
                plan.days.forEach { day ->
                    day.exercises.forEach { exercise ->
                        val key = "${day.dayNumber}_${exercise.id}"
                        completionMap[key] = repository.isExerciseComplete(day.dayNumber, exercise.id)
                    }
                }

                _uiState.value = _uiState.value.copy(
                    workoutPlan = plan,
                    completedExercises = completionMap,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectDay(dayNumber: Int) {
        _uiState.value = _uiState.value.copy(selectedDay = dayNumber)
    }

    fun markExerciseComplete(dayNumber: Int, exerciseId: String, completed: Boolean) {
        viewModelScope.launch {
            repository.markExerciseComplete(dayNumber, exerciseId, completed)

            val key = "${dayNumber}_$exerciseId"
            val updatedMap = _uiState.value.completedExercises.toMutableMap()
            updatedMap[key] = completed
            _uiState.value = _uiState.value.copy(completedExercises = updatedMap)

            if (completed) {
                voiceAssistant.speak("Exercise completed! Great job!")
            }
        }
    }

    fun generateNewPlan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true)

            val goal = repository.getUserGoal()
            val injuries = repository.getUserInjuries()

            repository.generateWorkoutPlan(goal, injuries).collect { result ->
                when (result) {
                    is Result.Success -> {
                        loadWorkoutPlan()
                        _uiState.value = _uiState.value.copy(isGenerating = false)
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isGenerating = false,
                            error = result.message
                        )
                    }
                    is Result.Loading -> {
                        // Already set
                    }
                }
            }
        }
    }

    fun toggleVoiceListening() {
        if (_voiceState.value is VoiceAssistant.VoiceState.Listening) {
            voiceAssistant.stopListening()
        } else {
            voiceAssistant.startListening { result ->
                when (result) {
                    is VoiceAssistant.RecognitionResult.Success -> {
                        handleVoiceCommand(result.text)
                    }
                    else -> {
                        // Handle other cases
                    }
                }
            }
        }
    }

    private fun handleVoiceCommand(text: String) {
        val command = voiceAssistant.detectCommand(text)
        when (command) {
            VoiceAssistant.VoiceCommand.COMPLETE_TASK -> {
                // Mark current exercise as complete
                val currentDay = _uiState.value.selectedDay
                val incompleteExercise = _uiState.value.workoutPlan?.days
                    ?.find { it.dayNumber == currentDay }
                    ?.exercises
                    ?.firstOrNull { exercise ->
                        !_uiState.value.completedExercises.getOrDefault("${currentDay}_${exercise.id}", false)
                    }

                incompleteExercise?.let {
                    markExerciseComplete(currentDay, it.id, true)
                }
            }
            else -> {
                // Unknown command
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        voiceAssistant.stopListening()
    }
}
