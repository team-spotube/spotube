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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension

class JvmLocalMediaDiscoveryService : LocalMediaDiscoveryService {
    override suspend fun discoverFolders(roots: List<String>): List<LocalMediaFolder> = withContext(Dispatchers.IO) {
        val resolvedRoots = resolveRoots(roots)
        if (resolvedRoots.isEmpty()) {
            return@withContext emptyList()
        }

        val folders = linkedMapOf<String, MutableList<LocalMediaTrack>>()
        val scanned = AtomicLong(0)

        resolvedRoots.forEach { root ->
            try {
                Files.walk(root).use { stream ->
                    stream
                        .filter { path -> !Files.isDirectory(path) }
                        .filter { path -> isSupportedAudioExtension(path.extension) }
                        .forEach { filePath ->
                            if (scanned.incrementAndGet() > MAX_FILES_PER_SCAN) return@forEach
                            val parent = filePath.parent?.absolutePathString() ?: return@forEach
                            val trackPath = filePath.toAbsolutePath().toString()

                            val metadata = try {
                                uniffi.compose_app.readAudioMetadata(trackPath)
                            } catch (_: Exception) {
                                null
                            }

                            val track = LocalMediaTrack(
                                path = trackPath,
                                name = metadata?.title?.takeIf { it.isNotBlank() }
                                    ?: filePath.nameWithoutExtension.ifBlank {
                                        filePath.fileName.toString()
                                    },
                                artists = metadata?.artists?.takeIf { it.isNotEmpty() } ?: emptyList(),
                                durationMs = metadata?.durationMs ?: 0L,
                                album = metadata?.album,
                                coverBytes = metadata?.coverBytes?.let(::coverThumbnail))
                            folders.getOrPut(parent) { mutableListOf() }.add(track)
                        }
                }
            } catch (e: Exception) {
                println("Debug: Skipping unreadable directory ${root}: ${e.message}")
            }
        }

        folders.entries
            .map { (path, tracks) ->
                LocalMediaFolder(
                    path = path,
                    name = Path.of(path).fileName?.toString().orEmpty().ifBlank { path },
                    tracks = tracks.sortedBy { it.name.lowercase(Locale.getDefault()) })
            }
            .sortedBy { it.name.lowercase(Locale.getDefault()) }
    }

    override fun observeChanges(
        roots: List<String>,
        onChanged: LocalMediaChangeCallback): LocalMediaObservation? {
        val resolvedRoots = resolveRoots(roots)
        if (resolvedRoots.isEmpty()) return null

        val watchService = FileSystems.getDefault().newWatchService()
        val watchedDirs = mutableSetOf<Path>()
        resolvedRoots.forEach { root ->
            try {
                Files.walk(root).use { stream ->
                    stream.filter { it.isDirectory() }.forEach { dir ->
                        if (watchedDirs.add(dir)) {
                            dir.register(
                                watchService,
                                StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_DELETE,
                                StandardWatchEventKinds.ENTRY_MODIFY)
                        }
                    }
                }
            } catch (e: Exception) {
                println("Debug: Skipping unwatchable directory ${root}: ${e.message}")
            }
        }

        val executor = Executors.newSingleThreadExecutor()
        val lastTriggeredAt = AtomicLong(0)
        executor.submit {
            while (!Thread.currentThread().isInterrupted) {
                val key: WatchKey = try {
                    watchService.poll(500, TimeUnit.MILLISECONDS) ?: continue
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }

                if (key.pollEvents().isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    val previous = lastTriggeredAt.get()
                    if (now - previous >= WATCH_DEBOUNCE_MS && lastTriggeredAt.compareAndSet(previous, now)) {
                        onChanged()
                    }
                }

                if (!key.reset()) {
                    break
                }
            }
        }

        return LocalMediaObservation {
            executor.shutdownNow()
            runCatching { watchService.close() }
        }
    }

    private fun isSupportedAudioExtension(extension: String): Boolean {
        return extension.lowercase(Locale.getDefault()) in SUPPORTED_EXTENSIONS
    }

    private fun resolveRoots(roots: List<String>): List<Path> {
        val customRoots = roots
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { Path.of(it) }
            .distinct()
            .toList()

        val home = System.getProperty("user.home").orEmpty()
        val defaults = if (home.isBlank()) {
            emptyList()
        } else {
            listOf(
                Path.of(home, "Music"),
                Path.of(home, "Downloads"))
        }

        return (defaults + customRoots)
            .distinct()
            .filter { it.exists() && it.isDirectory() && Files.isReadable(it) }
    }

    companion object {
        private const val WATCH_DEBOUNCE_MS = 2_500L
        private const val MAX_FILES_PER_SCAN = 100_000L
        private val SUPPORTED_EXTENSIONS = setOf(
            "mp3",
            "m4a",
            "aac",
            "flac",
            "wav",
            "ogg",
            "opus",
            "wma")
    }
}

/**
 * Embedded covers can be several MB each and the index is serialised to JSON, so keep a small
 * JPEG like Android does (#3193). Returns null for unreadable art.
 */
internal fun coverThumbnail(bytes: ByteArray, size: Int = COVER_THUMBNAIL_SIZE): ByteArray? = runCatching {
    Image.makeFromEncoded(bytes).use { source ->
        val scale = size.toFloat() / minOf(source.width, source.height)
        if (scale >= 1f && bytes.size <= MAX_SMALL_COVER_BYTES) return@runCatching bytes
        val width = (source.width * scale.coerceAtMost(1f)).toInt().coerceAtLeast(1)
        val height = (source.height * scale.coerceAtMost(1f)).toInt().coerceAtLeast(1)
        Surface.makeRasterN32Premul(width, height).use { surface ->
            surface.canvas.drawImageRect(
                source,
                Rect.makeWH(source.width.toFloat(), source.height.toFloat()),
                Rect.makeWH(width.toFloat(), height.toFloat()),
                FilterMipmap(FilterMode.LINEAR, MipmapMode.NONE),
                null,
                true)
            surface.makeImageSnapshot().use { it.encodeToData(EncodedImageFormat.JPEG, 85)?.bytes }
        }
    }
}.getOrNull()

private const val COVER_THUMBNAIL_SIZE = 160
private const val MAX_SMALL_COVER_BYTES = 64 * 1024

