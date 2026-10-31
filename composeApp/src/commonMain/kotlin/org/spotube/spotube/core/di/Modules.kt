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

package org.spotube.spotube.core.di

import org.koin.core.module.dsl.onClose
import org.spotube.spotube.core.network.HttpClientFactory
import co.touchlab.kermit.Logger
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.AudioPlayerQueueRepository
import org.spotube.spotube.core.audioplayer.DeviceAudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueStateRepository
import org.spotube.spotube.core.db.Database
import org.spotube.spotube.core.discovery.DeviceDiscoveryService
import org.spotube.spotube.core.discord.DiscordRpcService
import org.spotube.spotube.core.jam.JamRoomClient
import org.spotube.spotube.core.jam.JamRoomService
import org.spotube.spotube.core.navigation.navigationModule
import org.spotube.spotube.core.remote.RemoteControlClient
import org.spotube.spotube.core.remote.RemoteControlHandler
import org.spotube.spotube.core.remote.RemoteControlService
import org.spotube.spotube.core.remote.RemotePlaybackController
import org.spotube.spotube.core.scrobble.ScrobbleManager
import org.spotube.spotube.core.playback.CollectionPlaybackHelper
import org.spotube.spotube.core.server.AlternativeTracksRepository
import org.spotube.spotube.core.server.CacheManager
import org.spotube.spotube.core.server.LocalServer
import org.spotube.spotube.core.server.MatchedTracksRepository
import org.spotube.spotube.core.server.TrackSourceRepository
import org.spotube.spotube.core.server.StreamingUrlRepository
import org.spotube.spotube.core.webview.WebViewController
import org.spotube.spotube.modules.album.AlbumRepository
import org.spotube.spotube.modules.album.AlbumViewModel
import org.spotube.spotube.modules.artist.ArtistRepository
import org.spotube.spotube.modules.artist.ArtistViewModel
import org.spotube.spotube.modules.blacklist.BlacklistRepository
import org.spotube.spotube.modules.blacklist.BlacklistViewModel
import org.spotube.spotube.modules.devices.DevicesViewModel
import org.spotube.spotube.modules.devices.RemoteControlViewModel
import org.spotube.spotube.modules.shell.ConnectStatusViewModel
import org.spotube.spotube.modules.jam.JamViewModel
import org.spotube.spotube.modules.downloads.DownloadManager
import org.spotube.spotube.modules.downloads.DownloadsViewModel
import org.spotube.spotube.modules.home.HomeScreenRepository
import org.spotube.spotube.modules.home.HomeScreenViewModel
import org.spotube.spotube.modules.plugin.MetadataAuthViewModel
import org.spotube.spotube.modules.library.LibraryRepository
import org.spotube.spotube.modules.library.LibraryState
import org.spotube.spotube.modules.library.album.LibraryAlbumsViewModel
import org.spotube.spotube.modules.library.artist.LibraryArtistsViewModel
import org.spotube.spotube.modules.library.local_tracks.LibraryLocalTracksViewModel
import org.spotube.spotube.modules.library.local_tracks.LocalFolderViewModel
import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaCacheRepository
import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaFoldersConfig
import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaLibraryCoordinator
import org.spotube.spotube.modules.library.playlist.LibraryPlaylistsViewModel
import org.spotube.spotube.modules.lyrics.LyricsViewModel
import org.spotube.spotube.modules.playlist.AddTracksToPlaylistViewModel
import org.spotube.spotube.modules.playlist.PlaylistRepository
import org.spotube.spotube.modules.playlist.PlaylistViewModel
import org.spotube.spotube.modules.plugin.AudioPluginSource
import org.spotube.spotube.modules.plugin.PluginManager
import org.spotube.spotube.modules.plugin.PluginProvider
import org.spotube.spotube.modules.plugin.PluginViewModel
import org.spotube.spotube.modules.plugin.guard.MetadataPluginGuardViewModel
import org.spotube.spotube.modules.plugin.ScrobblePluginSource
import org.spotube.spotube.modules.saved_tracks.SavedTracksRepository
import org.spotube.spotube.modules.saved_tracks.SavedTracksViewModel
import org.spotube.spotube.modules.search.SearchRepository
import org.spotube.spotube.modules.search.SearchScreenViewModel
import org.spotube.spotube.modules.update.AppUpdateService
import org.spotube.spotube.modules.update.PluginUpdateService
import org.spotube.spotube.modules.settings.SettingsProvider
import org.spotube.spotube.modules.settings.SettingsRepository
import org.spotube.spotube.modules.settings.UserSettingsStore
import org.spotube.spotube.modules.settings.UserSettingsSource
import org.spotube.spotube.modules.settings.SettingsViewModel
import org.spotube.spotube.modules.settings.JamSettingsViewModel
import org.spotube.spotube.modules.settings.LocalMediaFoldersViewModel
import org.spotube.spotube.modules.shell.AppShellViewModel
import org.spotube.spotube.modules.shell.PlayerOptionsViewModel
import org.spotube.spotube.modules.shell.alternative_track.AlternativeTrackContentViewModel
import org.spotube.spotube.modules.shell.player_queue.PlayerQueueContentViewModel
import org.spotube.spotube.modules.welcome.WelcomeRepository
import org.spotube.spotube.modules.welcome.WelcomeViewModel
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
    single { HttpClientFactory() } withOptions { onClose { it?.close() } }

    singleOf(::WebViewController)

    // Shell
    viewModelOf(::AppShellViewModel)
    viewModelOf(::PlayerOptionsViewModel)
    viewModelOf(::PlayerQueueContentViewModel)
    viewModelOf(::AlternativeTrackContentViewModel)

    // Home
    singleOf(::HomeScreenRepository)
    viewModelOf(::HomeScreenViewModel)
    viewModelOf(::MetadataAuthViewModel)

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
    viewModel { (folderPath: String) -> LocalFolderViewModel(folderPath, get(), get()) }

    // Plugin system
    singleOf(::PluginManager) {
        bind<PluginProvider>()
        bind<AudioPluginSource>()
        bind<ScrobblePluginSource>()
    }
    viewModelOf(::PluginViewModel)
    viewModelOf(::MetadataPluginGuardViewModel)

    // Settings
    singleOf(::SettingsRepository) {
        bind<UserSettingsSource>()
        bind<UserSettingsStore>()
    }
    viewModelOf(::SettingsViewModel) { bind<SettingsProvider>() }
    viewModelOf(::JamSettingsViewModel)
    viewModel {
        val repository = get<SettingsRepository>()
        LocalMediaFoldersViewModel(repository.userSettings) { folders ->
            val current = repository.getCurrentSettings()
            repository.updateSettings(current.copy(localMediaFolders = folders))
        }
    }

    // Welcome / Onboarding
    singleOf(::WelcomeRepository)
    viewModelOf(::WelcomeViewModel)

    // Updates
    single { AppUpdateService(get(), get()) } withOptions { createdAtStart() }
    single { PluginUpdateService(get(), get()) } withOptions { createdAtStart() }

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
            remotePlaybackController = get())
    }
    viewModel { (playlistId: String) -> AddTracksToPlaylistViewModel(playlistId, get(), get()) }

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
            remotePlaybackController = get())
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
            remotePlaybackController = get())
    }

    // Blacklist
    singleOf(::BlacklistRepository)
    viewModelOf(::BlacklistViewModel)
    viewModel { DevicesViewModel(get()) }
    viewModelOf(::RemoteControlViewModel)
    viewModelOf(::ConnectStatusViewModel)
    viewModel {
        JamViewModel(
            jamRoomService = get(),
            shareService = get(),
            settingsProvider = get())
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
            remotePlaybackController = get())
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
            blacklistRepository = get())
    }
    singleOf(::MatchedTracksRepository) { bind<TrackSourceRepository>() }
    single {
        StreamingUrlRepository(
            pluginManager = get(),
            audioPlayerQueue = get(),
            matchedTracksRepository = get(),
            settingsRepository = get())
    }
    singleOf(::CacheManager)
    singleOf(::AlternativeTracksRepository)
    singleOf(::LocalServer) withOptions {
        createdAtStart()
    }
    single { RemoteControlHandler(get(), get(), get(), get()) }
    single { RemoteControlClient(get()) }
    singleOf(::DeviceDiscoveryService)
    single { RemoteControlService(get(), get(), get()) } withOptions {
        createdAtStart()
    }
    single { RemotePlaybackController(get(), get(), get(), get(), get()) }
    singleOf(::JamRoomClient)
    single {
        JamRoomService(
            get(), get(), get(), get(), get(), get(), get(),
            evictCachedAudio = get<CacheManager>()::invalidateCacheEntry)
    }
    singleOf(::AudioPlayerQueueRepository) { bind<QueueStateRepository>() }
    single<AudioPlayerQueue> {
        DeviceAudioPlayerQueue(get(), get(), get(), get(), get())
    }

    single { DiscordRpcService(get(), get()) } withOptions { createdAtStart() }

    // Scrobbling
    single { ScrobbleManager(get(), get(), get()) } withOptions { createdAtStart() }

    factory { (tag: String?) ->
        if (tag != null) {
            Logger.withTag(tag)
        } else {
            // Default logger if no tag is provided
            Logger
        }
    }
}
