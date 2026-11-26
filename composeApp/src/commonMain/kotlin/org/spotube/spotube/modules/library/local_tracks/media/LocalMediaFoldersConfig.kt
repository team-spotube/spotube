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

import org.spotube.spotube.core.paths.Paths
import org.spotube.spotube.modules.settings.SettingsRepository
import org.spotube.spotube.modules.settings.DownloadFolderLocation
import org.spotube.spotube.modules.settings.downloadFolderPath
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.fromBookmarkData
import org.spotube.spotube.PlatformType
import org.spotube.spotube.getPlatform
import kotlin.io.encoding.Base64

class LocalMediaFoldersConfig(
    private val settingsRepository: SettingsRepository,
    private val paths: Paths) {
    internal fun documentRoots(): List<Pair<String, PlatformFile>> {
        val settings = settingsRepository.userSettings.value
        return settings.localMediaFolders.mapNotNull { path ->
            val bookmark = settings.localMediaFolderBookmarks[path]
            if (!path.startsWith("content:") && (getPlatform().type != PlatformType.IOS || bookmark == null)) {
                return@mapNotNull null
            }
            runCatching {
                path to if (bookmark == null) PlatformFile(path) else PlatformFile.fromBookmarkData(Base64.decode(bookmark))
            }.getOrNull()
        }
    }
    val configuredRootsFlow: Flow<List<String>> = settingsRepository.userSettings.map { settings ->
        val downloadRoot = when {
            !settings.overloadedDownloadFolder.isNullOrBlank() -> settings.overloadedDownloadFolder
            settings.downloadFolderLocation != DownloadFolderLocation.PUBLIC -> settings.downloadFolderPath(paths)
            else -> null
        }
        val cacheRoot = if (settings.enableMusicCaching) {
            listOf(settings.cacheFolder ?: paths.getMusicCacheDirPath())
        } else {
            emptyList()
        }
        (listOfNotNull(downloadRoot) + settings.localMediaFolders + cacheRoot + listOf(
            "${paths.getApplicationDataDirPath()}/downloads", "${paths.getApplicationCacheDirPath()}/downloads"
        ))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }.distinctUntilChanged()
}
