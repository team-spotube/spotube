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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import org.spotube.spotube.core.di.injectLogger
import org.koin.core.component.KoinComponent
import java.io.File
import java.util.Locale

class AndroidLocalMediaDiscoveryService(
    private val context: Context) : LocalMediaDiscoveryService, KoinComponent {
    val logger by injectLogger<AndroidLocalMediaDiscoveryService>()

    private class TagInfo(
        val title: String?,
        val artists: List<String>,
        val album: String?,
        val durationMs: Long,
        val cover: ByteArray?)

    private class PendingTrack(
        val uri: String,
        val filePath: String?,
        val modifiedAt: Long,
        val folderPath: String,
        val fallbackName: String,
        val mediaStoreArtists: List<String>,
        val mediaStoreAlbum: String?,
        val mediaStoreDurationMs: Long)

    // Parsing tags + cover with lofty is the expensive part, so keep results across rescans.
    private val tagCache = ConcurrentHashMap<String, TagInfo>()

    override suspend fun discoverFolders(roots: List<String>): List<LocalMediaFolder> = withContext(Dispatchers.IO) {
        discoverFoldersBlocking(roots)
    }

    private suspend fun discoverFoldersBlocking(roots: List<String>): List<LocalMediaFolder> {
        logger.i { "discoverFolders: start roots=${roots.size}" }
        if (!hasReadPermission()) {
            logger.w { "discoverFolders: missing media permission, returning empty result" }
            return emptyList()
        }

        val pending = mutableListOf<PendingTrack>()
        val knownFilePaths = hashSetOf<String>()
        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        var totalRows = 0
        var acceptedRows = 0
        var skippedNonAudioRows = 0
        var unknownFolderRows = 0
        var nullCursor = false

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_MODIFIED) + if (Build.VERSION.SDK_INT >= 29) {
            arrayOf(MediaStore.Audio.Media.RELATIVE_PATH)
        } else {
            emptyArray()
        }

        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        logger.d { "discoverFolders: querying MediaStore with collection=${collection} projection=${projection.joinToString()} sortOrder=$sortOrder" }

        resolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val mimeTypeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
            val relativePathCol = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val modifiedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)

            logger.d {
                "discoverFolders: query ok columns(mimeType=$mimeTypeCol, relativePath=$relativePathCol)"
            }

            while (cursor.moveToNext()) {
                totalRows += 1
                val id = cursor.getLong(idCol)
                val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendPath(id.toString())
                    .build()
                    .toString()

                val rawTitle = cursor.getString(titleCol).orEmpty().trim()
                val displayName = cursor.getString(displayNameCol).orEmpty().trim()
                val mimeType = if (mimeTypeCol >= 0) {
                    cursor.getString(mimeTypeCol).orEmpty().trim()
                } else {
                    ""
                }
                if (!isAudioCandidate(displayName, mimeType)) {
                    skippedNonAudioRows += 1
                    if (skippedNonAudioRows <= 5) {
                        logger.d {
                            "discoverFolders: skip row id=$id displayName=$displayName mimeType=$mimeType"
                        }
                    }
                    continue
                }

                val relativePath = if (relativePathCol >= 0) {
                    cursor.getString(relativePathCol).orEmpty().trim()
                } else {
                    "Unknown"
                }
                val folderPath = relativePath.ifBlank { "Unknown" }
                if (folderPath == "Unknown") unknownFolderRows += 1
                val artist = cursor.getString(artistCol).orEmpty()
                    .takeUnless { it.equals("<unknown>", true) }
                val album = cursor.getString(albumCol).orEmpty().takeUnless { it.isBlank() }
                val durationMs = cursor.getLong(durationCol).coerceAtLeast(0L)
                val filePath = if (dataCol >= 0) cursor.getString(dataCol)?.takeIf { it.isNotBlank() } else null
                val modifiedAt = if (modifiedCol >= 0) cursor.getLong(modifiedCol) else 0L
                filePath?.let(knownFilePaths::add)

                val name = rawTitle.ifBlank {
                    displayName.substringBeforeLast('.').takeIf { it.isNotBlank() } ?: displayName
                }

                pending += PendingTrack(
                    uri = uri,
                    filePath = filePath,
                    modifiedAt = modifiedAt,
                    folderPath = folderPath,
                    fallbackName = name.ifBlank { "Unknown track" },
                    mediaStoreArtists = listOfNotNull(artist),
                    mediaStoreAlbum = album,
                    mediaStoreDurationMs = durationMs)
                acceptedRows += 1
            }
        } ?: run {
            nullCursor = true
        }

        if (nullCursor) {
            logger.w { "discoverFolders: MediaStore query returned null cursor" }
        }

        // Files MediaStore already indexed must not be listed a second time by the filesystem scan.
        discoverFileSystemFallback(roots, knownFilePaths, pending)

        val merged = buildFolders(pending)

        logger.i {
            "discoverFolders: done folders=${merged.size} mediaStoreTracks=$acceptedRows tracks=${pending.size} totalRows=$totalRows skippedNonAudio=$skippedNonAudioRows unknownFolderRows=$unknownFolderRows"
        }

        if (merged.isNotEmpty()) {
            val preview = merged.take(3).joinToString { "${it.name}:${it.trackCount}" }
            logger.d { "discoverFolders: folder preview=$preview" }
        }

        return merged
    }

    override fun observeChanges(
        roots: List<String>,
        onChanged: LocalMediaChangeCallback): LocalMediaObservation? {
        logger.i { "observeChanges: start roots=${roots.size}" }
        if (!hasReadPermission()) {
            logger.w { "observeChanges: missing media permission, observer not registered" }
            return null
        }

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                logger.d { "observeChanges: MediaStore changed selfChange=$selfChange" }
                onChanged()
            }
        }

        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer)

        logger.i { "observeChanges: observer registered" }

        return LocalMediaObservation {
            context.contentResolver.unregisterContentObserver(observer)
            logger.i { "observeChanges: observer unregistered" }
        }
    }

    private fun hasReadPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val granted =
            context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        logger.d { "hasReadPermission: permission=$permission granted=$granted" }
        return granted
    }

    private fun normalizeFolderName(path: String): String {
        return path.trimEnd('/', '\\').substringAfterLast('/').ifBlank {
            path.trimEnd('/', '\\').substringAfterLast('\\').ifBlank { "Unknown" }
        }
    }

    private fun isAudioCandidate(displayName: String, mimeType: String): Boolean {
        if (mimeType.startsWith("audio/", ignoreCase = true)) return true
        val extension = displayName.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.getDefault())
        return extension in SUPPORTED_EXTENSIONS
    }

    private fun discoverFileSystemFallback(
        roots: List<String>,
        knownFilePaths: Set<String>,
        out: MutableList<PendingTrack>) {
        val candidateRoots = resolveCandidateRoots(roots)
        if (candidateRoots.isEmpty()) {
            logger.d { "discoverFileSystemFallback: no candidate roots" }
            return
        }
        val storageRoot = Environment.getExternalStorageDirectory().absolutePath.trimEnd('/') + "/"
        val seen = hashSetOf<String>()
        var added = 0

        candidateRoots.forEach { root ->
            scanDirectory(root) { file ->
                if (file.extension.lowercase(Locale.getDefault()) !in SUPPORTED_EXTENSIONS) return@scanDirectory
                val absolute = file.absolutePath
                if (absolute in knownFilePaths || !seen.add(absolute)) return@scanDirectory

                // Same key shape as MediaStore's RELATIVE_PATH ("Download/") so folders merge.
                val parent = file.parentFile?.absolutePath.orEmpty().trimEnd('/') + "/"
                val folderPath = if (parent.startsWith(storageRoot)) parent.removePrefix(storageRoot) else parent
                out += PendingTrack(
                    uri = absolute,
                    filePath = absolute,
                    modifiedAt = file.lastModified() / 1000,
                    folderPath = folderPath,
                    fallbackName = file.nameWithoutExtension.ifBlank { file.name },
                    mediaStoreArtists = emptyList(),
                    mediaStoreAlbum = null,
                    mediaStoreDurationMs = 0L)
                added += 1
            }
        }
        logger.i { "discoverFileSystemFallback: extra files not in MediaStore=$added" }
    }

    private suspend fun buildFolders(pending: List<PendingTrack>): List<LocalMediaFolder> {
        val permits = Semaphore(4)
        val tracks = coroutineScope {
            pending.map { item ->
                async(Dispatchers.IO) {
                    val tag = permits.withPermit { readTags(item) }
                    item.folderPath to LocalMediaTrack(
                        path = item.uri,
                        name = tag?.title?.takeIf { it.isNotBlank() } ?: item.fallbackName,
                        artists = tag?.artists?.takeIf { it.isNotEmpty() } ?: item.mediaStoreArtists,
                        durationMs = tag?.durationMs?.takeIf { it > 0 } ?: item.mediaStoreDurationMs,
                        album = tag?.album?.takeIf { it.isNotBlank() } ?: item.mediaStoreAlbum,
                        coverBytes = tag?.cover)
                }
            }.awaitAll()
        }
        return tracks.groupBy({ it.first }, { it.second })
            .map { (path, list) ->
                LocalMediaFolder(
                    path = path,
                    name = normalizeFolderName(path),
                    tracks = list.sortedBy { it.name.lowercase(Locale.getDefault()) })
            }
            .sortedBy { it.name.lowercase(Locale.getDefault()) }
    }

    private fun readTags(item: PendingTrack): TagInfo? {
        val path = item.filePath ?: return null
        val key = "$path:${item.modifiedAt}"
        tagCache[key]?.let { return it }
        val file = File(path)
        if (!file.isFile || !file.canRead()) return null
        val info = try {
            val meta = uniffi.compose_app.readAudioMetadata(path)
            TagInfo(
                title = meta.title,
                artists = meta.artists,
                album = meta.album,
                durationMs = meta.durationMs,
                cover = meta.coverBytes?.let(::thumbnail))
        } catch (t: Throwable) {
            logger.d { "readTags: failed path=$path reason=${t.message}" }
            null
        }
        if (info != null) tagCache[key] = info
        return info
    }

    // Embedded covers can be several MB; the cache is serialised as JSON, so keep a small JPEG.
    private fun thumbnail(bytes: ByteArray): ByteArray? {
        if (bytes.isEmpty()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= THUMBNAIL_SIZE && bounds.outHeight / (sample * 2) >= THUMBNAIL_SIZE) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val scale = THUMBNAIL_SIZE.toFloat() / minOf(decoded.width, decoded.height)
        val bitmap = if (scale < 1f) Bitmap.createScaledBitmap(
            decoded, (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1), true) else decoded
        return ByteArrayOutputStream().use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it)
            it.toByteArray()
        }
    }

    private fun resolveCandidateRoots(roots: List<String>): List<File> {
        val configured = roots.mapNotNull { raw ->
            val normalized = raw.trim()
            if (normalized.isBlank()) return@mapNotNull null
            File(normalized)
        }

        val defaults = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS))

        return (configured + defaults)
            .map { it.absoluteFile }
            .distinctBy { it.absolutePath }
            .filter { it.exists() && it.isDirectory }
    }

    private fun scanDirectory(root: File, onFile: (File) -> Unit) {
        val queue = ArrayDeque<File>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val children = current.listFiles() ?: continue
            children.forEach { child ->
                if (child.isDirectory) {
                    queue.add(child)
                } else if (child.isFile) {
                    onFile(child)
                }
            }
        }
    }

    companion object {
        private const val THUMBNAIL_SIZE = 160
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
