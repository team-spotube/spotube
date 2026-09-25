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

package dev.krtirtho.spotube.modules.shell.player_queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerQueue
import dev.krtirtho.spotube.core.audioplayer.QueueEntry
import dev.krtirtho.spotube.core.jam.JamParticipant
import dev.krtirtho.spotube.core.jam.JamRole
import dev.krtirtho.spotube.core.jam.JamRoomService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QueueItemUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val durationLabel: String,
    val isCurrent: Boolean,
    val imageUrl: String?,
    val originalIndex: Int,
    /** Participant who added this item to the jam queue, if any. */
    val addedByParticipant: JamParticipant? = null,
)

data class QueueContentUiState(
    val filterQuery: String = "",
    val displayItems: List<QueueItemUi> = emptyList(),
    val isFiltered: Boolean = false,
    /** Guests cannot reorder/remove/clear the shared jam queue. */
    val isReadOnly: Boolean = false,
    val isJamHost: Boolean = false,
    val participants: List<JamParticipant> = emptyList(),
)

class PlayerQueueContentViewModel(
    private val audioPlayerQueue: AudioPlayerQueue,
    private val jamRoomService: JamRoomService,
) : ViewModel() {
    private val queueVisibilityFlow = MutableStateFlow(false)
    private val queueFilterFlow = MutableStateFlow("")
    private val reorderBuffer = MutableStateFlow<List<QueueItemUi>?>(null)
    private var moveFromOriginal: Int? = null
    private var moveToDisplay: Int? = null

    val isQueueVisible: StateFlow<Boolean> = queueVisibilityFlow.asStateFlow()

    private val computedItems: StateFlow<List<QueueItemUi>> = combine(
        audioPlayerQueue.queueFlow,
        audioPlayerQueue.currentQueueEntryFlow,
        jamRoomService.participants,
    ) { queue, currentEntry, participants ->
        val currentIndex = if (currentEntry != null) {
            queue.indexOfFirst { it.matchesCurrent(currentEntry) }
        } else {
            -1
        }
        queue.mapIndexed { index, entry ->
            val title: String
            var subtitle: String
            val durationMs: Long
            val imageUrl: String?

            when (entry) {
                is QueueEntry.StreamingTrack -> {
                    title = entry.track.title
                    subtitle = entry.track.artists.joinToString(", ") { it.name }
                    durationMs = entry.track.durationMs
                    imageUrl = entry.track.thumbnails?.maxByOrNull { it.width * it.height }?.url
                        ?: entry.track.album?.thumbnails?.maxByOrNull { it.width * it.height }?.url
                }

                is QueueEntry.LocalTrack -> {
                    title = entry.name
                    subtitle = entry.artists.joinToString(", ")
                    durationMs = entry.duration
                    imageUrl = null
                }
            }

            val addedByParticipant = participants.firstOrNull { it.id == entry.addedBy }
            if (addedByParticipant != null) {
                subtitle = "$subtitle • Added by ${addedByParticipant.displayName}"
            }

            QueueItemUi(
                id = "${entry.url}@$index",
                title = title,
                subtitle = subtitle,
                durationLabel = durationMs.toDurationLabel(),
                isCurrent = index == currentIndex,
                imageUrl = imageUrl,
                originalIndex = index,
                addedByParticipant = addedByParticipant,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val queueContentUiState: StateFlow<QueueContentUiState> = combine(
        computedItems,
        reorderBuffer,
        queueFilterFlow,
        jamRoomService.role,
        jamRoomService.participants,
    ) { items, buffer, filterQuery, role, participants ->
        val normalizedFilter = filterQuery.trim().lowercase()
        val isFiltered = normalizedFilter.isNotBlank()
        val filtered = if (isFiltered) {
            items.filter { item ->
                item.title.lowercase().contains(normalizedFilter) ||
                    item.subtitle.lowercase().contains(normalizedFilter)
            }
        } else {
            items
        }
        QueueContentUiState(
            filterQuery = filterQuery,
            displayItems = buffer ?: filtered,
            isFiltered = isFiltered,
            isReadOnly = role == JamRole.Guest,
            isJamHost = role == JamRole.Host,
            participants = participants,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = QueueContentUiState(),
    )

    fun toggleQueueVisibility() {
        queueVisibilityFlow.update { !it }
    }

    fun setQueueVisibility(isVisible: Boolean) {
        queueVisibilityFlow.value = isVisible
    }

    fun setQueueFilter(query: String) {
        queueFilterFlow.value = query
    }

    fun playQueueItem(index: Int) {
        if (index < 0 || queueContentUiState.value.isReadOnly) return
        viewModelScope.launch {
            audioPlayerQueue.jumpTo(index)
        }
    }

    fun removeQueueItem(index: Int) {
        if (index < 0 || queueContentUiState.value.isReadOnly) return
        viewModelScope.launch {
            val currentQueue = audioPlayerQueue.queueFlow.value
            if (index < currentQueue.size) {
                audioPlayerQueue.removeFromQueue(currentQueue[index])
            }
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex < 0 || toIndex < 0) return
        if (queueContentUiState.value.isReadOnly) return
        viewModelScope.launch {
            audioPlayerQueue.move(fromIndex, toIndex)
        }
    }

    fun clearQueue() {
        if (queueContentUiState.value.isReadOnly) return
        viewModelScope.launch {
            audioPlayerQueue.clear()
        }
    }

    // ---------- Jam participant moderation (host only) ----------

    fun kickParticipant(participantId: String) {
        if (!queueContentUiState.value.isJamHost) return
        viewModelScope.launch { jamRoomService.kickParticipant(participantId) }
    }

    fun banParticipant(participantId: String) {
        if (!queueContentUiState.value.isJamHost) return
        viewModelScope.launch { jamRoomService.banParticipant(participantId) }
    }

    /** Removes every queue item that the given participant suggested. */
    fun removeParticipantTracks(participantId: String) {
        if (!queueContentUiState.value.isJamHost) return
        viewModelScope.launch {
            val entries = audioPlayerQueue.queueFlow.value.filter { it.addedBy == participantId }
            entries.forEach { audioPlayerQueue.removeFromQueue(it) }
        }
    }

    fun onDragStart() {
        if (reorderBuffer.value != null) return
        if (queueContentUiState.value.isReadOnly) return
        val currentItems = queueContentUiState.value.displayItems
        reorderBuffer.value = currentItems.toList()
    }

    fun onMove(from: Int, to: Int) {
        if (queueContentUiState.value.isReadOnly) return
        val buffer = reorderBuffer.value ?: return
        if (from == to || from < 0 || to < 0 || from >= buffer.size || to >= buffer.size) return
        val item = buffer[from]
        val newList = buffer.toMutableList().apply {
            removeAt(from)
            add(to, item)
        }
        reorderBuffer.value = newList
        moveFromOriginal = item.originalIndex
        moveToDisplay = to
    }

    fun onDragStop() {
        val fromOriginal = moveFromOriginal
        val toDisplay = moveToDisplay
        if (fromOriginal != null && toDisplay != null) {
            moveQueueItem(fromOriginal, toDisplay)
        }
        moveFromOriginal = null
        moveToDisplay = null
        reorderBuffer.value = null
    }
}

private fun QueueEntry.matchesCurrent(current: QueueEntry): Boolean {
    return when {
        this is QueueEntry.StreamingTrack && current is QueueEntry.StreamingTrack -> {
            this.track.id == current.track.id
        }

        this is QueueEntry.LocalTrack && current is QueueEntry.LocalTrack -> {
            this.url == current.url && this.name == current.name
        }

        else -> false
    }
}

private fun Long.toDurationLabel(): String {
    val totalSeconds = (this / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
