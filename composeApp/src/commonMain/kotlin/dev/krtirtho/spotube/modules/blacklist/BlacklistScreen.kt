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

package dev.krtirtho.spotube.modules.blacklist

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import dev.krtirtho.spotube.core.ui.base.BaseScaffold
import dev.krtirtho.spotube.core.ui.base.GhostIconButton
import dev.krtirtho.spotube.core.ui.base.TextField
import dev.krtirtho.spotube.core.ui.component.ApplicationMainBar
import dev.krtirtho.spotube.core.ui.misc.shimmerApply
import dev.krtirtho.spotube.modules.shell.LocalAppShellBottomInset
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxFilterSearch
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusicCircle
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusicSquareRemoveFilled
import dev.krtirtho.spotube.resources.iconsax.IconsaxUserRemove
import dev.krtirtho.spotube.resources.iconsax.User
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BlacklistScreen() {
    val viewModel = koinViewModel<BlacklistViewModel>()
    BaseScaffold(
        topBar = {
            ApplicationMainBar(
                backButton = true,
                title = { Text("Blacklist") },
            )
        },
    ) { innerPadding ->
        BlacklistContent(
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * The blacklist UI without its own [BaseScaffold] / app bar, so it can be embedded
 * as a tab in the large-screen settings layout as well as hosted by [BlacklistScreen].
 */
@Composable
internal fun BlacklistContent(
    viewModel: BlacklistViewModel,
    modifier: Modifier = Modifier,
) {
    val blacklistedTracks by viewModel.blacklistedTracks.collectAsStateWithLifecycle()
    val blacklistedArtists by viewModel.blacklistedArtists.collectAsStateWithLifecycle()
    val shellBottomInset = LocalAppShellBottomInset.current

    var filterQuery by rememberSaveable { mutableStateOf("") }
    val normalizedQuery = remember(filterQuery) { filterQuery.trim().lowercase() }

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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextField(
            value = filterQuery,
            onValueChange = { filterQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Filter blacklist...") },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Iconsax.IconsaxFilterSearch,
                    contentDescription = "Search",
                )
            },
        )

        if (isEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = shellBottomInset),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (hasActiveFilter) {
                        "No blacklisted items match '$filterQuery'"
                    } else {
                        "No blacklisted items"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    bottom = 16.dp + shellBottomInset,
                ),
            ) {
                if (filteredTracks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Tracks",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    items(filteredTracks, key = { "track_${it.id}" }) { track ->
                        BlacklistedTrackRow(
                            track = track,
                            onRemove = { viewModel.toggleTrack(track) },
                        )
                    }
                }

                if (filteredArtists.isNotEmpty()) {
                    item {
                        Text(
                            text = "Artists",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    items(filteredArtists, key = { "artist_${it.id}" }) { artist ->
                        BlacklistedArtistRow(
                            artist = artist,
                            onRemove = { viewModel.toggleArtist(artist) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlacklistedTrackRow(
    track: MetadataTrack,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Iconsax.IconsaxMusicCircle,
            contentDescription = "Track",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AsyncImage(
            model = (track.album?.thumbnails ?: track.thumbnails)?.firstOrNull()?.url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small)
                .shimmerApply(),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        GhostIconButton(onClick = onRemove) {
            Icon(
                imageVector = Iconsax.IconsaxMusicSquareRemoveFilled,
                contentDescription = "Remove from blacklist",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun BlacklistedArtistRow(
    artist: MetadataArtist,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Iconsax.User,
            contentDescription = "Artist",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val imageUrl = artist.thumbnails.firstOrNull()?.url
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .shimmerApply(),
                )
            } else {
                Icon(
                    imageVector = Iconsax.User,
                    contentDescription = artist.name,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Artist",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        GhostIconButton(onClick = onRemove) {
            Icon(
                imageVector = Iconsax.IconsaxUserRemove,
                contentDescription = "Remove from blacklist",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
