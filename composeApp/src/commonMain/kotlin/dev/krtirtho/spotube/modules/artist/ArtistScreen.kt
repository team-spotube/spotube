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

package dev.krtirtho.spotube.modules.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerInterface
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerQueue
import dev.krtirtho.spotube.core.audioplayer.PlayerState
import dev.krtirtho.spotube.core.audioplayer.QueueEntry
import dev.krtirtho.spotube.core.jam.JamRole
import dev.krtirtho.spotube.core.jam.JamRoomService
import dev.krtirtho.spotube.core.navigation.NavigationCommands
import dev.krtirtho.spotube.core.navigation.Routes
import org.koin.compose.koinInject
import dev.krtirtho.spotube.core.ui.base.PrimaryButton
import dev.krtirtho.spotube.core.ui.base.PrimaryIconButton
import dev.krtirtho.spotube.core.ui.base.SecondaryButton
import dev.krtirtho.spotube.core.ui.base.SecondaryIconButton
import dev.krtirtho.spotube.core.ui.component.AlbumCard
import dev.krtirtho.spotube.core.ui.component.ApplicationMainBar
import dev.krtirtho.spotube.core.ui.component.ArtistCard
import dev.krtirtho.spotube.core.ui.component.PlaylistCard
import dev.krtirtho.spotube.core.ui.component.TrackList
import dev.krtirtho.spotube.core.ui.component.cards.PlayableCard
import dev.krtirtho.spotube.core.ui.component.dragScrollable
import dev.krtirtho.spotube.core.ui.misc.SkeletonTree
import dev.krtirtho.spotube.modules.library.playlist.AddToPlaylistPicker
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxAddSquare
import dev.krtirtho.spotube.resources.iconsax.IconsaxPlay
import dev.krtirtho.spotube.resources.iconsax.IconsaxUserRemove
import dev.krtirtho.spotube.resources.iconsax.User
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

@Composable
fun ArtistScreen(
    viewModel: ArtistViewModel,
    audioPlayerQueue: AudioPlayerQueue,
    audioPlayer: AudioPlayerInterface,
    navigationCommands: NavigationCommands
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val jamRoomService: JamRoomService = koinInject()
    val jamActive by jamRoomService.role.map { it != null }
        .collectAsStateWithLifecycle(initialValue = false)
    val isJamGuest by jamRoomService.role.map { it == JamRole.Guest }
        .collectAsStateWithLifecycle(initialValue = false)
    val currentQueueEntry by audioPlayerQueue.currentQueueEntryFlow.collectAsStateWithLifecycle()
    val playerState by audioPlayer.playerStateFlow.collectAsStateWithLifecycle()
    val savedArtistIds by viewModel.savedArtistIds.collectAsStateWithLifecycle()
    val blacklistedArtistIds by viewModel.blacklistedArtistIds.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val trackOptionsContext by viewModel.trackOptionsContext.collectAsStateWithLifecycle()
    val showAddToPlaylistPicker by viewModel.showAddToPlaylistPicker.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { ApplicationMainBar() }
    ) { innerPadding ->
        when (val currentState = state) {
            is ArtistScreenState.Loading -> {
                ArtistLoadingContent(innerPadding)
            }

            is ArtistScreenState.Error -> {
                ArtistErrorContent(
                    innerPadding = innerPadding,
                    message = currentState.message,
                    onRetry = { viewModel.refresh() }
                )
            }

            is ArtistScreenState.Loaded -> {
                val listState = rememberLazyListState()

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    item {
                        ArtistHeaderCard(
                            artist = currentState.artist,
                            isSaved = savedArtistIds.contains(currentState.artist.id),
                            onFollowClick = viewModel::toggleSavedArtist,
                            isBlacklisted = currentState.artist.id in blacklistedArtistIds,
                            onBlacklistClick = { viewModel.toggleArtistBlacklist(currentState.artist) },
                        )
                    }

                    item {
                        TopTracksHeader(
                            onPlay = viewModel::playTopTracks,
                            onAddToQueue = viewModel::addTopTracksToQueue,
                        )
                    }

                    item {
                        TrackList(
                            tracks = currentState.topTracks,
                            error = null,
                            hasMore = false,
                            isLoading = false,
                            isLoadingNextPage = false,
                            currentTrackId = (currentQueueEntry as? QueueEntry.StreamingTrack)?.track?.id,
                            isCurrentTrackPlaying = playerState == PlayerState.PLAYING,
                            onTrackClick = viewModel::playTopTracksFromTrack,
                            onTrackOptionsAction = viewModel::handleTrackOptionsAction,
                            trackOptionsState = trackOptionsContext::stateFor,
                            onArtistClick = { trackArtist ->
                                navigationCommands.navigateTo(
                                    Routes.Artist(
                                        trackArtist.id
                                    )
                                )
                            },
                            onAlbumClick = { album ->
                                navigationCommands.navigateTo(
                                    Routes.Album(
                                        album.id
                                    )
                                )
                            },
                            onLoadNextPage = {},
                            simplified = true,
                            scrollable = false,
                            onBulkDownload = viewModel::downloadTracks,
                            onBulkAddToQueue = viewModel::addTracksToQueue,
                            onBulkPlayNext = viewModel::playTracksNext,
                            onBulkAddToPlaylist = viewModel::showAddToPlaylistPicker,
                            onBulkAddToJam = viewModel::addTracksToJam,
                            isInJam = jamActive,
                            isJamGuest = isJamGuest,
                        )
                    }

                    if (currentState.albums.isNotEmpty() || currentState.albumsNextPagination != null) {
                        item {
                            AlbumsSection(
                                albums = currentState.albums,
                                onAlbumClick = { album ->
                                    navigationCommands.navigateTo(
                                        Routes.Album(
                                            album.id
                                        )
                                    )
                                },
                                onLoadMore = { viewModel.loadMoreAlbums() },
                            )
                        }
                    }

                    if (currentState.relatedArtists.isNotEmpty() || currentState.relatedArtistsNextPagination != null) {
                        item {
                            RelatedArtistsSection(
                                artists = currentState.relatedArtists,
                                onArtistClick = { artist ->
                                    navigationCommands.navigateTo(
                                        Routes.Artist(
                                            artist.id
                                        )
                                    )
                                },
                                onLoadMore = { viewModel.loadMoreRelatedArtists() },
                            )
                        }
                    }

                    if (currentState.featuredPlaylists.isNotEmpty() || currentState.featuredPlaylistsNextPagination != null) {
                        item {
                            FeaturedPlaylistsSection(
                                playlists = currentState.featuredPlaylists,
                                onPlaylistClick = { playlist ->
                                    navigationCommands.navigateTo(
                                        Routes.Playlist(
                                            playlist.id
                                        )
                                    )
                                },
                                onLoadMore = { viewModel.loadMoreFeaturedPlaylists() },
                            )
                        }
                    }
                }
            }
        }

        AddToPlaylistPicker(
            visible = showAddToPlaylistPicker,
            currentUserId = currentUserId,
            onDismiss = viewModel::dismissAddToPlaylistPicker,
            onPlaylistSelected = viewModel::addTracksToPlaylist,
        )
    }
}

@Composable
private fun ArtistLoadingContent(innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center,
    ) {
        SkeletonTree(true) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    ) {
                        items(4) {
                            PlayableCard(
                                title = "Item Title",
                                subtitle = "Subtitle",
                                imageURL = "https://placehold.co/600x400",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistErrorContent(
    innerPadding: PaddingValues,
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
            )
            PrimaryButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun ArtistHeaderCard(
    artist: MetadataArtist.Detailed,
    isSaved: Boolean,
    onFollowClick: () -> Unit,
    isBlacklisted: Boolean,
    onBlacklistClick: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isCompact = maxWidth < 600.dp

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            shape = RoundedCornerShape(20.dp),
        ) {
            if (isCompact) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ArtistAvatar(
                        artist = artist,
                        size = 180.dp,
                    )

                    ArtistMeta(
                        artist = artist,
                        isCompact = true,
                    )

                    ArtistHeaderActions(
                        isSaved = isSaved,
                        onFollowClick = onFollowClick,
                        isBlacklisted = isBlacklisted,
                        onBlacklistClick = onBlacklistClick,
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    ArtistAvatar(
                        artist = artist,
                        size = 220.dp,
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        ArtistMeta(
                            artist = artist,
                            isCompact = false,
                        )

                        ArtistHeaderActions(
                            isSaved = isSaved,
                            onFollowClick = onFollowClick,
                            isBlacklisted = isBlacklisted,
                            onBlacklistClick = onBlacklistClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistAvatar(
    artist: MetadataArtist.Detailed,
    size: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        val imageUrl = artist.thumbnails.firstOrNull()?.url.orEmpty()
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = artist.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Iconsax.User,
                contentDescription = artist.name,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.4f),
            )
        }
    }
}

@Composable
private fun ArtistMeta(
    artist: MetadataArtist.Detailed,
    isCompact: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCompact) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = artist.name,
            style = if (isCompact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = if (isCompact) TextAlign.Center else TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = buildString {
                append(formatFollowers(artist.followersCount))
                if (artist.genres.isNotEmpty()) {
                    append(" • ")
                    append(artist.genres.joinToString(", "))
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = if (isCompact) TextAlign.Center else TextAlign.Start,
        )

        artist.biography?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isCompact) 4 else 6,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (isCompact) TextAlign.Center else TextAlign.Start,
            )
        }
    }
}

@Composable
private fun ArtistHeaderActions(
    isSaved: Boolean,
    onFollowClick: () -> Unit,
    isBlacklisted: Boolean,
    onBlacklistClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSaved) {
            SecondaryButton(onClick = onFollowClick) {
                Text("Following", modifier = Modifier.width(65.dp), textAlign = TextAlign.Center)
            }
        } else {
            PrimaryButton(onClick = onFollowClick) {
                Text("Follow", modifier = Modifier.width(65.dp), textAlign = TextAlign.Center)
            }
        }

        if (isBlacklisted) {
            SecondaryIconButton(onClick = onBlacklistClick) {
                Icon(
                    imageVector = Iconsax.IconsaxUserRemove,
                    contentDescription = "Remove from blacklist",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        } else {
            SecondaryIconButton(onClick = onBlacklistClick) {
                Icon(
                    imageVector = Iconsax.IconsaxUserRemove,
                    contentDescription = "Add to blacklist",
                )
            }
        }
    }
}

@Composable
private fun TopTracksHeader(
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Top Tracks",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryIconButton(onClick = onPlay) {
                Icon(
                    imageVector = Iconsax.IconsaxPlay,
                    contentDescription = "Play top tracks",
                )
            }
            SecondaryIconButton(onClick = onAddToQueue) {
                Icon(
                    imageVector = Iconsax.IconsaxAddSquare,
                    contentDescription = "Add top tracks to queue",
                )
            }
        }
    }
}

@Composable
private fun AlbumsSection(
    albums: List<MetadataAlbum.Detailed>,
    onAlbumClick: (MetadataAlbum.Detailed) -> Unit,
    onLoadMore: () -> Unit,
) {
    val rowState = rememberLazyListState()

    LaunchedEffect(rowState) {
        snapshotFlow { rowState.layoutInfo }
            .map { layoutInfo ->
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                val totalItems = layoutInfo.totalItemsCount
                Pair(lastVisibleIndex, totalItems)
            }
            .distinctUntilChanged()
            .collect { (lastVisibleIndex, totalItems) ->
                if (lastVisibleIndex != null && totalItems > 0 && lastVisibleIndex >= totalItems - 2) {
                    onLoadMore()
                }
            }
    }

    SectionHeader(title = "Albums")
    Spacer(modifier = Modifier.size(8.dp))
    LazyRow(
        state = rowState,
        modifier = Modifier.dragScrollable(rowState),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        items(albums, key = { it.id }) { album ->
            AlbumCard(album = album)
        }
    }
}

@Composable
private fun RelatedArtistsSection(
    artists: List<MetadataArtist.Basic>,
    onArtistClick: (MetadataArtist.Basic) -> Unit,
    onLoadMore: () -> Unit,
) {
    val rowState = rememberLazyListState()

    LaunchedEffect(rowState) {
        snapshotFlow { rowState.layoutInfo }
            .map { layoutInfo ->
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                val totalItems = layoutInfo.totalItemsCount
                Pair(lastVisibleIndex, totalItems)
            }
            .distinctUntilChanged()
            .collect { (lastVisibleIndex, totalItems) ->
                if (lastVisibleIndex != null && totalItems > 0 && lastVisibleIndex >= totalItems - 2) {
                    onLoadMore()
                }
            }
    }

    SectionHeader(title = "Related Artists")
    Spacer(modifier = Modifier.size(8.dp))
    LazyRow(
        state = rowState,
        modifier = Modifier.dragScrollable(rowState),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        items(artists, key = { it.id }) { artist ->
            ArtistCard(artist = artist)
        }
    }
}

@Composable
private fun FeaturedPlaylistsSection(
    playlists: List<MetadataPlaylist>,
    onPlaylistClick: (MetadataPlaylist) -> Unit,
    onLoadMore: () -> Unit,
) {
    val rowState = rememberLazyListState()

    LaunchedEffect(rowState) {
        snapshotFlow { rowState.layoutInfo }
            .map { layoutInfo ->
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                val totalItems = layoutInfo.totalItemsCount
                Pair(lastVisibleIndex, totalItems)
            }
            .distinctUntilChanged()
            .collect { (lastVisibleIndex, totalItems) ->
                if (lastVisibleIndex != null && totalItems > 0 && lastVisibleIndex >= totalItems - 2) {
                    onLoadMore()
                }
            }
    }

    SectionHeader(title = "Featured Playlists")
    Spacer(modifier = Modifier.size(8.dp))
    LazyRow(
        state = rowState,
        modifier = Modifier.dragScrollable(rowState),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        items(playlists, key = { it.id }) { playlist ->
            PlaylistCard(playlist = playlist)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

private fun formatFollowers(count: Int?): String {
    if (count == null) return "Followers unavailable"
    return when {
        count >= 1_000_000_000 -> "${formatAbbreviatedCount(count, 1_000_000_000)}B followers"
        count >= 1_000_000 -> "${formatAbbreviatedCount(count, 1_000_000)}M followers"
        count >= 1_000 -> "${formatAbbreviatedCount(count, 1_000)}K followers"
        else -> "$count followers"
    }
}

private fun formatAbbreviatedCount(count: Int, divisor: Int): String {
    val scaled = count / divisor.toDouble()
    val rounded = (scaled * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}
