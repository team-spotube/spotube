/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.krtirtho.spotube.modules.jam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.krtirtho.spotube.PlatformType
import dev.krtirtho.spotube.core.jam.JamParticipant
import dev.krtirtho.spotube.core.jam.JamRole
import dev.krtirtho.spotube.core.jam.JamRoomCode
import dev.krtirtho.spotube.core.jam.JamRoomService
import dev.krtirtho.spotube.core.share.ShareService
import dev.krtirtho.spotube.getPlatform
import dev.krtirtho.spotube.modules.settings.SettingsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class JamUiState(
    val isActive: Boolean = false,
    val isConnected: Boolean = false,
    val role: JamRole? = null,
    val participants: List<JamParticipant> = emptyList(),
    val roomCode: String? = null,
    val brokerHost: String = "",
    val brokerConfigured: Boolean = false,
    val error: String? = null,
)

class JamViewModel(
    private val jamRoomService: JamRoomService,
    private val shareService: ShareService,
    private val settingsProvider: SettingsProvider,
) : ViewModel() {

    private val _localError = MutableStateFlow<String?>(null)

    val supportsNativeShare: Boolean =
        getPlatform().type == PlatformType.Android || getPlatform().type == PlatformType.IOS

    val uiState: StateFlow<JamUiState> = combine(
        jamRoomService.role,
        jamRoomService.participants,
        jamRoomService.isConnected,
        jamRoomService.roomCode,
        jamRoomService.connectionError,
        settingsProvider.settingsState,
        _localError,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val role = values[0] as JamRole?
        @Suppress("UNCHECKED_CAST")
        val participants = values[1] as List<JamParticipant>
        val isConnected = values[2] as Boolean
        val roomCode = values[3] as String?
        val connectionError = values[4] as String?
        val settings = values[5] as? dev.krtirtho.spotube.modules.settings.UserSettings
        val localError = values[6] as String?

        JamUiState(
            isActive = role != null,
            isConnected = isConnected,
            role = role,
            participants = participants,
            roomCode = roomCode,
            brokerHost = settings?.jamBroker?.host.orEmpty(),
            brokerConfigured = !settings?.jamBroker?.host.isNullOrBlank(),
            error = localError ?: connectionError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JamUiState())

    // ---------- Session lifecycle ----------

    fun createSession() {
        viewModelScope.launch {
            jamRoomService.createRoom()
                .onFailure { e ->
                    _localError.value = e.message ?: "Failed to create jam session"
                }
                .onSuccess { _localError.value = null }
        }
    }

    fun joinWithCode(input: String) {
        val code = JamRoomCode.normalize(input)
        if (!JamRoomCode.isValid(code)) {
            _localError.value = "Room codes are ${JamRoomCode.LENGTH} characters (letters and digits)"
            return
        }
        viewModelScope.launch {
            jamRoomService.joinRoom(code)
                .onFailure { e ->
                    _localError.value = e.message ?: "Failed to join jam session"
                }
                .onSuccess { _localError.value = null }
        }
    }

    fun shareRoomCode() {
        val code = uiState.value.roomCode ?: return
        shareService.share("Join my Spotube Jam with code: $code", "Spotube Group Jam")
    }

    fun leave() {
        viewModelScope.launch {
            jamRoomService.leaveRoom()
            _localError.value = null
        }
    }

    fun clearError() {
        _localError.value = null
    }

    // ---------- Host moderation ----------

    fun kickParticipant(participantId: String) {
        viewModelScope.launch { jamRoomService.kickParticipant(participantId) }
    }

    fun banParticipant(participantId: String) {
        viewModelScope.launch { jamRoomService.banParticipant(participantId) }
    }
}