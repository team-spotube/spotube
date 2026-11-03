package org.spotube.spotube.core.audioplayer

import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.db.Database
import org.spotube.spotube.core.paths.Paths
import org.spotube.spotube.modules.blacklist.BlacklistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeBlacklistRepository : BlacklistRepository(
    database = Database(Paths())
) {
    private val _blacklistedTracks = MutableStateFlow<List<MetadataTrack>>(emptyList())
    override val blacklistedTracks: Flow<List<MetadataTrack>> = _blacklistedTracks.asStateFlow()

    private val _blacklistedArtists = MutableStateFlow<List<MetadataArtist>>(emptyList())
    override val blacklistedArtists: Flow<List<MetadataArtist>> = _blacklistedArtists.asStateFlow()

    override suspend fun toggleTrack(track: MetadataTrack) {
        val current = _blacklistedTracks.value.toMutableList()
        val idx = current.indexOfFirst { it.id == track.id }
        if (idx >= 0) current.removeAt(idx) else current.add(track)
        _blacklistedTracks.value = current
    }

    override suspend fun toggleArtist(artist: MetadataArtist) {
        val current = _blacklistedArtists.value.toMutableList()
        val idx = current.indexOfFirst { it.id == artist.id }
        if (idx >= 0) current.removeAt(idx) else current.add(artist)
        _blacklistedArtists.value = current
    }
}
