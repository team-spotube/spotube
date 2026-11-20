/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.local_tracks.media

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.delete
import org.spotube.spotube.core.paths.Paths
import platform.Foundation.NSFileManager
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.library_browser_home

internal actual fun platformMusicRoots(): List<MusicRoot> =
    listOf(MusicRoot(Paths().getApplicationDataDirPath(), Res.string.library_browser_home))

internal actual fun readPlatformTrackTags(path: String): LocalTrackTags? = null
internal actual fun musicDocumentFilePath(uri: String): String? = null

internal actual fun musicFileAccess(path: String): MusicFileAccess = NSFileManager.defaultManager.let {
    MusicFileAccess(it.isReadableFileAtPath(path), it.isWritableFileAtPath(path), it.isDeletableFileAtPath(path))
}

internal actual suspend fun deleteMusicFile(file: MusicFile, requestPermission: MusicPermission?) = file.file.delete()

internal actual suspend fun saveMusicFileTags(file: MusicFile, tags: LocalTrackTags, documentUri: String?): MusicWriteRequest? {
    file.saveEmbeddedTags(tags)
    return null
}

@Composable
internal actual fun rememberMusicWriteLauncher(onResult: (MusicWriteDecision) -> Unit): (MusicWriteRequest) -> Unit =
    { onResult(MusicWriteDecision(false)) }
