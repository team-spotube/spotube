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

import android.content.Context
import dev.krtirtho.spotube.core.audioplayer.AudioPlayer
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerInterface
import dev.krtirtho.spotube.core.paths.Paths
import dev.krtirtho.spotube.core.share.AndroidShareService
import dev.krtirtho.spotube.core.share.ShareService
import dev.krtirtho.spotube.media.MediaBrowseHelper
import dev.krtirtho.spotube.modules.library.local_tracks.media.AndroidLocalMediaDiscoveryService
import dev.krtirtho.spotube.modules.library.local_tracks.media.LocalMediaDiscoveryService
import org.koin.dsl.module

actual val platformModules = module {
    single { Paths() }
    single<AudioPlayerInterface> { AudioPlayer(get<Context>()) }
    single<LocalMediaDiscoveryService> { AndroidLocalMediaDiscoveryService(get()) }
    single<ShareService> { AndroidShareService(get()) }
    single {
        MediaBrowseHelper(
            pluginManager = get(),
            homeScreenRepository = get(),
            libraryRepository = get(),
            playlistRepository = get(),
            albumRepository = get(),
            savedTracksRepository = get(),
            searchRepository = get(),
            collectionPlaybackHelper = get(),
            audioPlayerQueue = get(),
            settingsRepository = get(),
            blacklistRepository = get(),
        )
    }
}
