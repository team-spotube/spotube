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

package org.spotube.spotube.modules.devices


import org.spotube.millennium.composables.MillenniumDialogHeader
import org.spotube.millennium.composables.MillenniumDialogTitle
import org.spotube.millennium.composables.MillenniumDialogClose
import org.spotube.millennium.composables.MillenniumDialogBody
import org.spotube.millennium.composables.MillenniumDialogFooter
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumDialog
import org.spotube.millennium.composables.MillenniumListTile
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.spotube.spotube.core.remote.ConnectionState
import org.spotube.spotube.core.remote.PlaybackDestinationAction
import org.spotube.spotube.core.remote.RemoteControlClient
import org.spotube.spotube.core.remote.RemotePlaybackController
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxCd
import org.spotube.spotube.resources.iconsax.IconsaxMirroringScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.devices_destination_action_add_to_queue
import spotube.composeapp.generated.resources.devices_destination_action_play
import spotube.composeapp.generated.resources.devices_destination_action_play_next
import spotube.composeapp.generated.resources.common_cancel
import spotube.composeapp.generated.resources.devices_destination_prompt
import spotube.composeapp.generated.resources.devices_destination_remote_device
import spotube.composeapp.generated.resources.devices_destination_remote_device_host
import spotube.composeapp.generated.resources.devices_destination_remote_subtitle
import spotube.composeapp.generated.resources.devices_destination_this_device
import spotube.composeapp.generated.resources.devices_destination_this_device_subtitle
import spotube.composeapp.generated.resources.devices_destination_title

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
        is ConnectionState.Connected -> stringResource(Res.string.devices_destination_remote_device_host, state.host)
        else -> stringResource(Res.string.devices_destination_remote_device)
    }

    val actionLabel = when (pendingRequest.action) {
        PlaybackDestinationAction.Play -> stringResource(Res.string.devices_destination_action_play)
        PlaybackDestinationAction.AddToQueue -> stringResource(Res.string.devices_destination_action_add_to_queue)
        PlaybackDestinationAction.PlayNext -> stringResource(Res.string.devices_destination_action_play_next)
    }

    MillenniumDialog(visible = true, onDismissRequest = controller::dismissPicker) {
        MillenniumDialogHeader {
            MillenniumDialogTitle(stringResource(Res.string.devices_destination_title))
            MillenniumDialogClose()
        }
        MillenniumDialogBody {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(Res.string.devices_destination_prompt, actionLabel, pendingRequest.title),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant)

            MillenniumListTile(
                onClick = controller::playLocally,
                modifier = Modifier.fillMaxWidth(),
                leading = {
                    MillenniumIcon(
                        imageVector = Iconsax.IconsaxCd,
                        contentDescription = null,
                        tint = AppTheme.colors.primary)
                },
                title = {
                    Text(
                        text = stringResource(Res.string.devices_destination_this_device),
                        style = AppTheme.typography.bodyLarge)
                },
                subtitle = {
                    Text(
                        text = stringResource(Res.string.devices_destination_this_device_subtitle),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant)
                })

            if (connectionState is ConnectionState.Connected) {
                MillenniumListTile(
                    onClick = controller::playOnRemote,
                    modifier = Modifier.fillMaxWidth(),
                    leading = {
                        MillenniumIcon(
                            imageVector = Iconsax.IconsaxMirroringScreen,
                            contentDescription = null,
                            tint = AppTheme.colors.primary)
                    },
                    title = {
                        Text(
                            text = remoteDeviceName,
                            style = AppTheme.typography.bodyLarge)
                    },
                    subtitle = {
                        Text(
                            text = stringResource(Res.string.devices_destination_remote_subtitle),
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.onSurfaceVariant)
                    })
            }

        }
        }
        MillenniumDialogFooter {
            MillenniumButton(
                onClick = controller::dismissPicker,
                style = MillenniumButtonStyle.Quiet) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    }
}
