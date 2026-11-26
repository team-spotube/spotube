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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.component.cards.AvatarCard
import org.spotube.spotube.modules.library.local.LocalMediaArtist
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.media_artist

/**
 * A synthetic artist built from the local media index. Like [ArtistCard] it carries no play
 * action; tapping it opens the artist's track list.
 */
@Composable
fun LocalArtistCard(
    artist: LocalMediaArtist,
    modifier: Modifier = Modifier,
    listMode: Boolean = false
) {
    val navigationCommands = koinInject<NavigationCommands>()

    AvatarCard(
        title = artist.name,
        subtitle = stringResource(Res.string.media_artist),
        imageModel = artist.coverBytes,
        onClick = {
            navigationCommands.navigateTo(Routes.LocalArtist(artist.key))
        },
        modifier = modifier,
        listMode = listMode
    )
}
