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

package org.spotube.spotube.modules.library

import androidx.compose.ui.graphics.vector.ImageVector
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxCd
import org.spotube.spotube.resources.iconsax.IconsaxDirectboxReceive
import org.spotube.spotube.resources.iconsax.IconsaxMusicDashboard
import org.spotube.spotube.resources.iconsax.IconsaxMusicLibrary
import org.spotube.spotube.resources.iconsax.User
import org.jetbrains.compose.resources.StringResource
import spotube.composeapp.generated.resources.*

enum class LibraryTab(val icon: ImageVector) {
    Playlists(Iconsax.IconsaxMusicDashboard),
    Albums(Iconsax.IconsaxCd),
    Artists(Iconsax.User),
    LocalTracks(Iconsax.IconsaxMusicLibrary),
    Downloads(Iconsax.IconsaxDirectboxReceive);

    val titleResource: StringResource
        get() = when (this) {
            Playlists -> Res.string.library_tab_playlists
            Albums -> Res.string.library_tab_albums
            Artists -> Res.string.library_tab_artists
            LocalTracks -> Res.string.library_tab_local_tracks
            Downloads -> Res.string.library_tab_downloads
        }

    val filterPlaceholder: StringResource
        get() = when (this) {
            Playlists -> Res.string.library_filter_playlists
            Albums -> Res.string.library_filter_albums
            Artists -> Res.string.library_filter_artists
            LocalTracks -> Res.string.library_filter_local_tracks
            Downloads -> Res.string.search_filter_placeholder
        }
}
