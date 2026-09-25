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

package dev.krtirtho.spotube.core.navigation

import androidx.navigation3.runtime.NavKey
import dev.krtirtho.spotube.modules.album.AlbumScreen
import dev.krtirtho.spotube.modules.artist.ArtistScreen
import dev.krtirtho.spotube.modules.blacklist.BlacklistScreen
import dev.krtirtho.spotube.modules.devices.DevicesScreen
import dev.krtirtho.spotube.modules.devices.RemoteControlScreen
import dev.krtirtho.spotube.modules.jam.JamScreen
import dev.krtirtho.spotube.modules.home.HomeScreen
import dev.krtirtho.spotube.modules.library.LibraryScreen
import dev.krtirtho.spotube.modules.lyrics.LyricsScreen
import dev.krtirtho.spotube.modules.playlist.PlaylistScreen
import dev.krtirtho.spotube.modules.plugin.PluginScreen
import dev.krtirtho.spotube.modules.saved_tracks.SAVED_TRACKS_COLLECTION_ID
import dev.krtirtho.spotube.modules.saved_tracks.SavedTracksScreen
import dev.krtirtho.spotube.modules.search.SearchScreen
import dev.krtirtho.spotube.modules.settings.SettingsScreen
import dev.krtirtho.spotube.modules.settings.JamSettingsScreen
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@Serializable
sealed interface Routes : NavKey {
    @Serializable
    data object Home : Routes

    @Serializable
    data object Library : Routes

    @Serializable
    data object Search : Routes

    @Serializable
    data object Settings : Routes

    @Serializable
    data object Plugins : Routes

    @Serializable
    data object WebView : Routes

    @Serializable
    data object Lyrics : Routes

    @Serializable
    data class Playlist(val playlistId: String) : Routes

    @Serializable
    data class Artist(val artistId: String) : Routes

    @Serializable
    data class Album(val albumId: String) : Routes

    @Serializable
    data object SavedTracks : Routes

    @Serializable
    data object Blacklist : Routes

    @Serializable
    data object RemoteControl : Routes

    @Serializable
    data object Devices : Routes

    @Serializable
    data object Jam : Routes

    @Serializable
    data object JamSettings : Routes
}

@OptIn(KoinExperimentalAPI::class)
val navigationModule = module {
    singleOf(::NavigationCommands)
    navigation<Routes.Home> {
        HomeScreen(viewModel = koinViewModel())
    }
    navigation<Routes.Search> {
        SearchScreen()
    }
    navigation<Routes.Library> {
        LibraryScreen()
    }
    navigation<Routes.Settings> {
        SettingsScreen(settingsViewModel = koinViewModel())
    }
    navigation<Routes.Plugins> {
        PluginScreen(viewModel = koinViewModel())
    }
    navigation<Routes.WebView> {}
    navigation<Routes.Playlist> {
        PlaylistScreen(
            playlistId = it.playlistId,
            viewModel = koinViewModel(
                key = it.playlistId,
                parameters = { parametersOf(it.playlistId) }
            ),
            audioPlayerQueue = get(),
            audioPlayer = get(),
            navigationCommands = get(),
        )
    }
    navigation<Routes.Artist> {
        ArtistScreen(
            viewModel = koinViewModel(
                key = it.artistId,
                parameters = { parametersOf(it.artistId) }
            ),
            audioPlayerQueue = get(),
            audioPlayer = get(),
            navigationCommands = get(),
        )
    }
    navigation<Routes.Album> {
        AlbumScreen(
            albumId = it.albumId,
            viewModel = koinViewModel(
                key = it.albumId,
                parameters = { parametersOf(it.albumId) }
            ),
            audioPlayerQueue = get(),
            audioPlayer = get(),
            navigationCommands = get(),
        )
    }
    navigation<Routes.Lyrics> {
        LyricsScreen(viewModel = koinViewModel())
    }
    navigation<Routes.SavedTracks> {
        SavedTracksScreen(
            viewModel = koinViewModel(
                key = SAVED_TRACKS_COLLECTION_ID,
                parameters = { parametersOf(SAVED_TRACKS_COLLECTION_ID) }
            ),
            audioPlayerQueue = get(),
            audioPlayer = get(),
            navigationCommands = get(),
        )
    }
    navigation<Routes.Blacklist> {
        BlacklistScreen()
    }
    navigation<Routes.Devices> {
        DevicesScreen(navigationCommands = get())
    }
    navigation<Routes.RemoteControl> {
        RemoteControlScreen(
            onDisconnect = { get<NavigationCommands>().pop() }
        )
    }
    navigation<Routes.Jam> {
        JamScreen(navigationCommands = get())
    }
    navigation<Routes.JamSettings> {
        JamSettingsScreen(viewModel = koinViewModel())
    }
}
