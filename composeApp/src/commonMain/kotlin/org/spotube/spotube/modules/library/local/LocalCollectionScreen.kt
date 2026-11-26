/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.local

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import org.spotube.millennium.composables.MillenniumDropdownMenu
import org.spotube.millennium.composables.MillenniumCard
import org.spotube.millennium.composables.MillenniumDropdownMenuItem
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumListTile
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.core.ui.component.CollectionDetails
import org.spotube.spotube.core.ui.component.cards.MediaArtwork
import org.spotube.spotube.core.ui.theming.AppIcon
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.core.ui.theming.engine.AppComponents
import org.spotube.spotube.core.ui.theming.engine.AppState
import org.spotube.spotube.core.ui.theming.engine.ContentSlots
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.Iconsax3DotsMore
import org.spotube.spotube.resources.iconsax.IconsaxPauseCircle
import org.spotube.spotube.resources.iconsax.IconsaxPlayCircle2
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.*

private const val LocalTrackCardTop = "local_tracks_top"
private const val LocalTrackCardBottom = "local_tracks_bottom"
private const val LocalTrackCardPrefix = "local_track:"

/** Same bounded, virtualized card backdrop as TrackList: the rows remain independent lazy items. */
private fun Modifier.localTrackCardBounds(list: LazyListState): Modifier = layout { measurable, constraints ->
    val info = list.layoutInfo
    val visible = info.visibleItemsInfo.filter {
        val key = it.key as? String
        key == LocalTrackCardTop || key == LocalTrackCardBottom || key?.startsWith(LocalTrackCardPrefix) == true
    }
    if (visible.isEmpty()) return@layout layout(constraints.maxWidth, constraints.maxHeight) {}
    val offset = -info.viewportStartOffset
    val viewport = info.viewportEndOffset - info.viewportStartOffset
    val overscan = viewport * 3
    val first = visible.first()
    val last = visible.last()
    val top = if (first.key == LocalTrackCardTop) first.offset + offset else -overscan
    val bottom = if (last.key == LocalTrackCardBottom) last.offset + last.size + offset else viewport + overscan
    val height = (bottom - top).coerceAtLeast(0)
    val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, top) }
}

/** Local collections reuse the online collection header, scroll behavior and track-row rhythm. */
@Composable
fun LocalCollectionScreen(
    title: String,
    description: String,
    artwork: ByteArray?,
    tracks: List<LocalCollectionTrack>,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onAddToQueue: () -> Unit,
    onTrackClick: (String) -> Unit,
    isPlaying: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onRemoveTrack: ((String) -> Unit)? = null,
    extraActions: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    emptyContent: (@Composable () -> Unit)? = null,
    artworkShape: Shape? = null,
    placeholderIcon: ImageVector? = null,
    sharedElementKey: String? = null,
    /** Used by the headless layout gallery, which has no desktop Window for ApplicationMainBar. */
    showTopBar: Boolean = true,
) {
    val listState = rememberLazyListState()
    val collapsed by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 400 } }
    Column(Modifier.fillMaxSize()) {
        if (showTopBar) ApplicationMainBar(
            backButton = true,
            title = { if (collapsed) Text(title) },
            actions = {
                if (collapsed) MillenniumIconButton(onClick = onPlay, style = MillenniumIconButtonStyle.Quiet) {
                    AppIcon(
                        if (isPlaying) Iconsax.IconsaxPauseCircle else Iconsax.IconsaxPlayCircle2,
                        stringResource(if (isPlaying) Res.string.player_pause_content_description else Res.string.player_play_content_description)
                    )
                }
            }
        )
        Box(Modifier.fillMaxSize().widthIn(max = 1280.dp).padding(horizontal = 16.dp)) {
        if (tracks.isNotEmpty()) MillenniumCard(
            modifier = Modifier.matchParentSize().localTrackCardBounds(listState),
            contentPadding = PaddingValues(0.dp)
        ) {}
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = LocalAppShellBottomInset.current + 16.dp)
        ) {
            item(key = "local_collection_header") {
                Box(Modifier.fillMaxWidth()) {
                    CollectionDetails(
                        title = title, description = description,
                        imageURL = "", imageModel = artwork,
                        ownerName = "", ownerImageURL = null, onOwnerClick = {},
                        showOwner = false, showFollowButton = false,
                        onPlay = onPlay, onShufflePlay = onShuffle, onAddToQueue = onAddToQueue,
                        isPlaying = isPlaying, onEdit = onEdit,
                        extraActions = extraActions, artworkShape = artworkShape,
                        placeholderIcon = placeholderIcon,
                        sharedElementKey = sharedElementKey
                    )
                }
            }
            if (tracks.isEmpty()) {
                if (emptyContent != null) item(key = "local_collection_empty") {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { emptyContent() }
                }
            } else {
                item(key = LocalTrackCardTop) { Box(Modifier.fillMaxWidth().height(8.dp)) }
                itemsIndexed(tracks, key = { _, track -> LocalTrackCardPrefix + track.path }) { index, track ->
                    Box(Modifier.fillMaxWidth()) {
                        Column {
                            if (index > 0) MillenniumSeparator()
                            LocalCollectionTrackRow(index + 1, track, onTrackClick, onRemoveTrack)
                        }
                    }
                }
                item(key = LocalTrackCardBottom) { Box(Modifier.fillMaxWidth().height(8.dp)) }
            }
            if (footer != null) item(key = "local_collection_footer") {
                Box(Modifier.fillMaxWidth().padding(vertical = 12.dp)) { footer() }
            }
        }
        }
    }
}

@Composable
private fun LocalCollectionTrackRow(
    index: Int,
    track: LocalCollectionTrack,
    onTrackClick: (String) -> Unit,
    onRemoveTrack: ((String) -> Unit)?
) {
    val look = AppComponents.slot(ContentSlots.TrackRow)[AppState.Default]
    MillenniumListTile(
        onClick = { onTrackClick(track.path) },
        background = look.background,
        leading = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(look.leadingSpacing)) {
                Box(Modifier.width(look.indexWidth), contentAlignment = Alignment.Center) {
                    Text(index.toString(), style = look.indexStyle, textAlign = TextAlign.Center)
                }
                MediaArtwork(track.artwork, track.title, Modifier.size(look.artworkSize), shape = look.artworkShape)
            }
        },
        title = { Text(track.title, style = look.titleStyle, maxLines = 1) },
        subtitle = track.subtitle?.let { subtitle -> { Text(subtitle, style = look.artistStyle, maxLines = 1) } },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(look.trailingSpacing)) {
                track.durationLabel?.let { Text(it, style = look.durationStyle) }
                if (onRemoveTrack != null) {
                    // The destructive action is tucked into the row's options, not shown beside every title.
                    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    MillenniumDropdownMenu(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        anchor = {
                            MillenniumIconButton(onClick = { expanded = true }, style = MillenniumIconButtonStyle.Quiet) {
                                AppIcon(Iconsax.Iconsax3DotsMore, stringResource(Res.string.player_more_options_content_description))
                            }
                        }
                    ) {
                        MillenniumDropdownMenuItem(
                            onClick = { onRemoveTrack(track.path); expanded = false },
                            destructive = true,
                            label = { Text(stringResource(Res.string.local_playlist_remove_track)) }
                        )
                    }
                }
            }
        }
    )
}
