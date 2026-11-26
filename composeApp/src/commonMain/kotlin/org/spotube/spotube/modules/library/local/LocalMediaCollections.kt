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

package org.spotube.spotube.modules.library.local

import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaTrack

/**
 * An album synthesized from the local media index: every indexed track whose lofty `album`
 * tag matches (case-insensitive, trimmed) belongs to the same group. The cover is the first
 * track in alphabetical order that has embedded art.
 */
data class LocalMediaAlbum(
    /** Normalized album name (trimmed + lowercase); also used as its navigation id. */
    val key: String,
    /** The name as it first appeared in a track's metadata. */
    val name: String,
    /** Album tracks, alphabetically ordered by track name. */
    val tracks: List<LocalMediaTrack>,
    /** Cover of the first (alphabetically) track with embedded art, if any. */
    val coverBytes: ByteArray?) {

    val trackCount: Int get() = tracks.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocalMediaAlbum) return false
        return key == other.key && name == other.name && tracks == other.tracks
    }

    override fun hashCode(): Int = 31 * key.hashCode() + tracks.hashCode()
}

/**
 * An artist synthesized from the local media index: one group per distinct artist name found
 * in any track's `artists` tags (a track with several artists belongs to each of them).
 */
data class LocalMediaArtist(
    /** Normalized artist name (trimmed + lowercase); also used as its navigation id. */
    val key: String,
    /** The name as it first appeared in a track's metadata. */
    val name: String,
    /** Every track the artist appears on, alphabetically ordered by track name. */
    val tracks: List<LocalMediaTrack>,
    /** Cover of the first (alphabetically) track with embedded art, if any. */
    val coverBytes: ByteArray?) {

    val trackCount: Int get() = tracks.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocalMediaArtist) return false
        return key == other.key && name == other.name && tracks == other.tracks
    }

    override fun hashCode(): Int = 31 * key.hashCode() + tracks.hashCode()
}

private fun normalizeGroupKey(name: String): String = name.trim().lowercase()

/**
 * Groups [tracks] into albums by their album tag. Tracks without a usable album name are
 * skipped. The cover of an album is the art of whichever track comes first alphabetically
 * among the ones that have embedded art (most albums have the same art everywhere, this just
 * makes the pick deterministic when they don't).
 */
fun buildLocalAlbums(tracks: List<LocalMediaTrack>): List<LocalMediaAlbum> {
    val byName = LinkedHashMap<String, Pair<String, MutableList<LocalMediaTrack>>>()
    tracks.forEach { track ->
        val displayName = track.album?.trim()?.takeIf { it.isNotBlank() } ?: return@forEach
        val key = normalizeGroupKey(displayName)
        byName.getOrPut(key) { displayName to mutableListOf() }.second.add(track)
    }
    return byName.map { (key, pair) ->
        val (displayName, groupTracks) = pair
        val sorted = groupTracks.sortedBy { it.name.trim().lowercase() }
        LocalMediaAlbum(
            key = key,
            name = displayName,
            tracks = sorted,
            coverBytes = sorted.firstNotNullOfOrNull { it.coverBytes?.takeIf { c -> c.isNotEmpty() } })
    }.sortedBy { it.name.lowercase() }
}

/**
 * Groups [tracks] into artists: one group per distinct artist name, tracks appearing under
 * every artist they credit. Artist covers follow the same rule as album covers: the art of the
 * first track alphabetically that has one.
 */
fun buildLocalArtists(tracks: List<LocalMediaTrack>): List<LocalMediaArtist> {
    val byName = LinkedHashMap<String, Pair<String, MutableList<LocalMediaTrack>>>()
    tracks.forEach { track ->
        val artists = track.artists.mapNotNull { it.trim().takeIf { name -> name.isNotBlank() } }
        if (artists.isEmpty()) return@forEach
        val distinct = artists.distinctBy { normalizeGroupKey(it) }
        distinct.forEach { artistName ->
            val key = normalizeGroupKey(artistName)
            byName.getOrPut(key) { artistName to mutableListOf() }.second.add(track)
        }
    }
    return byName.map { (key, pair) ->
        val (displayName, groupTracks) = pair
        val sorted = groupTracks.distinctBy { it.path }
            .sortedBy { it.name.trim().lowercase() }
        LocalMediaArtist(
            key = key,
            name = displayName,
            tracks = sorted,
            coverBytes = sorted.firstNotNullOfOrNull { it.coverBytes?.takeIf { c -> c.isNotEmpty() } })
    }.sortedBy { it.name.lowercase() }
}
