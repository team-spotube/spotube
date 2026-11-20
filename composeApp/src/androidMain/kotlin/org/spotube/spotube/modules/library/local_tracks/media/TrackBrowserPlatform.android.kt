/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.local_tracks.media

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.os.Process
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import androidx.core.content.getSystemService
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import org.koin.core.context.GlobalContext
import org.spotube.spotube.modules.library.local.encodeLocalCoverThumbnail
import spotube.composeapp.generated.resources.*
import uniffi.compose_app.readAudioMetadataFromDescriptor
import java.io.File

private fun appContext() = GlobalContext.get().get<Context>()

internal actual fun encodeTrackCover(bytes: ByteArray): ByteArray? = encodeLocalCoverThumbnail(bytes)

internal actual fun musicDocumentFilePath(uri: String): String? {
    val document = Uri.parse(uri)
    if (document.scheme != "content") return null
    val id = runCatching { DocumentsContract.getDocumentId(document) }.getOrNull() ?: return null
    return when (document.authority) {
        "com.android.externalstorage.documents" -> {
            val volume = id.substringBefore(':')
            val relative = id.substringAfter(':', "")
            if (relative.split('/').any { it == ".." }) return null
            val root = when {
                volume.equals("primary", ignoreCase = true) -> Environment.getExternalStorageDirectory()
                volume.matches(Regex("[0-9a-fA-F-]+")) -> File("/storage", volume)
                else -> return null
            }
            File(root, relative).absolutePath
        }
        "com.android.providers.downloads.documents" -> id.removePrefix("raw:").takeIf { id.startsWith("raw:/") }
        else -> null
    }
}

internal actual fun platformMusicRoots(): List<MusicRoot> {
    val context = appContext()
    val primary = Environment.getExternalStorageDirectory().absolutePath
    val volumes = context.getSystemService<StorageManager>()?.storageVolumes.orEmpty()
    val external = if (android.os.Build.VERSION.SDK_INT >= 30) {
        volumes.filterNot { it.isPrimary }.mapNotNull { volume ->
            volume.directory?.absolutePath?.let { MusicRoot(it, displayName = volume.getDescription(context)) }
        }
    } else {
        context.getExternalFilesDirs(null).drop(1).mapNotNull { directory ->
            val path = directory?.absolutePath ?: return@mapNotNull null
            path.substringBefore("/Android/data/", missingDelimiterValue = "")
                .takeIf(String::isNotBlank)?.let { MusicRoot(it, Res.string.library_browser_external_storage) }
        }
    }
    return listOf(MusicRoot(primary, Res.string.library_browser_device_storage)) + external
}

internal actual fun readPlatformTrackTags(path: String): LocalTrackTags? = runCatching {
    val document = path.takeIf { it.startsWith("content:") }?.let(Uri::parse)
    if (document != null) {
        val embedded = runCatching {
            appContext().contentResolver.openFileDescriptor(document, "r")?.use { handle ->
                readAudioMetadataFromDescriptor(handle.fd, PlatformFile(path).name).toLocalTrackTags()
            }
        }.getOrNull()
        if (embedded != null) return embedded
    }
    val retriever = MediaMetadataRetriever()
    try {
        if (document != null) retriever.setDataSource(appContext(), document)
        else retriever.setDataSource(path)
        if (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == null) return null
        LocalTrackTags(
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty(),
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty(),
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty(),
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.substringBefore('/')?.toIntOrNull(),
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)?.substringBefore('/')?.toIntOrNull(),
            retriever.embeddedPicture
        )
    } finally {
        retriever.release()
    }
}.getOrNull()

internal actual fun musicFileAccess(path: String): MusicFileAccess {
    if (!path.startsWith("content:")) {
        val file = File(path)
        return MusicFileAccess(file.canRead(), file.canWrite(), file.parentFile?.let { it.canWrite() && it.canExecute() })
    }
    val context = appContext()
    val uri = Uri.parse(path)
    val readable = runCatching { context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false }.getOrDefault(false)
    if (!DocumentsContract.isDocumentUri(context, uri)) return MusicFileAccess(readable, null, null)
    val granted = context.checkUriPermission(uri, Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_WRITE_URI_PERMISSION) ==
        PackageManager.PERMISSION_GRANTED
    val flags = runCatching {
        context.contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_FLAGS), null, null, null)
            ?.use { if (it.moveToFirst()) it.getInt(0) else null }
    }.getOrNull()
    return MusicFileAccess(readable,
        if (granted) flags?.let { it and DocumentsContract.Document.FLAG_SUPPORTS_WRITE != 0 } else false,
        if (granted) flags?.let { it and DocumentsContract.Document.FLAG_SUPPORTS_DELETE != 0 } else false)
}
