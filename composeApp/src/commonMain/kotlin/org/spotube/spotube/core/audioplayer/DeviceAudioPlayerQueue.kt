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

import org.spotube.plugin_interfaces.plugin_apis.audio.StreamProtocol
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.Thumbnail
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.di.injectLogger
import org.spotube.spotube.core.newpipe.NewPipeService
import org.spotube.spotube.core.newpipe.SearchKind
import org.spotube.spotube.core.server.LocalServer
import org.spotube.spotube.modules.blacklist.BlacklistRepository
import org.spotube.spotube.modules.plugin.PluginProvider
import org.spotube.spotube.modules.settings.SettingsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.spotube.spotube.PlatformType
import org.spotube.spotube.core.server.StreamingUrlRepository
import org.spotube.spotube.getPlatform
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class DeviceAudioPlayerQueue(
    private val audioPlayer: AudioPlayerInterface,
    private val settingsProvider: SettingsProvider,
    private val repository: QueueStateRepository,
    private val pluginProvider: PluginProvider,
    private val blacklistRepository: BlacklistRepository) : AudioPlayerQueue, KoinComponent {

    private val logger by injectLogger<DeviceAudioPlayerQueue>()
    private val streamingUrlRepository by inject<StreamingUrlRepository>()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val entryByMediaUrl = MutableStateFlow<Map<String, QueueEntry>>(emptyMap())
    private val currentCollectionEntryState = MutableStateFlow<QueueCollectionEntry?>(null)
    private val collectionHistoryState = MutableStateFlow<List<QueueCollectionEntry>>(emptyList())

    private var isRestoringPersistedState = true

    override val queueFlow: StateFlow<List<QueueEntry>> = combine(
        audioPlayer.playlistFlow,
        entryByMediaUrl) { playlist, knownEntries ->
        playlist.map { mediaItem ->
            knownEntries[mediaItem.url] ?: mediaItem.toFallbackQueueEntry()
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    override val currentQueueEntryFlow: StateFlow<QueueEntry?> = combine(
        audioPlayer.currentMediaItemFlow,
        entryByMediaUrl) { mediaItem, knownEntries ->
        mediaItem?.let { knownEntries[it.url] ?: it.toFallbackQueueEntry() }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)

    override val currentCollectionEntryFlow: StateFlow<QueueCollectionEntry?> =
        currentCollectionEntryState.asStateFlow()

    override val collectionHistoryFlow: StateFlow<List<QueueCollectionEntry>> =
        collectionHistoryState.asStateFlow()

    init {
        logger.d { "Initializing DeviceAudioPlayerQueue" }
        scope.launch {
            logger.d { "Starting restoration of persisted queue state" }
            restorePersistedState()
            isRestoringPersistedState = false
            logger.d { "Persisted queue state restoration completed" }
        }

        scope.launch {
            audioPlayer.playlistFlow.collect {
                logger.d { "Playlist changed, persisting queue state" }
                persistQueueStateIfReady()
            }
        }

        scope.launch {
            audioPlayer.errorFlow.collect { error ->
                logger.e(error) { "AudioPlayer error encountered in queue: ${error.message}" }
            }
        }

        scope.launch {
            audioPlayer.currentMediaItemFlow.collect {
                logger.d { "Current media item changed, persisting queue state" }
                persistQueueStateIfReady()
            }
        }

        scope.launch {
            combine(
                audioPlayer.currentMediaItemFlow,
                audioPlayer.playlistFlow,
                audioPlayer.loopStateFlow) { currentMedia, playlist, loopState ->
                Triple(currentMedia, playlist, loopState)
            }.collect { (currentMedia, playlist, loopState) ->
                if (currentMedia == null || playlist.isEmpty()) return@collect
                if (loopState == LoopState.ALL) return@collect

                val currentIndex = playlist.indexOfFirst { it.url == currentMedia.url }
                if (currentIndex < 0) return@collect

                val tracksAfterCurrent = playlist.size - currentIndex - 1
                if (tracksAfterCurrent <= 0) {
                    handleQueueCompletion()
                }
            }
        }
    }

    override suspend fun load(
        entries: List<QueueEntry>,
        autoPlay: Boolean,
        startPosition: Int,
        collectionEntry: QueueCollectionEntry?) {
        logger.i { "Loading queue with ${entries.size} entries, autoPlay=$autoPlay, startPosition=$startPosition" }
        val paired = entries.mapNotNull { entry -> entry.toMediaItem()?.let { mediaItem -> mediaItem to entry } }
        if (paired.isEmpty()) {
            logger.w { "load: no valid media items after filtering blanks" }
        }
        val mediaItems = paired.map { it.first }
        val normalizedStartPosition =
            if (mediaItems.isEmpty()) 0 else startPosition.coerceIn(0, mediaItems.lastIndex)
        logger.d { "Normalized start position: $startPosition -> $normalizedStartPosition" }
        entryByMediaUrl.value = paired.associate { (mediaItem, entry) -> mediaItem.url to entry }
        updateCollectionContext(collectionEntry)
        audioPlayer.load(mediaItems, autoPlay, normalizedStartPosition)
        persistQueueStateIfReady()
        logger.i { "Queue loaded successfully" }
    }

    override suspend fun addToQueue(entry: QueueEntry) {
        val mediaItem = entry.toMediaItem() ?: run {
            logger.w { "addToQueue: skipping entry with blank url" }
            return
        }
        logger.d { "Adding entry to queue: ${mediaItem.title}" }
        entryByMediaUrl.update { it + (mediaItem.url to entry) }
        audioPlayer.addMediaItem(mediaItem)
        persistQueueStateIfReady()
        logger.d { "Entry added successfully" }
    }

    override suspend fun addAllToQueue(entries: List<QueueEntry>, collectionEntry: QueueCollectionEntry?) {
        if (entries.isEmpty()) {
            logger.d { "addAllToQueue called with empty list, skipping" }
            return
        }

        logger.i { "Adding ${entries.size} entries to queue" }
        val paired = entries.mapNotNull { entry -> entry.toMediaItem()?.let { mediaItem -> mediaItem to entry } }
        if (paired.isEmpty()) {
            logger.w { "addAllToQueue: all entries had blank urls, skipping" }
            return
        }
        val mediaItems = paired.map { it.first }
        entryByMediaUrl.update { current ->
            current + paired.associate { (mediaItem, entry) -> mediaItem.url to entry }
        }

        updateCollectionContext(collectionEntry)

        mediaItems.forEach { mediaItem ->
            audioPlayer.addMediaItem(mediaItem)
        }

        persistQueueStateIfReady()
        logger.d { "All entries added successfully" }
    }

    override suspend fun addAllAfterCurrent(entries: List<QueueEntry>) {
        if (entries.isEmpty()) {
            logger.d { "addAllAfterCurrent called with empty list, skipping" }
            return
        }

        logger.i { "Adding ${entries.size} entries after current position" }
        val paired = entries.mapNotNull { entry -> entry.toMediaItem()?.let { mediaItem -> mediaItem to entry } }
        if (paired.isEmpty()) {
            logger.w { "addAllAfterCurrent: all entries had blank urls, skipping" }
            return
        }
        val mediaItems = paired.map { it.first }
        entryByMediaUrl.update { current ->
            current + paired.associate { (mediaItem, entry) -> mediaItem.url to entry }
        }

        mediaItems.forEach { mediaItem ->
            audioPlayer.insertMediaItemAtNextIndex(mediaItem)
        }

        persistQueueStateIfReady()
        logger.d { "All entries added after current successfully" }
    }

    override suspend fun removeFromQueue(entry: QueueEntry) {
        val mediaUrl = entryByMediaUrl.value.entries.firstOrNull { it.value == entry }?.key
            ?: entry.toMediaItem()?.url
            ?: return
        removeFromQueueByMediaUrl(mediaUrl)
    }

    override suspend fun removeFromQueueByMediaUrl(mediaUrl: String) {
        val mediaItem = audioPlayer.playlistFlow.value.firstOrNull { it.url == mediaUrl }
        if (mediaItem == null) {
            logger.w { "Attempted to remove non-existent media item: $mediaUrl" }
            return
        }
        logger.d { "Removing media item from queue: ${mediaItem.title}" }
        audioPlayer.removeMediaItem(mediaItem)
        entryByMediaUrl.update { it - mediaUrl }
        persistQueueStateIfReady()
        logger.d { "Media item removed successfully" }
    }

    override suspend fun move(fromIndex: Int, toIndex: Int) {
        logger.d { "Moving queue item from index $fromIndex to $toIndex" }
        audioPlayer.moveMediaItem(fromIndex, toIndex)
        persistQueueStateIfReady()
        logger.d { "Queue item moved successfully" }
    }

    override suspend fun jumpTo(index: Int, autoPlay: Boolean) {
        val queueSize = audioPlayer.playlistFlow.value.size
        if (queueSize == 0) {
            logger.w { "jumpTo called with empty queue" }
            return
        }

        val normalizedIndex = index.coerceIn(0, queueSize - 1)
        logger.d { "Jumping to queue index $index (normalized: $normalizedIndex), autoPlay=$autoPlay" }
        audioPlayer.jumpTo(normalizedIndex)
        if (autoPlay) {
            audioPlayer.play()
        }
        persistQueueStateIfReady()
    }

    override suspend fun reloadCurrent() {
        val current = currentQueueEntryFlow.value ?: run {
            logger.w { "reloadCurrent called with no current entry" }
            return
        }
        val queue = queueFlow.value

        // Using separate current index entry as the current track's source url has changed so it won't match
        val index = queue.indexOfFirst { entry ->
            when (entry) {
                is QueueEntry.StreamingTrack if current is QueueEntry.StreamingTrack ->
                    entry.track.id == current.track.id

                is QueueEntry.LocalTrack if current is QueueEntry.LocalTrack ->
                    entry.url == current.url && entry.name == current.name

                else -> false
            }
        }
        if (index < 0) {
            logger.w { "reloadCurrent: current entry not found in queue" }
            return
        }
        logger.i { "Reloading current track at queue index $index" }
        jumpTo(index, autoPlay = true)
    }

    override suspend fun clear() {
        logger.i { "Clearing queue and collection history" }
        entryByMediaUrl.value = emptyMap()
        currentCollectionEntryState.value = null
        collectionHistoryState.value = emptyList()
        audioPlayer.load(emptyList(), autoPlay = false, startPosition = 0)
        repository.clearState()
        logger.i { "Queue cleared successfully" }
    }

    override suspend fun getQueue(): List<QueueEntry> {
        val knownEntries = entryByMediaUrl.value
        return audioPlayer.playlistFlow.value.map { mediaItem ->
            knownEntries[mediaItem.url] ?: mediaItem.toFallbackQueueEntry()
        }
    }

    override suspend fun getCurrentQueueEntry(): QueueEntry? {
        val mediaItem = audioPlayer.currentMediaItemFlow.value ?: return null
        return entryByMediaUrl.value[mediaItem.url] ?: mediaItem.toFallbackQueueEntry()
    }

    override suspend fun getCurrentCollectionEntry(): QueueCollectionEntry? {
        return currentCollectionEntryState.value
    }

    override suspend fun getCollectionHistory(): List<QueueCollectionEntry> {
        return collectionHistoryState.value
    }

    private var isFetchingRecommendations = false

    private suspend fun handleQueueCompletion() {
        if (isFetchingRecommendations) return

        val settings = settingsProvider.settingsState.value ?: return
        if (!settings.enableEndlessPlayback) return

        val queue = getQueue()
        val streamingTracks = queue.filterIsInstance<QueueEntry.StreamingTrack>()
        if (streamingTracks.isEmpty()) return

        val seedTracks = if (streamingTracks.size <= 5) {
            streamingTracks
        } else {
            val shuffled = streamingTracks.shuffled(Random)
            shuffled.take(5)
        }

        val seedTrackIds = seedTracks.map { it.track.id }
        if (seedTrackIds.isEmpty()) return

        isFetchingRecommendations = true
        logger.i { "Endless playback: fetching recommendations with ${seedTrackIds.size} seed tracks" }

        val metadataService = pluginProvider.selectedMetadataPlugin.value
        var recommendations: List<MetadataTrack> = emptyList()

        if (metadataService != null) {
            recommendations = runCatching {
                metadataService.use {
                    metadataTrackAPI.recommendationsBasedOnTracks(seedTrackIds, limit = 20)
                }
            }.getOrDefault(emptyList())
        }

        if (recommendations.isEmpty()) {
            logger.i { "Endless playback: metadata recommendations unavailable or empty, falling back to native YouTube radio mix" }
            val newPipeService = NewPipeService()
            for (seedTrack in seedTracks) {
                val seed = seedTrack.track
                val videoId = if (seed.id.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
                    seed.id
                } else {
                    seed.externalUri?.let { uri ->
                        Regex("""[?&]v=([a-zA-Z0-9_-]{11})(?:[&#]|$)""").find(uri)?.groupValues?.get(1)
                            ?: Regex("""youtu\.be/([a-zA-Z0-9_-]{11})(?:[?#/]|$)""").find(uri)?.groupValues?.get(1)
                            ?: Regex("""youtube\.com/(?:embed|v|shorts)/([a-zA-Z0-9_-]{11})(?:[?#/]|$)""").find(uri)?.groupValues?.get(1)
                    }
                }

                if (!videoId.isNullOrBlank()) {
                    val radioMix = runCatching { newPipeService.getRadioMix(videoId) }.getOrDefault(emptyList())
                    val existingTrackIds = queue.filterIsInstance<QueueEntry.StreamingTrack>().map { it.track.id }.toSet()
                    val newRadioTracks = radioMix.filter { it.id !in existingTrackIds && it.id != videoId }
                    if (newRadioTracks.isNotEmpty()) {
                        logger.i { "Endless playback: got ${newRadioTracks.size} new tracks from YouTube radio mix for video $videoId" }
                        recommendations = newRadioTracks.map { video ->
                            MetadataTrack(
                                id = video.id,
                                title = video.title,
                                durationMs = video.durationMs,
                                trackNumber = null,
                                discNumber = null,
                                artists = listOf(
                                    MetadataArtist.Basic(
                                        id = video.uploader,
                                        name = video.uploader,
                                        thumbnails = emptyList(),
                                        externalUri = null
                                    )
                                ),
                                album = null,
                                thumbnails = if (video.thumbnailUrl.isNotBlank()) {
                                    listOf(Thumbnail(url = video.thumbnailUrl, width = 300, height = 300))
                                } else null,
                                explicit = null,
                                popularity = null,
                                isrcCode = null,
                                externalUri = "https://www.youtube.com/watch?v=${video.id}"
                            )
                        }
                        break
                    }
                }
            }
        }

        if (recommendations.isEmpty() && metadataService != null) {
            val albumId = seedTracks.mapNotNull { it.track.album?.id }.firstOrNull { it.isNotBlank() }
            if (!albumId.isNullOrBlank()) {
                logger.i { "Endless playback: radio mix unavailable or empty, falling back to album tracks for album $albumId" }
                val albumTracks = runCatching {
                    metadataService.use {
                        metadataAlbumAPI.getAlbumTracks(albumId).items
                    }
                }.getOrDefault(emptyList())

                val existingTrackIds = queue.filterIsInstance<QueueEntry.StreamingTrack>().map { it.track.id }.toSet()
                val newAlbumTracks = albumTracks.filter { it.id !in existingTrackIds }
                if (newAlbumTracks.isNotEmpty()) {
                    logger.i { "Endless playback: got ${newAlbumTracks.size} remaining album tracks for album $albumId" }
                    recommendations = newAlbumTracks
                }
            }
        }

        if (recommendations.isEmpty() && metadataService != null) {
            logger.i { "Endless playback: album tracks unavailable or empty, falling back to artist top tracks" }
            val existingTrackIds = queue.filterIsInstance<QueueEntry.StreamingTrack>().map { it.track.id }.toSet()
            val artistIds = seedTracks.flatMap { it.track.artists }.map { it.id }.filter { it.isNotBlank() }.distinct()
            for (artistId in artistIds) {
                val topTracks = runCatching {
                    metadataService.use {
                        metadataArtistAPI.getArtistTop10Tracks(artistId)
                    }
                }.getOrDefault(emptyList())

                val newTracks = topTracks.filter { it.id !in existingTrackIds }
                if (newTracks.isNotEmpty()) {
                    logger.i { "Endless playback: got ${newTracks.size} top tracks for artist $artistId" }
                    recommendations = newTracks
                    break
                }
            }
        }

        try {
            if (recommendations.isEmpty()) {
                logger.w { "Endless playback: no recommendations, radio mix, album tracks, or artist top tracks found" }
                isFetchingRecommendations = false
                return
            }

            val blacklistedTrackIds = blacklistRepository.blacklistedTracks.first().map { it.id }.toSet()
            val blacklistedArtistIds = blacklistRepository.blacklistedArtists.first().map { it.id }.toSet()

            val filteredRecommendations = recommendations.filter { track ->
                val trackBlacklisted = track.id in blacklistedTrackIds
                val artistBlacklisted = track.artists.any { it.id in blacklistedArtistIds }
                !trackBlacklisted && !artistBlacklisted
            }

            if (filteredRecommendations.isEmpty()) {
                logger.w { "Endless playback: all recommendations were blacklisted" }
                isFetchingRecommendations = false
                return
            }

            val newEntries = filteredRecommendations.map { track ->
                QueueEntry.StreamingTrack(track = track, url = "")
            }

            logger.i { "Endless playback: adding ${newEntries.size} recommended tracks to queue" }
            addAllToQueue(newEntries)
        } catch (e: Exception) {
            logger.e(e) { "Endless playback: failed to add recommendations to queue" }
        } finally {
            isFetchingRecommendations = false
        }
    }

    private suspend fun restorePersistedState() {
        val state = repository.getPersistedState()
        if (state == null) {
            logger.d { "No persisted state found" }
            return
        }

        logger.i { "Restoring persisted state with ${state.entries.size} entries, currentIndex=${state.currentIndex}" }
        currentCollectionEntryState.value = state.currentCollectionEntry
        collectionHistoryState.value = state.collectionHistory.ifEmpty {
            state.currentCollectionEntry?.let(::listOf) ?: emptyList()
        }

        if (state.entries.isEmpty()) {
            logger.d { "Persisted state has no entries" }
            return
        }

        val paired = state.entries.mapNotNull { entry -> entry.toMediaItem()?.let { mediaItem -> mediaItem to entry } }
        if (paired.isEmpty()) {
            logger.w { "restorePersistedState: all entries had blank urls" }
            return
        }
        val mediaItems = paired.map { it.first }
        entryByMediaUrl.value = paired.associate { (mediaItem, entry) -> mediaItem.url to entry }
        val normalizedStartPosition = state.currentIndex.coerceIn(0, mediaItems.lastIndex)
        logger.d { "Loading restored playlist with normalized start position: ${state.currentIndex} -> $normalizedStartPosition" }
        audioPlayer.load(mediaItems, autoPlay = false, startPosition = normalizedStartPosition)
        logger.i { "Persisted state restored successfully" }
    }

    private suspend fun persistQueueStateIfReady() {
        if (isRestoringPersistedState) {
            logger.v { "Skipping persist: still restoring persisted state" }
            return
        }

        val queueEntries = getQueue()
        if (queueEntries.isEmpty()) {
            logger.d { "Persisting queue state: clearing state (queue is empty)" }
            repository.clearState()
            return
        }

        val playlist = audioPlayer.playlistFlow.value
        val currentMedia = audioPlayer.currentMediaItemFlow.value
        val currentIndex = currentMedia?.let { current ->
            playlist.indexOfFirst { it.url == current.url }.takeIf { it >= 0 }
        } ?: 0
        val normalizedCurrentIndex = currentIndex.coerceIn(0, queueEntries.lastIndex)

        logger.d { "Persisting queue state: ${queueEntries.size} entries, currentIndex=$currentIndex (normalized: $normalizedCurrentIndex)" }
        repository.saveState(
            PersistedQueueState(
                entries = queueEntries,
                currentIndex = normalizedCurrentIndex,
                currentCollectionEntry = currentCollectionEntryState.value,
                collectionHistory = collectionHistoryState.value)
        )
    }

    private fun updateCollectionContext(collectionEntry: QueueCollectionEntry?) {
        if (collectionEntry != null) {
            logger.d { "Updating collection context: ${collectionEntry.id}" }
        } else {
            logger.d { "Clearing collection context" }
        }

        currentCollectionEntryState.value = collectionEntry

        if (collectionEntry == null) return

        collectionHistoryState.update { history ->
            val updated = listOf(collectionEntry) + history.filterNot { it == collectionEntry }
            logger.v { "Collection history updated, size now: ${updated.size}" }
            updated
        }
    }

    private suspend fun QueueEntry.toMediaItem(): MediaItem? {
        return when (this) {
            is QueueEntry.StreamingTrack -> {
                val isIos = getPlatform().type == PlatformType.IOS
                val requiresIosResolution = isIos && (
                    url.isBlank() ||
                        url.startsWith("http://127.0.0.1") ||
                        url.startsWith("https://127.0.0.1") ||
                        url.startsWith("http://[::1]") ||
                        url.startsWith("https://[::1]") ||
                        url.startsWith("http://localhost") ||
                        url.startsWith("https://localhost"))
                val resolvedStream = if (requiresIosResolution) {
                    streamingUrlRepository.resolveStreamInfo(track)
                } else {
                    null
                }
                val resolvedUrl = if (requiresIosResolution) {
                    resolvedStream?.url ?: buildStreamingUrl(track.id, protocol)
                } else {
                    url.ifBlank { buildStreamingUrl(track.id, protocol) }
                }
                if (resolvedUrl.isBlank()) return null
                MediaItem(
                    title = track.title,
                    artist = track.artists.joinToString(", ") { artist -> artist.name },
                    album = track.album?.title.orEmpty(),
                    duration = track.durationMs.milliseconds,
                    coverURL = artworkUrl.orEmpty(),
                    url = resolvedUrl,
                    protocol = resolvedStream?.protocol ?: protocol)
            }

            is QueueEntry.LocalTrack -> {
                if (url.isBlank()) return null
                MediaItem(
                    title = name,
                    artist = artists.joinToString(", "),
                    album = album.orEmpty(),
                    duration = duration.milliseconds,
                    coverURL = "",
                    url = url,
                    coverBytes = coverBytes?.takeIf { it.isNotEmpty() })
            }
        }
    }

    private fun MediaItem.toFallbackQueueEntry(): QueueEntry {
        return QueueEntry.LocalTrack(
            name = title,
            artists = artist.split(',').map { it.trim() }.filter { it.isNotEmpty() },
            duration = duration.inWholeMilliseconds,
            album = album.ifBlank { null },
            coverBytes = null,
            url = url)
    }

    private val localServerOrNull: LocalServer?
        get() = runCatching { getKoin().getOrNull<LocalServer>() }.getOrNull()

    private suspend fun buildStreamingUrl(trackId: String, protocol: StreamProtocol): String {
        val baseUrl = localServerOrNull?.awaitBaseUrl() ?: run {
            val port: Int =
                settingsProvider.settingsState.mapNotNull { it?.playbackProxyServerPort }.first()
            "http://127.0.0.1:$port"
        }
        return when (protocol) {
            StreamProtocol.HLS, StreamProtocol.DASH -> "${baseUrl.trimEnd('/')}/manifest/$trackId"
            StreamProtocol.PROGRESSIVE -> "${baseUrl.trimEnd('/')}/stream/$trackId"
        }
    }
}
