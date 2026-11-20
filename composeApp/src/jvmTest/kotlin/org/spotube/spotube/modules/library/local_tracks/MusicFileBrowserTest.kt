/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.local_tracks

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.test.runTest
import org.spotube.spotube.modules.library.local_tracks.media.*
import java.nio.file.Files
import kotlin.io.path.createDirectory
import kotlin.io.path.writeText
import kotlin.test.*
import uniffi.compose_app.AudioTagException
import uniffi.compose_app.readAudioMetadata

class MusicFileBrowserTest {
    @Test
    fun `editing saves native tags and retires a legacy sidecar only after success`() = runTest {
        val root = Files.createTempDirectory("spotube-edit").toFile()
        try {
            val location = MusicLocation(PlatformFile(root.path))
            val audio = root.resolve("track.wav").apply {
                writeBytes(byteArrayOf(82, 73, 70, 70, 38, 0, 0, 0, 87, 65, 86, 69, 102, 109, 116, 32,
                    16, 0, 0, 0, 1, 0, 1, 0, 64, 31, 0, 0, -128, 62, 0, 0, 2, 0, 16, 0,
                    100, 97, 116, 97, 2, 0, 0, 0, 0, 0))
            }
            val sidecar = root.resolve(".track.wav.spotube-tags.json").apply { writeText("""{"title":"Old shadow title"}""") }
            val track = MusicFile(location, PlatformFile(audio.path), LocalTrackTags("Old shadow title"))
            val edits = LocalTrackTags("Saved in the audio file", "Artist", "Album", track = 2, disc = 1)
            track.saveEmbeddedTags(edits)
            val embedded = readAudioMetadata(audio.path)
            assertEquals(edits.title, embedded.title)
            assertEquals(listOf("Artist"), embedded.artists)
            assertEquals("Album", embedded.album)
            assertEquals(2u, embedded.trackNumber)
            assertEquals(1u, embedded.discNumber)
            assertFalse(sidecar.exists())

            val unsupported = root.resolve("unknown.wma").apply { writeText("unsupported sample") }
            val originalSidecar = root.resolve(".unknown.wma.spotube-tags.json").apply { writeText("""{"title":"Original"}""") }
            val failedTrack = MusicFile(location, PlatformFile(unsupported.path), LocalTrackTags("Original"))
            assertFailsWith<AudioTagException.UnsupportedFormat> { failedTrack.saveEmbeddedTags(edits) }
            assertEquals("""{"title":"Original"}""", originalSidecar.readText())
            assertEquals("unsupported sample", unsupported.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `new folder rejects path traversal and does not replace existing entries`() = runTest {
        val temp = Files.createTempDirectory("spotube-folder-test").toFile()
        try {
            val location = MusicLocation(PlatformFile(temp.path))
            assertFailsWith<IllegalArgumentException> { location.createFolder("../escape") }
            assertFailsWith<IllegalArgumentException> { location.createFolder("parent/child") }
            assertTrue(location.createFolder("New album").isDirectory())
            assertFailsWith<MusicDestinationExists> { location.createFolder("New album") }
            assertEquals(listOf("New album"), location.listDestinationFolders().map { it.name })
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test
    fun `bulk actions continue after conflicts and permission failures`() = runTest {
        val batch = runFileBatch(listOf("conflict", "denied", "ok")) { item ->
            when (item) {
                "conflict" -> throw MusicDestinationExists()
                "denied" -> throw SecurityException()
            }
        }
        assertEquals(listOf("ok"), batch.succeeded)
        assertEquals(listOf("conflict"), batch.conflicts)
        assertEquals(listOf("denied"), batch.errors)
        assertEquals(FileBatchReport(1, 1, 1), batch.report)
    }

    @Test
    fun `listing keeps only branches with music even when the track is several levels deep`() = runTest {
        val root = Files.createTempDirectory("spotube-browser").toFile()
        try {
            val child = root.toPath().resolve("Albums").createDirectory()
            val deepest = child.resolve("Artist").createDirectory().resolve("Release").createDirectory()
            val empty = root.toPath().resolve("Other").createDirectory()
            empty.resolve("Nested").createDirectory().resolve("notes.txt").writeText("not audio")
            child.resolve("Empty").createDirectory()
            root.resolve("notes.txt").writeText("not audio")
            deepest.resolve("music.webm").writeText("sample placeholder")
            deepest.resolve(".music.webm.spotube-tags.json").writeText("""{"title":"Tagged track"}""")
            val location = MusicLocation(PlatformFile(root.path))
            val folders = scanMusicTree(location.root)
            val index = buildMusicBrowserIndex(LocalMediaCache(folders), listOf(BrowserFolder(root.path, "Root") to location))
            val indexedRoot = index.listings.getValue(null).folders.single().second
            val album = index.listings.getValue(indexedRoot).folders.single()
            assertEquals("Albums", album.first.name)
            val artist = index.listings.getValue(album.second).folders.single()
            assertEquals("Artist", artist.first.name)
            val release = index.listings.getValue(artist.second).folders.single()
            assertEquals("Tagged track", index.listings.getValue(release.second).tracks.single().tags.title)
            assertEquals(listOf("Albums", "Other"), location.listDestinationFolders().map { it.name })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `metadata follows copies and moves and duplicate destination is untouched`() = runTest {
        val root = Files.createTempDirectory("spotube-browser").toFile()
        try {
            val source = root.resolve("source").apply { mkdir() }
            val dest = root.resolve("destination").apply { mkdir() }
            val name = "music.webm"
            source.resolve(name).writeText("original")
            source.resolve(".$name.spotube-tags.json").writeText("""{"title":"Tagged track"}""")
            val sourceFolder = PlatformFile(source.path)
            val track = MusicLocation(sourceFolder).listDirectMusic().second.single()
            val details = track.details()
            assertEquals(name, details.fileName)
            assertEquals(source.resolve(name).path, details.path)
            assertEquals(8L, details.bytes)
            assertEquals(true, details.access.read)
            val destination = MusicLocation(PlatformFile(dest.path))
            track.copyTo(destination, move = false)
            assertEquals("Tagged track", destination.listDirectMusic().second.single().tags.title)
            assertFailsWith<MusicDestinationExists> { track.copyTo(destination, move = true) }
            assertEquals("original", dest.resolve(name).readText())
            assertTrue(source.resolve(name).exists())
            destination.listDirectMusic().second.single().delete()
            track.copyTo(destination, move = true)
            assertFalse(source.resolve(name).exists())
            assertFalse(source.resolve(".$name.spotube-tags.json").exists())
            assertEquals("Tagged track", destination.listDirectMusic().second.single().tags.title)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `indexed Android tracks keep their playback URI and project into folders without touching disk`() {
        val physical = "/storage/emulated/0"
        val root = MusicLocation(PlatformFile(physical))
        val uri = "content://media/external/audio/media/9"
        val rawPath = "$physical/Music/Release/test.m4a"
        val index = buildMusicBrowserIndex(
            LocalMediaCache(listOf(LocalMediaFolder("Music/Release/", "Release",
                listOf(LocalMediaTrack(uri, "Track", listOf("Artist"), 1500, browserPath = rawPath))))),
            listOf(BrowserFolder(physical, "Phone") to root)
        )
        val music = index.listings.getValue(root).folders.single().second
        val release = index.listings.getValue(music).folders.single().second
        val track = index.listings.getValue(release).tracks.single()
        assertEquals(rawPath, track.path)
        assertEquals(uri, track.playbackPath)
    }
}
