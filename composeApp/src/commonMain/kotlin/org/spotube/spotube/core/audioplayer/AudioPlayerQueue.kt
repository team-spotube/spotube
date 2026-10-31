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

package org.spotube.spotube.core.audioplayer

import kotlinx.coroutines.flow.StateFlow

interface AudioPlayerQueue {
	val queueFlow: StateFlow<List<QueueEntry>>
	val currentQueueEntryFlow: StateFlow<QueueEntry?>
	val currentCollectionEntryFlow: StateFlow<QueueCollectionEntry?>
	val collectionHistoryFlow: StateFlow<List<QueueCollectionEntry>>

	suspend fun load(
		entries: List<QueueEntry>,
		autoPlay: Boolean = true,
		startPosition: Int = 0,
		collectionEntry: QueueCollectionEntry? = null)
	suspend fun addToQueue(entry: QueueEntry)
	suspend fun addAllToQueue(entries: List<QueueEntry>, collectionEntry: QueueCollectionEntry? = null)
	suspend fun addAllAfterCurrent(entries: List<QueueEntry>)
	suspend fun removeFromQueue(entry: QueueEntry)
	suspend fun removeFromQueueByMediaUrl(mediaUrl: String)
	suspend fun move(fromIndex: Int, toIndex: Int)
	suspend fun jumpTo(index: Int, autoPlay: Boolean = true)
	suspend fun reloadCurrent()
	suspend fun clear()
	suspend fun getQueue(): List<QueueEntry>
	suspend fun getCurrentQueueEntry(): QueueEntry?
	suspend fun getCurrentCollectionEntry(): QueueCollectionEntry?
	suspend fun getCollectionHistory(): List<QueueCollectionEntry>

	/**
	 * Stops endless playback from adding recommendations when the queue runs out. A jam
	 * guest's queue belongs to the host; recommendations of its own would split them.
	 */
	fun setEndlessPlaybackSuppressed(suppressed: Boolean) = Unit

	fun isPlaylistPlaying(playlistId: String? = null): Boolean {
		val entry = currentCollectionEntryFlow.value as? QueueCollectionEntry.Playlist ?: return false
		return playlistId == null || entry.id == playlistId
	}

	fun isAlbumPlaying(albumId: String? = null): Boolean {
		val entry = currentCollectionEntryFlow.value as? QueueCollectionEntry.Album ?: return false
		return albumId == null || entry.id == albumId
	}

	fun isSavedTracksPlaying(): Boolean {
		return currentCollectionEntryFlow.value is QueueCollectionEntry.SavedTracks
	}
}
