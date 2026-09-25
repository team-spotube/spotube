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

package dev.krtirtho.spotube.modules.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krtirtho.spotube.core.remote.ConnectionState
import dev.krtirtho.spotube.core.remote.PlaybackDestinationAction
import dev.krtirtho.spotube.core.remote.RemoteControlClient
import dev.krtirtho.spotube.core.remote.RemotePlaybackController
import dev.krtirtho.spotube.core.ui.base.ListRowTile
import dev.krtirtho.spotube.core.ui.base.ThemedDialog
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxCd
import dev.krtirtho.spotube.resources.iconsax.IconsaxMirroringScreen
import org.koin.compose.koinInject

/**
 * Globally hosted dialog shown when the user tries to play / add to queue /
 * play next and there is more than one place it could go (a connected remote
 * device and/or an active jam session). Lets the user choose the destination.
 */
@Composable
fun PlayDestinationPickerHost() {
    val controller = koinInject<RemotePlaybackController>()
    val remoteControlClient = koinInject<RemoteControlClient>()
    val request by controller.pendingRequest.collectAsStateWithLifecycle()
    val connectionState by remoteControlClient.connectionState.collectAsStateWithLifecycle()

    val pendingRequest = request ?: return

    val remoteDeviceName = when (val state = connectionState) {
        is ConnectionState.Connected -> "Remote Device (${state.host})"
        else -> "Remote Device"
    }

    val actionLabel = when (pendingRequest.action) {
        PlaybackDestinationAction.Play -> "Play"
        PlaybackDestinationAction.AddToQueue -> "Add to queue"
        PlaybackDestinationAction.PlayNext -> "Play next"
    }

    ThemedDialog(
        onDismissRequest = controller::dismissPicker,
        title = {
            Text(
                text = "Where to $actionLabel?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "$actionLabel \"${pendingRequest.title}\" on:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                ListRowTile(
                    onClick = controller::playLocally,
                    modifier = Modifier.fillMaxWidth(),
                    leading = {
                        Icon(
                            imageVector = Iconsax.IconsaxCd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    title = {
                        Text(
                            text = "This Device",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    subtitle = {
                        Text(
                            text = "$actionLabel here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )

                if (connectionState is ConnectionState.Connected) {
                    ListRowTile(
                        onClick = controller::playOnRemote,
                        modifier = Modifier.fillMaxWidth(),
                        leading = {
                            Icon(
                                imageVector = Iconsax.IconsaxMirroringScreen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        title = {
                            Text(
                                text = remoteDeviceName,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        subtitle = {
                            Text(
                                text = "$actionLabel on the connected device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }
        },
        actions = {
            TextButton(onClick = controller::dismissPicker) {
                Text("Cancel")
            }
        },
    )
}