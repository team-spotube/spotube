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

package dev.krtirtho.spotube.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import dev.krtirtho.spotube.core.ui.base.GhostIconButton
import dev.krtirtho.spotube.core.ui.base.IconButton
import dev.krtirtho.spotube.core.ui.misc.shimmerApply
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.Iconsax3DotsMore
import dev.krtirtho.spotube.resources.iconsax.IconsaxAddSquare
import dev.krtirtho.spotube.resources.iconsax.IconsaxCd
import dev.krtirtho.spotube.resources.iconsax.IconsaxDirectboxReceive
import dev.krtirtho.spotube.resources.iconsax.IconsaxHeart
import dev.krtirtho.spotube.resources.iconsax.IconsaxHeart2
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusicCircle
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusicPlaylist
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusicSquareRemove
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusicSquareRemoveFilled
import dev.krtirtho.spotube.resources.iconsax.IconsaxNext
import dev.krtirtho.spotube.resources.iconsax.IconsaxShare

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
    val isBlacklisted: Boolean = false,
)

data class TrackOptionsContext(
    val currentTrackId: String? = null,
    val queueTrackIds: Set<String> = emptySet(),
    val savedTrackIds: Set<String> = emptySet(),
    val blacklistedTrackIds: Set<String> = emptySet(),
    val blacklistedArtistIds: Set<String> = emptySet(),
    val forceFavorite: Boolean = false,
) {
    fun stateFor(track: MetadataTrack): TrackOptionsState = TrackOptionsState(
        isInQueue = track.id in queueTrackIds,
        isCurrentlyPlaying = track.id == currentTrackId,
        isFavorite = forceFavorite || track.id in savedTrackIds,
        isBlacklisted = track.id in blacklistedTrackIds ||
            track.artists.any { it.id in blacklistedArtistIds },
    )
}

@Composable
fun TrackOptions(
    track: MetadataTrack,
    state: TrackOptionsState,
    onAction: (TrackOptionsAction) -> Unit,
    onAlbumClick: () -> Unit,
    modifier: Modifier = Modifier,
    isInJam: Boolean = false,
    isJamGuest: Boolean = false,
) {
    AdaptiveDropdownBottomSheet(
        items = buildTrackMenuItems(
            track = track,
            state = state,
            onAction = onAction,
            onAlbumClick = onAlbumClick,
            isInJam = isInJam,
            isJamGuest = isJamGuest,
        ),
        trigger = { onClick ->
            GhostIconButton(onClick = onClick) {
                Icon(
                    imageVector = Iconsax.Iconsax3DotsMore,
                    contentDescription = "Track options",
                    modifier = Modifier.shimmerApply()
                )
            }
        },
        headerDisplayMode = HeaderDisplayMode.OnlyInBottomSheet,
        header = { TrackOptionsSheetHeader(track = track) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsBottomSheet(
    track: MetadataTrack,
    state: TrackOptionsState,
    onDismiss: () -> Unit,
    onAction: (TrackOptionsAction) -> Unit,
    onAlbumClick: () -> Unit,
    isInJam: Boolean = false,
    isJamGuest: Boolean = false,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TrackOptionsSheetHeader(track = track)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
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
                    isJamGuest = isJamGuest,
                ).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = item.enabled,
                                onClick = item.onClick,
                            )
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item.icon?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = if (item.enabled) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                },
                            )
                        }
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (item.enabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                        )
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = (track.album?.thumbnails ?: track.thumbnails)?.firstOrNull()?.url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.small),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            track.album?.let { album ->
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun buildTrackMenuItems(
    track: MetadataTrack,
    state: TrackOptionsState,
    onAction: (TrackOptionsAction) -> Unit,
    onAlbumClick: () -> Unit,
    isInJam: Boolean = false,
    isJamGuest: Boolean = false,
): List<AdaptiveMenuItem> = buildList {
    if (isInJam) {
        add(
            AdaptiveMenuItem(
                icon = Iconsax.IconsaxAddSquare,
                label = "Add to Jam",
                onClick = { onAction(TrackOptionsAction.AddToJam) },
            ),
        )
    }

    add(
        AdaptiveMenuItem(
            icon = Iconsax.IconsaxMusicCircle,
            label = "Start radio",
            onClick = { onAction(TrackOptionsAction.StartRadio) },
        ),
    )

    // A guest's queue is the shared jam queue — mutating it locally is not
    // allowed, so queue actions are replaced by "Add to Jam".
    if (!isJamGuest) {
        if (!state.isInQueue && !state.isCurrentlyPlaying) {
            add(
                AdaptiveMenuItem(
                    icon = Iconsax.IconsaxNext,
                    label = "Play next",
                    onClick = { onAction(TrackOptionsAction.PlayNext) },
                ),
            )
        } else if (state.isInQueue && !state.isCurrentlyPlaying) {
            add(
                AdaptiveMenuItem(
                    icon = Iconsax.IconsaxNext,
                    label = "Move to next",
                    onClick = { onAction(TrackOptionsAction.PlayNext) },
                ),
            )
        }

        if (!state.isInQueue) {
            add(
                AdaptiveMenuItem(
                    icon = Iconsax.IconsaxAddSquare,
                    label = "Add to queue",
                    onClick = { onAction(TrackOptionsAction.AddToQueue) },
                ),
            )
        } else {
            add(
                AdaptiveMenuItem(
                    icon = Iconsax.IconsaxMusicSquareRemove,
                    label = "Remove from queue",
                    onClick = { onAction(TrackOptionsAction.RemoveFromQueue) },
                ),
            )
        }
    }

    add(
        AdaptiveMenuItem(
            icon = if (state.isFavorite) Iconsax.IconsaxHeart2 else Iconsax.IconsaxHeart,
            label = if (state.isFavorite) "Remove from favorites" else "Save as favorite",
            onClick = { onAction(TrackOptionsAction.ToggleFavorite) },
        ),
    )

    add(
        AdaptiveMenuItem(
            icon = Iconsax.IconsaxMusicPlaylist,
            label = "Add to playlist",
            onClick = { onAction(TrackOptionsAction.AddToPlaylist) },
        ),
    )

    add(
        AdaptiveMenuItem(
            icon = Iconsax.IconsaxDirectboxReceive,
            label = "Download",
            onClick = { onAction(TrackOptionsAction.Download) },
        ),
    )

    add(
        AdaptiveMenuItem(
            icon = Iconsax.IconsaxMusicSquareRemoveFilled,
            label = if (state.isBlacklisted) "Remove from blacklist" else "Add to blacklist",
            onClick = { onAction(TrackOptionsAction.ToggleBlacklist) },
        ),
    )

    if (!track.externalUri.isNullOrBlank()) {
        add(
            AdaptiveMenuItem(
                icon = Iconsax.IconsaxShare,
                label = "Share",
                onClick = { onAction(TrackOptionsAction.Share) },
            ),
        )
    }

    if (track.album != null) {
        add(
            AdaptiveMenuItem(
                icon = Iconsax.IconsaxCd,
                label = "Go to album",
                onClick = onAlbumClick,
            ),
        )
    }
}
