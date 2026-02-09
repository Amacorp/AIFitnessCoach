package com.aicoach.fitness.domain.model

data class WorkoutPlan(
    val id: String,
    val userGoal: String,
    val injuries: String,
    val days: List<WorkoutDay>,
    val createdAt: Long = System.currentTimeMillis()
)

data class WorkoutDay(
    val dayNumber: Int,
    val exercises: List<Exercise>,
    val isCompleted: Boolean = false
)

data class Exercise(
    val id: String,
    val name: String,
    val description: String,
    val sets: Int,
    val reps: Int,
    val durationSeconds: Int? = null,
    val isCompleted: Boolean = false,
    val targetMuscleGroups: List<String> = emptyList()
)

data class UserProfile(
    val fitnessGoal: String = "",
    val injuries: String = "",
    val hasCompletedOnboarding: Boolean = false
)
