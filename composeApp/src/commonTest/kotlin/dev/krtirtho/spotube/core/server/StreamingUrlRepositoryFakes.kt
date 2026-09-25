package dev.krtirtho.spotube.core.server

import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioFormat
import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioSource
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerQueue
import dev.krtirtho.spotube.core.audioplayer.QueueCollectionEntry
import dev.krtirtho.spotube.core.audioplayer.QueueEntry
import dev.krtirtho.spotube.core.zipline.PluginService
import dev.krtirtho.spotube.core.zipline.PluginServiceScope
import dev.krtirtho.spotube.modules.plugin.AudioPluginSource
import dev.krtirtho.spotube.modules.settings.UserSettings
import dev.krtirtho.spotube.modules.settings.UserSettingsSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory [AudioAPI] whose behaviour for each call is fully scripted by the test. */
class FakeAudioAPI(
    override val supportedQualities: List<AudioFormat> = emptyList(),
    var onGetStreamsByTrack: suspend (MetadataTrack) -> List<AudioSource> = { emptyList() },
    var onGetStreamsOfAudioSource: suspend (AudioSource.Basic) -> List<AudioSource.Streamed> = { emptyList() },
) : AudioAPI {
    var getStreamsByTrackCallCount = 0
    var getStreamsOfAudioSourceCallCount = 0
    val getStreamsOfAudioSourceCalls = mutableListOf<AudioSource.Basic>()

    override suspend fun getStreamsByTrack(track: MetadataTrack): List<AudioSource> {
        getStreamsByTrackCallCount++
        return onGetStreamsByTrack(track)
    }

    override suspend fun getStreamsOfAudioSource(source: AudioSource.Basic): List<AudioSource.Streamed> {
        getStreamsOfAudioSourceCallCount++
        getStreamsOfAudioSourceCalls.add(source)
        return onGetStreamsOfAudioSource(source)
    }
}

/** Minimal [PluginService] that only ever exposes an [AudioAPI], identified by [pluginId]. */
class FakeAudioPluginService(
    override val pluginId: String,
    val audioAPI: FakeAudioAPI = FakeAudioAPI(),
) : PluginService {
    override val loggedInFlow: StateFlow<Boolean> = MutableStateFlow(true)

    override suspend fun start() = Unit
    override suspend fun stop() = Unit

    override suspend fun <T> use(block: suspend PluginServiceScope.() -> T): T {
        val scope = PluginServiceScope(mapOf(AudioAPI::class to audioAPI))
        return scope.block()
    }
}

class FakeAudioPluginSource(
    initialPlugin: PluginService? = null
) : AudioPluginSource {
    private val _selectedAudioPlugin = MutableStateFlow(initialPlugin)
    override val selectedAudioPlugin: StateFlow<PluginService?> = _selectedAudioPlugin

    fun setPlugin(service: PluginService?) {
        _selectedAudioPlugin.value = service
    }
}

/** In-memory [TrackSourceRepository], mirroring the plugin-scoped persistence contract of
 * the real [MatchedTracksRepository] without touching disk. */
class FakeTrackSourceRepository : TrackSourceRepository {
    private val store = mutableMapOf<String, PluginTaggedAudioSource>()
    var getCallCount = 0
    var saveCallCount = 0

    override suspend fun getTrackSource(track: MetadataTrack, pluginId: String): AudioSource.Basic? {
        getCallCount++
        val tagged = store[track.id] ?: return null
        if (tagged.pluginId != pluginId) return null
        return tagged.source
    }

    override suspend fun saveTrackSource(track: MetadataTrack, source: AudioSource.Basic, pluginId: String) {
        saveCallCount++
        store[track.id] = PluginTaggedAudioSource(pluginId, source)
    }

    fun rawEntryFor(trackId: String): PluginTaggedAudioSource? = store[trackId]

    fun seed(track: MetadataTrack, source: AudioSource.Basic, pluginId: String) {
        store[track.id] = PluginTaggedAudioSource(pluginId, source)
    }
}

class FakeUserSettingsSource(initial: UserSettings = UserSettings()) : UserSettingsSource {
    private val _userSettings = MutableStateFlow(initial)
    override val userSettings: StateFlow<UserSettings> = _userSettings

    fun update(transform: UserSettings.() -> UserSettings) {
        _userSettings.value = _userSettings.value.transform()
    }
}

/** Bare-bones [AudioPlayerQueue] that only backs [queueFlow]; every other member is unused
 * by [StreamingUrlRepository] and throws if accidentally exercised. */
class FakeStreamingAudioPlayerQueue : AudioPlayerQueue {
    private val _queueFlow = MutableStateFlow<List<QueueEntry>>(emptyList())
    override val queueFlow: StateFlow<List<QueueEntry>> = _queueFlow
    override val currentQueueEntryFlow: StateFlow<QueueEntry?> = MutableStateFlow(null)
    override val currentCollectionEntryFlow: StateFlow<QueueCollectionEntry?> = MutableStateFlow(null)
    override val collectionHistoryFlow: StateFlow<List<QueueCollectionEntry>> = MutableStateFlow(emptyList())

    fun setQueue(entries: List<QueueEntry>) {
        _queueFlow.value = entries
    }

    override suspend fun load(
        entries: List<QueueEntry>,
        autoPlay: Boolean,
        startPosition: Int,
        collectionEntry: QueueCollectionEntry?,
    ) = error("not used")

    override suspend fun addToQueue(entry: QueueEntry) = error("not used")
    override suspend fun addAllToQueue(entries: List<QueueEntry>, collectionEntry: QueueCollectionEntry?) =
        error("not used")

    override suspend fun addAllAfterCurrent(entries: List<QueueEntry>) = error("not used")
    override suspend fun removeFromQueue(entry: QueueEntry) = error("not used")
    override suspend fun removeFromQueueByMediaUrl(mediaUrl: String) = error("not used")
    override suspend fun move(fromIndex: Int, toIndex: Int) = error("not used")
    override suspend fun jumpTo(index: Int, autoPlay: Boolean) = error("not used")
    override suspend fun reloadCurrent() = error("not used")
    override suspend fun clear() = error("not used")
    override suspend fun getQueue(): List<QueueEntry> = _queueFlow.value
    override suspend fun getCurrentQueueEntry(): QueueEntry? = null
    override suspend fun getCurrentCollectionEntry(): QueueCollectionEntry? = null
    override suspend fun getCollectionHistory(): List<QueueCollectionEntry> = emptyList()
}
