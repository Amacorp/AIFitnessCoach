package com.aicoach.fitness.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aicoach.fitness.data.repository.FitnessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val hasApiKey: Boolean = false,
    val userGoal: String = "",
    val userInjuries: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: FitnessRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState(
                hasApiKey = repository.hasApiKey(),
                userGoal = repository.getUserGoal(),
                userInjuries = repository.getUserInjuries()
            )
        }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            repository.clearApiKey()
            loadSettings()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            loadSettings()
        }
    }
}
