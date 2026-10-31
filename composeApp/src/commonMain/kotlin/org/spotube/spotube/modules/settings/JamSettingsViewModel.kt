package org.spotube.spotube.modules.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.spotube.spotube.core.jam.JamRoomClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JamSettingsUiState(
    val settings: UserSettings = UserSettings(),
    val isTesting: Boolean = false,
    val testResult: Result<String>? = null)

class JamSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val jamRoomClient: JamRoomClient) : ViewModel() {
    private val _uiState = MutableStateFlow(JamSettingsUiState())
    val uiState: StateFlow<JamSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.userSettings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    /** A blank URL goes back to the public server. */
    fun setServerUrl(url: String) {
        viewModelScope.launch {
            val serverUrl = url.trim().trimEnd('/').ifBlank { DEFAULT_JAM_SERVER_URL }
            settingsRepository.updateSettings(uiState.value.settings.copy(jamServerUrl = serverUrl))
            _uiState.update { it.copy(testResult = null) }
        }
    }

    fun testConnection() {
        if (_uiState.value.isTesting) return
        _uiState.update { it.copy(isTesting = true, testResult = null) }
        viewModelScope.launch {
            try {
                val result = jamRoomClient.testConnection(uiState.value.settings.jamServerUrl)
                _uiState.update { it.copy(testResult = result) }
            } finally {
                _uiState.update { it.copy(isTesting = false) }
            }
        }
    }

    fun clearTestResult() {
        _uiState.update { it.copy(testResult = null) }
    }
}
