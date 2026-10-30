package org.spotube.spotube.metadata

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking

/**
 * Android implementation of YouTubeAudioDataSource.
 * Uses the existing YouTube engine to search videos and convert to metadata.
 *
 * Based on DuranMusic's YouTubeAudioDataSource in `youtube_audio_data_source.dart`.
 */
@ExperimentalCoroutinesApi
class YouTubeAudioDataSourceImpl(
    private val youtubeEngine: Any // Actual type: YouTubeEngine from spotube
) : YouTubeAudioDataSource {

    override val name = "YouTube Audio"
    override val isAvailable = true
    override val strategy = MetadataSourceStrategy.PRIMARY_WITH_FALLBACK

    override suspend fun search(query: String): SpotubeSearchResponse {
        if (query.isBlank()) {
            return SpotubeSearchResponse()
        }

        // Search YouTube videos
        val videos = searchVideos(query)

        // Convert to metadata objects
        val artists = videos.map { video ->
            SpotubeFullArtist(
                id = video.id,
                name = video.author,
                externalUri = "https://www.youtube.com/channel/${video.id}",
                images = emptyList()
            )
        }.distinctBy { it.name }.toList()

        val tracks = videos.map { video ->
            SpotubeFullTrack(
                id = video.id,
                name = video.title,
                artists = listOf(SpotubeSimpleArtist(
                    id = video.id,
                    name = video.author,
                    externalUri = "https://www.youtube.com/channel/${video.id}"
                )),
                album = SpotubeSimpleAlbum(
                    id = video.id,
                    name = video.title,
                    externalUri = "https://www.youtube.com/watch?v=${video.id}",
                    artists = listOf(SpotubeSimpleArtist(
                        id = video.id,
                        name = video.author,
                        externalUri = "https://www.youtube.com/channel/${video.id}"
                    )),
                    albumType = SpotubeAlbumType.SINGLE,
                    images = emptyList()
                ),
                durationMs = video.durationMs ?: 0,
                isrc = "",
                explicit = false,
                externalUri = "https://www.youtube.com/watch?v=${video.id}"
            )
        }.toList()

        return SpotubeSearchResponse(
            albums = emptyList(),
            artists = artists,
            playlists = emptyList(),
            tracks = tracks
        )
    }

    override suspend fun searchVideos(query: String): List<YouTubeVideo> {
        // TODO: Implement actual YouTube engine call
        // Example: youtubeEngine.searchVideos(query)
        return emptyList()
    }

    // User library operations - YouTube Audio doesn't have these natively
    override suspend fun savedArtists(offset: Int?, limit: Int?): SpotubePaginationResponse<SpotubeFullArtist> {
        return SpotubePaginationResponse()
    }

    override suspend fun savedAlbums(offset: Int?, limit: Int?): SpotubePaginationResponse<SpotubeSimpleAlbum> {
        return SpotubePaginationResponse()
    }

    override suspend fun savedTracks(offset: Int?, limit: Int?): SpotubePaginationResponse<SpotubeFullTrack> {
        return SpotubePaginationResponse()
    }

    override suspend fun isSavedTrack(trackId: String): Boolean = false
    override suspend fun isSavedAlbum(albumId: String): Boolean = false
    override suspend fun isSavedArtist(artistId: String): Boolean = false
    override suspend fun isSavedPlaylist(playlistId: String): Boolean = false

    // Mutations - YouTube Audio doesn't support these natively
    override suspend fun saveArtists(artistIds: List<String>) {}
    override suspend fun unsaveArtists(artistIds: List<String>) {}
    override suspend fun saveAlbums(albumIds: List<String>) {}
    override suspend fun unsaveAlbums(albumIds: List<String>) {}
    override suspend fun saveTracks(trackIds: List<String>) {}
    override suspend fun unsaveTracks(trackIds: List<String>) {}
    override suspend fun savePlaylists(playlistIds: List<String>) {}
    override suspend fun unsavePlaylists(playlistIds: List<String>) {}
}