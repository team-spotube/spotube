/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.bookmarkData
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import spotube.composeapp.generated.resources.*
import kotlin.io.encoding.Base64

data class LocalMediaFolderRowUi(val path: String, val name: String)

data class LocalMediaFoldersUiState(
    val isOpen: Boolean = false,
    val configuredCount: Int = 0,
    val singleConfiguredPath: String? = null,
    val folders: List<LocalMediaFolderRowUi> = emptyList(),
    val isSaving: Boolean = false,
    val canSave: Boolean = false,
    val saveFailed: Boolean = false,
    val automaticNote: StringResource? = null
)

private data class FolderDraft(
    val isOpen: Boolean = false,
    val original: List<String> = emptyList(),
    val paths: List<String> = emptyList(),
    /** Picker handles for folders added in this draft; used to persist their access bookmark. */
    val picked: Map<String, PlatformFile> = emptyMap(),
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false
)

/** The editor keeps a snapshot until Save; incoming settings updates never reset a draft. */
class LocalMediaFoldersViewModel(
    private val settings: StateFlow<UserSettings>,
    private val persistFolders: suspend (List<String>, Map<String, String>) -> Unit
) : ViewModel() {
    private val draft = MutableStateFlow(FolderDraft())

    val uiState = combine(settings, draft, ::toUiState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, toUiState(settings.value, draft.value))

    fun open() {
        if (draft.value.isOpen || draft.value.isSaving) return
        val paths = normalizedFolders(settings.value.localMediaFolders)
        draft.value = FolderDraft(isOpen = true, original = paths, paths = paths)
    }

    fun addFolder(path: String?) {
        val normalized = normalizeMediaFolderPath(path.orEmpty())
        if (normalized.isBlank()) return
        draft.update { current ->
            if (!current.isOpen || current.isSaving || normalized in current.paths) current
            else current.copy(paths = current.paths + normalized, saveFailed = false)
        }
    }

    /** Adds a folder picked through the platform dialog and remembers it for bookmark persistence. */
    fun addPickedFolder(folder: PlatformFile?) {
        if (folder == null) return
        val normalized = normalizeMediaFolderPath(folder.path)
        if (normalized.isBlank()) return
        draft.update { current ->
            if (!current.isOpen || current.isSaving) current
            else if (normalized in current.paths) current.copy(picked = current.picked + (normalized to folder))
            else current.copy(
                paths = current.paths + normalized,
                picked = current.picked + (normalized to folder),
                saveFailed = false)
        }
    }

    fun removeFolder(path: String) {
        draft.update { current ->
            if (!current.isOpen || current.isSaving) current
            else current.copy(
                paths = current.paths.filterNot { it == path },
                picked = current.picked - path,
                saveFailed = false)
        }
    }

    fun dismiss() {
        if (!draft.value.isSaving) draft.value = FolderDraft()
    }

    fun save() {
        val current = draft.value
        if (!current.isOpen || current.isSaving || current.paths == current.original) return
        val paths = current.paths.toList()
        val picked = current.picked
        draft.value = current.copy(isSaving = true, saveFailed = false)
        viewModelScope.launch {
            try {
                persistFolders(paths, bookmarksFor(paths, picked))
                draft.value = FolderDraft()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                draft.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    /** Keeps existing bookmarks and resolves the picked folders the platform needs to reopen them. */
    private suspend fun bookmarksFor(paths: List<String>, picked: Map<String, PlatformFile>): Map<String, String> {
        // Stored paths may predate normalization (or come from the browser), so match on the
        // normalized form to avoid dropping a valid bookmark when the list is saved again.
        val known = settings.value.localMediaFolderBookmarks
            .mapKeys { (path, _) -> normalizeMediaFolderPath(path) }
        val bookmarks = linkedMapOf<String, String>()
        for (path in paths) {
            known[path]?.let { bookmarks[path] = it }
        }
        for ((path, folder) in picked) {
            if (path !in paths || path in bookmarks) continue
            runCatching { Base64.encode(folder.bookmarkData().bytes) }.getOrNull()
                ?.let { bookmarks[path] = it }
        }
        return bookmarks
    }

    private fun toUiState(settings: UserSettings, draft: FolderDraft): LocalMediaFoldersUiState {
        val configured = normalizedFolders(settings.localMediaFolders)
        val hasDownloadFolder = !settings.overloadedDownloadFolder.isNullOrBlank()
        return LocalMediaFoldersUiState(
            isOpen = draft.isOpen,
            configuredCount = configured.size,
            singleConfiguredPath = configured.singleOrNull(),
            folders = draft.paths.map { path ->
                LocalMediaFolderRowUi(
                    path,
                    path.substringAfterLast('/').substringAfterLast('\\').ifBlank { path })
            },
            isSaving = draft.isSaving,
            canSave = draft.isOpen && !draft.isSaving && draft.paths != draft.original,
            saveFailed = draft.saveFailed,
            automaticNote = when {
                hasDownloadFolder && settings.enableMusicCaching -> Res.string.settings_local_media_folders_automatic_both
                hasDownloadFolder -> Res.string.settings_local_media_folders_automatic_downloads
                settings.enableMusicCaching -> Res.string.settings_local_media_folders_automatic_cache
                else -> null
            }
        )
    }
}

private fun normalizedFolders(paths: List<String>) =
    paths.map(::normalizeMediaFolderPath).filter { it.isNotBlank() }.distinct()

internal fun normalizeMediaFolderPath(path: String): String {
    val trimmed = path.trim()
    if (trimmed == "/" || trimmed == "\\") return trimmed
    if (trimmed.length >= 3 && trimmed[0].isLetter() && trimmed[1] == ':' &&
        trimmed.substring(2).all { it == '/' || it == '\\' }
    ) return trimmed.take(3)
    return trimmed.trimEnd('/', '\\')
}
