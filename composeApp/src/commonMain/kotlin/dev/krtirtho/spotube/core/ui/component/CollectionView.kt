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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import dev.krtirtho.spotube.core.ui.misc.TextWithShimmer
import dev.krtirtho.spotube.core.ui.misc.shimmerApply
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxPauseCircle
import dev.krtirtho.spotube.resources.iconsax.IconsaxPlayCircle2
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun CollectionView(
    title: String,
    description: String,
    imageURL: String,
    imageResource: DrawableResource? = null,
    ownerName: String,
    ownerImageURL: String?,
    onOwnerClick: () -> Unit,
    onPlay: () -> Unit,
    onShufflePlay: () -> Unit,
    onAddToQueue: () -> Unit,
    isPlaying: Boolean = false,
    isFollowing: Boolean = false,
    onFollowClick: () -> Unit = {},
    showFollowButton: Boolean = true,
    onEdit: (() -> Unit)? = null,
    sharedElementKey: String? = null,
    isLoading: Boolean = false,
    error: String? = null,
    onRetry: (() -> Unit)? = null,
    tracks: List<MetadataTrack> = emptyList(),
    hasMore: Boolean = false,
    isLoadingNextPage: Boolean = false,
    currentTrackId: String? = null,
    isCurrentTrackPlaying: Boolean = false,
    onTrackClick: (MetadataTrack) -> Unit = {},
    onLoadNextPage: () -> Unit = {},
    onTrackOptionsAction: (MetadataTrack, TrackOptionsAction) -> Unit = { _, _ -> },
    onArtistClick: (MetadataArtist.Basic) -> Unit = {},
    onAlbumClick: (MetadataAlbum.Detailed) -> Unit = {},
    onArtistsOverflowClick: (MetadataTrack) -> Unit = {},
    onBulkDownload: (List<MetadataTrack>) -> Unit = {},
    onBulkAddToQueue: (List<MetadataTrack>) -> Unit = {},
    onBulkPlayNext: (List<MetadataTrack>) -> Unit = {},
    onBulkAddToPlaylist: (List<MetadataTrack>) -> Unit = {},
    onBulkAddToJam: (List<MetadataTrack>) -> Unit = {},
    isInJam: Boolean = false,
    isJamGuest: Boolean = false,
    onAddToJam: (List<MetadataTrack>) -> Unit = {},
    trackOptionsState: (MetadataTrack) -> TrackOptionsState = { TrackOptionsState() },
    footerContent: (@Composable () -> Unit)? = null,
    trailingContent: @Composable () -> Unit = {},
) {
    val listState = rememberLazyListState()
    val isCollapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > 400
        }
    }

    Scaffold(
        topBar = {
            ApplicationMainBar(
                backButton = true,
                title = if (isCollapsed) {
                    {
                        CollapsedCollectionTitle(
                            title = title,
                            imageURL = imageURL,
                            imageResource = imageResource,
                        )
                    }
                } else {
                    {}
                },
                actions = if (isCollapsed && !isJamGuest) {
                    {
                        IconButton(onClick = onPlay) {
                            Icon(
                                imageVector = if (isPlaying) Iconsax.IconsaxPauseCircle else Iconsax.IconsaxPlayCircle2,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                            )
                        }
                    }
                } else {
                    {}
                },
            )
        }
    ) { innerPadding ->
        if (error != null && tracks.isEmpty() && !isLoading) {
            ErrorDisplay(
                errorMessage = error,
                onRetry = onRetry ?: {},
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            TrackList(
                modifier = Modifier.padding(innerPadding),
                state = listState,
                headerContent = {
                    if (isLoading && tracks.isEmpty()) {
                        dev.krtirtho.spotube.core.ui.misc.SkeletonTree(true) {
                            CollectionDetails(
                                title = title.ifBlank { "Loading..." },
                                description = description,
                                imageURL = imageURL,
                                imageResource = imageResource,
                                ownerName = ownerName.ifBlank { "Unknown" },
                                ownerImageURL = ownerImageURL,
                                onOwnerClick = onOwnerClick,
                                onPlay = onPlay,
                                onShufflePlay = onShufflePlay,
                                onAddToQueue = onAddToQueue,
                                isPlaying = isPlaying,
                                isFollowing = isFollowing,
                                onFollowClick = onFollowClick,
                                showFollowButton = showFollowButton,
                                onEdit = onEdit,
                                sharedElementKey = sharedElementKey,
                                isJamGuest = isJamGuest,
                                onAddToJam = { onAddToJam(tracks) },
                            )
                        }
                    } else {
                        CollectionDetails(
                            title = title,
                            description = description,
                            imageURL = imageURL,
                            imageResource = imageResource,
                            ownerName = ownerName,
                            ownerImageURL = ownerImageURL,
                            onOwnerClick = onOwnerClick,
                            onPlay = onPlay,
                            onShufflePlay = onShufflePlay,
                            onAddToQueue = onAddToQueue,
                            isPlaying = isPlaying,
                            isFollowing = isFollowing,
                            onFollowClick = onFollowClick,
                            showFollowButton = showFollowButton,
                            onEdit = onEdit,
                            sharedElementKey = sharedElementKey,
                            isJamGuest = isJamGuest,
                            onAddToJam = { onAddToJam(tracks) },
                        )
                    }
                },
                footerContent = footerContent,
                tracks = tracks,
                error = error,
                hasMore = hasMore,
                isLoading = isLoading,
                isLoadingNextPage = isLoadingNextPage,
                currentTrackId = currentTrackId,
                isCurrentTrackPlaying = isCurrentTrackPlaying,
                onTrackClick = onTrackClick,
                onLoadNextPage = onLoadNextPage,
                onTrackOptionsAction = onTrackOptionsAction,
                onArtistClick = onArtistClick,
                onAlbumClick = onAlbumClick,
                onArtistsOverflowClick = onArtistsOverflowClick,
                onBulkDownload = onBulkDownload,
                onBulkAddToQueue = onBulkAddToQueue,
                onBulkPlayNext = onBulkPlayNext,
                onBulkAddToPlaylist = onBulkAddToPlaylist,
                onBulkAddToJam = onBulkAddToJam,
                isInJam = isInJam,
                isJamGuest = isJamGuest,
                trackOptionsState = trackOptionsState,
            )
        }

        trailingContent()
    }
}

@Composable
private fun CollapsedCollectionTitle(
    title: String,
    imageURL: String,
    imageResource: DrawableResource? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .shimmerApply(),
            contentAlignment = Alignment.Center,
        ) {
            if (imageURL.isNotBlank()) {
                AsyncImage(
                    model = imageURL,
                    contentDescription = title,
                    modifier = Modifier.size(36.dp),
                    contentScale = ContentScale.Crop,
                )
            } else if (imageResource != null) {
                androidx.compose.foundation.Image(
                    painter = painterResource(imageResource),
                    contentDescription = title,
                    modifier = Modifier.size(36.dp),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = title.take(1).ifBlank { "?" }.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextWithShimmer(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}
