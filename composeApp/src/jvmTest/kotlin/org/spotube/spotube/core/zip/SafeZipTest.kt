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

package org.spotube.spotube.core.zip

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SafeZipTest {
    private val fs = FileSystem.SYSTEM
    private val base: Path = "build/tmp/safezip-test".toPath()
    private val sandbox: Path = base / "sandbox"
    private val dest: Path = sandbox / "dest"

    @AfterTest
    fun cleanup() {
        if (fs.exists(base)) fs.deleteRecursively(base)
    }

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            for ((name, data) in entries) {
                z.putNextEntry(ZipEntry(name)); z.write(data); z.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun reasonOf(bytes: ByteArray, limits: ZipLimits = ZipLimits()): UnsafeZipReason =
        assertFailsWith<UnsafeZipException> { extractZipSafely(bytes, dest, fs, limits) }.reason

    /** Nothing may appear anywhere in the sandbox except under `dest`. */
    private fun assertNothingEscaped() {
        val outside = fs.listRecursively(sandbox).filter { !it.toString().startsWith(dest.toString()) && it != dest }.toList()
        assertEquals(emptyList(), outside)
    }

    @Test
    fun extractsAGoodArchiveWithHashes() {
        val content = "hello".encodeToByteArray()
        val seen = mutableListOf<String>()
        val written = extractZipSafely(
            zip("plugin.json" to "{}".encodeToByteArray(), "js/app.js" to content, "dir/" to ByteArray(0)),
            dest, fs, onEntry = { seen += it.path },
        )
        assertEquals(listOf("plugin.json", "js/app.js"), written.map { it.path })
        assertEquals(written.map { it.path }, seen)
        assertEquals("hello", fs.read(dest / "js/app.js") { readUtf8() })
        val expected = MessageDigest.getInstance("SHA-256").digest(content).joinToString("") { "%02x".format(it) }
        assertEquals(expected, written[1].sha256)
    }

    @Test
    fun zipSlipNamesAreRejectedAndNothingEscapes() {
        val evil = listOf(
            "../escaped.txt", "a/../../escaped.txt", "/../escaped.txt", "..", "a/./b.txt",
            "..\\escaped.txt", "a\\..\\..\\escaped.txt", "C:/escaped.txt", "C:\\escaped.txt", "a/b:stream", "a//b.txt", "x\u0000y",
        )
        for (name in evil) {
            assertEquals(UnsafeZipReason.UnsafePath, reasonOf(zip("ok.txt" to ByteArray(1), name to "pwned".encodeToByteArray())), name)
            assertNothingEscaped()
            if (fs.exists(base)) fs.deleteRecursively(base)
        }
    }

    @Test
    fun harmlessOddFormsAreTreatedAsRelativeAndStayInsideDest() {
        assertEquals("escaped.txt", safeZipEntryPath("/escaped.txt"))
        assertEquals("a/b.txt", safeZipEntryPath("./a/b.txt"))
        assertEquals("a/b.txt", safeZipEntryPath("a\\b.txt"))
        val written = extractZipSafely(zip("/abs/plugin.json" to ByteArray(1), "./x.js" to ByteArray(1), "w\\y.js" to ByteArray(1)), dest, fs)
        assertEquals(listOf("abs/plugin.json", "x.js", "w/y.js"), written.map { it.path })
        assertTrue(fs.exists(dest / "abs/plugin.json") && fs.exists(dest / "w/y.js"))
        assertNothingEscaped()
        assertEquals("{}", readZipEntryCapped(zip("/plugin.json" to "{}".encodeToByteArray()), "plugin.json", 10)!!.decodeToString())
    }

    @Test
    fun normalizerAcceptsOrdinaryPluginPaths() {
        for (name in listOf("plugin.json", "manifest.zipline.json", "a/b/c.js", "sub dir/file-1_2.mjs", "dir/", "kotlin-kotlin-stdlib.js")) {
            assertNotNull(safeZipEntryPath(name), name)
        }
        assertNull(safeZipEntryPath("a/".repeat(20) + "f"))
        assertNull(safeZipEntryPath("x".repeat(300)))
        assertNull(safeZipEntryPath(""))
    }

    @Test
    fun zipBombStopsAtTheEntryCap() {
        val bomb = zip("big.bin" to ByteArray(5 * 1024 * 1024))
        assertTrue(bomb.size < 20_000)
        assertEquals(UnsafeZipReason.EntryTooLarge, reasonOf(bomb, ZipLimits(maxEntryBytes = 1024 * 1024)))
        // The partial file must not be left claiming to be a complete extraction.
        assertTrue(!fs.exists(dest / "big.bin") || fs.metadata(dest / "big.bin").size!! <= 1024 * 1024 + 8192)
    }

    @Test
    fun totalSizeAndEntryCountAreCapped() {
        val many = (0 until 5).map { "f$it" to ByteArray(600_000) }.toTypedArray()
        assertEquals(UnsafeZipReason.TotalTooLarge, reasonOf(zip(*many), ZipLimits(maxTotalBytes = 2_000_000)))
        val files = (0 until 20).map { "f$it" to ByteArray(1) }.toTypedArray()
        assertEquals(UnsafeZipReason.TooManyEntries, reasonOf(zip(*files), ZipLimits(maxEntries = 10)))
        assertEquals(UnsafeZipReason.ArchiveTooLarge, reasonOf(ByteArray(2_000), ZipLimits(maxArchiveBytes = 1_000)))
    }

    @Test
    fun duplicatesIncludingCaseVariantsAndGarbageAreRejected() {
        assertEquals(UnsafeZipReason.DuplicateEntry, reasonOf(zip("A.js" to ByteArray(1), "a.js" to ByteArray(1))))
        assertEquals(UnsafeZipReason.Corrupt, reasonOf("PK\u0003\u0004 definitely not a real zip".encodeToByteArray()))
    }

    @Test
    fun fileThenDirectoryCollisionFailsInsteadOfOverwriting() {
        val r = runCatching { extractZipSafely(zip("a" to ByteArray(1), "a/b.txt" to ByteArray(1)), dest, fs) }
        assertTrue(r.isFailure)
        assertNothingEscaped()
    }

    @Test
    fun osJunkIsSkipped() {
        val written = extractZipSafely(zip("plugin.json" to ByteArray(1), "__MACOSX/._plugin.json" to ByteArray(1), ".DS_Store" to ByteArray(1)), dest, fs)
        assertEquals(listOf("plugin.json"), written.map { it.path })
    }

    @Test
    fun readEntryCappedMatchesNormalizedNamesAndStopsAtTheCap() {
        val z = zip("sub/plugin.json" to "x".encodeToByteArray(), "plugin.json" to "{\"a\":1}".encodeToByteArray())
        assertEquals("{\"a\":1}", readZipEntryCapped(z, "plugin.json", 100)!!.decodeToString())
        assertNull(readZipEntryCapped(zip("other" to ByteArray(1)), "plugin.json", 100))
        assertEquals(
            UnsafeZipReason.EntryTooLarge,
            assertFailsWith<UnsafeZipException> { readZipEntryCapped(zip("plugin.json" to ByteArray(5_000_000)), "plugin.json", 1000) }.reason,
        )
    }

    /** Real archives built by the plugin Gradle tooling must keep extracting. */
    @Test
    fun realPluginArchivesStillExtract() {
        val candidates = listOf(
            "../spotube-plugin-spotify/out/plugin.smplug",
            "../spotube-plugin-jiosaavn/spotube_plugin_jiosaavn/build/distributions/plugin-development.smplug",
            "../spotube-plugin-spotify/spotube_plugin_spotify/build/distributions/plugin-development.smplug",
        ).map(::File).filter { it.exists() }
        for (f in candidates) {
            val bytes = f.readBytes()
            val written = extractZipSafely(bytes, dest, fs)
            assertTrue(written.any { it.path == "plugin.json" }, f.name)
            assertTrue(written.any { it.path == "manifest.zipline.json" }, f.name)
            assertNotNull(readZipEntryCapped(bytes, "plugin.json", 1024 * 1024), f.name)
            fs.deleteRecursively(dest)
        }
    }
}
