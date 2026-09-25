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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krtirtho.spotube.core.jam.JamRole
import dev.krtirtho.spotube.core.jam.JamRoomCode
import dev.krtirtho.spotube.core.navigation.NavigationCommands
import dev.krtirtho.spotube.core.ui.component.ApplicationMainBar
import dev.krtirtho.spotube.modules.shell.LocalAppShellBottomInset
import org.koin.compose.viewmodel.koinViewModel

/**
 * Group Jam session screen. Playback controls and the queue live in the app's
 * regular player / queue sheet (the shared jam queue is the local queue), so
 * this screen only covers participation and session management.
 */
@Composable
fun JamScreen(
    navigationCommands: NavigationCommands,
) {
    val viewModel = koinViewModel<JamViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val shellBottomInset = LocalAppShellBottomInset.current

    Scaffold(
        topBar = {
            ApplicationMainBar(
                backButton = true,
                title = { Text("Group Jam") },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .padding(bottom = shellBottomInset)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.error?.let { error ->
                ErrorBanner(text = error, onDismiss = viewModel::clearError)
            }

            when {
                !state.isActive -> CreateOrJoinView(
                    state = state,
                    onCreate = viewModel::createSession,
                    onJoin = viewModel::joinWithCode,
                )

                else -> SessionView(
                    state = state,
                    onShareCode = viewModel::shareRoomCode,
                    onLeave = viewModel::leave,
                    onKick = viewModel::kickParticipant,
                    onBan = viewModel::banParticipant,
                )
            }
        }
    }
}

@Composable
private fun ErrorBanner(text: String, onDismiss: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(onClick = onDismiss) {
            Text("Dismiss")
        }
    }
}

@Composable
private fun CreateOrJoinView(
    state: JamUiState,
    onCreate: () -> Unit,
    onJoin: (String) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var pasted by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Listen together with friends over an MQTT broker. Everyone hears the same queue.",
            style = MaterialTheme.typography.titleMedium,
        )

        if (!state.brokerConfigured) {
            Text(
                text = "No jam broker configured — set one up in Settings to host or join a session.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            Text(
                text = "Broker: ${state.brokerHost}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = tab == 0,
                onClick = { tab = 0 },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
            ) { Text("Create") }
            SegmentedButton(
                selected = tab == 1,
                onClick = { tab = 1 },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
            ) { Text("Join") }
        }

        if (tab == 0) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Start a session as the host. You'll get a 6-character room code to " +
                        "share with friends; you control the queue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onCreate,
                    enabled = state.brokerConfigured,
                ) {
                    Text("Create Session")
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter the 6-character room code the host shared with you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = pasted,
                    onValueChange = { pasted = JamRoomCode.normalize(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Room code") },
                    placeholder = { Text("ABC123") },
                    singleLine = true,
                )
                Button(
                    onClick = { onJoin(pasted) },
                    enabled = state.brokerConfigured && JamRoomCode.isValid(pasted),
                ) {
                    Text("Join Session")
                }
            }
        }
    }
}

@Composable
private fun SessionView(
    state: JamUiState,
    onShareCode: () -> Unit,
    onLeave: () -> Unit,
    onKick: (String) -> Unit,
    onBan: (String) -> Unit,
) {
    val isHost = state.role == JamRole.Host

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!state.isConnected) {
            Text(
                text = "Connecting to the session…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ParticipantsSection(
            participants = state.participants,
            isHost = isHost,
            onKick = onKick,
            onBan = onBan,
        )

        if (isHost) {
            HorizontalDivider()

            Text(
                text = "Invite someone",
                style = MaterialTheme.typography.titleSmall,
            )
            RoomCodeBox(code = state.roomCode.orEmpty(), onShare = onShareCode)
        }

        HorizontalDivider()

        Text(
            text = "The queue and playback controls are in the player at the bottom of the app — " +
                "the jam queue is shared with every participant.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LeaveButton(onLeave)
    }
}

@Composable
private fun ParticipantsSection(
    participants: List<dev.krtirtho.spotube.core.jam.JamParticipant>,
    isHost: Boolean,
    onKick: (String) -> Unit,
    onBan: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Participants (${participants.size})",
            style = MaterialTheme.typography.titleSmall,
        )
        participants.forEach { participant ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = participant.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (participant.isHost) {
                    Text(
                        text = "Host",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (isHost && !participant.isHost) {
                    OutlinedButton(
                        onClick = { onKick(participant.id) },
                        modifier = Modifier.height(32.dp),
                    ) {
                        Text("Kick", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { onBan(participant.id) },
                        modifier = Modifier.height(32.dp),
                    ) {
                        Text("Ban", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomCodeBox(
    code: String,
    onShare: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val viewModel: JamViewModel = koinViewModel()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectionContainer {
            Text(
                text = code,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                Text("Copy")
            }
            if (viewModel.supportsNativeShare) {
                OutlinedButton(onClick = onShare) {
                    Text("Share")
                }
            }
        }
    }
}

@Composable
private fun LeaveButton(onLeave: () -> Unit) {
    OutlinedButton(onClick = onLeave) {
        Text("Leave Session")
    }
}