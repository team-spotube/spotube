/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.local_tracks

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import okio.Path
import okio.Path.Companion.toPath
import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaCache
import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaDirectory
import org.spotube.spotube.modules.library.local_tracks.media.LocalTrackTags
import org.spotube.spotube.modules.library.local_tracks.media.MusicFile
import org.spotube.spotube.modules.library.local_tracks.media.MusicLocation

internal typealias FolderEntry = Pair<BrowserFolder, MusicLocation>

internal data class BrowserListing(
    val folders: List<FolderEntry> = emptyList(),
    val tracks: List<MusicFile> = emptyList(),
    val trackRows: List<BrowserTrack> = tracks.map { file ->
        BrowserTrack(file.path, file.tags.title,
            listOf(file.tags.artists.replace(';', ','), file.tags.album).filter(String::isNotBlank)
                .joinToString(" • ").ifBlank { null }, file.tags.cover)
    }
)

internal data class MusicBrowserIndex(
    val locations: List<FolderEntry> = emptyList(),
    val listings: Map<MusicLocation?, BrowserListing> = emptyMap()
)

private class Branch(val entry: FolderEntry) {
    val children = linkedMapOf<String, Branch>()
    val tracks = linkedMapOf<String, MusicFile>()

    fun descend(path: String, name: String, file: PlatformFile): Branch = children.getOrPut(path) {
        Branch(BrowserFolder(path, name) to entry.second.copy(folder = file))
    }
}

/** Project the existing media index into a tree; no filesystem traversal or tag reading. */
internal fun buildMusicBrowserIndex(
    cache: LocalMediaCache,
    locations: List<FolderEntry>,
    legacySharedRoot: Path? = null
): MusicBrowserIndex {
    val roots = locations.associate { it.first.id to Branch(it) }
    val filesystemRoots = roots.values.mapNotNull { root ->
        root.entry.first.id.absoluteBrowserPath()?.let { it to root }
    }
    for (folder in cache.folders) {
        val document = folder.documentTrail.takeIf { it.isNotEmpty() }?.let { trail ->
            roots[trail.first().path]?.let { root ->
                trail.drop(1).fold(root) { branch, (path, name) -> branch.descend(path, name, PlatformFile(path)) }
            }
        }
        val fallback = folder.path.absoluteBrowserPath() ?: legacySharedRoot?.let {
            if (folder.path == "Unknown" || folder.path.startsWith("content:")) null
            else (it / folder.path.toPath()).normalized()
        }
        for (track in folder.tracks) {
            val tags = LocalTrackTags(track.name, track.artists.joinToString("; "), track.album.orEmpty(),
                track.durationMs, cover = track.coverBytes)
            if (document != null) {
                val file = MusicFile(document.entry.second, PlatformFile(track.path), tags, track.path)
                document.tracks[file.path] = file
                continue
            }
            if (folder.documentTrail.isNotEmpty()) continue
            val filePath = track.browserPath?.absoluteBrowserPath() ?: track.path.absoluteBrowserPath()
            val parent = filePath?.parent ?: fallback ?: continue
            for ((rootPath, root) in filesystemRoots) {
                if (parent.root != rootPath.root || parent.segments.take(rootPath.segments.size) != rootPath.segments) continue
                var branch = root
                var path = rootPath
                for (segment in parent.segments.drop(rootPath.segments.size)) {
                    // The disk browser excluded hidden files and directories.
                    if (segment.startsWith('.')) break
                    path /= segment
                    branch = branch.descend(path.toString(), segment, PlatformFile(path.toString()))
                }
                if (path != parent || filePath?.name?.startsWith('.') == true) continue
                val file = MusicFile(branch.entry.second, PlatformFile(filePath?.toString() ?: track.path), tags, track.path)
                branch.tracks[file.path] = file
            }
        }
    }

    val listings = mutableMapOf<MusicLocation?, BrowserListing>()
    fun assemble(branch: Branch): Boolean {
        val children = branch.children.values.filter(::assemble).map { it.entry }.sortedBy { it.first.name.lowercase() }
        if (children.isEmpty() && branch.tracks.isEmpty()) return false
        listings[branch.entry.second] = BrowserListing(children, branch.tracks.values.sortedBy { it.tags.title.lowercase() })
        return true
    }
    listings[null] = BrowserListing(roots.values.filter(::assemble).map { it.entry })
    return MusicBrowserIndex(locations, listings)
}

private fun String.absoluteBrowserPath(): Path? =
    takeUnless { it.startsWith("content:") }?.toPath(normalize = true)?.takeIf { it.isAbsolute }
