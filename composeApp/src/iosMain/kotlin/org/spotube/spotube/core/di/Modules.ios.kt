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

import org.spotube.spotube.core.audioplayer.AudioPlayer
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.paths.Paths
import org.spotube.spotube.core.share.IosShareService
import org.spotube.spotube.core.share.ShareService
import org.spotube.spotube.core.webview.WebViewController
import org.spotube.spotube.modules.library.local_tracks.media.IosLocalMediaDiscoveryService
import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaDiscoveryService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual val platformModules = module {
    singleOf(::Paths)
    singleOf(::WebViewController)
    single<AudioPlayerInterface> { AudioPlayer(Unit) }
    single<LocalMediaDiscoveryService> { IosLocalMediaDiscoveryService() }
    single<ShareService> { IosShareService() }
}
