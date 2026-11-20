/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.local_tracks.media

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.isRegularFile
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.lastModified
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.nameWithoutExtension
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.startAccessingSecurityScopedResource
import io.github.vinceglb.filekit.stopAccessingSecurityScopedResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.jetbrains.compose.resources.StringResource
import org.spotube.spotube.core.paths.Paths
import org.spotube.spotube.modules.library.local.encodeLocalCoverThumbnail
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.modules.settings.downloadFolderPath
import spotube.composeapp.generated.resources.*

internal data class MusicRoot(val path: String, val label: StringResource? = null, val displayName: String? = null)

internal expect fun platformMusicRoots(): List<MusicRoot>

internal fun musicRoots(paths: Paths, settings: UserSettings): List<MusicRoot> = (
    platformMusicRoots() + listOf(
        MusicRoot("${paths.getApplicationDataDirPath()}/downloads", Res.string.library_browser_private_data),
        MusicRoot("${paths.getApplicationCacheDirPath()}/downloads", Res.string.library_browser_private_cache),
        MusicRoot(settings.downloadFolderPath(paths), Res.string.library_tab_downloads)
    ) + settings.localMediaFolders.map(::MusicRoot)
).distinctBy { it.path }

internal data class MusicLocation(val root: PlatformFile, val folder: PlatformFile = root)

internal suspend fun <T> PlatformFile.withBrowserAccess(operation: suspend () -> T): T {
    val scoped = startAccessingSecurityScopedResource()
    try { return operation() } finally { if (scoped) stopAccessingSecurityScopedResource() }
}
internal data class MusicFile(
    val location: MusicLocation,
    val file: PlatformFile,
    val tags: LocalTrackTags,
    val playbackPath: String = file.path
) {
    val path get() = file.path
}

/** Index only explicitly chosen document trees or platform folders not covered by the system index. */
internal suspend fun scanMusicTree(root: PlatformFile, rootId: String = root.path): List<LocalMediaFolder> =
    root.withBrowserAccess {
        val result = mutableListOf<LocalMediaFolder>()
        val pending = ArrayDeque<Pair<MusicLocation, List<LocalMediaDirectory>>>()
        val seen = hashSetOf<String>()
        pending.addLast(MusicLocation(root) to listOf(LocalMediaDirectory(rootId, root.name)))
        while (pending.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val (location, trail) = pending.removeLast()
            if (!seen.add(location.folder.path)) continue
            val (children, tracks) = try {
                location.listDirectMusic()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                continue
            }
            if (tracks.isNotEmpty()) result += LocalMediaFolder(
                location.folder.path, trail.last().name,
                tracks.map { track ->
                    track.tags.toTrack(track.path).let { it.copy(coverBytes = it.coverBytes?.let(::encodeLocalCoverThumbnail)) }
                }, documentTrail = trail
            )
            children.forEach { child ->
                pending.addLast(location.copy(folder = child) to (trail + LocalMediaDirectory(child.path, child.name)))
            }
        }
        result
    }

/** Read the current folder only; used by background discovery, not by normal navigation. */
internal suspend fun MusicLocation.listDirectMusic(): Pair<List<PlatformFile>, List<MusicFile>> = root.withBrowserAccess {
    val files = folder.visibleChildren()
    val directories = files.filter {
        currentCoroutineContext().ensureActive()
        it.isBrowsableDirectory()
    }.sortedBy { it.name.lowercase() }
    val tracks = files.filter { it.isMusicCandidate() }
        .sortedBy { it.name.lowercase() }.mapNotNull { file ->
            currentCoroutineContext().ensureActive()
            val tags = readLocalTrackTags(folder, file)
            // Ambiguous containers can also contain video-only files. Only show them if readable as audio.
            if (tags == null && file.extension.lowercase() in mixedMusicExtensions) return@mapNotNull null
            val title = tags?.title?.takeIf(String::isNotBlank) ?: file.nameWithoutExtension
            MusicFile(this, file, tags?.copy(title = title) ?: LocalTrackTags(title))
        }
    directories to tracks
}

internal suspend fun MusicLocation.listDestinationFolders(): List<PlatformFile> = root.withBrowserAccess {
    folder.visibleChildren().filter { it.isBrowsableDirectory() }.sortedBy { it.name.lowercase() }
}

private fun PlatformFile.visibleChildren() = list().filterNot { it.name.startsWith('.') }

private fun PlatformFile.isMusicCandidate() = extension.lowercase() in playableMusicExtensions && isRegularFile()

private fun PlatformFile.isBrowsableDirectory(): Boolean = try {
    // Follow the tree, not filesystem links that can point back to an ancestor or another volume.
    val candidate = path
    if (candidate.startsWith("content:")) isDirectory()
    else FileSystem.SYSTEM.metadataOrNull(candidate.toPath())?.let { it.isDirectory && it.symlinkTarget == null } == true
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    false
}

internal class MusicDestinationExists : Exception()

internal data class MusicFileAccess(val read: Boolean?, val write: Boolean?, val delete: Boolean?)

internal expect fun musicFileAccess(path: String): MusicFileAccess

internal data class MusicFileDetails(
    val title: String,
    val fileName: String,
    val path: String,
    val format: String,
    val bytes: Long?,
    val modified: String?,
    val access: MusicFileAccess
)

internal suspend fun MusicFile.details(): MusicFileDetails = location.root.withBrowserAccess {
    MusicFileDetails(tags.title, file.name, path, file.extension.uppercase(),
        runCatching { file.size().takeIf { it >= 0 } }.getOrNull(),
        runCatching { file.lastModified().toString() }.getOrNull(), musicFileAccess(path))
}

internal fun isValidMusicFolderName(name: String): Boolean =
    name.isNotEmpty() && name.length <= 255 && !name.startsWith('.') && !name.endsWith('.') &&
        name.none { it in "/\\:*?\"<>|" || it.isISOControl() }

internal suspend fun MusicLocation.createFolder(name: String): PlatformFile = root.withBrowserAccess {
    require(isValidMusicFolderName(name))
    val child = PlatformFile(folder, name)
    if (child.exists()) throw MusicDestinationExists()
    child.createDirectories(mustCreate = true)
    // A document provider can assign an opaque ID unrelated to the requested filename.
    folder.list().firstOrNull { it.name == name && it.isDirectory() } ?: child
}

internal suspend fun MusicFile.copyTo(location: MusicLocation, move: Boolean) {
    this.location.root.withBrowserAccess {
        location.root.withBrowserAccess {
            val destination = PlatformFile(location.folder, file.name)
            val sourceTags = trackTagFile(this.location.folder, file.name)
            val destinationTags = trackTagFile(location.folder, file.name)
            if (destination.exists() || destinationTags.exists()) throw MusicDestinationExists()
            try {
                file.copyTo(destination)
                if (sourceTags.exists()) sourceTags.copyTo(destinationTags)
            } catch (e: Exception) {
                cleanupCopy(destinationTags)
                cleanupCopy(destination)
                throw e
            }
            // Never remove the original until the destination and its metadata are written.
            if (move) delete()
        }
    }
}

internal suspend fun MusicFile.delete() = location.root.withBrowserAccess {
    file.delete()
    trackTagFile(location.folder, file.name).delete(mustExist = false)
}

private suspend fun cleanupCopy(file: PlatformFile) {
    try { file.delete(mustExist = false) } catch (e: CancellationException) { throw e } catch (_: Exception) { }
}
