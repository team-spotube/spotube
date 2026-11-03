package org.spotube.spotube.core.audioplayer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioPlayerQueueTest {

    private fun createQueue(
        currentCollectionEntry: QueueCollectionEntry? = null
    ): AudioPlayerQueue {
        val collectionEntryFlow = MutableStateFlow(currentCollectionEntry)
        return object : AudioPlayerQueue {
            override val queueFlow: StateFlow<List<QueueEntry>> = MutableStateFlow(emptyList())
            override val currentQueueEntryFlow: StateFlow<QueueEntry?> = MutableStateFlow(null)
            override val currentCollectionEntryFlow: StateFlow<QueueCollectionEntry?> = collectionEntryFlow
            override val collectionHistoryFlow: StateFlow<List<QueueCollectionEntry>> = MutableStateFlow(emptyList())

            override suspend fun load(entries: List<QueueEntry>, autoPlay: Boolean, startPosition: Int, collectionEntry: QueueCollectionEntry?) = Unit
            override suspend fun addToQueue(entry: QueueEntry) = Unit
            override suspend fun addAllToQueue(entries: List<QueueEntry>, collectionEntry: QueueCollectionEntry?) = Unit
            override suspend fun addAllAfterCurrent(entries: List<QueueEntry>) = Unit
            override suspend fun removeFromQueue(entry: QueueEntry) = Unit
            override suspend fun removeFromQueueByMediaUrl(mediaUrl: String) = Unit
            override suspend fun move(fromIndex: Int, toIndex: Int) = Unit
            override suspend fun jumpTo(index: Int, autoPlay: Boolean) = Unit
            override suspend fun reloadCurrent() = Unit
            override suspend fun clear() = Unit
            override suspend fun getQueue(): List<QueueEntry> = emptyList()
            override suspend fun getCurrentQueueEntry(): QueueEntry? = null
            override suspend fun getCurrentCollectionEntry(): QueueCollectionEntry? = null
            override suspend fun getCollectionHistory(): List<QueueCollectionEntry> = emptyList()
        }
    }

    @Test
    fun `isPlaylistPlaying returns true when current collection is matching playlist`() = runTest {
        val queue = createQueue(currentCollectionEntry = QueueCollectionEntry.Playlist("pl-1"))
        assertTrue(queue.isPlaylistPlaying("pl-1"))
    }

    @Test
    fun `isPlaylistPlaying returns false when playlist id does not match`() = runTest {
        val queue = createQueue(currentCollectionEntry = QueueCollectionEntry.Playlist("pl-1"))
        assertFalse(queue.isPlaylistPlaying("pl-2"))
    }

    @Test
    fun `isPlaylistPlaying returns false when current is not a playlist`() = runTest {
        val queue = createQueue(currentCollectionEntry = QueueCollectionEntry.Album("al-1"))
        assertFalse(queue.isPlaylistPlaying())
    }

    @Test
    fun `isAlbumPlaying returns true when current collection is matching album`() = runTest {
        val queue = createQueue(currentCollectionEntry = QueueCollectionEntry.Album("al-1"))
        assertTrue(queue.isAlbumPlaying("al-1"))
    }

    @Test
    fun `isAlbumPlaying returns false when album id does not match`() = runTest {
        val queue = createQueue(currentCollectionEntry = QueueCollectionEntry.Album("al-1"))
        assertFalse(queue.isAlbumPlaying("al-2"))
    }

    @Test
    fun `isAlbumPlaying returns false when current is not an album`() = runTest {
        val queue = createQueue(currentCollectionEntry = QueueCollectionEntry.Playlist("pl-1"))
        assertFalse(queue.isAlbumPlaying())
    }

    @Test
    fun `isSavedTracksPlaying returns true when current is SavedTracks`() = runTest {
        val queue = createQueue(currentCollectionEntry = QueueCollectionEntry.SavedTracks)
        assertTrue(queue.isSavedTracksPlaying())
    }

    @Test
    fun `isSavedTracksPlaying returns false when current is not SavedTracks`() = runTest {
        val queue = createQueue(currentCollectionEntry = QueueCollectionEntry.Album("al-1"))
        assertFalse(queue.isSavedTracksPlaying())
    }
}
