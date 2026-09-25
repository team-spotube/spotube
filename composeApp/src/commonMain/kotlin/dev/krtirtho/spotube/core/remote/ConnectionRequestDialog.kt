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

package dev.krtirtho.spotube.core.remote

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import dev.krtirtho.spotube.core.ui.component.AdaptiveDialogBottomSheet
import org.koin.compose.koinInject

@Composable
fun ConnectionRequestDialogHost() {
    val handler: RemoteControlHandler = koinInject()
    var pendingRequest by remember { mutableStateOf<ConnectionRequest?>(null) }

    LaunchedEffect(handler) {
        handler.incomingConnectionRequests.collect { request ->
            pendingRequest = request
        }
    }

    pendingRequest?.let { request ->
        AdaptiveDialogBottomSheet(
            onDismiss = {
                handler.resolveConnectionRequest(request.sessionId, ConnectionRequestResponse.Deny)
                pendingRequest = null
            },
            title = {
                Text(
                    text = "Remote Control Request",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            content = {
                Text(
                    text = "\"${request.deviceName}\" wants to control playback on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                )
            },
            actions = {
                Button(
                    onClick = {
                        handler.resolveConnectionRequest(request.sessionId, ConnectionRequestResponse.Deny)
                        pendingRequest = null
                    },
                ) {
                    Text("Deny")
                }
                Button(
                    onClick = {
                        handler.resolveConnectionRequest(request.sessionId, ConnectionRequestResponse.Allow)
                        pendingRequest = null
                    },
                ) {
                    Text("Allow")
                }
                Button(
                    onClick = {
                        handler.resolveConnectionRequest(request.sessionId, ConnectionRequestResponse.AllowAlways)
                        pendingRequest = null
                    },
                ) {
                    Text("Allow Always")
                }
            },
        )
    }
}