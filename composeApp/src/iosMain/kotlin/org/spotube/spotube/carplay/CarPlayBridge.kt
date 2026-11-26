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

package org.spotube.spotube.carplay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.spotube.spotube.AppBootstrap
import org.spotube.spotube.core.i18n.getString
import org.spotube.spotube.core.playback.CollectionPlaybackHelper
import org.spotube.spotube.modules.library.LibraryRepository
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.library_tab_albums
import spotube.composeapp.generated.resources.library_tab_playlists
import spotube.composeapp.generated.resources.library_toolbar_compact_title
import spotube.composeapp.generated.resources.media_browser_saved_tracks

enum class CarPlayItemKind { Playlists, Albums, SavedTracks, Playlist, Album }

data class CarPlayItem(
    val id: String,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val kind: CarPlayItemKind,
) {
    /** Playlists and Albums are folders that open a list; everything else starts playing. */
    val opensList: Boolean
        get() = kind == CarPlayItemKind.Playlists || kind == CarPlayItemKind.Albums
}

/**
 * What the CarPlay scene (iosApp/iosApp/CarPlaySceneDelegate.swift) shows and plays. Swift owns the
 * CarPlay templates, since CarPlay is UIKit only; this owns the content and the localized titles, so
 * no user-facing string lives in Swift. The suspend functions are async/await in Swift.
 *
 * Every suspend function is `@Throws`: in Kotlin/Native an exception escaping a suspend function
 * called from Swift ends the process, and a flaky network must not close CarPlay mid-drive.
 *
 * Deliberately small: a car screen should offer a few big choices, not the whole library tree.
 */
object CarPlayBridge : KoinComponent {
    private val library: LibraryRepository by inject()
    private val playback: CollectionPlaybackHelper by inject()

    /** Starts what CarPlay needs when it launches the app without the phone UI. */
    @Throws(Exception::class)
    suspend fun start() = withContext(Dispatchers.Main) {
        AppBootstrap.start()
        AppBootstrap.ensureNewPipe()
    }

    @Throws(Exception::class)
    suspend fun libraryTitle(): String = getString(Res.string.library_toolbar_compact_title)

    @Throws(Exception::class)
    suspend fun root(): List<CarPlayItem> = listOf(
        CarPlayItem(
            id = "playlists",
            title = getString(Res.string.library_tab_playlists),
            subtitle = null,
            imageUrl = null,
            kind = CarPlayItemKind.Playlists),
        CarPlayItem(
            id = "albums",
            title = getString(Res.string.library_tab_albums),
            subtitle = null,
            imageUrl = null,
            kind = CarPlayItemKind.Albums),
        CarPlayItem(
            id = "saved_tracks",
            title = getString(Res.string.media_browser_saved_tracks),
            subtitle = null,
            imageUrl = null,
            kind = CarPlayItemKind.SavedTracks),
    )

    /** The playlists or albums inside a folder item; empty on failure, like the Android browse tree. */
    @Throws(Exception::class)
    suspend fun items(kind: CarPlayItemKind): List<CarPlayItem> = withContext(Dispatchers.Default) {
        runCatching {
            when (kind) {
                CarPlayItemKind.Playlists ->
                    library.savedPlaylists()?.items.orEmpty().map {
                        CarPlayItem(
                            id = it.id,
                            title = it.title,
                            subtitle = it.owner?.displayName ?: it.owner?.username,
                            imageUrl = it.thumbnails.firstOrNull()?.url,
                            kind = CarPlayItemKind.Playlist)
                    }
                CarPlayItemKind.Albums ->
                    library.savedAlbums()?.items.orEmpty().map {
                        CarPlayItem(
                            id = it.id,
                            title = it.title,
                            subtitle = it.artists.joinToString(", ") { artist -> artist.name },
                            imageUrl = it.thumbnails.firstOrNull()?.url,
                            kind = CarPlayItemKind.Album)
                    }
                else -> emptyList()
            }
        }.getOrDefault(emptyList())
    }

    @Throws(Exception::class)
    suspend fun play(item: CarPlayItem) = withContext(Dispatchers.Default) {
        when (item.kind) {
            CarPlayItemKind.Playlist -> playback.playPlaylist(item.id)
            CarPlayItemKind.Album -> playback.playAlbum(item.id)
            CarPlayItemKind.SavedTracks -> playback.playSavedTracks()
            CarPlayItemKind.Playlists, CarPlayItemKind.Albums -> Unit
        }
    }
}
