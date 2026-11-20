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

package org.spotube.spotube.modules.library.local_tracks.media

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.spotube.spotube.core.paths.Paths

class IosLocalMediaDiscoveryService : LocalMediaDiscoveryService {
    override suspend fun discoverFolders(roots: List<String>): List<LocalMediaFolder> = withContext(Dispatchers.IO) {
        (listOf(Paths().getApplicationDataDirPath()) + roots).distinct()
            .flatMap { scanMusicTree(PlatformFile(it)) }
            .distinctBy { it.path }
            .map { it.copy(documentTrail = emptyList()) }
    }

    override fun observeChanges(
        roots: List<String>,
        onChanged: LocalMediaChangeCallback): LocalMediaObservation? {
        return null
    }
}
