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

package dev.krtirtho.spotube.modules.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import compose.icons.FeatherIcons
import compose.icons.feathericons.X
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.search.MetadataSupportedSearchType
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.user.MetadataUser
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerQueue
import dev.krtirtho.spotube.core.audioplayer.QueueEntry
import dev.krtirtho.spotube.core.navigation.NavigationCommands
import dev.krtirtho.spotube.core.navigation.Routes
import dev.krtirtho.spotube.core.jam.JamRole
import dev.krtirtho.spotube.core.jam.JamRoomService
import org.koin.compose.koinInject
import dev.krtirtho.spotube.core.remote.RemotePlaybackController
import dev.krtirtho.spotube.core.share.ShareService
import dev.krtirtho.spotube.core.ui.base.AutocompleteTextField
import dev.krtirtho.spotube.core.ui.base.ChipTab
import dev.krtirtho.spotube.core.ui.component.AlbumCard
import dev.krtirtho.spotube.core.ui.component.ApplicationMainBar
import dev.krtirtho.spotube.core.ui.component.ArtistCard
import dev.krtirtho.spotube.core.ui.component.ErrorDisplay
import dev.krtirtho.spotube.core.ui.component.PlaylistCard
import dev.krtirtho.spotube.core.ui.component.TrackList
import dev.krtirtho.spotube.core.ui.component.TrackOptionsAction
import dev.krtirtho.spotube.core.ui.component.TrackOptionsState
import dev.krtirtho.spotube.core.ui.component.UserCard
import dev.krtirtho.spotube.core.ui.component.cards.PlayableCard
import dev.krtirtho.spotube.core.ui.misc.SkeletonTree
import dev.krtirtho.spotube.modules.blacklist.BlacklistRepository
import dev.krtirtho.spotube.modules.downloads.DownloadsViewModel
import dev.krtirtho.spotube.modules.library.LibraryRepository
import dev.krtirtho.spotube.modules.library.playlist.AddToPlaylistPicker
import dev.krtirtho.spotube.modules.shell.LocalAppShellBottomInset
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxSearchBroken
import dev.krtirtho.spotube.resources.iconsax.IconsaxTrash
import dev.krtirtho.spotube.resources.iconsax.InconsaxClock
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private val GridMinCellSize = 180.dp

@Composable
fun SearchScreen(viewModel: SearchScreenViewModel = koinViewModel()) {
    val audioPlayerQueue: AudioPlayerQueue = koinInject()
    val shareService: ShareService = koinInject()
    val remotePlaybackController: RemotePlaybackController = koinInject()
    val downloadsViewModel: DownloadsViewModel = koinViewModel()
    val libraryRepository: LibraryRepository = koinInject()
    val blacklistRepository: BlacklistRepository = koinInject()
    val navigationCommands: NavigationCommands = koinInject()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val jamRoomService: JamRoomService = koinInject()
    val jamActive by jamRoomService.role.map { it != null }
        .collectAsStateWithLifecycle(initialValue = false)
    val isJamGuest by jamRoomService.role.map { it == JamRole.Guest }
        .collectAsStateWithLifecycle(initialValue = false)
    val selectedType = state.selectedSearchType
    val scope = rememberCoroutineScope()
    val savedTrackIds by viewModel.savedTrackIds.collectAsStateWithLifecycle()
    var showAddToPlaylistPicker by remember { mutableStateOf(false) }
    var tracksToAddToPlaylist by remember { mutableStateOf<List<MetadataTrack>>(emptyList()) }
    var currentUserId by remember { mutableStateOf<String?>(null) }

    val blacklistedTracks by blacklistRepository.blacklistedTracks.collectAsStateWithLifecycle(emptyList())
    val blacklistedArtists by blacklistRepository.blacklistedArtists.collectAsStateWithLifecycle(emptyList())

    fun isTrackBlacklisted(track: MetadataTrack): Boolean {
        val trackIds = blacklistedTracks.map { it.id }.toSet()
        val artistIds = blacklistedArtists.map { it.id }.toSet()
        return track.id in trackIds || track.artists.any { it.id in artistIds }
    }

    fun toggleTrackBlacklist(track: MetadataTrack) {
        scope.launch {
            blacklistRepository.toggleTrack(track)
        }
    }

    LaunchedEffect(Unit) {
        currentUserId = libraryRepository.currentUser()?.id
    }

    val focusManager = LocalFocusManager.current

    fun playSingleTrack(track: MetadataTrack) {
        scope.launch {
            audioPlayerQueue.load(
                entries = listOf(QueueEntry.StreamingTrack(track = track, url = "")),
                autoPlay = true,
                startPosition = 0,
                collectionEntry = null,
            )
        }
    }

    fun handleTrackOptionsAction(track: MetadataTrack, action: TrackOptionsAction) {
        scope.launch {
            when (action) {
                is TrackOptionsAction.StartRadio -> {}
                is TrackOptionsAction.PlayNext -> {
                    val queue = audioPlayerQueue.getQueue()
                    val existing = queue.find { entry ->
                        (entry as? QueueEntry.StreamingTrack)?.track?.id == track.id
                    }
                    if (existing != null) {
                        // Already in the local queue: move it to the next position
                        audioPlayerQueue.removeFromQueue(existing)
                        audioPlayerQueue.addAllAfterCurrent(listOf(QueueEntry.StreamingTrack(track = track, url = "")))
                    } else {
                        remotePlaybackController.requestTrackPlayNext(track)
                    }
                }

                is TrackOptionsAction.AddToQueue -> {
                    remotePlaybackController.requestTrackAddToQueue(track)
                }
                is TrackOptionsAction.AddToJam -> {
                    remotePlaybackController.addTrackToJam(track)
                }

                is TrackOptionsAction.RemoveFromQueue -> {
                    val queue = audioPlayerQueue.getQueue()
                    queue.find { entry ->
                        (entry as? QueueEntry.StreamingTrack)?.track?.id == track.id
                    }?.let { audioPlayerQueue.removeFromQueue(it) }
                }

                is TrackOptionsAction.ToggleFavorite -> viewModel.toggleTrackIsFavorite(track.id)
                is TrackOptionsAction.Download -> downloadsViewModel.downloadTrack(track)
                is TrackOptionsAction.ToggleBlacklist -> toggleTrackBlacklist(track)
                is TrackOptionsAction.Share -> {
                    val uri = track.externalUri?.takeIf { it.isNotBlank() }
                    if (uri != null) {
                        shareService.share(uri, track.title)
                    }
                }
                is TrackOptionsAction.AddToPlaylist -> {
                    tracksToAddToPlaylist = listOf(track)
                    showAddToPlaylistPicker = true
                }
            }
        }
    }

    fun getTrackOptionsState(track: MetadataTrack): TrackOptionsState {
        val currentQueueEntry = audioPlayerQueue.currentQueueEntryFlow.value
        val currentTrackId = (currentQueueEntry as? QueueEntry.StreamingTrack)?.track?.id
        val queue = audioPlayerQueue.queueFlow.value
        val queueTrackIds = queue.mapNotNull { entry ->
            (entry as? QueueEntry.StreamingTrack)?.track?.id
        }.toSet()
        return TrackOptionsState(
            isInQueue = queueTrackIds.contains(track.id),
            isCurrentlyPlaying = track.id == currentTrackId,
            isFavorite = savedTrackIds.contains(track.id),
            isBlacklisted = isTrackBlacklisted(track),
        )
    }

    val queue by audioPlayerQueue.queueFlow.collectAsStateWithLifecycle(emptyList())
    val currentQueueEntry by audioPlayerQueue.currentQueueEntryFlow.collectAsStateWithLifecycle()

    fun getTrackOptionsStateReactive(track: MetadataTrack): TrackOptionsState {
        val currentTrackId = (currentQueueEntry as? QueueEntry.StreamingTrack)?.track?.id
        val queueTrackIds = queue.mapNotNull { entry ->
            (entry as? QueueEntry.StreamingTrack)?.track?.id
        }.toSet()
        return TrackOptionsState(
            isInQueue = queueTrackIds.contains(track.id),
            isCurrentlyPlaying = track.id == currentTrackId,
            isFavorite = savedTrackIds.contains(track.id),
            isBlacklisted = isTrackBlacklisted(track),
        )
    }

    fun bulkAddToQueue(tracks: List<MetadataTrack>) {
        remotePlaybackController.requestTracksAddToQueue(tracks, "Search results")
    }

    fun bulkAddToJam(tracks: List<MetadataTrack>) {
        remotePlaybackController.addTracksToJam(tracks)
    }

    fun bulkPlayNext(tracks: List<MetadataTrack>) {
        remotePlaybackController.requestTracksPlayNext(tracks, "Search results")
    }

    Scaffold(
        topBar = { ApplicationMainBar(backButton = false) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                SearchBar(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    onClear = viewModel::clearQuery,
                    onSearch = {
                        focusManager.clearFocus()
                    },
                    recentSearches = state.recentSearches,
                    onRecentSearchClick = { search ->
                        viewModel.applyRecentSearch(search)
                        focusManager.clearFocus()
                    },
                    onRecentSearchRemove = viewModel::removeRecentSearch,
                    onClearAllRecentSearches = {
                        viewModel.clearAllRecentSearches()
                        focusManager.clearFocus()
                    },
                )

                if (state.supportedSearchTypes.isNotEmpty()) {
                    SearchTabs(
                        types = state.supportedSearchTypes,
                        selectedType = selectedType,
                        onTabSelected = viewModel::onTabSelected,
                    )
                }
            }

            when {
                state.isLoadingSearchTypes -> {
                    SearchLoadingIndicator(
                        modifier = Modifier
                            .fillMaxSize(),
                    )
                }

                selectedType == null -> {
                    SearchMessage(
                        message = "No search types available",
                        modifier = Modifier
                            .fillMaxSize(),
                    )
                }

                selectedType == MetadataSupportedSearchType.ALL -> {
                    SearchAllTab(
                        query = state.query,
                        tracks = state.tracks,
                        albums = state.albums,
                        artists = state.artists,
                        playlists = state.playlists,
                        users = state.users,
                        supportedSearchTypes = state.supportedSearchTypes,
                        onSeeAll = viewModel::onTabSelected,
                        onTrackClick = ::playSingleTrack,
                        onTrackOptionsAction = ::handleTrackOptionsAction,
                        onTrackOptionsState = ::getTrackOptionsStateReactive,
                        onBulkDownload = { tracks -> downloadsViewModel.downloadTracks(tracks) },
                        onBulkAddToQueue = ::bulkAddToQueue,
                        onBulkPlayNext = ::bulkPlayNext,
                        onBulkAddToPlaylist = { tracks ->
                            tracksToAddToPlaylist = tracks
                            showAddToPlaylistPicker = true
                        },
                        onBulkAddToJam = ::bulkAddToJam,
                        isInJam = jamActive,
                        isJamGuest = isJamGuest,
                        onArtistClick = { artist ->
                            navigationCommands.navigateTo(Routes.Artist(artist.id))
                        },
                        onAlbumClick = { album ->
                            navigationCommands.navigateTo(Routes.Album(album.id))
                        },
                        onArtistsOverflowClick = { track ->
                            // No-op for now, could show a dialog with all artists
                        },
                        modifier = Modifier
                            .fillMaxSize(),
                    )
                }

                selectedType == MetadataSupportedSearchType.TRACK -> {
                    SearchTracksTab(
                        query = state.query,
                        tracks = state.tracks,
                        onLoadNextPage = viewModel::loadNextTracks,
                        onTrackClick = ::playSingleTrack,
                        onTrackOptionsAction = ::handleTrackOptionsAction,
                        onTrackOptionsState = ::getTrackOptionsStateReactive,
                        onBulkDownload = { tracks -> downloadsViewModel.downloadTracks(tracks) },
                        onBulkAddToQueue = ::bulkAddToQueue,
                        onBulkPlayNext = ::bulkPlayNext,
                        onBulkAddToPlaylist = { tracks ->
                            tracksToAddToPlaylist = tracks
                            showAddToPlaylistPicker = true
                        },
                        onBulkAddToJam = ::bulkAddToJam,
                        isInJam = jamActive,
                        isJamGuest = isJamGuest,
                        onArtistClick = { artist ->
                            navigationCommands.navigateTo(Routes.Artist(artist.id))
                        },
                        onAlbumClick = { album ->
                            navigationCommands.navigateTo(Routes.Album(album.id))
                        },
                        onArtistsOverflowClick = { track ->
                            // No-op for now, could show a dialog with all artists
                        },
                        modifier = Modifier
                            .fillMaxSize(),
                    )
                }

                selectedType == MetadataSupportedSearchType.PLAYLIST -> {
                    SearchPlaylistsTab(
                        query = state.query,
                        playlists = state.playlists,
                        onLoadNextPage = viewModel::loadNextPlaylists,
                        modifier = Modifier
                            .fillMaxSize(),
                    )
                }

                selectedType == MetadataSupportedSearchType.ALBUM -> {
                    SearchAlbumsTab(
                        query = state.query,
                        albums = state.albums,
                        onLoadNextPage = viewModel::loadNextAlbums,
                        modifier = Modifier
                            .fillMaxSize(),
                    )
                }

                selectedType == MetadataSupportedSearchType.ARTIST -> {
                    SearchArtistsTab(
                        query = state.query,
                        artists = state.artists,
                        onLoadNextPage = viewModel::loadNextArtists,
                        modifier = Modifier
                            .fillMaxSize(),
                    )
                }

                selectedType == MetadataSupportedSearchType.USER -> {
                    SearchUsersTab(
                        query = state.query,
                        users = state.users,
                        onLoadNextPage = viewModel::loadNextUsers,
                        modifier = Modifier
                            .fillMaxSize(),
                    )
                }
            }
        }

        AddToPlaylistPicker(
            visible = showAddToPlaylistPicker,
            currentUserId = currentUserId,
            onDismiss = { showAddToPlaylistPicker = false },
            onPlaylistSelected = { playlistId ->
                scope.launch {
                    libraryRepository.addTracksToPlaylist(
                        playlistId,
                        tracksToAddToPlaylist.map { it.id }
                    )
                }
            },
        )
    }
}

private sealed interface SearchDropdownEntry {
    val onClick: () -> Unit

    data class ClearAll(override val onClick: () -> Unit) : SearchDropdownEntry
    data class RecentSearch(
        val query: String,
        override val onClick: () -> Unit,
        val onRemove: () -> Unit,
    ) : SearchDropdownEntry
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    recentSearches: List<String>,
    onRecentSearchClick: (String) -> Unit,
    onRecentSearchRemove: (String) -> Unit,
    onClearAllRecentSearches: () -> Unit,
) {
    val showRecentSearches = query.isBlank() && recentSearches.isNotEmpty()
    val dropdownItems = remember(showRecentSearches, recentSearches) {
        buildList {
            if (showRecentSearches) {
                add(SearchDropdownEntry.ClearAll(onClick = onClearAllRecentSearches))
                recentSearches.forEach { search ->
                    add(
                        SearchDropdownEntry.RecentSearch(
                            query = search,
                            onClick = { onRecentSearchClick(search) },
                            onRemove = { onRecentSearchRemove(search) },
                        )
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
    ) {
        AutocompleteTextField(
            value = query,
            onValueChange = onQueryChange,
            items = dropdownItems,
            onItemSelected = { entry -> entry.onClick() },
            placeholder = {
                Text(
                    "Search songs, artists, albums...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Iconsax.IconsaxSearchBroken,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = FeatherIcons.X,
                            contentDescription = "Clear",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            itemContent = { entry, isSelected ->
                when (entry) {
                    is SearchDropdownEntry.ClearAll -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                                    else Color.Transparent
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Iconsax.IconsaxTrash,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Clear all history",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    is SearchDropdownEntry.RecentSearch -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                                    else Color.Transparent
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Iconsax.InconsaxClock,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    entry.query,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = entry.onRemove,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = FeatherIcons.X,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun SearchTabs(
    types: List<MetadataSupportedSearchType>,
    selectedType: MetadataSupportedSearchType?,
    onTabSelected: (MetadataSupportedSearchType) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(types) { type ->
            val isSelected = selectedType == type
            ChipTab(
                selected = isSelected,
                onClick = { onTabSelected(type) }
            ) {
                Text(type.tabTitle())
            }
        }
    }
}

@Composable
private fun SearchAllTab(
    query: String,
    tracks: SearchPagedState<MetadataTrack>,
    albums: SearchPagedState<MetadataAlbum.Basic>,
    artists: SearchPagedState<MetadataArtist.Basic>,
    playlists: SearchPagedState<MetadataPlaylist>,
    users: SearchPagedState<MetadataUser>,
    supportedSearchTypes: List<MetadataSupportedSearchType>,
    onSeeAll: (MetadataSupportedSearchType) -> Unit,
    onTrackClick: (MetadataTrack) -> Unit,
    onTrackOptionsAction: (MetadataTrack, TrackOptionsAction) -> Unit,
    onTrackOptionsState: (MetadataTrack) -> TrackOptionsState,
    onBulkDownload: (List<MetadataTrack>) -> Unit,
    onBulkAddToQueue: (List<MetadataTrack>) -> Unit,
    onBulkPlayNext: (List<MetadataTrack>) -> Unit,
    onBulkAddToPlaylist: (List<MetadataTrack>) -> Unit,
    onBulkAddToJam: (List<MetadataTrack>) -> Unit,
    isInJam: Boolean,
    isJamGuest: Boolean,
    onArtistClick: (MetadataArtist.Basic) -> Unit,
    onAlbumClick: (MetadataAlbum.Detailed) -> Unit,
    onArtistsOverflowClick: (MetadataTrack) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (query.isBlank()) {
        SearchMessage(message = "Start typing to search", modifier = modifier)
        return
    }

    val hasAnyContent =
        tracks.items.isNotEmpty() || playlists.items.isNotEmpty() || albums.items.isNotEmpty() ||
                artists.items.isNotEmpty() || users.items.isNotEmpty()

    val hasAnyError = listOfNotNull(
        tracks.error,
        playlists.error,
        albums.error,
        artists.error,
        users.error
    ).firstOrNull()
    val isLoadingAll =
        tracks.isLoading || playlists.isLoading || albums.isLoading || artists.isLoading || users.isLoading

    if (!hasAnyContent && !isLoadingAll && hasAnyError != null) {
        ErrorDisplay(
            errorMessage = hasAnyError,
            onRetry = { },
            modifier = modifier,
        )
        return
    }

    if (!hasAnyContent && !isLoadingAll) {
        SearchMessage(message = "No results found", modifier = modifier)
        return
    }

    val bottomInset = LocalAppShellBottomInset.current
    val showTracks =
        MetadataSupportedSearchType.TRACK in supportedSearchTypes && tracks.items.isNotEmpty()

    TrackList(
        tracks = if (showTracks) tracks.items.take(20) else emptyList(),
        isLoading = tracks.isLoading && showTracks,
        error = if (showTracks) tracks.error else null,
        hasMore = false,
        simplified = true,
        showEmptyMessage = false,
        modifier = modifier,
        onTrackClick = onTrackClick,
        onTrackOptionsAction = onTrackOptionsAction,
        trackOptionsState = onTrackOptionsState,
        onBulkDownload = onBulkDownload,
        onBulkAddToQueue = onBulkAddToQueue,
        onBulkPlayNext = onBulkPlayNext,
        onBulkAddToPlaylist = onBulkAddToPlaylist,
        onBulkAddToJam = onBulkAddToJam,
        isInJam = isInJam,
        isJamGuest = isJamGuest,
        onArtistClick = onArtistClick,
        onAlbumClick = onAlbumClick,
        onArtistsOverflowClick = onArtistsOverflowClick,
        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp + bottomInset),
        headerContent = {
            if (showTracks) {
                Text(
                    text = "Tracks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
                )
            }
            if (MetadataSupportedSearchType.TRACK in supportedSearchTypes) {
                if (tracks.isLoading && tracks.items.isEmpty()) {
                    SearchLoadingIndicator()
                }
                if (tracks.error != null && tracks.items.isEmpty()) {
                    SearchMessageInline(tracks.error, isError = true)
                }
            }
        },
        footerContent = {
            Column(
                modifier = Modifier.padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (MetadataSupportedSearchType.PLAYLIST in supportedSearchTypes && playlists.items.isNotEmpty()) {
                    SearchHorizontalSection(
                        title = "Playlists",
                        onSeeAll = { onSeeAll(MetadataSupportedSearchType.PLAYLIST) },
                    ) {
                        playlists.items.forEach { playlist ->
                            item(key = playlist.id) { PlaylistCard(playlist = playlist) }
                        }
                    }
                }

                if (MetadataSupportedSearchType.ALBUM in supportedSearchTypes && albums.items.isNotEmpty()) {
                    SearchHorizontalSection(
                        title = "Albums",
                        onSeeAll = { onSeeAll(MetadataSupportedSearchType.ALBUM) },
                    ) {
                        albums.items.forEach { album ->
                            item(key = album.id) { AlbumCard(album = album) }
                        }
                    }
                }

                if (MetadataSupportedSearchType.ARTIST in supportedSearchTypes && artists.items.isNotEmpty()) {
                    SearchHorizontalSection(
                        title = "Artists",
                        onSeeAll = { onSeeAll(MetadataSupportedSearchType.ARTIST) },
                    ) {
                        artists.items.forEach { artist ->
                            item(key = artist.id) { ArtistCard(artist = artist) }
                        }
                    }
                }

                if (MetadataSupportedSearchType.USER in supportedSearchTypes && users.items.isNotEmpty()) {
                    SearchHorizontalSection(
                        title = "Users",
                        onSeeAll = { onSeeAll(MetadataSupportedSearchType.USER) },
                    ) {
                        users.items.forEach { user ->
                            item(key = user.id) { UserCard(user = user) }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun SearchTracksTab(
    query: String,
    tracks: SearchPagedState<MetadataTrack>,
    onLoadNextPage: () -> Unit,
    onTrackClick: (MetadataTrack) -> Unit,
    onTrackOptionsAction: (MetadataTrack, TrackOptionsAction) -> Unit,
    onTrackOptionsState: (MetadataTrack) -> TrackOptionsState,
    onBulkDownload: (List<MetadataTrack>) -> Unit,
    onBulkAddToQueue: (List<MetadataTrack>) -> Unit,
    onBulkPlayNext: (List<MetadataTrack>) -> Unit,
    onBulkAddToPlaylist: (List<MetadataTrack>) -> Unit,
    onBulkAddToJam: (List<MetadataTrack>) -> Unit,
    isInJam: Boolean,
    isJamGuest: Boolean,
    onArtistClick: (MetadataArtist.Basic) -> Unit,
    onAlbumClick: (MetadataAlbum.Detailed) -> Unit,
    onArtistsOverflowClick: (MetadataTrack) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (query.isBlank()) {
        SearchMessage(message = "Start typing to search tracks", modifier = modifier)
        return
    }

    if (tracks.isLoading && tracks.items.isEmpty()) {
        SearchLoadingIndicator(message = "Loading tracks...", modifier = modifier)
        return
    }

    TrackList(
        tracks = tracks.items,
        error = tracks.error,
        hasMore = tracks.hasNextPage,
        isLoading = false,
        isLoadingNextPage = tracks.isLoading && tracks.items.isNotEmpty(),
        onLoadNextPage = onLoadNextPage,
        onTrackClick = onTrackClick,
        onTrackOptionsAction = onTrackOptionsAction,
        trackOptionsState = onTrackOptionsState,
        onBulkDownload = onBulkDownload,
        onBulkAddToQueue = onBulkAddToQueue,
        onBulkPlayNext = onBulkPlayNext,
        onBulkAddToPlaylist = onBulkAddToPlaylist,
        onBulkAddToJam = onBulkAddToJam,
        isInJam = isInJam,
        isJamGuest = isJamGuest,
        onArtistClick = onArtistClick,
        onAlbumClick = onAlbumClick,
        onArtistsOverflowClick = onArtistsOverflowClick,
        simplified = true,
        modifier = modifier,
    )
}

@Composable
private fun SearchAlbumsTab(
    query: String,
    albums: SearchPagedState<MetadataAlbum.Basic>,
    onLoadNextPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchGridTab(
        query = query,
        items = albums.items,
        isLoading = albums.isLoading,
        error = albums.error,
        hasNextPage = albums.hasNextPage,
        onLoadNextPage = onLoadNextPage,
        emptyMessage = "No albums found",
        loadingMessage = "Loading albums...",
        modifier = modifier,
    ) { album ->
        AlbumCard(album = album, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SearchArtistsTab(
    query: String,
    artists: SearchPagedState<MetadataArtist.Basic>,
    onLoadNextPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchGridTab(
        query = query,
        items = artists.items,
        isLoading = artists.isLoading,
        error = artists.error,
        hasNextPage = artists.hasNextPage,
        onLoadNextPage = onLoadNextPage,
        emptyMessage = "No artists found",
        loadingMessage = "Loading artists...",
        modifier = modifier,
    ) { artist ->
        ArtistCard(artist = artist, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SearchPlaylistsTab(
    query: String,
    playlists: SearchPagedState<MetadataPlaylist>,
    onLoadNextPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchGridTab(
        query = query,
        items = playlists.items,
        isLoading = playlists.isLoading,
        error = playlists.error,
        hasNextPage = playlists.hasNextPage,
        onLoadNextPage = onLoadNextPage,
        emptyMessage = "No playlists found",
        loadingMessage = "Loading playlists...",
        modifier = modifier,
    ) { playlist ->
        PlaylistCard(playlist = playlist, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SearchUsersTab(
    query: String,
    users: SearchPagedState<MetadataUser>,
    onLoadNextPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchGridTab(
        query = query,
        items = users.items,
        isLoading = users.isLoading,
        error = users.error,
        hasNextPage = users.hasNextPage,
        onLoadNextPage = onLoadNextPage,
        emptyMessage = "No users found",
        loadingMessage = "Loading users...",
        modifier = modifier,
    ) { user ->
        UserCard(user = user, modifier = Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> SearchGridTab(
    query: String,
    items: List<T>,
    isLoading: Boolean,
    error: String?,
    hasNextPage: Boolean,
    onLoadNextPage: () -> Unit,
    emptyMessage: String,
    loadingMessage: String,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit,
) {
    if (query.isBlank()) {
        SearchMessage(message = "Start typing to search", modifier = modifier)
        return
    }

    val gridState = rememberLazyGridState()
    val bottomInset = LocalAppShellBottomInset.current

    LaunchedEffect(gridState, items.size, hasNextPage, isLoading) {
        snapshotFlow { gridState.layoutInfo }
            .map { layoutInfo ->
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                lastVisible to layoutInfo.totalItemsCount
            }
            .distinctUntilChanged()
            .collect { (lastVisible, totalItems) ->
                if (totalItems > 0 && lastVisible >= totalItems - 6 && hasNextPage && !isLoading) {
                    onLoadNextPage()
                }
            }
    }

    when {
        isLoading && items.isEmpty() -> {
            SearchLoadingIndicator(
                message = loadingMessage,
                modifier = modifier,
            )
        }

        error != null && items.isEmpty() -> {
            ErrorDisplay(
                errorMessage = error,
                onRetry = onLoadNextPage,
                modifier = modifier,
            )
        }

        !isLoading && items.isEmpty() -> {
            SearchMessage(message = emptyMessage, modifier = modifier)
        }

        else -> {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = GridMinCellSize),
                modifier = modifier.padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp + bottomInset),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items) { item ->
                    itemContent(item)
                }

                if (isLoading && items.isNotEmpty()) {
                    items(4) {
                        SkeletonTree(true) {
                            PlayableCard(
                                title = "Sample Title",
                                subtitle = "Sample Subtitle",
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
private fun SearchHorizontalSection(
    title: String,
    onSeeAll: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = "See all",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onSeeAll),
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun SearchTrackRow(
    index: Int,
    track: MetadataTrack,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = (index + 1).toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
            )
            Text(
                text = track.artists.joinToString { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }

        if (track.album != null) Text(
            text = track.album!!.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
    }
}

private val WarmSearchMessages = listOf(
    "Digging through the crates...",
    "Searching the cosmos...",
    "Warming up the speakers...",
    "Dusting off the vinyl...",
    "Consulting the music gods...",
    "Tuning the antennas...",
    "Flipping through the records...",
)

@Composable
private fun SearchLoadingIndicator(
    message: String? = null,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "search-loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "icon-rotation",
    )
    val bounce by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "icon-bounce",
    )
    val messageIndex by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = WarmSearchMessages.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = WarmSearchMessages.size * 2000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "message-cycle",
    )
    val displayMessage =
        message ?: WarmSearchMessages[messageIndex.toInt() % WarmSearchMessages.size]

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Iconsax.IconsaxSearchBroken,
                contentDescription = "Searching",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                        translationY = bounce
                    },
            )
        }
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
        )
        AnimatedContent(
            targetState = displayMessage,
            transitionSpec = {
                fadeIn(tween(300)) togetherWith fadeOut(tween(300))
            },
            label = "message-transition",
        ) { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SearchMessage(
    message: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun SearchMessageInline(
    message: String,
    isError: Boolean = false,
) {
    Text(
        text = message,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun MetadataSupportedSearchType.tabTitle(): String = when (this) {
    MetadataSupportedSearchType.ALL -> "All"
    MetadataSupportedSearchType.TRACK -> "Tracks"
    MetadataSupportedSearchType.PLAYLIST -> "Playlists"
    MetadataSupportedSearchType.ALBUM -> "Albums"
    MetadataSupportedSearchType.ARTIST -> "Artists"
    MetadataSupportedSearchType.USER -> "Users"
}
