/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.local_tracks.media

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.lastModified
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.source
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.CancellationException
import kotlinx.io.buffered
import kotlinx.io.readString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.spotube.spotube.PlatformType
import org.spotube.spotube.getPlatform
import org.spotube.spotube.modules.library.local.encodeLocalCoverThumbnail
import uniffi.compose_app.AudioMetadata
import uniffi.compose_app.readAudioMetadata
import uniffi.compose_app.writeAudioMetadata

internal val playableMusicExtensions = setOf(
    "mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "wma", "webm", "mka", "aiff", "aif", "alac", "mp4"
)
internal val mixedMusicExtensions = setOf("mp4", "webm")

@Serializable
internal data class LocalTrackTags(
    val title: String,
    val artists: String = "",
    val album: String = "",
    val durationMs: Long = 0,
    val track: Int? = null,
    val disc: Int? = null,
    val cover: ByteArray? = null
) {
    fun toTrack(path: String) = LocalMediaTrack(
        path, title, artists.split(';').map(String::trim).filter(String::isNotBlank),
        durationMs, album.takeIf(String::isNotBlank), cover
    )
}

internal fun trackTagFile(folder: PlatformFile, name: String) = PlatformFile(folder, ".$name.spotube-tags.json")

private const val MAX_TAG_BYTES = 2L * 1024 * 1024
private val tagJson = Json { ignoreUnknownKeys = true }

internal fun readLocalTrackTags(folder: PlatformFile, file: PlatformFile): LocalTrackTags? {
    val sidecar = trackTagFile(folder, file.name)
    val stored = try {
        val size = if (sidecar.exists()) sidecar.size() else 0L
        if (size in 1..MAX_TAG_BYTES) {
            sidecar.source().buffered().use { source ->
                tagJson.decodeFromString<LocalTrackTags>(source.readString(size))
            }
        } else null
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
    // Keep old download/legacy tags unless the audio file has since been edited.
    if (stored != null && runCatching { file.lastModified() <= sidecar.lastModified() }.getOrDefault(true)) return stored
    val path = file.path
    val android = getPlatform().type == PlatformType.Android
    val ambiguous = file.extension.lowercase() in mixedMusicExtensions
    if (path.startsWith("content:")) return readPlatformTrackTags(path)
    val androidTags = if (ambiguous && android) readPlatformTrackTags(path) ?: return null else null
    return try {
        readAudioMetadata(path).toLocalTrackTags()
    } catch (_: Exception) {
        androidTags ?: readPlatformTrackTags(path)
    }
}

internal fun AudioMetadata.toLocalTrackTags() = LocalTrackTags(title.orEmpty(), artists.joinToString("; "), album.orEmpty(),
    durationMs, trackNumber?.toInt(), discNumber?.toInt(), coverBytes)

internal expect fun readPlatformTrackTags(path: String): LocalTrackTags?

/** Use native tags where supported; preserve metadata for otherwise untaggable downloads. */
internal suspend fun saveDownloadedTrackTags(folder: PlatformFile, file: PlatformFile, tags: LocalTrackTags) {
    val sidecar = trackTagFile(folder, file.name)
    // Keep an existing sidecar authoritative. Android document URIs are not native file paths.
    if (!file.path.startsWith("content:") && !sidecar.exists()) {
        try {
            writeAudioMetadata(file.path, tags.title, tags.artists, tags.album.takeIf(String::isNotBlank),
                tags.track, tags.disc, tags.cover)
            return
        } catch (_: Exception) {
            // Some supported playback containers are not supported by the native tag writer.
        }
    }
    val compact = tags.copy(cover = tags.cover?.let { cover ->
        cover.takeIf { it.size <= 384 * 1024 }
            ?: encodeLocalCoverThumbnail(cover)?.takeIf { it.size <= 384 * 1024 }
    })
    val encoded = tagJson.encodeToString(compact)
    require(encoded.encodeToByteArray().size <= MAX_TAG_BYTES) { "Tags exceed size limit" }
    sidecar.writeString(encoded)
}

/** The metadata editor only succeeds after writing to the audio file itself. */
internal suspend fun MusicFile.saveEmbeddedTags(tags: LocalTrackTags, write: () -> Unit = {
    writeAudioMetadata(path, tags.title, tags.artists, tags.album.takeIf(String::isNotBlank),
        tags.track, tags.disc, tags.cover)
}) = location.root.withBrowserAccess {
    write()
    // Retire old app-generated sidecars after migration. A per-file Android grant may not let
    // us remove siblings; readLocalTrackTags ignores an older sidecar in that case.
    runCatching { trackTagFile(location.folder, file.name).delete(mustExist = false) }
    Unit
}
