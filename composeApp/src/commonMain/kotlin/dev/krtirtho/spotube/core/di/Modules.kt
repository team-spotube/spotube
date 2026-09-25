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

package dev.krtirtho.spotube.core.di

import co.touchlab.kermit.Logger
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerQueue
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerQueueRepository
import dev.krtirtho.spotube.core.audioplayer.DeviceAudioPlayerQueue
import dev.krtirtho.spotube.core.audioplayer.QueueStateRepository
import dev.krtirtho.spotube.core.db.Database
import dev.krtirtho.spotube.core.discovery.DeviceDiscoveryService
import dev.krtirtho.spotube.core.discord.DiscordRpcService
import dev.krtirtho.spotube.core.jam.JamRoomClient
import dev.krtirtho.spotube.core.jam.JamRoomService
import dev.krtirtho.spotube.core.navigation.navigationModule
import dev.krtirtho.spotube.core.remote.RemoteControlClient
import dev.krtirtho.spotube.core.remote.RemoteControlHandler
import dev.krtirtho.spotube.core.remote.RemoteControlService
import dev.krtirtho.spotube.core.remote.RemotePlaybackController
import dev.krtirtho.spotube.core.playback.CollectionPlaybackHelper
import dev.krtirtho.spotube.core.server.AlternativeTracksRepository
import dev.krtirtho.spotube.core.server.CacheManager
import dev.krtirtho.spotube.core.server.LocalServer
import dev.krtirtho.spotube.core.server.MatchedTracksRepository
import dev.krtirtho.spotube.core.server.StreamingUrlRepository
import dev.krtirtho.spotube.core.webview.WebViewController
import dev.krtirtho.spotube.modules.album.AlbumRepository
import dev.krtirtho.spotube.modules.album.AlbumViewModel
import dev.krtirtho.spotube.modules.artist.ArtistRepository
import dev.krtirtho.spotube.modules.artist.ArtistViewModel
import dev.krtirtho.spotube.modules.blacklist.BlacklistRepository
import dev.krtirtho.spotube.modules.blacklist.BlacklistViewModel
import dev.krtirtho.spotube.modules.devices.DevicesViewModel
import dev.krtirtho.spotube.modules.devices.RemoteControlViewModel
import dev.krtirtho.spotube.modules.jam.JamViewModel
import dev.krtirtho.spotube.modules.downloads.DownloadManager
import dev.krtirtho.spotube.modules.downloads.DownloadsViewModel
import dev.krtirtho.spotube.modules.home.HomeScreenRepository
import dev.krtirtho.spotube.modules.home.HomeScreenViewModel
import dev.krtirtho.spotube.modules.library.LibraryRepository
import dev.krtirtho.spotube.modules.library.LibraryState
import dev.krtirtho.spotube.modules.library.album.LibraryAlbumsViewModel
import dev.krtirtho.spotube.modules.library.artist.LibraryArtistsViewModel
import dev.krtirtho.spotube.modules.library.local_tracks.LibraryLocalTracksViewModel
import dev.krtirtho.spotube.modules.library.local_tracks.media.LocalMediaCacheRepository
import dev.krtirtho.spotube.modules.library.local_tracks.media.LocalMediaFoldersConfig
import dev.krtirtho.spotube.modules.library.local_tracks.media.LocalMediaLibraryCoordinator
import dev.krtirtho.spotube.modules.library.playlist.LibraryPlaylistsViewModel
import dev.krtirtho.spotube.modules.lyrics.LyricsViewModel
import dev.krtirtho.spotube.modules.playlist.PlaylistRepository
import dev.krtirtho.spotube.modules.playlist.PlaylistViewModel
import dev.krtirtho.spotube.modules.plugin.PluginManager
import dev.krtirtho.spotube.modules.plugin.PluginProvider
import dev.krtirtho.spotube.modules.plugin.PluginViewModel
import dev.krtirtho.spotube.modules.saved_tracks.SavedTracksRepository
import dev.krtirtho.spotube.modules.saved_tracks.SavedTracksViewModel
import dev.krtirtho.spotube.modules.search.SearchRepository
import dev.krtirtho.spotube.modules.search.SearchScreenViewModel
import dev.krtirtho.spotube.modules.settings.SettingsProvider
import dev.krtirtho.spotube.modules.settings.SettingsRepository
import dev.krtirtho.spotube.modules.settings.SettingsViewModel
import dev.krtirtho.spotube.modules.shell.AppShellViewModel
import dev.krtirtho.spotube.modules.shell.PlayerOptionsViewModel
import dev.krtirtho.spotube.modules.shell.alternative_track.AlternativeTrackContentViewModel
import dev.krtirtho.spotube.modules.shell.player_queue.PlayerQueueContentViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

expect val platformModules: Module

val sharedModules = module {
    includes(navigationModule)
    singleOf(::Database)

    singleOf(::WebViewController)

    // Shell
    viewModelOf(::AppShellViewModel)
    viewModelOf(::PlayerOptionsViewModel)
    viewModelOf(::PlayerQueueContentViewModel)
    viewModelOf(::AlternativeTrackContentViewModel)

    // Home
    singleOf(::HomeScreenRepository)
    viewModelOf(::HomeScreenViewModel)

    // Search
    singleOf(::SearchRepository)
    viewModelOf(::SearchScreenViewModel)

    // Library
    singleOf(::LibraryState)
    singleOf(::LibraryRepository)
    singleOf(::LocalMediaCacheRepository)
    singleOf(::LocalMediaFoldersConfig)
    singleOf(::LocalMediaLibraryCoordinator)
    viewModelOf(::LibraryPlaylistsViewModel)
    viewModelOf(::LibraryAlbumsViewModel)
    viewModelOf(::LibraryArtistsViewModel)
    viewModelOf(::LibraryLocalTracksViewModel)

    // Plugin system
    singleOf(::PluginManager) { bind<PluginProvider>() }
    viewModelOf(::PluginViewModel)

    // Settings
    singleOf(::SettingsRepository)
    viewModelOf(::SettingsViewModel) { bind<SettingsProvider>() }

    // Downloads
    singleOf(::DownloadManager)
    viewModelOf(::DownloadsViewModel)

    // Playlist
    singleOf(::PlaylistRepository)
    viewModel { (playlistId: String) ->
        PlaylistViewModel(
            playlistId = playlistId,
            repository = get(),
            savedTracksRepository = get(),
            libraryRepository = get(),
            playbackHelper = get(),
            audioPlayerQueue = get(),
            blacklistRepository = get(),
            shareService = get(),
            downloadManager = get(),
            remotePlaybackController = get(),
        )
    }

    // Saved Tracks
    singleOf(::SavedTracksRepository)
    viewModel {
        SavedTracksViewModel(
            repository = get(),
            playbackHelper = get(),
            audioPlayerQueue = get(),
            blacklistRepository = get(),
            libraryRepository = get(),
            shareService = get(),
            downloadManager = get(),
            remotePlaybackController = get(),
        )
    }

    // Artist
    singleOf(::ArtistRepository)
    viewModel { (artistId: String) ->
        ArtistViewModel(
            artistId = artistId,
            repository = get(),
            libraryRepository = get(),
            savedTracksRepository = get(),
            audioPlayerQueue = get(),
            blacklistRepository = get(),
            shareService = get(),
            downloadManager = get(),
            remotePlaybackController = get(),
        )
    }

    // Blacklist
    singleOf(::BlacklistRepository)
    viewModelOf(::BlacklistViewModel)
    viewModel { DevicesViewModel(get()) }
    viewModelOf(::RemoteControlViewModel)
    viewModel {
        JamViewModel(
            jamRoomService = get(),
            shareService = get(),
            settingsProvider = get(),
        )
    }

    // Album
    singleOf(::AlbumRepository)
    viewModel { (albumId: String) ->
        AlbumViewModel(
            albumId = albumId,
            repository = get(),
            savedTracksRepository = get(),
            playbackHelper = get(),
            libraryRepository = get(),
            audioPlayerQueue = get(),
            blacklistRepository = get(),
            shareService = get(),
            downloadManager = get(),
            remotePlaybackController = get(),
        )
    }

    // Lyrics
    viewModelOf(::LyricsViewModel)

    // Local playback proxy server
    single {
        CollectionPlaybackHelper(
            albumRepository = get(),
            playlistRepository = get(),
            savedTracksRepository = get(),
            artistRepository = get(),
            audioPlayerQueue = get(),
            blacklistRepository = get(),
        )
    }
    singleOf(::MatchedTracksRepository)
    singleOf(::StreamingUrlRepository)
    singleOf(::CacheManager)
    singleOf(::AlternativeTracksRepository)
    singleOf(::LocalServer) withOptions {
        createdAtStart()
    }
    single { RemoteControlHandler(get(), get(), get(), get()) }
    single { RemoteControlClient() }
    singleOf(::DeviceDiscoveryService)
    single { RemoteControlService(get(), get(), get()) } withOptions {
        createdAtStart()
    }
    single { RemotePlaybackController(get(), get(), get(), get(), get()) }
    singleOf(::JamRoomClient)
    single { JamRoomService(get(), get(), get(), get()) }
    singleOf(::AudioPlayerQueueRepository) { bind<QueueStateRepository>() }
    single<AudioPlayerQueue> {
        DeviceAudioPlayerQueue(get(), get(), get(), get(), get())
    }

    single { DiscordRpcService(get(), get()) } withOptions { createdAtStart() }

    factory { (tag: String?) ->
        if (tag != null) {
            Logger.withTag(tag)
        } else {
            // Default logger if no tag is provided
            Logger
        }
    }
}
