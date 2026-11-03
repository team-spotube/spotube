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

package org.spotube.spotube.core.ui.component


import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumModalBottomSheet
import com.composeunstyled.SheetDetent
import com.composeunstyled.rememberModalBottomSheetState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.ui.misc.shimmerApply
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.Iconsax3DotsMore
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.spotube.spotube.resources.iconsax.IconsaxCd
import org.spotube.spotube.resources.iconsax.IconsaxDirectboxReceive
import org.spotube.spotube.resources.iconsax.IconsaxHeart
import org.spotube.spotube.resources.iconsax.IconsaxHeart2
import org.spotube.spotube.resources.iconsax.IconsaxMusicCircle
import org.spotube.spotube.resources.iconsax.IconsaxMusicPlaylist
import org.spotube.spotube.resources.iconsax.IconsaxMusicSquareRemove
import org.spotube.spotube.resources.iconsax.IconsaxMusicSquareRemoveFilled
import org.spotube.spotube.resources.iconsax.IconsaxNext
import org.spotube.spotube.resources.iconsax.IconsaxShare
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.*

sealed interface TrackOptionsAction {
    data object AddToJam : TrackOptionsAction
    data object StartRadio : TrackOptionsAction
    data object PlayNext : TrackOptionsAction
    data object AddToQueue : TrackOptionsAction
    data object RemoveFromQueue : TrackOptionsAction
    data object ToggleFavorite : TrackOptionsAction
    data object AddToPlaylist : TrackOptionsAction
    data object Download : TrackOptionsAction
    data object ToggleBlacklist : TrackOptionsAction
    data object Share : TrackOptionsAction
}

data class TrackOptionsState(
    val isInQueue: Boolean = false,
    val isCurrentlyPlaying: Boolean = false,
    val isFavorite: Boolean = false,
    val isBlacklisted: Boolean = false)

data class TrackOptionsContext(
    val currentTrackId: String? = null,
    val queueTrackIds: Set<String> = emptySet(),
    val savedTrackIds: Set<String> = emptySet(),
    val blacklistedTrackIds: Set<String> = emptySet(),
    val blacklistedArtistIds: Set<String> = emptySet(),
    val forceFavorite: Boolean = false) {
    fun stateFor(track: MetadataTrack): TrackOptionsState = TrackOptionsState(
        isInQueue = track.id in queueTrackIds,
        isCurrentlyPlaying = track.id == currentTrackId,
        isFavorite = forceFavorite || track.id in savedTrackIds,
        isBlacklisted = track.id in blacklistedTrackIds ||
            track.artists.any { it.id in blacklistedArtistIds })
}

@Composable
fun TrackOptions(
    track: MetadataTrack,
    state: TrackOptionsState,
    onAction: (TrackOptionsAction) -> Unit,
    onAlbumClick: () -> Unit,
    modifier: Modifier = Modifier,
    isInJam: Boolean = false,
    isJamGuest: Boolean = false) {
    AdaptiveDropdownBottomSheet(
        items = buildTrackMenuItems(
            track = track,
            state = state,
            onAction = onAction,
            onAlbumClick = onAlbumClick,
            isInJam = isInJam,
            isJamGuest = isJamGuest),
        trigger = { onClick ->
            MillenniumIconButton(
                onClick = onClick,
                style = MillenniumIconButtonStyle.Quiet) {
                MillenniumIcon(
                    imageVector = Iconsax.Iconsax3DotsMore,
                    contentDescription = stringResource(Res.string.track_options_title),
                    modifier = Modifier.shimmerApply()
                )
            }
        },
        headerDisplayMode = HeaderDisplayMode.OnlyInBottomSheet,
        header = { TrackOptionsSheetHeader(track = track) },
        modifier = modifier)
}

@Composable
fun TrackOptionsBottomSheet(
    track: MetadataTrack,
    state: TrackOptionsState,
    onDismiss: () -> Unit,
    onAction: (TrackOptionsAction) -> Unit,
    onAlbumClick: () -> Unit,
    isInJam: Boolean = false,
    isJamGuest: Boolean = false) {
    val sheetState = rememberModalBottomSheetState(initialDetent = SheetDetent.FullyExpanded)
    LaunchedEffect(sheetState.currentDetent) {
        if (sheetState.currentDetent == SheetDetent.Hidden) {
            onDismiss()
        }
    }
    MillenniumModalBottomSheet(
        state = sheetState,
        contentPadding = PaddingValues(top = 10.dp, bottom = 8.dp),
        contentSpacing = 4.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TrackOptionsSheetHeader(track = track)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                buildTrackMenuItems(
                    track = track,
                    state = state,
                    onAction = { action ->
                        onAction(action)
                        onDismiss()
                    },
                    onAlbumClick = {
                        onAlbumClick()
                        onDismiss()
                    },
                    isInJam = isInJam,
                    isJamGuest = isJamGuest).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = item.enabled,
                                onClick = item.onClick)
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        item.icon?.let { icon ->
                            MillenniumIcon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = if (item.enabled) {
                                    AppTheme.colors.onSurface
                                } else {
                                    AppTheme.colors.onSurface.copy(alpha = 0.38f)
                                })
                        } ?: Spacer(modifier = Modifier.size(22.dp))
                        Text(
                            text = item.label,
                            style = AppTheme.typography.bodyLarge,
                            color = if (item.enabled) {
                                AppTheme.colors.onSurface
                            } else {
                                AppTheme.colors.onSurface.copy(alpha = 0.38f)
                            })
                    }
                }
            }
        }
    }
}

@Composable
fun TrackOptionsSheetHeader(track: MetadataTrack) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AsyncImage(
            model = (track.album?.thumbnails ?: track.thumbnails)?.firstOrNull()?.url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(AppTheme.shapes.small))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = AppTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            Text(
                text = track.artists.joinToString(", ") { it.name },
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            track.album?.let { album ->
                Text(
                    text = album.title,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun buildTrackMenuItems(
    track: MetadataTrack,
    state: TrackOptionsState,
    onAction: (TrackOptionsAction) -> Unit,
    onAlbumClick: () -> Unit,
    isInJam: Boolean = false,
    isJamGuest: Boolean = false): List<AdaptiveMenuItem> = buildList {
    if (isInJam) {
        add(
            AdaptiveMenuItem(
                icon = Iconsax.IconsaxAddSquare,
                label = stringResource(Res.string.track_options_add_to_jam),
                onClick = { onAction(TrackOptionsAction.AddToJam) }))
    }

    add(
        AdaptiveMenuItem(
            icon = Iconsax.IconsaxMusicCircle,
            label = stringResource(Res.string.track_options_start_radio),
            onClick = { onAction(TrackOptionsAction.StartRadio) }))

    // A guest's queue is the shared jam queue — mutating it locally is not
    // allowed, so queue actions are replaced by "Add to Jam".
    if (!isJamGuest) {
        if (!state.isInQueue && !state.isCurrentlyPlaying) {
            add(
                AdaptiveMenuItem(
                    icon = Iconsax.IconsaxNext,
                    label = stringResource(Res.string.track_options_play_next),
                    onClick = { onAction(TrackOptionsAction.PlayNext) }))
        } else if (state.isInQueue && !state.isCurrentlyPlaying) {
            add(
                AdaptiveMenuItem(
                    icon = Iconsax.IconsaxNext,
                    label = stringResource(Res.string.track_options_move_to_next),
                    onClick = { onAction(TrackOptionsAction.PlayNext) }))
        }

        if (!state.isInQueue) {
            add(
                AdaptiveMenuItem(
                    icon = Iconsax.IconsaxAddSquare,
                    label = stringResource(Res.string.player_add_to_queue_content_description),
                    onClick = { onAction(TrackOptionsAction.AddToQueue) }))
        } else {
            add(
                AdaptiveMenuItem(
                    icon = Iconsax.IconsaxMusicSquareRemove,
                    label = stringResource(Res.string.track_options_remove_from_queue),
                    onClick = { onAction(TrackOptionsAction.RemoveFromQueue) }))
        }
    }

    add(
        AdaptiveMenuItem(
            icon = if (state.isFavorite) Iconsax.IconsaxHeart2 else Iconsax.IconsaxHeart,
            label = if (state.isFavorite) stringResource(Res.string.track_options_remove_from_favorites) else stringResource(Res.string.track_options_save_as_favorite),
            onClick = { onAction(TrackOptionsAction.ToggleFavorite) }))

    add(
        AdaptiveMenuItem(
            icon = Iconsax.IconsaxMusicPlaylist,
            label = stringResource(Res.string.track_options_add_to_playlist),
            onClick = { onAction(TrackOptionsAction.AddToPlaylist) }))

    add(
        AdaptiveMenuItem(
            icon = Iconsax.IconsaxDirectboxReceive,
            label = stringResource(Res.string.track_options_download),
            onClick = { onAction(TrackOptionsAction.Download) }))

    add(
        AdaptiveMenuItem(
            icon = Iconsax.IconsaxMusicSquareRemoveFilled,
            label = if (state.isBlacklisted) stringResource(Res.string.blacklist_remove) else stringResource(Res.string.blacklist_add),
            onClick = { onAction(TrackOptionsAction.ToggleBlacklist) }))

    if (!track.externalUri.isNullOrBlank()) {
        add(
            AdaptiveMenuItem(
                icon = Iconsax.IconsaxShare,
                label = stringResource(Res.string.track_options_share),
                onClick = { onAction(TrackOptionsAction.Share) }))
    }

    if (track.album != null) {
        add(
            AdaptiveMenuItem(
                icon = Iconsax.IconsaxCd,
                label = stringResource(Res.string.player_go_to_album),
                onClick = onAlbumClick))
    }
}
