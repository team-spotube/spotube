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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumType
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.common.Thumbnail
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import dev.krtirtho.spotube.core.ui.base.ButtonGroup
import dev.krtirtho.spotube.core.ui.base.ButtonGroupDivider
import dev.krtirtho.spotube.core.ui.base.Card
import dev.krtirtho.spotube.core.ui.base.CheckBox
import dev.krtirtho.spotube.core.ui.base.CheckBoxState
import dev.krtirtho.spotube.core.ui.base.GhostIconButton
import dev.krtirtho.spotube.core.ui.base.GroupIconButton
import dev.krtirtho.spotube.core.ui.base.LocalBaseUITheme
import dev.krtirtho.spotube.core.ui.base.TextField
import dev.krtirtho.spotube.core.ui.base.highlight
import dev.krtirtho.spotube.core.ui.misc.SkeletonTree
import dev.krtirtho.spotube.core.ui.misc.TextWithShimmer
import dev.krtirtho.spotube.core.ui.misc.shimmerApply
import dev.krtirtho.spotube.getPlatform
import dev.krtirtho.spotube.isDesktop
import dev.krtirtho.spotube.modules.shell.LocalAppShellBottomInset
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.Iconsax3DotsMore
import dev.krtirtho.spotube.resources.iconsax.IconsaxAddSquare
import dev.krtirtho.spotube.resources.iconsax.IconsaxDirectboxReceive
import dev.krtirtho.spotube.resources.iconsax.IconsaxFilterSearch
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusicPlaylist
import dev.krtirtho.spotube.resources.iconsax.IconsaxNext
import dev.krtirtho.spotube.resources.iconsax.IconsaxPause
import dev.krtirtho.spotube.resources.iconsax.IconsaxPlay
import dev.krtirtho.spotube.resources.iconsax.IconsaxSort

private val CompactTrackListBreakpoint = 600.dp
private val LargeTrackListBreakpoint = 840.dp
private const val ShimmerRowCount = 6

enum class TrackSortOption(val label: String) {
    None("Original"),
    Title("Title"),
    Artist("Artist"),
    Album("Album"),
    Duration("Duration"),
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TrackList(
    tracks: List<MetadataTrack> = emptyList(),
    error: String? = null,
    hasMore: Boolean = false,
    isLoading: Boolean = false,
    isLoadingNextPage: Boolean = false,
    showEmptyMessage: Boolean = true,
    headerContent: (@Composable () -> Unit)? = null,
    footerContent: (@Composable () -> Unit)? = null,
    onLoadNextPage: () -> Unit = {},
    onTrackClick: (MetadataTrack) -> Unit = {},
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
    currentTrackId: String? = null,
    isCurrentTrackPlaying: Boolean = false,
    trackOptionsState: (MetadataTrack) -> TrackOptionsState = { TrackOptionsState() },
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = LocalAppShellBottomInset.current),
    simplified: Boolean = false,
    scrollable: Boolean = true,
    state: LazyListState? = null,
) {
    var filterQuery by rememberSaveable { mutableStateOf("") }
    var sortBy by rememberSaveable { mutableStateOf(TrackSortOption.None) }
    var selectedTrackForOptions by remember { mutableStateOf<MetadataTrack?>(null) }
    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedTrackIds by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }

    val normalizedQuery = remember(filterQuery) { filterQuery.trim().lowercase() }
    val visibleTracks = remember(tracks, normalizedQuery, sortBy) {
        val filtered = if (normalizedQuery.isBlank()) {
            tracks
        } else {
            tracks.filter { track ->
                val searchableText = buildString {
                    append(track.title).append(' ')
                    append(track.album?.title.orEmpty()).append(' ')
                    append(track.artists.joinToString(" ") { it.name })
                }.lowercase()
                searchableText.contains(normalizedQuery)
            }
        }

        when (sortBy) {
            TrackSortOption.None -> filtered
            TrackSortOption.Title -> filtered.sortedBy { it.title.lowercase() }
            TrackSortOption.Artist -> filtered.sortedBy {
                it.artists.firstOrNull()?.name?.lowercase().orEmpty()
            }

            TrackSortOption.Album -> filtered.sortedBy { it.album?.title?.lowercase() }
            TrackSortOption.Duration -> filtered.sortedBy { it.durationMs }
        }
    }

    val listState = state ?: rememberLazyListState()
    val density = LocalDensity.current
    val shouldLoadMore = remember(density) {
        derivedStateOf {
            val loadMoreThreshold = with(density) { 200.dp.toPx() }
            val cardItem = listState.layoutInfo.visibleItemsInfo.find { it.key == "track-card" }
            // Trigger when the bottom of the track card is close to/inside the viewport,
            // which means the user has scrolled through (or is about to scroll through)
            // all loaded tracks.
            cardItem != null && cardItem.offset + cardItem.size <= listState.layoutInfo.viewportEndOffset + loadMoreThreshold
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && hasMore && !isLoading && !isLoadingNextPage) {
            onLoadNextPage()
        }
    }

    val windowInfo = LocalWindowInfo.current
    val maxWidth = with(density) { windowInfo.containerSize.width.toDp() }
    val isCompact = maxWidth < CompactTrackListBreakpoint
    val isDesktop = remember { getPlatform().isDesktop() }
    val showIndex = maxWidth >= LargeTrackListBreakpoint
    val showAlbum = !isCompact
    val useDropdownForOptions = isDesktop && !isCompact

    val trackCardContent: @Composable () -> Unit = {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp)
            ) {
                visibleTracks.forEachIndexed { displayedIndex, track ->
                    if (displayedIndex > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                    TrackListRow(
                        index = displayedIndex + 1,
                        track = track,
                        showIndex = showIndex,
                        showAlbum = showAlbum,
                        useDropdownForOptions = useDropdownForOptions,
                        isCurrentTrack = track.id == currentTrackId,
                        isCurrentTrackPlaying = isCurrentTrackPlaying,
                        isSelectionMode = isSelectionMode,
                        isSelected = selectedTrackIds.contains(track.id),
                        onTrackClick = {
                            if (isSelectionMode) {
                                selectedTrackIds =
                                    if (selectedTrackIds.contains(track.id)) {
                                        selectedTrackIds - track.id
                                    } else {
                                        selectedTrackIds + track.id
                                    }
                            } else if (isJamGuest) {
                                onTrackOptionsAction(track, TrackOptionsAction.AddToJam)
                            } else {
                                onTrackClick(track)
                            }
                        },
                        onLongClick = {
                            if (!useDropdownForOptions && !isSelectionMode) {
                                isSelectionMode = true
                                selectedTrackIds = setOf(track.id)
                            } else if (!useDropdownForOptions) {
                                selectedTrackForOptions = track
                            }
                        },
                        onSelectionToggle = { checked ->
                            isSelectionMode = true
                            selectedTrackIds = if (checked) {
                                selectedTrackIds + track.id
                            } else {
                                selectedTrackIds - track.id
                            }
                        },
                        onTrackOptionsAction = { action ->
                            onTrackOptionsAction(
                                track,
                                action
                            )
                        },
                        trackOptionsState = trackOptionsState(track),
                        isInJam = isInJam,
                        isJamGuest = isJamGuest,
                        onShowOptionsClick = { selectedTrackForOptions = track },
                        onArtistClick = onArtistClick,
                        onAlbumClick = onAlbumClick,
                        onArtistsOverflowClick = { onArtistsOverflowClick(track) },
                    )
                }

                if (isLoading && tracks.isEmpty()) {
                    repeat(ShimmerRowCount) { shimmerIndex ->
                        if (visibleTracks.isNotEmpty() || shimmerIndex > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                        ShimmerTrackListRow(
                            index = shimmerIndex + 1,
                            showIndex = showIndex,
                            showAlbum = showAlbum,
                            useDropdownForOptions = useDropdownForOptions,
                        )
                    }
                }

                if (error != null) {
                    if (visibleTracks.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                    TrackListFeedbackRow(
                        message = error,
                        isError = true,
                    )
                }

                if (showEmptyMessage && !isLoading && visibleTracks.isEmpty() && error == null) {
                    TrackListFeedbackRow("No tracks found")
                }

                if (isLoadingNextPage) {
                    if (visibleTracks.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                    ShimmerTrackListRow(
                        index = visibleTracks.size + 1,
                        showIndex = showIndex,
                        showAlbum = showAlbum,
                        useDropdownForOptions = useDropdownForOptions,
                    )
                }
            }
        }
    }

    val filterSortRow: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                .heightIn(max = 60.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        ) {
            if (showIndex || isSelectionMode) {
                val allSelected =
                    visibleTracks.isNotEmpty() && selectedTrackIds.size == visibleTracks.size
                val anySelected = selectedTrackIds.isNotEmpty()
                val headerState = when {
                    allSelected -> CheckBoxState.SELECTED
                    anySelected -> CheckBoxState.INDETERMINATE
                    else -> CheckBoxState.UNSELECTED
                }
                CheckBox(
                    state = headerState,
                    onClick = {
                        if (allSelected) {
                            isSelectionMode = false
                            selectedTrackIds = emptySet()
                        } else {
                            isSelectionMode = true
                            selectedTrackIds = visibleTracks.map { it.id }.toSet()
                        }
                    },
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
            ) {
                TextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .align(Alignment.CenterEnd),
                    placeholder = { TextWithShimmer("Filter") },
                    leadingIcon = {
                        Icon(
                            imageVector = Iconsax.IconsaxFilterSearch,
                            contentDescription = "Filter",
                        )
                    },
                    singleLine = true,
                )
            }

            ButtonGroup {
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    AdaptiveDropdownBottomSheet(
                        items = TrackSortOption.entries.map { option ->
                            AdaptiveMenuItem(
                                label = option.label,
                                onClick = { sortBy = option },
                                selected = sortBy == option,
                            )
                        },
                        trigger = { onClick ->
                            GroupIconButton(
                                onClick = onClick,
                            ) {
                                Icon(
                                    imageVector = Iconsax.IconsaxSort,
                                    contentDescription = "Sort",
                                )
                            }
                        },
                    )
                }
                ButtonGroupDivider()
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    val targetTracks = if (selectedTrackIds.isNotEmpty()) {
                        visibleTracks.filter { selectedTrackIds.contains(it.id) }
                    } else {
                        visibleTracks
                    }
                    val trackCount = targetTracks.size
                    val isAll =
                        selectedTrackIds.isEmpty() || trackCount == visibleTracks.size
                    AdaptiveDropdownBottomSheet(
                        items = buildList {
                            add(
                                AdaptiveMenuItem(
                                    icon = Iconsax.IconsaxDirectboxReceive,
                                    label = if (isAll) "Download All" else "Download $trackCount",
                                    onClick = { onBulkDownload(targetTracks) },
                                ),
                            )
                            if (!isJamGuest) {
                                add(
                                    AdaptiveMenuItem(
                                        icon = Iconsax.IconsaxAddSquare,
                                        label = if (isAll) "Add All to Queue" else "Add $trackCount to Queue",
                                        onClick = { onBulkAddToQueue(targetTracks) },
                                    ),
                                )
                                add(
                                    AdaptiveMenuItem(
                                        icon = Iconsax.IconsaxNext,
                                        label = if (isAll) "Play All Next" else "Play $trackCount Next",
                                        onClick = { onBulkPlayNext(targetTracks) },
                                    ),
                                )
                            }
                            add(
                                AdaptiveMenuItem(
                                    icon = Iconsax.IconsaxMusicPlaylist,
                                    label = if (isAll) "Add All to Playlist" else "Add $trackCount to Playlist",
                                    onClick = { onBulkAddToPlaylist(targetTracks) },
                                ),
                            )
                        } + if (isInJam) {
                            listOf(
                                AdaptiveMenuItem(
                                    icon = Iconsax.IconsaxAddSquare,
                                    label = if (isAll) "Add All to Jam" else "Add $trackCount to Jam",
                                    onClick = { onBulkAddToJam(targetTracks) },
                                ),
                            )
                        } else {
                            emptyList()
                        },
                        trigger = { onClick ->
                            GroupIconButton(
                                onClick = onClick,
                            ) {
                                Icon(
                                    imageVector = Iconsax.Iconsax3DotsMore,
                                    contentDescription = "Bulk actions",
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    if (scrollable) {
        Box(modifier = modifier.fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .widthIn(max = 1280.dp)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = if (isCompact) 6.dp else 16.dp, vertical = 8.dp),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (headerContent != null) {
                    item {
                        headerContent()
                    }
                }
                if (!simplified) {
                    item { filterSortRow() }
                }
                item(key = "track-card") { trackCardContent() }
                if (footerContent != null) {
                    item {
                        footerContent()
                    }
                }
            }

            if (!useDropdownForOptions) {
                selectedTrackForOptions?.let { track ->
                    TrackOptionsBottomSheet(
                        track = track,
                        state = trackOptionsState(track),
                        onDismiss = { selectedTrackForOptions = null },
                        onAction = { action ->
                            onTrackOptionsAction(track, action)
                            selectedTrackForOptions = null
                        },
                        onAlbumClick = { track.album?.let { onAlbumClick(it) } },
                        isInJam = isInJam,
                        isJamGuest = isJamGuest,
                    )
                }
            }
            VerticalScrollbar(listState, modifier = Modifier.align(Alignment.CenterEnd))
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = if (isCompact) 6.dp else 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (headerContent != null) {
                headerContent()
            }
            if (!simplified) {
                filterSortRow()
            }
            trackCardContent()
            if (footerContent != null) {
                footerContent()
            }
        }

        if (!useDropdownForOptions) {
            selectedTrackForOptions?.let { track ->
                TrackOptionsBottomSheet(
                    track = track,
                    state = trackOptionsState(track),
                    onDismiss = { selectedTrackForOptions = null },
                    onAction = { action ->
                        onTrackOptionsAction(track, action)
                        selectedTrackForOptions = null
                    },
                    onAlbumClick = { track.album?.let { onAlbumClick(it) } },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackListRow(
    index: Int,
    track: MetadataTrack,
    showIndex: Boolean,
    showAlbum: Boolean,
    useDropdownForOptions: Boolean,
    isCurrentTrack: Boolean,
    isCurrentTrackPlaying: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onTrackClick: () -> Unit,
    onLongClick: () -> Unit,
    onSelectionToggle: (Boolean) -> Unit,
    onTrackOptionsAction: (TrackOptionsAction) -> Unit,
    trackOptionsState: TrackOptionsState,
    isInJam: Boolean,
    isJamGuest: Boolean,
    onShowOptionsClick: () -> Unit,
    onArtistClick: (MetadataArtist.Basic) -> Unit,
    onAlbumClick: (MetadataAlbum.Detailed) -> Unit,
    onArtistsOverflowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowTheme = LocalBaseUITheme.current.listRowTile
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val artworkInteractionSource = remember { MutableInteractionSource() }
    val isArtworkHovered by artworkInteractionSource.collectIsHoveredAsState()
    val rowInteractionSource = remember { MutableInteractionSource() }
    val isRowHovered by rowInteractionSource.collectIsHoveredAsState()
    val rowBackgroundColor = when {
        isSelected -> rowTheme.background.selected
        isCurrentTrack && isCurrentTrackPlaying -> rowTheme.background.hovered
        isCurrentTrack -> rowTheme.background.focused
        trackOptionsState.isBlacklisted -> androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
        else -> rowTheme.background.normal
    }
    val highlightColor = Color.White.copy(
        alpha = if (isLight) 0.9f else 0.06f
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(rowInteractionSource)
            .combinedClickable(
                onClick = onTrackClick,
                onLongClick = onLongClick,
            )
            .background(rowBackgroundColor)
            .then(
                if (isRowHovered || isSelected || (isCurrentTrack && isCurrentTrackPlaying)) {
                    Modifier.highlight(highlightColor)
                } else {
                    Modifier
                }
            )
            .clip(MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .padding(end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (isSelectionMode) {
            CheckBox(
                state = if (isSelected) CheckBoxState.SELECTED else CheckBoxState.UNSELECTED,
                onClick = { onSelectionToggle(!isSelected) },
                modifier = Modifier.shimmerApply()
            )
        } else if (showIndex) {
            Box(
                modifier = Modifier.width(30.dp),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = isRowHovered,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "index-checkbox",
                ) { hovered ->
                    if (hovered) {
                        CheckBox(
                            state = if (isSelected) CheckBoxState.SELECTED else CheckBoxState.UNSELECTED,
                            onClick = { onSelectionToggle(!isSelected) },
                            modifier = Modifier.shimmerApply()
                        )
                    } else {
                        TextWithShimmer(
                            text = index.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(MaterialTheme.shapes.small)
                .hoverable(interactionSource = artworkInteractionSource),
            contentAlignment = Alignment.Center,
        ) {

            val imageUrl = (track.album?.thumbnails ?: track.thumbnails)?.firstOrNull()?.url
            val platformContext = LocalPlatformContext.current
            val imageRequest = remember(imageUrl) {
                ImageRequest.Builder(platformContext)
                    .data(imageUrl)
                    .size(128) // 44.dp * ~3x density = ~132px. 128 is a perfect power-of-2 size.
                    .crossfade(false) // Crucial for scroll performance
                    .build()
            }

            AsyncImage(
                model = imageRequest,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().shimmerApply(),
            )

            if (isArtworkHovered || isCurrentTrack) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isCurrentTrack && isCurrentTrackPlaying) Iconsax.IconsaxPause else Iconsax.IconsaxPlay,
                        contentDescription = if (isCurrentTrack && isCurrentTrackPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp).shimmerApply(),
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1.2f)) {
            TextWithShimmer(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ArtistChips(
                artists = track.artists,
                onArtistClick = onArtistClick,
                onArtistsOverflowClick = onArtistsOverflowClick,
            )
        }

        if (showAlbum && track.album != null) {
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            val isPressed by interactionSource.collectIsPressedAsState()
            val colorScheme = MaterialTheme.colorScheme

            Row(
                modifier = Modifier.weight(1f)
            ) {
                TextWithShimmer(
                    text = track.album!!.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .hoverable(interactionSource)
                        .clickable(
                            indication = null,
                            interactionSource = interactionSource
                        ) { onAlbumClick(track.album!!) }
                        // Hide ripple and show underline on hover/click
                        .then(
                            if (isHovered || isPressed) {
                                Modifier.drawWithCache {
                                    val underlineHeight = 1.dp.toPx()
                                    onDrawWithContent {
                                        drawContent()
                                        drawLine(
                                            color = colorScheme.primary,
                                            start = Offset(0f, size.height - underlineHeight),
                                            end = Offset(size.width, size.height - underlineHeight),
                                            strokeWidth = underlineHeight,
                                        )
                                    }
                                }
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }

        TextWithShimmer(
            text = track.durationMs.toDurationString(),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp),
        )

        if (useDropdownForOptions) {
            TrackOptions(
                track = track,
                state = trackOptionsState,
                onAction = onTrackOptionsAction,
                onAlbumClick = { track.album?.let { onAlbumClick(it) } },
                isInJam = isInJam,
                isJamGuest = isJamGuest,
            )
        } else {
            GhostIconButton(onClick = onShowOptionsClick) {
                Icon(
                    imageVector = Iconsax.Iconsax3DotsMore,
                    contentDescription = "Track options",
                )
            }
        }
    }
}

@Composable
private fun ArtistChips(
    artists: List<MetadataArtist.Basic>,
    onArtistClick: (MetadataArtist.Basic) -> Unit,
    onArtistsOverflowClick: () -> Unit,
) {
    val visibleArtists = artists.take(2)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        visibleArtists.forEachIndexed { index, artist ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                val isPressed by interactionSource.collectIsPressedAsState()

                TextWithShimmer(
                    text = artist.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isHovered || isPressed) TextDecoration.Underline else null,
                    modifier = Modifier
                        .clickable(
                        indication = null,
                        interactionSource = interactionSource
                    ) { onArtistClick(artist) }
                    ,
                )
                if (index < visibleArtists.size - 1)
                    Text(
                        ",",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
            }
        }

        val remaining = artists.size - visibleArtists.size
        if (remaining > 0) {
            TextWithShimmer(
                text = "+$remaining more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onArtistsOverflowClick),
            )
        }
    }
}

@Composable
private fun ShimmerTrackListRow(
    index: Int,
    showIndex: Boolean,
    showAlbum: Boolean,
    useDropdownForOptions: Boolean,
) {
    val dummyTrack = remember(index) { dummyTrackForShimmer(index) }
    SkeletonTree(true) {
        TrackListRow(
            index = index,
            track = dummyTrack,
            showIndex = showIndex,
            showAlbum = showAlbum,
            useDropdownForOptions = useDropdownForOptions,
            isCurrentTrack = false,
            isCurrentTrackPlaying = false,
            isSelectionMode = false,
            isSelected = false,
            onTrackClick = {},
            onLongClick = {},
            onSelectionToggle = {},
            onTrackOptionsAction = {},
            trackOptionsState = TrackOptionsState(),
            isInJam = false,
            isJamGuest = false,
            onShowOptionsClick = {},
            onArtistClick = {},
            onAlbumClick = {},
            onArtistsOverflowClick = {},
        )
    }
}

private fun dummyTrackForShimmer(index: Int): MetadataTrack {
    val artist = MetadataArtist.Basic(
        id = "shimmer_artist_$index",
        name = "Artist Name",
        thumbnails = emptyList(),
        externalUri = null,
    )
    return MetadataTrack(
        id = "shimmer_track_$index",
        title = "Track Title Here",
        durationMs = 210_000,
        trackNumber = index,
        discNumber = 1,
        artists = listOf(
            artist,
            artist.copy(id = "shimmer_artist_${index}_2", name = "Another Artist")
        ),
        album = MetadataAlbum.Detailed(
            releaseDate = null,
            genres = emptyList(),
            trackCount = 10,
            id = "shimmer_album_$index",
            title = "Album Name",
            description = null,
            thumbnails = emptyList(),
            albumType = MetadataAlbumType.Album,
            artists = listOf(artist),
            externalUri = null,
        ),
        thumbnails = null,
        explicit = false,
        popularity = 0,
        isrcCode = null,
        externalUri = null,
    )
}

@Composable
private fun TrackListFeedbackRow(
    message: String,
    isError: Boolean = false,
) {
    TextWithShimmer(
        text = message,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
    )
}

private fun Long.toDurationString(): String {
    val totalSeconds = (this / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

fun previewTracks(): List<MetadataTrack> {
    val artists = listOf(
        MetadataArtist.Basic(
            id = "artist_1",
            name = "Aria Vale",
            thumbnails = emptyList(),
            externalUri = null,
        ),
        MetadataArtist.Basic(
            id = "artist_2",
            name = "Neon Harbor",
            thumbnails = emptyList(),
            externalUri = null,
        ),
        MetadataArtist.Basic(
            id = "artist_3",
            name = "Echo Drift",
            thumbnails = emptyList(),
            externalUri = null,
        ),
    )

    val album = MetadataAlbum.Detailed(
        releaseDate = null,
        genres = emptyList(),
        trackCount = 10,
        id = "album_1",
        title = "Night Drive Archives",
        description = null,
        thumbnails = listOf(
            Thumbnail(
                url = "https://picsum.photos/120",
                width = 120,
                height = 120,
            )
        ),
        albumType = MetadataAlbumType.Album,
        artists = artists.take(2),
        externalUri = null,
    )

    return listOf(
        MetadataTrack(
            id = "track_1",
            title = "Signal Bloom",
            durationMs = 198_000,
            trackNumber = 1,
            discNumber = 1,
            artists = artists,
            album = album,
            thumbnails = null,
            explicit = false,
            popularity = 92,
            isrcCode = null,
            externalUri = null,
        ),
        MetadataTrack(
            id = "track_2",
            title = "Static Horizon",
            durationMs = 224_000,
            trackNumber = 2,
            discNumber = 1,
            artists = artists.take(2),
            album = album.copy(id = "album_2", title = "City Pulse"),
            explicit = false,
            popularity = 80,
            isrcCode = null,
            externalUri = null,
            thumbnails = null,
        ),
        MetadataTrack(
            id = "track_3",
            title = "Last Train Echo",
            durationMs = 246_000,
            trackNumber = 3,
            discNumber = 1,
            artists = artists.take(1),
            album = album.copy(id = "album_3", title = "Afterlight"),
            explicit = false,
            popularity = 77,
            isrcCode = null,
            externalUri = null,
            thumbnails = null,
        ),
    )
}

@Composable
@Preview
private fun TrackListPreview() {
    Scaffold {
        TrackList(
            tracks = previewTracks(),
            hasMore = true,
            isLoadingNextPage = true,
        )
    }
}
