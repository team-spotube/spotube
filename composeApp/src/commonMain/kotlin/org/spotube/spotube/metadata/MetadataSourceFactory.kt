package org.spotube.spotube.metadata

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.spotube.spotube.resilience.ResiliencePipeline
import org.spotube.spotube.resilience.ResiliencePipelineConfig

/**
 * MusicBrainz/ListenBrainz data source wrapping the Hetu plugin.
 * Uses resilience pipeline for all API calls.
 *
 * This is the commonMain interface - actual implementation is in androidMain
 * where the plugin is available via Hetu runtime.
 */
@ExperimentalCoroutinesApi
interface MusicBrainzDataSource : MetadataDataSource {
    val resiliencePipeline: ResiliencePipeline
}

/**
 * YouTube Audio data source for fallback search.
 * Converts YouTube video search results to metadata objects.
 *
 * Based on DuranMusic's YouTubeAudioDataSource in `youtube_audio_data_source.dart`.
 */
@ExperimentalCoroutinesApi
interface YouTubeAudioDataSource : MetadataDataSource {
    suspend fun searchVideos(query: String): List<YouTubeVideo>
}

data class YouTubeVideo(
    val id: String,
    val title: String,
    val author: String,
    val durationMs: Long?,
    val viewCount: Long?,
    val likeCount: Long?,
    val isLive: Boolean,
    val uploadDate: String?,
    val description: String?
)

/**
 * Factory for creating metadata sources with proper configuration.
 */
@ExperimentalCoroutinesApi
object MetadataSourceFactory {

    /**
     * Creates the default fallback chain:
     * 1. MusicBrainz/ListenBrainz (primary)
     * 2. YouTube Audio (fallback for search)
     */
    fun createDefaultSources(
        musicBrainzSource: MusicBrainzDataSource,
        youTubeAudioSource: YouTubeAudioDataSource
    ): List<MetadataDataSource> {
        return listOf(musicBrainzSource, youTubeAudioSource)
    }

    /**
     * Creates a repository with the default fallback chain.
     */
    fun createDefaultRepository(
        musicBrainzSource: MusicBrainzDataSource,
        youTubeAudioSource: YouTubeAudioDataSource
    ): MetadataRepository {
        return MetadataRepository(
            sources = createDefaultSources(musicBrainzSource, youTubeAudioSource),
            strategy = MetadataSourceStrategy.PRIMARY_WITH_FALLBACK
        )
    }
}