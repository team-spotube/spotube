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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalMediaCollectionsTest {
    private fun track(
        path: String,
        name: String = path,
        artists: List<String> = emptyList(),
        album: String? = null,
        coverBytes: ByteArray? = null) = LocalMediaTrack(
        path = path, name = name, artists = artists,
        durationMs = 0, album = album, coverBytes = coverBytes)

    @Test
    fun tracksAreGroupedByAlbumNameCaseInsensitively() {
        val albums = buildLocalAlbums(
            listOf(
                track("a", name = "Alpha", album = "Greatest Hits"),
                track("b", name = "Beta", album = "greatest hits"),
                track("c", name = "Gamma", album = " Greatest Hits "),
                track("d", name = "Delta", album = null)))
        assertEquals(1, albums.size)
        assertEquals("greatest hits", albums[0].key)
        assertEquals("Greatest Hits", albums[0].name)
        // The first casing seen wins; unknown albums are skipped.
        assertEquals(listOf("a", "b", "c"), albums[0].tracks.map { it.path })
    }

    @Test
    fun albumCoverComesFromTheFirstAlphabeticalTrackThatHasArt() {
        val albums = buildLocalAlbums(
            listOf(
                track("z", name = "zeta", album = "Album", coverBytes = byteArrayOf(2)),
                track("m", name = "Mike", album = "Album", coverBytes = byteArrayOf(1)),
                track("a", name = "alpha", album = "Album", coverBytes = null)))
        assertEquals(3, albums[0].tracks.size)
        // Alphabetical order first (alpha, Mike, zeta), then the first with a cover.
        assertTrue(byteArrayOf(1).contentEquals(albums[0].coverBytes!!))
    }

    @Test
    fun albumsAreSortedByName() {
        val albums = buildLocalAlbums(
            listOf(
                track("1", album = "Zeta"),
                track("2", album = "Alpha"),
                track("3", album = "middle")))
        assertEquals(listOf("Alpha", "middle", "Zeta"), albums.map { it.name })
    }

    @Test
    fun artistGroupsCoverEveryCreditedArtistWithoutDuplicates() {
        val artists = buildLocalArtists(
            listOf(
                track("a", name = "Song", artists = listOf("A", "B")),
                track("b", name = "Duet", artists = listOf("B", "a")),
                track("c", name = "Solo", artists = listOf("B"))))
        assertEquals(listOf("A", "B"), artists.map { it.name })
        // Casing differences merge into one group; a track appears under every credited artist.
        // Group tracks are alphabetical by name: Duet, Song.
        assertEquals(listOf("b", "a"), artists[0].tracks.map { it.path })
        // Duet, Solo, Song.
        assertEquals(listOf("b", "c", "a"), artists[1].tracks.map { it.path })
    }

    @Test
    fun tracksWithoutArtistsAreSkippedWhenGroupingArtists() {
        val artists = buildLocalArtists(
            listOf(track("a", artists = emptyList()), track("b", artists = listOf(""))))
        assertTrue(artists.isEmpty())
    }

    @Test
    fun artistCoverFollowsTheSameAlphabeticalRule() {
        val artists = buildLocalArtists(
            listOf(
                track("z", name = "Zeta", artists = listOf("X"), coverBytes = byteArrayOf(9)),
                track("a", name = "alpha", artists = listOf("X"))))
        assertTrue(byteArrayOf(9).contentEquals(artists[0].coverBytes!!))
    }

    @Test
    fun blankAlbumAndArtistNamesAreIgnored() {
        assertTrue(buildLocalAlbums(listOf(track("a", album = "  "))).isEmpty())
        assertTrue(buildLocalArtists(listOf(track("a", artists = listOf("  ")))).isEmpty())
    }

    @Test
    fun emptyCoversAreTreatedAsMissing() {
        val albums = buildLocalAlbums(
            listOf(track("a", name = "a", album = "Album", coverBytes = ByteArray(0))))
        assertNull(albums[0].coverBytes)
    }
}
