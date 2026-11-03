/*
 * Copyright (C) 2026 Kingmor Roy Tirtho and Spotube Contributors
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

package org.spotube.spotube.modules.blacklist


import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.core.ui.misc.shimmerApply
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxFilterSearch
import org.spotube.spotube.resources.iconsax.IconsaxMusicCircle
import org.spotube.spotube.resources.iconsax.IconsaxMusicSquareRemoveFilled
import org.spotube.spotube.resources.iconsax.IconsaxUserRemove
import org.spotube.spotube.resources.iconsax.User
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.*

@Composable
fun BlacklistScreen() {
    val viewModel = koinViewModel<BlacklistViewModel>()
    Column(modifier = Modifier.fillMaxSize()) {
        ApplicationMainBar(
            backButton = true,
            title = { Text(stringResource(Res.string.settings_blacklist_title)) })
        BlacklistContent(
            viewModel = viewModel,
            modifier = Modifier.weight(1f))
    }
}

/**
 * The blacklist UI without its own app bar, so it can be embedded
 * as a tab in the large-screen settings layout as well as hosted by [BlacklistScreen].
 */
@Composable
internal fun BlacklistContent(
    viewModel: BlacklistViewModel,
    modifier: Modifier = Modifier) {
    val blacklistedTracks by viewModel.blacklistedTracks.collectAsStateWithLifecycle()
    val blacklistedArtists by viewModel.blacklistedArtists.collectAsStateWithLifecycle()
    val shellBottomInset = LocalAppShellBottomInset.current

    val filterState = rememberTextFieldState("")
    val filterQuery = filterState.text.toString()
    val normalizedQuery = filterQuery.trim().lowercase()

    val filteredTracks = remember(blacklistedTracks, normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            blacklistedTracks
        } else {
            blacklistedTracks.filter { track ->
                val searchableText = buildString {
                    append(track.title).append(' ')
                    append(track.album?.title.orEmpty()).append(' ')
                    append(track.artists.joinToString(" ") { it.name })
                }.lowercase()
                searchableText.contains(normalizedQuery)
            }
        }
    }

    val filteredArtists = remember(blacklistedArtists, normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            blacklistedArtists
        } else {
            blacklistedArtists.filter { artist ->
                artist.name.lowercase().contains(normalizedQuery)
            }
        }
    }

    val isEmpty = filteredTracks.isEmpty() && filteredArtists.isEmpty()
    val hasActiveFilter = normalizedQuery.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MillenniumTextField(
            state = filterState,
            label = "",
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(Res.string.blacklist_filter_placeholder))

        if (isEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = shellBottomInset),
                contentAlignment = Alignment.Center) {
                Text(
                    text = if (hasActiveFilter) {
                        stringResource(Res.string.blacklist_no_results, filterQuery)
                    } else {
                        stringResource(Res.string.blacklist_empty)
                    },
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    bottom = 16.dp + shellBottomInset)) {
                if (filteredTracks.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(Res.string.blacklist_section_tracks),
                            style = AppTheme.typography.titleSmall,
                            color = AppTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(filteredTracks, key = { "track_${it.id}" }) { track ->
                        BlacklistedTrackRow(
                            track = track,
                            onRemove = { viewModel.toggleTrack(track) })
                    }
                }

                if (filteredArtists.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(Res.string.library_tab_artists),
                            style = AppTheme.typography.titleSmall,
                            color = AppTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(filteredArtists, key = { "artist_${it.id}" }) { artist ->
                        BlacklistedArtistRow(
                            artist = artist,
                            onRemove = { viewModel.toggleArtist(artist) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BlacklistedTrackRow(
    track: MetadataTrack,
    onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MillenniumIcon(
            imageVector = Iconsax.IconsaxMusicCircle,
            contentDescription = stringResource(Res.string.blacklist_item_track),
            modifier = Modifier.size(20.dp),
            tint = AppTheme.colors.onSurfaceVariant)

        AsyncImage(
            model = (track.album?.thumbnails ?: track.thumbnails)?.firstOrNull()?.url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(AppTheme.shapes.small)
                .shimmerApply())

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = AppTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(
                text = track.artists.joinToString(", ") { it.name },
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }

        MillenniumIconButton(
            onClick = onRemove,
            style = MillenniumIconButtonStyle.Quiet) {
            MillenniumIcon(
                imageVector = Iconsax.IconsaxMusicSquareRemoveFilled,
                contentDescription = stringResource(Res.string.blacklist_remove),
                tint = AppTheme.colors.error)
        }
    }
}

@Composable
private fun BlacklistedArtistRow(
    artist: MetadataArtist,
    onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MillenniumIcon(
            imageVector = Iconsax.User,
            contentDescription = stringResource(Res.string.media_artist),
            modifier = Modifier.size(20.dp),
            tint = AppTheme.colors.onSurfaceVariant)

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center) {
            val imageUrl = artist.thumbnails.firstOrNull()?.url
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .shimmerApply())
            } else {
                MillenniumIcon(
                    imageVector = Iconsax.User,
                    contentDescription = artist.name,
                    tint = AppTheme.colors.onSurfaceVariant,
                    modifier = Modifier.size(24.dp))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = AppTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(
                text = stringResource(Res.string.media_artist),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant)
        }

        MillenniumIconButton(
            onClick = onRemove,
            style = MillenniumIconButtonStyle.Quiet) {
            MillenniumIcon(
                imageVector = Iconsax.IconsaxUserRemove,
                contentDescription = stringResource(Res.string.blacklist_remove),
                tint = AppTheme.colors.error)
        }
    }
}
