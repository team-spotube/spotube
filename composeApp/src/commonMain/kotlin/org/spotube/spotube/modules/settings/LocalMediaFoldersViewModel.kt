/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false
)

/** The editor keeps a snapshot until Save; incoming settings updates never reset a draft. */
class LocalMediaFoldersViewModel(
    private val settings: StateFlow<UserSettings>,
    private val persistFolders: suspend (List<String>) -> Unit
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

    fun removeFolder(path: String) {
        draft.update { current ->
            if (!current.isOpen || current.isSaving) current
            else current.copy(paths = current.paths.filterNot { it == path }, saveFailed = false)
        }
    }

    fun dismiss() {
        if (!draft.value.isSaving) draft.value = FolderDraft()
    }

    fun save() {
        val current = draft.value
        if (!current.isOpen || current.isSaving || current.paths == current.original) return
        val paths = current.paths.toList()
        draft.value = current.copy(isSaving = true, saveFailed = false)
        viewModelScope.launch {
            try {
                persistFolders(paths)
                draft.value = FolderDraft()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                draft.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
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
            canSave = draft.isOpen && draft.paths != draft.original,
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
