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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.component.cards.AvatarCard
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.*

@Composable
fun ArtistCard(
    artist: MetadataArtist.Basic,
    modifier: Modifier = Modifier,
    isBlacklisted: Boolean = false,
    listMode: Boolean = false
) {
    val navigationCommands = koinInject<NavigationCommands>()

    AvatarCard(
        title = artist.name,
        subtitle = stringResource(if (isBlacklisted) Res.string.media_blacklisted else Res.string.media_artist),
        imageURL = artist.thumbnails.firstOrNull()?.url,
        onClick = {
            navigationCommands.navigateTo(Routes.Artist(artist.id))
        },
        modifier = modifier,
        isBlacklisted = isBlacklisted,
        listMode = listMode
    )
}
