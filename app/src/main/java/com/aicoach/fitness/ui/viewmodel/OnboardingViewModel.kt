package com.aicoach.fitness.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aicoach.fitness.data.repository.FitnessRepository
import com.aicoach.fitness.domain.model.Result
import com.aicoach.fitness.utils.VoiceAssistant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val recognizedText: String = "",
    val confirmationText: String = "",
    val fitnessGoal: String = "",
    val injuries: String = "",
    val isGeneratingPlan: Boolean = false,
    val isComplete: Boolean = false,
    val showManualInput: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val voiceAssistant: VoiceAssistant,
    private val repository: FitnessRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _voiceState = MutableStateFlow<VoiceAssistant.VoiceState>(VoiceAssistant.VoiceState.Idle)
    val voiceState: StateFlow<VoiceAssistant.VoiceState> = _voiceState.asStateFlow()

    private var currentStep = OnboardingStep.WELCOME

    enum class OnboardingStep {
        WELCOME,
        ASK_GOAL,
        ASK_INJURIES,
        CONFIRM,
        GENERATING
    }

    init {
        viewModelScope.launch {
            voiceAssistant.voiceState.collect { state ->
                _voiceState.value = state
            }
        }
    }

    fun startOnboarding() {
        currentStep = OnboardingStep.WELCOME
        viewModelScope.launch {
            delay(500) // Small delay after screen load
            speakWelcome()
        }
    }

    private fun speakWelcome() {
        val welcomeMessage = "Welcome! I'm your AI Coach. What is your fitness goal, and do you have any injuries?"
        voiceAssistant.speak(welcomeMessage) {
            startListeningForGoal()
        }
    }

    private fun startListeningForGoal() {
        voiceAssistant.startListening { result ->
            when (result) {
                is VoiceAssistant.RecognitionResult.Success -> {
                    val text = result.text
                    _uiState.value = _uiState.value.copy(recognizedText = text)
                    processUserInput(text)
                }
                is VoiceAssistant.RecognitionResult.Partial -> {
                    _uiState.value = _uiState.value.copy(recognizedText = result.text)
                }
                is VoiceAssistant.RecognitionResult.Error -> {
                    // Error handling - show manual input option
                }
            }
        }
    }

    private fun processUserInput(text: String) {
        viewModelScope.launch {
            // Parse goal and injuries from the combined response
            // Simple parsing - in production, use NLP or separate questions
            val lowerText = text.lowercase()

            // Extract goal
            val goal = when {
                lowerText.contains("muscle") || lowerText.contains("strength") || lowerText.contains("bulk") ->
                    "Build muscle and strength"
                lowerText.contains("weight") || lowerText.contains("fat") || lowerText.contains("lose") ->
                    "Lose weight and burn fat"
                lowerText.contains("endurance") || lowerText.contains("cardio") || lowerText.contains("stamina") ->
                    "Improve endurance and cardio"
                lowerText.contains("tone") || lowerText.contains("shape") ->
                    "Tone and shape body"
                lowerText.contains("flexibility") || lowerText.contains("mobility") ->
                    "Improve flexibility and mobility"
                else -> text // Use full text as goal
            }

            // Extract injuries
            val injuries = when {
                lowerText.contains("no injury") || lowerText.contains("none") || lowerText.contains("healthy") ->
                    ""
                lowerText.contains("knee") -> "Knee issues"
                lowerText.contains("back") -> "Back issues"
                lowerText.contains("shoulder") -> "Shoulder issues"
                lowerText.contains("wrist") -> "Wrist issues"
                lowerText.contains("ankle") -> "Ankle issues"
                else -> "" // No specific injury mentioned
            }

            _uiState.value = _uiState.value.copy(
                fitnessGoal = goal,
                injuries = injuries
            )

            // Confirm the information
            confirmAndGeneratePlan()
        }
    }

    private fun confirmAndGeneratePlan() {
        val confirmation = "Got it! Your goal is ${_uiState.value.fitnessGoal}. " +
                if (_uiState.value.injuries.isNotBlank())
                    "I'll account for your ${_uiState.value.injuries}."
                else
                    "No injuries noted."

        _uiState.value = _uiState.value.copy(confirmationText = confirmation)

        voiceAssistant.speak("$confirmation Generating your personalized workout plan now.") {
            generateWorkoutPlan()
        }
    }

    private fun generateWorkoutPlan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingPlan = true)

            repository.generateWorkoutPlan(
                goal = _uiState.value.fitnessGoal,
                injuries = _uiState.value.injuries
            ).collect { result ->
                when (result) {
                    is Result.Success -> {
                        repository.saveUserProfile(
                            _uiState.value.fitnessGoal,
                            _uiState.value.injuries
                        )
                        repository.setOnboardingComplete(true)

                        voiceAssistant.speak("Your 7-day workout plan is ready! Let's get started.") {
                            _uiState.value = _uiState.value.copy(
                                isGeneratingPlan = false,
                                isComplete = true
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isGeneratingPlan = false,
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

    fun showManualInput() {
        _uiState.value = _uiState.value.copy(showManualInput = true)
    }

    fun hideManualInput() {
        _uiState.value = _uiState.value.copy(showManualInput = false)
    }

    fun submitManualInput(goal: String, injuries: String) {
        _uiState.value = _uiState.value.copy(
            fitnessGoal = goal,
            injuries = injuries,
            recognizedText = "Goal: $goal. Injuries: ${injuries.ifBlank { "None" }}",
            showManualInput = false
        )
        confirmAndGeneratePlan()
    }

    override fun onCleared() {
        super.onCleared()
        voiceAssistant.stopListening()
    }
}
