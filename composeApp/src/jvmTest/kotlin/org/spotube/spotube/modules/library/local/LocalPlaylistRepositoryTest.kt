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

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalPlaylistRepositoryTest {
    private fun repository(): Pair<LocalPlaylistRepository, File> {
        val dir = Files.createTempDirectory("local-playlists-test").toFile()
        val file = File(dir, "local_playlists.preferences_pb")
        return LocalPlaylistRepository(
            PreferenceDataStoreFactory.createWithPath(produceFile = { file.absolutePath.toPath() })) to file
    }

    private suspend fun LocalPlaylistRepository.await(): List<LocalPlaylist> = playlistsFlow.first()

    @Test
    fun startsEmpty() = runBlocking {
        val (repository, _) = repository()
        assertTrue(repository.await().isEmpty())
    }

    @Test
    fun createPersistsAndRoundTrips() = runBlocking {
        val (repository, _) = repository()
        val created = repository.create("Roadtrip", "Summer 2026", coverBase64 = "aGk=")

        val stored = repository.await().single()
        assertEquals(created.id, stored.id)
        assertEquals("Roadtrip", stored.name)
        assertEquals("Summer 2026", stored.description)
        assertEquals("aGk=", stored.coverBase64)
        assertTrue(created.id.startsWith(LOCAL_PLAYLIST_ID_PREFIX))
        assertTrue(isLocalPlaylistId(created.id))
    }

    @Test
    fun addTracksAppendsWithoutDuplicatesAndKeepsOrder() = runBlocking {
        val (repository, _) = repository()
        val created = repository.create("Mix")
        repository.addTracks(created.id, listOf("a", "b"))
        repository.addTracks(created.id, listOf("b", "c"))

        assertEquals(listOf("a", "b", "c"), repository.await().single().trackPaths)
    }

    @Test
    fun removeTrackAndMoveTrackUpdateTheOrder() = runBlocking {
        val (repository, _) = repository()
        val created = repository.create("Mix", trackPaths = listOf("a", "b", "c"))
        repository.moveTrack(created.id, fromIndex = 2, toIndex = 0)
        assertEquals(listOf("c", "a", "b"), repository.await().single().trackPaths)

        repository.removeTrack(created.id, "a")
        assertEquals(listOf("c", "b"), repository.await().single().trackPaths)
    }

    @Test
    fun movingAFileUpdatesEveryPlaylistWithoutDuplicatingAliases() = runBlocking {
        val (repository, _) = repository()
        repository.create("First", trackPaths = listOf("a", "media-uri", "b", "document-uri"))
        repository.create("Second", trackPaths = listOf("old-path", "c"))
        repository.remapMovedTracks(listOf("media-uri", "document-uri", "old-path").associateWith { "new-path" })

        assertEquals(listOf(listOf("a", "new-path", "b"), listOf("new-path", "c")),
            repository.await().map { it.trackPaths })
    }

    @Test
    fun renameKeepsCoverWhenCoverArgumentIsNullAndRemovesItWhenBlank() = runBlocking {
        val (repository, _) = repository()
        val created = repository.create("Mix", coverBase64 = "aGk=")

        repository.rename(created.id, "Renamed", " desc ", null)
        val renamed = repository.await().single()
        assertEquals("Renamed", renamed.name)
        assertEquals("desc", renamed.description)
        assertEquals("aGk=", renamed.coverBase64)

        repository.rename(created.id, "Renamed", null, "")
        assertNull(repository.await().single().coverBase64)
    }

    @Test
    fun deleteRemovesOnlyTheTargetPlaylist() = runBlocking {
        val (repository, _) = repository()
        val one = repository.create("One")
        val two = repository.create("Two")
        repository.delete(one.id)

        assertEquals(listOf(two.id), repository.await().map { it.id })
    }

    @Test
    fun mutationsOnMissingIdsAreNoOps() = runBlocking {
        val (repository, _) = repository()
        repository.addTracks("local:none", listOf("a"))
        repository.rename("local:none", "x", null, null)
        repository.removeTrack("local:none", "a")
        assertTrue(repository.await().isEmpty())
    }

    @Test
    fun corruptedStoreFallsBackToEmpty() = runBlocking {
        // Corrupt the file before the store ever reads it, like a fresh process would find it.
        val dir = Files.createTempDirectory("local-playlists-test").toFile()
        val file = File(dir, "local_playlists.preferences_pb")
        file.writeText("not protobuf")
        val repository = LocalPlaylistRepository(
            PreferenceDataStoreFactory.createWithPath(produceFile = { file.absolutePath.toPath() }))

        assertTrue(repository.await().isEmpty())
    }
}
