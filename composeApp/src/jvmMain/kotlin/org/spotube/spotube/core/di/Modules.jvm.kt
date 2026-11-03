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
import org.spotube.spotube.core.mediacontrol.SystemMediaControlService
import org.spotube.spotube.core.paths.Paths
import org.spotube.spotube.core.share.JvmShareService
import org.spotube.spotube.core.share.ShareService
import org.spotube.spotube.core.systemtray.SystemTrayService
import org.spotube.spotube.modules.library.local_tracks.media.JvmLocalMediaDiscoveryService
import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaDiscoveryService
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module
import org.spotube.spotube.core.db.Database
import org.spotube.spotube.core.window.WindowStateRepository

actual val platformModules = module {
    singleOf(::Paths)
    single { WindowStateRepository(get<Database>()) }
    single<AudioPlayerInterface> { AudioPlayer(Unit) }
    single<LocalMediaDiscoveryService> { JvmLocalMediaDiscoveryService() }
    single<ShareService> { JvmShareService() }
    single { SystemTrayService(get(), get(), get()) }
    single { SystemMediaControlService(get()) } withOptions { createdAtStart() }
}
