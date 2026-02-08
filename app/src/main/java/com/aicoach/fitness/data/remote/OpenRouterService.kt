package com.aicoach.fitness.data.remote

import com.aicoach.fitness.domain.model.OpenRouterResponse
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenRouterService {

    @POST("api/v1/chat/completions")
    suspend fun generateChatCompletion(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: ChatCompletionRequest
    ): Response<OpenRouterResponse>

    companion object {
        const val BASE_URL = "https://openrouter.ai/"
        const val MODEL_GEMINI_FLASH = "google/gemini-flash-1.5"
    }
}

data class ChatCompletionRequest(
    @SerializedName("model")
    val model: String = OpenRouterService.MODEL_GEMINI_FLASH,

    @SerializedName("messages")
    val messages: List<MessageRequest>,

    @SerializedName("temperature")
    val temperature: Double = 0.7,

    @SerializedName("max_tokens")
    val maxTokens: Int = 2000,

    @SerializedName("top_p")
    val topP: Double = 1.0,

    @SerializedName("stream")
    val stream: Boolean = false
)

data class MessageRequest(
    @SerializedName("role")
    val role: String,

    @SerializedName("content")
    val content: String
)

// Request builders for specific use cases
object OpenRouterPrompts {

    fun buildWorkoutPlanPrompt(goal: String, injuries: String): String {
        return """
            Create a personalized 7-day workout plan for someone with the following:
            
            Fitness Goal: $goal
            Injuries/Limitations: ${if (injuries.isBlank()) "None" else injuries}
            
            Return ONLY a JSON object in this exact format (no markdown, no code blocks):
            {
                "days": [
                    {
                        "dayNumber": 1,
                        "exercises": [
                            {
                                "id": "ex1",
                                "name": "Exercise Name",
                                "description": "Brief description",
                                "sets": 3,
                                "reps": 12,
                                "targetMuscleGroups": ["muscle1", "muscle2"]
                            }
                        ]
                    }
                ]
            }
            
            Include 4-6 exercises per day. Vary the exercises throughout the week.
            If there are injuries, provide safe alternatives.
        """.trimIndent()
    }

    fun buildPoseAnalysisPrompt(
        exerciseName: String,
        poseJson: String
    ): String {
        return """
            Analyze these body coordinates for a $exerciseName exercise.
            
            Coordinates (x, y, z for each joint):
            $poseJson
            
            Respond with ONLY a JSON object in this format:
            {
                "isCorrect": true/false,
                "feedback": "Your feedback message here (max 5 words if correcting)"
            }
            
            If the posture is correct, set isCorrect to true and feedback to "Perfect form".
            If incorrect, set isCorrect to false and provide a brief 3-5 word correction like "Straighten your back" or "Lower your hips".
        """.trimIndent()
    }

    fun buildOnboardingConfirmationPrompt(goal: String, injuries: String): String {
        return """
            Confirm the user's fitness information:
            Goal: $goal
            Injuries: ${if (injuries.isBlank()) "None reported" else injuries}
            
            Respond with a brief, encouraging confirmation message (1-2 sentences).
        """.trimIndent()
    }
}
