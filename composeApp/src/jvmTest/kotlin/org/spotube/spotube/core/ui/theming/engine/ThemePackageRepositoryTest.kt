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

package org.spotube.spotube.core.ui.theming.engine

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Install, update, reload, tamper and remove: the whole theme store lifecycle on a temp folder. */
class ThemePackageRepositoryTest {
    private val fs = FileSystem.SYSTEM
    private val base = "build/tmp/theme-repo-test".toPath()

    @AfterTest
    fun cleanup() {
        if (fs.exists(base)) fs.deleteRecursively(base)
    }

    private fun repo() = ThemePackageRepository(dataDir = "$base/data", cacheDir = "$base/cache").also {
        runBlocking { withTimeout(10_000) { it.ready.first { ready -> ready } } }
    }

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z -> entries.forEach { (n, d) -> z.putNextEntry(ZipEntry(n)); z.write(d); z.closeEntry() } }
        return out.toByteArray()
    }

    private fun theme(id: String, name: String = id, version: String? = "1.0.0", author: String? = "Me") = zip(
        "theme.json" to buildString {
            append("""{"id":"$id","name":"$name"""")
            version?.let { append(""","version":"$it"""") }
            author?.let { append(""","author":"$it"""") }
            append(""","repository":"https://example.com/$id","license":"MIT"}""")
        }.encodeToByteArray(),
        "LICENSE" to "MIT License text".encodeToByteArray(),
    )

    @Test
    fun installsMultipleThemesAndReloadsThemFromDisk() = runBlocking {
        val repo = repo()
        assertEquals(emptyList(), repo.installed.value)
        repo.install(repo.prepare(theme("zeta", "Zeta")))
        repo.install(repo.prepare(theme("alpha", "Alpha")))
        assertEquals(listOf("Alpha", "Zeta"), repo.installed.value.map { it.name })
        assertEquals("MIT License text", repo.installed.value.first().licenseText)

        val reloaded = repo()
        assertEquals(listOf("alpha", "zeta"), reloaded.installed.value.map { it.id })
        assertEquals("https://example.com/alpha", reloaded.installed.value.first().meta.repository)
    }

    @Test
    fun reinstallingTheSameIdReplacesAndReportsTheOldVersion() = runBlocking {
        val repo = repo()
        repo.install(repo.prepare(theme("t", version = "1.0.0")))
        val prepared = repo.prepare(theme("t", version = "2.0.0"))
        assertEquals("1.0.0", prepared.replaces?.meta?.version)
        repo.install(prepared)
        assertEquals(listOf("2.0.0"), repo.installed.value.map { it.meta.version })
    }

    @Test
    fun removeDeletesTheFolderAndTheTheme() = runBlocking {
        val repo = repo()
        repo.install(repo.prepare(theme("gone")))
        repo.remove("gone")
        assertEquals(emptyList(), repo.installed.value)
        assertTrue(!fs.exists("$base/data/themes/gone".toPath()))
        assertEquals(emptyList(), repo().installed.value)
    }

    @Test
    fun removeRefusesPathLikeIds() = runBlocking {
        val repo = repo()
        val sentinel = "$base/data/keep.txt".toPath()
        fs.createDirectories(sentinel.parent!!); fs.write(sentinel) { writeUtf8("x") }
        repo.remove("../")
        repo.remove("..")
        assertTrue(fs.exists(sentinel))
    }

    @Test
    fun prepareRequiresNameVersionAndAuthor() = runBlocking {
        val repo = repo()
        for ((bytes, field) in listOf(
            theme("a", version = null) to "version",
            theme("b", author = null) to "author",
            zip("theme.json" to """{"id":"c","version":"1","author":"a"}""".encodeToByteArray()) to "name",
        )) {
            val e = assertFailsWith<ThemePackageException> { repo.prepare(bytes) }
            assertEquals(ThemePackageError.MissingMetadata, e.error)
            assertEquals(field, e.detail)
        }
    }

    @Test
    fun tamperedOrMismatchedFilesOnDiskAreSkippedNotTrusted() = runBlocking {
        val repo = repo()
        repo.install(repo.prepare(theme("good")))
        repo.install(repo.prepare(theme("victim")))
        // A corrupt archive, and a valid theme dropped into another theme's folder.
        fs.write("$base/data/themes/victim/theme.zip".toPath()) { write(ByteArray(64) { 7 }) }
        fs.createDirectories("$base/data/themes/liar".toPath())
        fs.write("$base/data/themes/liar/theme.zip".toPath()) { write(theme("good")) }
        val reloaded = repo()
        assertEquals(listOf("good"), reloaded.installed.value.map { it.id })
    }

    @Test
    fun hostileArchivesNeverReachTheDisk() = runBlocking {
        val repo = repo()
        val evil = zip("theme.json" to """{"id":"x","name":"x","version":"1","author":"a"}""".encodeToByteArray(), "../../escape.png" to ByteArray(10))
        assertFailsWith<ThemePackageException> { repo.prepare(evil) }
        assertNotNull(repo.installed.value)
        assertTrue(!fs.exists("$base/data/themes".toPath()) || fs.list("$base/data/themes".toPath()).isEmpty())
    }
}
