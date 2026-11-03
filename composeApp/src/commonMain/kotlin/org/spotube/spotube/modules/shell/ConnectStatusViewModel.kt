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

package org.spotube.spotube.modules.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.spotube.spotube.core.jam.JamRole
import org.spotube.spotube.core.jam.JamRoomService
import org.spotube.spotube.core.remote.ConnectionState
import org.spotube.spotube.core.remote.RemoteControlClient

/** Set while this app is remote-controlling another device. */
data class RemoteConnectStatus(val deviceName: String?)

/** Set while this app is in a Group Jam room. */
data class JamConnectStatus(val isHost: Boolean, val listenerCount: Int)

data class ConnectStatusUiState(
    val remote: RemoteConnectStatus? = null,
    val jam: JamConnectStatus? = null)

/** Live remote-control / Group Jam status shown by the Home header and the sidebar. */
class ConnectStatusViewModel(
    remoteControlClient: RemoteControlClient,
    jamRoomService: JamRoomService
) : ViewModel() {
    val uiState: StateFlow<ConnectStatusUiState> = combine(
        remoteControlClient.connectionState,
        jamRoomService.role,
        jamRoomService.participants
    ) { connection, role, participants ->
        ConnectStatusUiState(
            remote = (connection as? ConnectionState.Connected)?.let {
                RemoteConnectStatus(it.deviceName.ifBlank { null })
            },
            jam = role?.let { JamConnectStatus(isHost = it == JamRole.Host, listenerCount = participants.size) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConnectStatusUiState())
}
