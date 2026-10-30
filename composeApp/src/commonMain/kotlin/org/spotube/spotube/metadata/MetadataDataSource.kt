package org.spotube.spotube.metadata

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.Serializable

/**
 * Strategy for selecting metadata providers.
 */
enum class MetadataSourceStrategy {
    PRIMARY_ONLY,           // Use only primary; fail if unavailable
    PRIMARY_WITH_FALLBACK,  // Try primary, then fallback sequentially (default)
    FASTEST_FIRST           // Try all, use first response
}

/**
 * Core metadata model interfaces.
 * These should align with the plugin API models.
 */
@Serializable
data class SpotubeSearchResponse(
    val albums: List<SpotubeSimpleAlbum> = emptyList(),
    val artists: List<SpotubeFullArtist> = emptyList(),
    val playlists: List<SpotubePlaylist> = emptyList(),
    val tracks: List<SpotubeFullTrack> = emptyList()
)

@Serializable
data class SpotubePaginationResponse<T>(
    val items: List<T> = emptyList(),
    val total: Long = 0,
    val limit: Int = 20,
    val hasMore: Boolean = false,
    val nextOffset: Int? = null
)

@Serializable
data class SpotubeFullArtist(
    val id: String,
    val name: String,
    val externalUri: String = "",
    val images: List<SpotubeImage> = emptyList()
)

@Serializable
data class SpotubeSimpleArtist(
    val id: String,
    val name: String,
    val externalUri: String = ""
)

@Serializable
data class SpotubeSimpleAlbum(
    val id: String,
    val name: String,
    val externalUri: String = "",
    val artists: List<SpotubeSimpleArtist> = emptyList(),
    val albumType: SpotubeAlbumType = SpotubeAlbumType.ALBUM,
    val images: List<SpotubeImage> = emptyList(),
    val releaseDate: String = ""
)

@Serializable
data class SpotubeFullTrack(
    val id: String,
    val name: String,
    val artists: List<SpotubeSimpleArtist> = emptyList(),
    val album: SpotubeSimpleAlbum = SpotubeSimpleAlbum(id = "", name = ""),
    val durationMs: Long = 0,
    val isrc: String = "",
    val explicit: Boolean = false,
    val externalUri: String = ""
)

@Serializable
data class SpotubeImage(
    val url: String,
    val width: Int = 0,
    val height: Int = 0
)

@Serializable
data class SpotubePlaylist(
    val id: String,
    val name: String,
    val description: String = "",
    val externalUri: String = "",
    val owner: SpotubeSimpleArtist? = null,
    val images: List<SpotubeImage> = emptyList(),
    val collaborative: Boolean = false,
    val public: Boolean = false
)

enum class SpotubeAlbumType {
    ALBUM, SINGLE, COMPILATION, EP
}

/**
 * Contract that all metadata providers must implement.
 * Enables pluggable architecture with automatic fallback.
 */
@ExperimentalCoroutinesApi
interface MetadataDataSource {

    /** Human-readable name for logging/debugging. */
    val name: String

    /** Whether this provider is currently available. */
    val isAvailable: Boolean

    /** Recommended strategy for this provider. */
    val strategy: MetadataSourceStrategy = MetadataSourceStrategy.PRIMARY_WITH_FALLBACK

    // Search operations
    suspend fun search(query: String): SpotubeSearchResponse

    // User library operations (paginated)
    suspend fun savedArtists(offset: Int?, limit: Int?): SpotubePaginationResponse<SpotubeFullArtist>
    suspend fun savedAlbums(offset: Int?, limit: Int?): SpotubePaginationResponse<SpotubeSimpleAlbum>
    suspend fun savedTracks(offset: Int?, limit: Int?): SpotubePaginationResponse<SpotubeFullTrack>

    // Favorite status checks
    suspend fun isSavedTrack(trackId: String): Boolean
    suspend fun isSavedAlbum(albumId: String): Boolean
    suspend fun isSavedArtist(artistId: String): Boolean
    suspend fun isSavedPlaylist(playlistId: String): Boolean

    // Favorite mutations (try each source until one succeeds)
    suspend fun saveArtists(artistIds: List<String>)
    suspend fun unsaveArtists(artistIds: List<String>)
    suspend fun saveAlbums(albumIds: List<String>)
    suspend fun unsaveAlbums(albumIds: List<String>)
    suspend fun saveTracks(trackIds: List<String>)
    suspend fun unsaveTracks(trackIds: List<String>)
    suspend fun savePlaylists(playlistIds: List<String>)
    suspend fun unsavePlaylists(playlistIds: List<String>)
}

/**
 * Exception thrown when all metadata providers fail.
 */
class MetadataRepositoryException(
    message: String,
    val errors: List<Throwable> = emptyList()
) : Exception(message)