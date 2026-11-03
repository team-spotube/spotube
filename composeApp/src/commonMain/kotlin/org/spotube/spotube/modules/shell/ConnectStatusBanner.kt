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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.Text
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronRight
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.spotube.core.ui.theming.AppTheme
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.connect_jam_hosting
import spotube.composeapp.generated.resources.connect_jam_joined
import spotube.composeapp.generated.resources.devices_remote_playing_on
import spotube.composeapp.generated.resources.devices_status_connected

/**
 * Slim "something is live" strip under the Home header: one row while remote-controlling a device,
 * one while in a Group Jam. Renders nothing when neither is active.
 */
@Composable
fun ConnectStatusBanner(
    onOpenRemote: () -> Unit,
    onOpenJam: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConnectStatusViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val remote = state.remote
    val jam = state.jam
    if (remote == null && jam == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (remote != null) StatusRow(remoteStatusLabel(remote), onOpenRemote)
        if (jam != null) StatusRow(jamStatusLabel(jam), onOpenJam)
    }
}

@Composable
private fun StatusRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.medium)
            .background(AppTheme.colors.primary.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LiveDot()
        Text(
            text = label,
            style = AppTheme.typography.labelLarge,
            color = AppTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        MillenniumIcon(
            imageVector = FeatherIcons.ChevronRight,
            contentDescription = null,
            tint = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
internal fun remoteStatusLabel(status: RemoteConnectStatus): String =
    status.deviceName?.let { stringResource(Res.string.devices_remote_playing_on, it) }
        ?: stringResource(Res.string.devices_status_connected)

@Composable
internal fun jamStatusLabel(status: JamConnectStatus): String = pluralStringResource(
    if (status.isHost) Res.plurals.connect_jam_hosting else Res.plurals.connect_jam_joined,
    status.listenerCount,
    status.listenerCount)

/** Small "something is live" marker, ringed so it reads on any background. */
@Composable
internal fun LiveDot(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .background(MillenniumTheme.colors.success, CircleShape)
            .border(2.dp, MillenniumTheme.colors.surface, CircleShape)
    )
}
