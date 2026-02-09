package com.aicoach.fitness.ui.viewmodel

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aicoach.fitness.data.repository.FitnessRepository
import com.aicoach.fitness.domain.model.FormFeedback
import com.aicoach.fitness.domain.model.PoseData
import com.aicoach.fitness.domain.model.Result
import com.aicoach.fitness.utils.PoseAnalyzer
import com.aicoach.fitness.utils.VoiceAssistant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

data class CameraUiState(
    val exerciseName: String = "",
    val poseData: PoseData? = null,
    val timer: Int = 0,
    val repCount: Int = 0,
    val feedback: String = "",
    val isFeedbackPositive: Boolean = true,
    val isAnalyzing: Boolean = false,
    val isRecording: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val poseAnalyzer: PoseAnalyzer,
    private val repository: FitnessRepository,
    private val voiceAssistant: VoiceAssistant
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var analysisJob: Job? = null
    private var lastFeedbackTime = 0L
    private val feedbackCooldown = 3000L // 3 seconds between feedback

    private val repCounter = AtomicInteger(0)
    private var lastPoseState: PoseState? = null

    enum class PoseState {
        UP, DOWN, UNKNOWN
    }

    init {
        poseAnalyzer.setOnPoseDetectedListener { poseData ->
            _uiState.value = _uiState.value.copy(poseData = poseData)
            if (_uiState.value.isRecording) {
                detectReps(poseData)
            }
        }
    }

    fun setExerciseName(name: String) {
        _uiState.value = _uiState.value.copy(exerciseName = name)
    }

    fun analyzeFrame(imageProxy: ImageProxy) {
        poseAnalyzer.analyzeImage(imageProxy)
    }

    fun toggleRecording() {
        val newRecordingState = !_uiState.value.isRecording
        _uiState.value = _uiState.value.copy(isRecording = newRecordingState)

        if (newRecordingState) {
            startTimer()
            startPoseAnalysis()
            voiceAssistant.speak("Starting ${uiState.value.exerciseName}. Let's go!")
        } else {
            stopTimer()
            stopPoseAnalysis()
            voiceAssistant.speak("Workout paused.")
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.value = _uiState.value.copy(
                    timer = _uiState.value.timer + 1
                )
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun startPoseAnalysis() {
        analysisJob = viewModelScope.launch {
            while (isActive) {
                delay(2000) // Analyze every 2 seconds
                analyzePoseForFeedback()
            }
        }
    }

    private fun stopPoseAnalysis() {
        analysisJob?.cancel()
        analysisJob = null
    }

    private fun analyzePoseForFeedback() {
        val poseData = _uiState.value.poseData ?: return
        val exerciseName = _uiState.value.exerciseName

        // Check cooldown
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFeedbackTime < feedbackCooldown) return

        // Convert pose to JSON
        val poseJson = poseAnalyzer.convertPoseToJson(poseData)

        _uiState.value = _uiState.value.copy(isAnalyzing = true)

        viewModelScope.launch {
            repository.analyzePose(exerciseName, poseJson).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val feedback = result.data
                        lastFeedbackTime = System.currentTimeMillis()

                        _uiState.value = _uiState.value.copy(
                            feedback = feedback.feedback,
                            isFeedbackPositive = feedback.isCorrect,
                            isAnalyzing = false
                        )

                        // Speak feedback if it's a correction
                        if (!feedback.isCorrect) {
                            voiceAssistant.speak(feedback.feedback)
                        }
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isAnalyzing = false,
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

    private fun detectReps(poseData: PoseData) {
        // Simple rep detection based on exercise type
        when (_uiState.value.exerciseName.lowercase()) {
            "squat", "squats" -> detectSquat(poseData)
            "push-up", "pushup", "push ups" -> detectPushUp(poseData)
            "lunge", "lunges" -> detectLunge(poseData)
            else -> detectGenericRep(poseData)
        }
    }

    private fun detectSquat(poseData: PoseData) {
        val leftHip = poseAnalyzer.getLandmark(poseData, "left_hip")
        val leftKnee = poseAnalyzer.getLandmark(poseData, "left_knee")
        val leftAnkle = poseAnalyzer.getLandmark(poseData, "left_ankle")

        if (leftHip != null && leftKnee != null && leftAnkle != null) {
            val kneeAngle = poseAnalyzer.calculateAngle(leftHip, leftKnee, leftAnkle)

            val currentState = when {
                kneeAngle < 100 -> PoseState.DOWN
                kneeAngle > 160 -> PoseState.UP
                else -> PoseState.UNKNOWN
            }

            if (lastPoseState == PoseState.DOWN && currentState == PoseState.UP) {
                incrementRep()
            }
            lastPoseState = currentState
        }
    }

    private fun detectPushUp(poseData: PoseData) {
        val leftShoulder = poseAnalyzer.getLandmark(poseData, "left_shoulder")
        val leftElbow = poseAnalyzer.getLandmark(poseData, "left_elbow")
        val leftWrist = poseAnalyzer.getLandmark(poseData, "left_wrist")

        if (leftShoulder != null && leftElbow != null && leftWrist != null) {
            val elbowAngle = poseAnalyzer.calculateAngle(leftShoulder, leftElbow, leftWrist)

            val currentState = when {
                elbowAngle < 90 -> PoseState.DOWN
                elbowAngle > 160 -> PoseState.UP
                else -> PoseState.UNKNOWN
            }

            if (lastPoseState == PoseState.DOWN && currentState == PoseState.UP) {
                incrementRep()
            }
            lastPoseState = currentState
        }
    }

    private fun detectLunge(poseData: PoseData) {
        val leftHip = poseAnalyzer.getLandmark(poseData, "left_hip")
        val leftKnee = poseAnalyzer.getLandmark(poseData, "left_knee")
        val leftAnkle = poseAnalyzer.getLandmark(poseData, "left_ankle")

        if (leftHip != null && leftKnee != null && leftAnkle != null) {
            val kneeAngle = poseAnalyzer.calculateAngle(leftHip, leftKnee, leftAnkle)

            val currentState = when {
                kneeAngle < 100 -> PoseState.DOWN
                kneeAngle > 160 -> PoseState.UP
                else -> PoseState.UNKNOWN
            }

            if (lastPoseState == PoseState.DOWN && currentState == PoseState.UP) {
                incrementRep()
            }
            lastPoseState = currentState
        }
    }

    private fun detectGenericRep(poseData: PoseData) {
        // Generic rep detection based on vertical movement of hands or shoulders
        val leftShoulder = poseAnalyzer.getLandmark(poseData, "left_shoulder")
        val rightShoulder = poseAnalyzer.getLandmark(poseData, "right_shoulder")

        if (leftShoulder != null && rightShoulder != null) {
            val shoulderY = (leftShoulder.y + rightShoulder.y) / 2

            val currentState = when {
                shoulderY > 0.6f -> PoseState.DOWN
                shoulderY < 0.4f -> PoseState.UP
                else -> PoseState.UNKNOWN
            }

            if (lastPoseState == PoseState.DOWN && currentState == PoseState.UP) {
                incrementRep()
            }
            lastPoseState = currentState
        }
    }

    private fun incrementRep() {
        val newCount = repCounter.incrementAndGet()
        _uiState.value = _uiState.value.copy(repCount = newCount)

        // Announce milestone reps
        if (newCount % 5 == 0) {
            voiceAssistant.speak("$newCount reps!")
        }
    }

    fun shutdown() {
        stopTimer()
        stopPoseAnalysis()
        poseAnalyzer.close()
    }

    override fun onCleared() {
        super.onCleared()
        shutdown()
    }
}
