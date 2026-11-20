/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.local_tracks.media

import androidx.compose.runtime.Composable
import java.io.File
import org.spotube.spotube.PlatformType
import org.spotube.spotube.getPlatform
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.library_browser_home

internal actual fun platformMusicRoots(): List<MusicRoot> =
    if (getPlatform().type == PlatformType.Windows) File.listRoots().orEmpty().map { MusicRoot(it.path) }
    else listOf(MusicRoot(System.getProperty("user.home"), Res.string.library_browser_home))

internal actual fun readPlatformTrackTags(path: String): LocalTrackTags? = null

internal actual fun musicFileAccess(path: String): MusicFileAccess {
    val file = File(path)
    return MusicFileAccess(file.canRead(), file.canWrite(), file.parentFile?.let { it.canWrite() && it.canExecute() })
}

internal actual suspend fun saveMusicFileTags(file: MusicFile, tags: LocalTrackTags, documentUri: String?): MusicWriteRequest? {
    file.saveEmbeddedTags(tags)
    return null
}

@Composable
internal actual fun rememberMusicWriteLauncher(onResult: (MusicWriteDecision) -> Unit): (MusicWriteRequest) -> Unit =
    { onResult(MusicWriteDecision(false)) }
