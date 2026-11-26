/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.local_tracks.media

import androidx.compose.runtime.Composable

internal interface MusicWriteRequest

internal class WrongMusicDocument : Exception()

internal data class MusicWriteDecision(val granted: Boolean, val documentUri: String? = null)

/** Returns a scoped permission request if needed, or null once the metadata has been saved. */
internal expect suspend fun saveMusicFileTags(file: MusicFile, tags: LocalTrackTags, documentUri: String?): MusicWriteRequest?

@Composable
internal expect fun rememberMusicWriteLauncher(onResult: (MusicWriteDecision) -> Unit): (MusicWriteRequest) -> Unit
