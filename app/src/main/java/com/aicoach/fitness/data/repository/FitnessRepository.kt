package com.aicoach.fitness.data.repository

import com.aicoach.fitness.data.local.SecurePreferences
import com.aicoach.fitness.data.local.WorkoutPlanDao
import com.aicoach.fitness.data.remote.ChatCompletionRequest
import com.aicoach.fitness.data.remote.MessageRequest
import com.aicoach.fitness.data.remote.OpenRouterPrompts
import com.aicoach.fitness.data.remote.OpenRouterService
import com.aicoach.fitness.domain.model.Exercise
import com.aicoach.fitness.domain.model.FormFeedback
import com.aicoach.fitness.domain.model.Result
import com.aicoach.fitness.domain.model.WorkoutDay
import com.aicoach.fitness.domain.model.WorkoutPlan
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FitnessRepository @Inject constructor(
    private val openRouterService: OpenRouterService,
    private val securePreferences: SecurePreferences,
    private val workoutPlanDao: WorkoutPlanDao,
    private val gson: Gson
) {

    fun hasApiKey(): Boolean = securePreferences.hasApiKey()

    fun getApiKey(): String? = securePreferences.getApiKey()

    fun saveApiKey(apiKey: String) = securePreferences.saveApiKey(apiKey)

    fun clearApiKey() = securePreferences.clearApiKey()

    fun isOnboardingComplete(): Boolean = securePreferences.isOnboardingComplete()

    fun setOnboardingComplete(complete: Boolean) = securePreferences.setOnboardingComplete(complete)

    fun saveUserProfile(goal: String, injuries: String) {
        securePreferences.saveUserProfile(goal, injuries)
    }

    fun getUserGoal(): String = securePreferences.getUserGoal()

    fun getUserInjuries(): String = securePreferences.getUserInjuries()

    suspend fun generateWorkoutPlan(goal: String, injuries: String): Flow<Result<WorkoutPlan>> = flow {
        emit(Result.Loading)

        val apiKey = securePreferences.getApiKey()
        if (apiKey.isNullOrBlank()) {
            emit(Result.Error("API key not found. Please add your OpenRouter API key in settings."))
            return@flow
        }

        try {
            val prompt = OpenRouterPrompts.buildWorkoutPlanPrompt(goal, injuries)
            val request = ChatCompletionRequest(
                messages = listOf(
                    MessageRequest(
                        role = "system",
                        content = "You are a professional fitness coach. Create safe, effective workout plans."
                    ),
                    MessageRequest(role = "user", content = prompt)
                )
            )

            val response = openRouterService.generateChatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )

            if (response.isSuccessful) {
                val body = response.body()
                val content = body?.choices?.firstOrNull()?.message?.content

                if (content != null) {
                    val workoutPlan = parseWorkoutPlanFromResponse(content, goal, injuries)
                    if (workoutPlan != null) {
                        workoutPlanDao.saveWorkoutPlan(workoutPlan)
                        emit(Result.Success(workoutPlan))
                    } else {
                        emit(Result.Error("Failed to parse workout plan"))
                    }
                } else {
                    emit(Result.Error("Empty response from API"))
                }
            } else {
                emit(Result.Error("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            emit(Result.Error("Network error: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)

    private fun parseWorkoutPlanFromResponse(
        content: String,
        goal: String,
        injuries: String
    ): WorkoutPlan? {
        return try {
            // Clean up the response - remove markdown code blocks if present
            val cleanContent = content
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val jsonObject = gson.fromJson(cleanContent, JsonObject::class.java)
            val daysArray = jsonObject.getAsJsonArray("days")

            val days = mutableListOf<WorkoutDay>()
            daysArray?.forEach { dayElement ->
                val dayObj = dayElement.asJsonObject
                val dayNumber = dayObj.get("dayNumber")?.asInt ?: 1
                val exercisesArray = dayObj.getAsJsonArray("exercises")

                val exercises = mutableListOf<Exercise>()
                exercisesArray?.forEach { exElement ->
                    val exObj = exElement.asJsonObject
                    val targetMuscles = exObj.getAsJsonArray("targetMuscleGroups")?.map { it.asString } ?: emptyList()

                    exercises.add(
                        Exercise(
                            id = exObj.get("id")?.asString ?: "ex_${dayNumber}_${exercises.size}",
                            name = exObj.get("name")?.asString ?: "Unknown Exercise",
                            description = exObj.get("description")?.asString ?: "",
                            sets = exObj.get("sets")?.asInt ?: 3,
                            reps = exObj.get("reps")?.asInt ?: 10,
                            targetMuscleGroups = targetMuscles
                        )
                    )
                }

                days.add(WorkoutDay(dayNumber = dayNumber, exercises = exercises))
            }

            WorkoutPlan(
                id = System.currentTimeMillis().toString(),
                userGoal = goal,
                injuries = injuries,
                days = days
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun analyzePose(exerciseName: String, poseJson: String): Flow<Result<FormFeedback>> = flow {
        emit(Result.Loading)

        val apiKey = securePreferences.getApiKey()
        if (apiKey.isNullOrBlank()) {
            emit(Result.Error("API key not found"))
            return@flow
        }

        try {
            val prompt = OpenRouterPrompts.buildPoseAnalysisPrompt(exerciseName, poseJson)
            val request = ChatCompletionRequest(
                messages = listOf(
                    MessageRequest(
                        role = "system",
                        content = "You are a fitness form expert. Analyze exercise form precisely."
                    ),
                    MessageRequest(role = "user", content = prompt)
                ),
                maxTokens = 100
            )

            val response = openRouterService.generateChatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )

            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                if (content != null) {
                    val feedback = parseFormFeedback(content)
                    emit(Result.Success(feedback))
                } else {
                    emit(Result.Error("Empty response"))
                }
            } else {
                emit(Result.Error("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Result.Error("Error: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)

    private fun parseFormFeedback(content: String): FormFeedback {
        return try {
            val cleanContent = content
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val jsonObject = gson.fromJson(cleanContent, JsonObject::class.java)
            FormFeedback(
                isCorrect = jsonObject.get("isCorrect")?.asBoolean ?: false,
                feedback = jsonObject.get("feedback")?.asString ?: "Keep going!"
            )
        } catch (e: Exception) {
            FormFeedback(isCorrect = true, feedback = "Keep going!")
        }
    }

    fun getWorkoutPlan(): WorkoutPlan? = workoutPlanDao.getWorkoutPlan()

    fun markExerciseComplete(dayNumber: Int, exerciseId: String, completed: Boolean) {
        workoutPlanDao.markExerciseComplete(dayNumber, exerciseId, completed)
    }

    fun isExerciseComplete(dayNumber: Int, exerciseId: String): Boolean {
        return workoutPlanDao.isExerciseComplete(dayNumber, exerciseId)
    }

    fun clearAllData() {
        workoutPlanDao.clearWorkoutPlan()
        workoutPlanDao.clearAllCompletionData()
        securePreferences.clearAll()
    }
}
