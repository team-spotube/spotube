package org.spotube.spotube.metadata

import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Multi-source metadata repository with automatic fallback.
 *
 * Tries providers in order based on configured strategy.
 * For read operations: first success wins.
 * For write operations: tries each until one succeeds.
 *
 * Based on DuranMusic's MetadataRepository in `metadata_data_source.dart`.
 */
@ExperimentalCoroutinesApi
class MetadataRepository(
    private val sources: List<MetadataDataSource>,
    private val strategy: MetadataSourceStrategy = MetadataSourceStrategy.PRIMARY_WITH_FALLBACK
) : MetadataDataSource by DelegatingMetadataDataSource(sources) {

    override val name: String = sources.joinToString(" + ") { it.name }
    override val isAvailable: Boolean = sources.any { it.isAvailable }
    override val strategy: MetadataSourceStrategy = strategy

    // Read operations with fallback chain
    override suspend fun search(query: String): SpotubeSearchResponse =
        withFallback { it.search(query) }

    override suspend fun savedArtists(offset: Int?, limit: Int?): SpotubePaginationResponse<SpotubeFullArtist> =
        withFallback { it.savedArtists(offset, limit) }

    override suspend fun savedAlbums(offset: Int?, limit: Int?): SpotubePaginationResponse<SpotubeSimpleAlbum> =
        withFallback { it.savedAlbums(offset, limit) }

    override suspend fun savedTracks(offset: Int?, limit: Int?): SpotubePaginationResponse<SpotubeFullTrack> =
        withFallback { it.savedTracks(offset, limit) }

    override suspend fun isSavedTrack(trackId: String): Boolean {
        for (source in sources) {
            if (!source.isAvailable) continue
            try {
                return source.isSavedTrack(trackId)
            } catch (_: Exception) {
                continue // Try next source
            }
        }
        return false // Conservative: assume not saved if we can't verify
    }

    override suspend fun isSavedAlbum(albumId: String): Boolean {
        for (source in sources) {
            if (!source.isAvailable) continue
            try {
                return source.isSavedAlbum(albumId)
            } catch (_: Exception) {
                continue
            }
        }
        return false
    }

    override suspend fun isSavedArtist(artistId: String): Boolean {
        for (source in sources) {
            if (!source.isAvailable) continue
            try {
                return source.isSavedArtist(artistId)
            } catch (_: Exception) {
                continue
            }
        }
        return false
    }

    override suspend fun isSavedPlaylist(playlistId: String): Boolean {
        for (source in sources) {
            if (!source.isAvailable) continue
            try {
                return source.isSavedPlaylist(playlistId)
            } catch (_: Exception) {
                continue
            }
        }
        return false
    }

    // Write operations: try each source until one succeeds
    override suspend fun saveArtists(artistIds: List<String>) =
        writeWithFallback { it.saveArtists(artistIds) }

    override suspend fun unsaveArtists(artistIds: List<String>) =
        writeWithFallback { it.unsaveArtists(artistIds) }

    override suspend fun saveAlbums(albumIds: List<String>) =
        writeWithFallback { it.saveAlbums(albumIds) }

    override suspend fun unsaveAlbums(albumIds: List<String>) =
        writeWithFallback { it.unsaveAlbums(albumIds) }

    override suspend fun saveTracks(trackIds: List<String>) =
        writeWithFallback { it.saveTracks(trackIds) }

    override suspend fun unsaveTracks(trackIds: List<String>) =
        writeWithFallback { it.unsaveTracks(trackIds) }

    override suspend fun savePlaylists(playlistIds: List<String>) =
        writeWithFallback { it.savePlaylists(playlistIds) }

    override suspend fun unsavePlaylists(playlistIds: List<String>) =
        writeWithFallback { it.unsavePlaylists(playlistIds) }

    // Generic fallback for read operations
    private suspend fun <T> withFallback(operation: (MetadataDataSource) -> T): T {
        val errors = mutableListOf<Throwable>()
        for (source in sources) {
            if (!source.isAvailable) continue
            try {
                return operation(source)
            } catch (e: Exception) {
                errors.add(e)
                // Silent log: repository decides fallback
            }
        }
        throw MetadataRepositoryException("All metadata providers failed", errors)
    }

    // Generic fallback for write operations
    private suspend fun writeWithFallback(operation: (MetadataDataSource) -> Unit) {
        var lastError: Throwable? = null
        for (source in sources) {
            if (!source.isAvailable) continue
            try {
                operation(source)
                return // Success
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw MetadataRepositoryException(
            "All metadata providers failed on write operation",
            lastError?.let { listOf(it) } ?: emptyList()
        )
    }
}

/**
 * Delegating implementation for read-only properties.
 */
private class DelegatingMetadataDataSource(private val sources: List<MetadataDataSource>) : MetadataDataSource {
    override val name: String = sources.joinToString(" + ") { it.name }
    override val isAvailable: Boolean = sources.any { it.isAvailable }
    override val strategy: MetadataSourceStrategy = sources.firstOrNull()?.strategy ?: MetadataSourceStrategy.PRIMARY_WITH_FALLBACK

    override suspend fun search(query: String) = TODO()
    override suspend fun savedArtists(offset: Int?, limit: Int?) = TODO()
    override suspend fun savedAlbums(offset: Int?, limit: Int?) = TODO()
    override suspend fun savedTracks(offset: Int?, limit: Int?) = TODO()
    override suspend fun isSavedTrack(trackId: String) = TODO()
    override suspend fun isSavedAlbum(albumId: String) = TODO()
    override suspend fun isSavedArtist(artistId: String) = TODO()
    override suspend fun isSavedPlaylist(playlistId: String) = TODO()
    override suspend fun saveArtists(artistIds: List<String>) = TODO()
    override suspend fun unsaveArtists(artistIds: List<String>) = TODO()
    override suspend fun saveAlbums(albumIds: List<String>) = TODO()
    override suspend fun unsaveAlbums(albumIds: List<String>) = TODO()
    override suspend fun saveTracks(trackIds: List<String>) = TODO()
    override suspend fun unsaveTracks(trackIds: List<String>) = TODO()
    override suspend fun savePlaylists(playlistIds: List<String>) = TODO()
    override suspend fun unsavePlaylists(playlistIds: List<String>) = TODO()
}