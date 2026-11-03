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

package org.spotube.spotube.core.remote

import org.spotube.millennium.composables.MillenniumAlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.remote_request_allow
import spotube.composeapp.generated.resources.remote_request_allow_always
import spotube.composeapp.generated.resources.remote_request_deny
import spotube.composeapp.generated.resources.remote_request_message
import spotube.composeapp.generated.resources.remote_request_title

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
        MillenniumAlertDialog(
            visible = true,
            onDismissRequest = {
                handler.resolveConnectionRequest(request.sessionId, ConnectionRequestResponse.Deny)
                pendingRequest = null
            },
            title = { Text(stringResource(Res.string.remote_request_title)) },
            description = {
                Text(stringResource(Res.string.remote_request_message, request.deviceName))
            },
            ) {
                MillenniumButton(
                    onClick = {
                        handler.resolveConnectionRequest(request.sessionId, ConnectionRequestResponse.AllowAlways)
                        pendingRequest = null
                    },
                    style = MillenniumButtonStyle.Quiet) {
                    Text(stringResource(Res.string.remote_request_allow_always))
                }
                MillenniumButton(
                    onClick = {
                        handler.resolveConnectionRequest(request.sessionId, ConnectionRequestResponse.Deny)
                        pendingRequest = null
                    },
                    style = MillenniumButtonStyle.Secondary) {
                    Text(stringResource(Res.string.remote_request_deny))
                }
                MillenniumButton(
                    onClick = {
                        handler.resolveConnectionRequest(request.sessionId, ConnectionRequestResponse.Allow)
                        pendingRequest = null
                    }) {
                    Text(stringResource(Res.string.remote_request_allow))
                }
            }
    }
}
