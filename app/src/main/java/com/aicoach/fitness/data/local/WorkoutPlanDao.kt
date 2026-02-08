package com.aicoach.fitness.data.local

import android.content.Context
import android.content.SharedPreferences
import com.aicoach.fitness.domain.model.Exercise
import com.aicoach.fitness.domain.model.WorkoutDay
import com.aicoach.fitness.domain.model.WorkoutPlan
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutPlanDao @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    companion object {
        private const val PREFS_NAME = "workout_plan_prefs"
        private const val KEY_WORKOUT_PLAN = "current_workout_plan"
        private const val KEY_EXERCISE_COMPLETION = "exercise_completion_"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveWorkoutPlan(plan: WorkoutPlan) {
        val json = gson.toJson(plan)
        prefs.edit().putString(KEY_WORKOUT_PLAN, json).apply()
    }

    fun getWorkoutPlan(): WorkoutPlan? {
        val json = prefs.getString(KEY_WORKOUT_PLAN, null) ?: return null
        return try {
            gson.fromJson(json, WorkoutPlan::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun clearWorkoutPlan() {
        prefs.edit().remove(KEY_WORKOUT_PLAN).apply()
    }

    fun markExerciseComplete(dayNumber: Int, exerciseId: String, completed: Boolean) {
        val key = "${KEY_EXERCISE_COMPLETION}${dayNumber}_$exerciseId"
        prefs.edit().putBoolean(key, completed).apply()
    }

    fun isExerciseComplete(dayNumber: Int, exerciseId: String): Boolean {
        val key = "${KEY_EXERCISE_COMPLETION}${dayNumber}_$exerciseId"
        return prefs.getBoolean(key, false)
    }

    fun getCompletedExercisesForDay(dayNumber: Int): Set<String> {
        val allPrefs = prefs.all
        val prefix = "${KEY_EXERCISE_COMPLETION}${dayNumber}_"
        return allPrefs
            .filter { it.key.startsWith(prefix) && it.value == true }
            .map { it.key.removePrefix(prefix) }
            .toSet()
    }

    fun clearAllCompletionData() {
        val editor = prefs.edit()
        prefs.all.keys
            .filter { it.startsWith(KEY_EXERCISE_COMPLETION) }
            .forEach { editor.remove(it) }
        editor.apply()
    }
}
