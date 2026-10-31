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

package org.spotube.spotube.modules.jam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import org.spotube.spotube.PlatformType
import org.spotube.spotube.core.jam.JamParticipant
import org.spotube.spotube.core.jam.JamRole
import org.spotube.spotube.core.jam.JamRoomCode
import org.spotube.spotube.core.jam.JamRoomService
import org.spotube.spotube.core.share.ShareService
import org.spotube.spotube.getPlatform
import org.spotube.spotube.modules.settings.SettingsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import org.jetbrains.compose.resources.getString
import spotube.composeapp.generated.resources.*

enum class JamSetupMode { Create, Join }

data class JamParticipantUi(
    val id: String,
    val displayName: String,
    val initial: String,
    val isHost: Boolean,
    val canModerate: Boolean,
    val menuExpanded: Boolean = false
)

data class JamUiState(
    val isActive: Boolean = false,
    val isConnected: Boolean = false,
    val role: JamRole? = null,
    val participants: List<JamParticipantUi> = emptyList(),
    val roomCode: String? = null,
    val serverHost: String = "",
    val serverConfigured: Boolean = false,
    val error: String? = null,
    val setupMode: JamSetupMode = JamSetupMode.Create,
    val codeInput: TextFieldState = TextFieldState(),
    val canCreate: Boolean = false,
    val canJoin: Boolean = false,
    val isBusy: Boolean = false,
    val isLeaving: Boolean = false,
    val isHost: Boolean = false,
    val supportsNativeShare: Boolean = false,
    val codeCopied: Boolean = false
)

class JamViewModel(
    private val jamRoomService: JamRoomService,
    private val shareService: ShareService,
    private val settingsProvider: SettingsProvider
) : ViewModel() {

    private val _localError = MutableStateFlow<String?>(null)
    private val setupMode = MutableStateFlow(JamSetupMode.Create)
    private val isBusy = MutableStateFlow(false)
    private val isLeaving = MutableStateFlow(false)
    private val codeCopied = MutableStateFlow(false)
    private val codeInput = TextFieldState()
    private var copyFeedbackJob: Job? = null
    private val participantMenu = MutableStateFlow<String?>(null)

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
        setupMode,
        isBusy,
        codeCopied,
        snapshotFlow { codeInput.text.toString() },
        participantMenu,
        isLeaving
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val role = values[0] as JamRole?

        @Suppress("UNCHECKED_CAST")
        val participants = values[1] as List<JamParticipant>
        val isConnected = values[2] as Boolean
        val roomCode = values[3] as String?
        val connectionError = values[4] as String?
        val settings = values[5] as? org.spotube.spotube.modules.settings.UserSettings
        val localError = values[6] as String?
        val busy = values[8] as Boolean
        val serverConfigured = !settings?.jamServerUrl.isNullOrBlank()

        JamUiState(
            isActive = role != null,
            isConnected = isConnected,
            role = role,
            participants = participants.sortedByDescending { it.isHost }.map { participant ->
                JamParticipantUi(
                    id = participant.id,
                    displayName = participant.displayName,
                    initial = participant.displayName.firstOrNull()?.uppercase() ?: "?",
                    isHost = participant.isHost,
                    canModerate = role == JamRole.Host && !participant.isHost,
                    menuExpanded = values[11] == participant.id
                )
            },
            roomCode = roomCode,
            serverHost = settings?.jamServerUrl.orEmpty()
                .substringAfter("://").substringBefore('/'),
            serverConfigured = serverConfigured,
            error = localError ?: connectionError,
            setupMode = values[7] as JamSetupMode,
            isBusy = busy,
            isLeaving = values[12] as Boolean,
            codeCopied = values[9] as Boolean,
            codeInput = codeInput,
            canCreate = serverConfigured,
            canJoin = serverConfigured &&
                    JamRoomCode.isValid(JamRoomCode.normalize(values[10] as String)),
            isHost = role == JamRole.Host,
            supportsNativeShare = supportsNativeShare
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        JamUiState(codeInput = codeInput)
    )

    // ---------- Session lifecycle ----------

    fun createSession() {
        if (isBusy.value) return
        isBusy.value = true
        viewModelScope.launch {
            try {
                jamRoomService.createRoom()
                    .onFailure { e ->
                        _localError.value = e.message ?: getString(Res.string.jam_error_create)
                    }
                    .onSuccess { _localError.value = null }
            } finally {
                isBusy.value = false
            }
        }
    }

    fun selectSetupMode(mode: JamSetupMode) {
        setupMode.value = mode
        _localError.value = null
    }

    fun joinSession() = joinWithCode(codeInput.text.toString())

    fun onCodeCopied() {
        copyFeedbackJob?.cancel()
        copyFeedbackJob = viewModelScope.launch {
            codeCopied.value = true
            delay(2_000)
            codeCopied.value = false
        }
    }

    fun joinWithCode(input: String) {
        if (isBusy.value) return
        val code = JamRoomCode.normalize(input)
        if (!JamRoomCode.isValid(code)) {
            viewModelScope.launch {
                _localError.value = getString(Res.string.jam_error_invalid_code)
            }
            return
        }
        isBusy.value = true
        viewModelScope.launch {
            try {
                jamRoomService.joinRoom(code)
                    .onFailure { e ->
                        _localError.value = e.message ?: getString(Res.string.jam_error_join)
                    }
                    .onSuccess { _localError.value = null }
            } finally {
                isBusy.value = false
            }
        }
    }

    fun shareRoomCode() {
        val code = uiState.value.roomCode ?: return
        viewModelScope.launch {
            shareService.share(
                getString(Res.string.jam_share_message, code),
                getString(Res.string.jam_screen_title)
            )
        }
    }

    fun leave() {
        if (isLeaving.value) return
        isLeaving.value = true
        viewModelScope.launch {
            try {
                jamRoomService.leaveRoom()
                _localError.value = null
            } finally {
                isLeaving.value = false
            }
        }
    }

    fun clearError() {
        _localError.value = null
    }

    // ---------- Host moderation ----------

    fun kickParticipant(participantId: String) {
        participantMenu.value = null
        viewModelScope.launch { jamRoomService.kickParticipant(participantId) }
    }

    fun banParticipant(participantId: String) {
        participantMenu.value = null
        viewModelScope.launch { jamRoomService.banParticipant(participantId) }
    }

    fun showParticipantMenu(participantId: String?) {
        participantMenu.value = participantId
    }
}
