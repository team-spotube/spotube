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

package org.spotube.spotube.modules.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine

enum class DownloadRowAction { Cancel, Retry, Remove }

data class DownloadRowUiState(
    val id: String,
    val title: String,
    val artists: String,
    val artworkUrl: String?,
    val status: DownloadStatus,
    val progress: Float,
    val percent: Int,
    val error: String?,
    val action: DownloadRowAction
)

data class DownloadsUiState(
    val items: List<DownloadRowUiState> = emptyList(),
    val activeCount: Int = 0,
    val completedCount: Int = 0,
    val canClearCompleted: Boolean = false,
    /** Folder new downloads are saved to, shown so users can find their files. */
    val folder: String = ""
)

internal fun downloadsUiState(items: List<DownloadItem>, folder: String = ""): DownloadsUiState {
    val completedCount = items.count { it.status == DownloadStatus.Completed }
    return DownloadsUiState(
        items = items.map { item ->
            val progress = item.progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
            DownloadRowUiState(
                item.id, item.title, item.artists,
                (item.track?.album?.thumbnails
                    ?: item.track?.thumbnails)?.firstOrNull()?.url?.takeIf { it.isNotBlank() },
                item.status, progress, (progress * 100).toInt(),
                item.errorMessage ?: (item.status as? DownloadStatus.Failed)?.error,
                when (item.status) {
                    DownloadStatus.Completed -> DownloadRowAction.Remove
                    is DownloadStatus.Failed, DownloadStatus.Cancelled -> DownloadRowAction.Retry
                    else -> DownloadRowAction.Cancel
                }
            )
        },
        activeCount = items.count { it.status == DownloadStatus.Downloading || it.status == DownloadStatus.Queued },
        completedCount = completedCount,
        canClearCompleted = completedCount > 0,
        folder = folder
    )
}

class DownloadsViewModel(
    private val downloadManager: DownloadManager
) : ViewModel() {
    val downloads: StateFlow<List<DownloadItem>> = downloadManager.downloadsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val uiState = combine(downloadManager.downloadsFlow, downloadManager.downloadFolder, ::downloadsUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadsUiState())

    fun downloadTrack(track: MetadataTrack) {
        downloadManager.enqueue(track)
    }

    fun downloadTracks(tracks: List<MetadataTrack>) {
        tracks.forEach { track ->
            downloadManager.enqueue(track)
        }
    }

    fun cancel(id: String) {
        downloadManager.cancel(id)
    }

    fun retry(id: String) {
        downloadManager.retry(id)
    }

    fun remove(id: String) {
        downloadManager.remove(id)
    }

    fun clearCompleted() {
        downloadManager.clearCompleted()
    }
}
