package com.aicoach.fitness.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_FILE_NAME = "secure_prefs"
        private const val KEY_API_KEY = "openrouter_api_key"
        private const val KEY_USER_GOAL = "user_fitness_goal"
        private const val KEY_USER_INJURIES = "user_injuries"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: EncryptedSharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }

    private val _apiKeyFlow = MutableStateFlow(getApiKey())
    val apiKeyFlow: StateFlow<String?> = _apiKeyFlow.asStateFlow()

    fun saveApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_API_KEY, apiKey).apply()
        _apiKeyFlow.value = apiKey
    }

    fun getApiKey(): String? {
        return encryptedPrefs.getString(KEY_API_KEY, null)
    }

    fun clearApiKey() {
        encryptedPrefs.edit().remove(KEY_API_KEY).apply()
        _apiKeyFlow.value = null
    }

    fun hasApiKey(): Boolean {
        return !getApiKey().isNullOrBlank()
    }

    fun saveUserProfile(goal: String, injuries: String) {
        encryptedPrefs.edit()
            .putString(KEY_USER_GOAL, goal)
            .putString(KEY_USER_INJURIES, injuries)
            .apply()
    }

    fun getUserGoal(): String {
        return encryptedPrefs.getString(KEY_USER_GOAL, "") ?: ""
    }

    fun getUserInjuries(): String {
        return encryptedPrefs.getString(KEY_USER_INJURIES, "") ?: ""
    }

    fun setOnboardingComplete(complete: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, complete).apply()
    }

    fun isOnboardingComplete(): Boolean {
        return encryptedPrefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
        _apiKeyFlow.value = null
    }
}
