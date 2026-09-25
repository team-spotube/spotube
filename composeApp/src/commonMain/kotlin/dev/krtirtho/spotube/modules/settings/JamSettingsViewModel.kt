package dev.krtirtho.spotube.modules.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.krtirtho.spotube.core.jam.JamRoomClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JamSettingsUiState(
    val settings: UserSettings = UserSettings(),
    val isTesting: Boolean = false,
    val testResult: Result<String>? = null,
)

class JamSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val jamRoomClient: JamRoomClient,
) : ViewModel() {
    private val _uiState = MutableStateFlow(JamSettingsUiState())
    val uiState: StateFlow<JamSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.userSettings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun update(transform: UserSettings.() -> UserSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(uiState.value.settings.transform())
        }
    }

    fun testConnection() {
        if (_uiState.value.isTesting) return
        _uiState.update { it.copy(isTesting = true, testResult = null) }
        viewModelScope.launch {
            val result = jamRoomClient.testConnection(uiState.value.settings.jamBroker)
            _uiState.update { it.copy(isTesting = false, testResult = result) }
        }
    }

    fun clearTestResult() {
        _uiState.update { it.copy(testResult = null) }
    }
}
