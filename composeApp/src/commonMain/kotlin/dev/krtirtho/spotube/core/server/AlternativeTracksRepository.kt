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

package dev.krtirtho.spotube.core.server

import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioSource
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerQueue
import dev.krtirtho.spotube.core.di.injectLogger
import dev.krtirtho.spotube.modules.plugin.AudioPluginSource
import org.koin.core.component.KoinComponent

class AlternativeTracksRepository(
    private val pluginManager: AudioPluginSource,
    private val matchedTracksRepository: TrackSourceRepository,
    private val streamingUrlRepository: StreamingUrlRepository,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val cacheManager: CacheManager,
) : KoinComponent {

    private val logger by injectLogger<AlternativeTracksRepository>()

    suspend fun resolveAlternatives(track: MetadataTrack): List<AudioSource> {
        val trackId = track.id

        val audioPlugin = pluginManager.selectedAudioPlugin.value
        if (audioPlugin == null) {
            logger.w { "No audio plugin selected while resolving alternatives for track $trackId" }
            return emptyList()
        }

        streamingUrlRepository.getCachedAlternatives(trackId, audioPlugin.pluginId)?.let { cached ->
            logger.v { "Using cached alternatives for track $trackId" }
            return cached
        }

        val sources = runCatching {
            audioPlugin.use { audioAPI.getStreamsByTrack(track) }
        }.getOrElse { throwable ->
            logger.w(throwable) { "Failed to fetch alternative sources for track $trackId" }
            return emptyList()
        }

        if (sources.isEmpty()) {
            logger.d { "Plugin returned no alternative sources for track $trackId" }
            return emptyList()
        }

        streamingUrlRepository.cacheAlternatives(trackId, sources, audioPlugin.pluginId)
        logger.d { "Resolved ${sources.size} alternative sources for track $trackId" }
        return sources
    }

    suspend fun getActiveSourceId(track: MetadataTrack): String? {
        val audioPlugin = pluginManager.selectedAudioPlugin.value ?: return null
        return matchedTracksRepository.getTrackSource(track, audioPlugin.pluginId)?.id
    }

    suspend fun selectAlternative(track: MetadataTrack, source: AudioSource) {
        val trackId = track.id
        val audioPlugin = pluginManager.selectedAudioPlugin.value
        if (audioPlugin == null) {
            logger.w { "No audio plugin selected while selecting alternative for track $trackId" }
            return
        }
        val basic = when (source) {
            is AudioSource.Streamed -> source.toBasic()
            is AudioSource.Basic -> source
        }

        logger.i { "Selecting alternative source for track $trackId: ${basic.id} (${basic.title})" }
        matchedTracksRepository.saveTrackSource(track, basic, audioPlugin.pluginId)
        streamingUrlRepository.invalidateCachedStreamUrl(trackId)
        streamingUrlRepository.invalidateCachedAlternatives(trackId)
        cacheManager.invalidateCacheEntry(trackId)
        audioPlayerQueue.reloadCurrent()
        logger.d { "Alternative source selection complete for track $trackId" }
    }
}
