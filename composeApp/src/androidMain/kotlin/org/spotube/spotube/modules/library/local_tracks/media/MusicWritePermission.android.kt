/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.local_tracks.media

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.name
import org.koin.core.context.GlobalContext
import uniffi.compose_app.AudioTagException
import uniffi.compose_app.writeAudioMetadataToDescriptor
import java.io.File
import java.io.FileNotFoundException

private sealed interface AndroidMusicWriteRequest : MusicWriteRequest {
    data class Media(val sender: IntentSender) : AndroidMusicWriteRequest
    data class Document(val uri: Uri) : AndroidMusicWriteRequest
}

internal actual suspend fun deleteMusicFile(file: MusicFile, requestPermission: MusicPermission?) {
    val context = GlobalContext.get().get<Context>()
    val resolver = context.contentResolver
    val target = Uri.parse(file.playbackPath)
    if (target.scheme == "content" && DocumentsContract.isDocumentUri(context, target)) {
        check(DocumentsContract.deleteDocument(resolver, target)) { "Document provider refused deletion" }
        return
    }
    if (target.scheme != "content" || target.authority != MediaStore.AUTHORITY) {
        file.file.delete()
        return
    }
    try {
        check(resolver.delete(target, null, null) > 0) { "MediaStore did not delete the track" }
    } catch (e: SecurityException) {
        if (requestPermission == null) throw e
        when {
            Build.VERSION.SDK_INT >= 30 -> {
                val request = MediaStore.createDeleteRequest(resolver, listOf(target))
                // The system executes a delete request itself before returning RESULT_OK.
                if (!requestPermission(AndroidMusicWriteRequest.Media(request.intentSender)).granted) throw e
            }
            Build.VERSION.SDK_INT >= 29 && e is RecoverableSecurityException -> {
                if (!requestPermission(AndroidMusicWriteRequest.Media(e.userAction.actionIntent.intentSender)).granted) throw e
                check(resolver.delete(target, null, null) > 0) { "MediaStore did not delete the track" }
            }
            else -> throw e
        }
    }
}

internal actual suspend fun saveMusicFileTags(file: MusicFile, tags: LocalTrackTags, documentUri: String?): MusicWriteRequest? {
    val context = GlobalContext.get().get<Context>()
    val playbackUri = Uri.parse(file.playbackPath)
    val originalDocument = listOf(playbackUri, Uri.parse(file.path)).firstOrNull {
        it.scheme == "content" && DocumentsContract.isDocumentUri(context, it)
    }
    val expectedDocument by lazy(LazyThreadSafetyMode.NONE) { originalDocument ?: externalDocumentUri(file.path) }
    val mediaStoreUri = playbackUri.takeIf { it.scheme == "content" && it.authority == MediaStore.AUTHORITY }

    val selectedDocument = if (documentUri == null) null else {
        val selected = Uri.parse(documentUri)
        // Never apply the draft to another file just because the user chose it in the picker.
        val expected = expectedDocument
        if (expected == null || !isSameDocument(context, expected, selected)) throw WrongMusicDocument()
        selected
    }

    val providerUri = selectedDocument ?: mediaStoreUri ?: originalDocument
    if (providerUri != null) {
        val descriptor = try {
            context.contentResolver.openFileDescriptor(providerUri, "rw") ?: error("Media file is unavailable")
        } catch (e: Exception) {
            // Some providers report a denied open as FileNotFoundException. A readable item
            // still exists and can be offered to the system permission dialog.
            if (e !is SecurityException && !(e is FileNotFoundException && canOpenForRead(context, providerUri))) throw e
            if (selectedDocument != null) throw AudioTagException.AccessDenied()
            return when {
                providerUri == mediaStoreUri && Build.VERSION.SDK_INT >= 30 -> AndroidMusicWriteRequest.Media(
                    MediaStore.createWriteRequest(context.contentResolver, listOf(providerUri)).intentSender
                )
                Build.VERSION.SDK_INT >= 29 && e is RecoverableSecurityException ->
                    AndroidMusicWriteRequest.Media(e.userAction.actionIntent.intentSender)
                else -> expectedDocument?.let { AndroidMusicWriteRequest.Document(it) } ?: throw AudioTagException.AccessDenied()
            }
        }
        descriptor.use { handle ->
            file.saveEmbeddedTags(tags) {
                writeAudioMetadataToDescriptor(handle.fd, file.file.name, tags.title, tags.artists,
                    tags.album.takeIf(String::isNotBlank), tags.track, tags.disc, tags.cover)
            }
        }
        return null
    }

    try {
        file.saveEmbeddedTags(tags)
    } catch (e: AudioTagException.AccessDenied) {
        return expectedDocument?.let { AndroidMusicWriteRequest.Document(it) } ?: throw e
    }
    return null
}

private fun canOpenForRead(context: Context, uri: Uri): Boolean = runCatching {
    context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
}.getOrDefault(false)

private fun isSameDocument(context: Context, expected: Uri, selected: Uri): Boolean =
    expected.authority == selected.authority && DocumentsContract.isDocumentUri(context, selected) &&
        DocumentsContract.getDocumentId(expected) == DocumentsContract.getDocumentId(selected)

private fun externalDocumentUri(path: String): Uri? {
    if (!File(path).isAbsolute) return null
    val file = File(path).canonicalFile
    for ((index, root) in platformMusicRoots().withIndex()) {
        val volume = File(root.path).canonicalFile
        val prefix = volume.path.trimEnd('/') + "/"
        if (file.path.startsWith(prefix)) {
            val id = if (index == 0) "primary" else volume.name
            return DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "$id:${file.path.removePrefix(prefix)}")
        }
    }
    return null
}

@Composable
internal actual fun rememberMusicWriteLauncher(onResult: (MusicWriteDecision) -> Unit): (MusicWriteRequest) -> Unit {
    val media = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        onResult(MusicWriteDecision(it.resultCode == Activity.RESULT_OK))
    }
    val document = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val selected = it.data?.data?.toString()
        onResult(MusicWriteDecision(it.resultCode == Activity.RESULT_OK && selected != null, selected))
    }
    return { request ->
        try {
            when (val android = request as AndroidMusicWriteRequest) {
                is AndroidMusicWriteRequest.Media -> media.launch(IntentSenderRequest.Builder(android.sender).build())
                is AndroidMusicWriteRequest.Document -> document.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "video/webm", "video/mp4", "application/ogg"))
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, android.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                })
            }
        } catch (_: Exception) {
            onResult(MusicWriteDecision(false))
        }
    }
}
