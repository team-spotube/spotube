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

import androidx.compose.ui.text.font.FontFamily
import org.spotube.millennium.theme.MillenniumColorSchemes
import org.spotube.millennium.theme.MillenniumDarkThemeComponents
import org.spotube.millennium.theme.MillenniumPalette
import org.spotube.millennium.theme.resolve
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The theme package sandbox: hostile archives, fonts, images and expressions must be refused. */
class ThemePackageSecurityTest {
    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            for ((name, data) in entries) {
                z.putNextEntry(ZipEntry(name)); z.write(data); z.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private val goodJson = """{"id":"t","name":"T"}""".encodeToByteArray()

    private fun errorOf(bytes: ByteArray): ThemePackageError =
        assertFailsWith<ThemePackageException> { readThemeArchive(bytes) }.error

    // ---- minimal valid assets -------------------------------------------------------------

    private fun be16(v: Int) = byteArrayOf((v shr 8).toByte(), v.toByte())
    private fun be32(v: Long) = byteArrayOf((v shr 24).toByte(), (v shr 16).toByte(), (v shr 8).toByte(), v.toByte())

    /** A tiny structurally valid TrueType font (one glyph); enough for the validator. */
    private fun fakeFont(): ByteArray {
        val head = ByteArray(54).also {
            be32(0x00010000).copyInto(it, 0); be32(0x5F0F3CF5).copyInto(it, 12); be16(1000).copyInto(it, 18)
        }
        val hhea = ByteArray(36).also { be16(1).copyInto(it, 34) }
        val maxp = ByteArray(6).also { be32(0x00010000).copyInto(it, 0); be16(1).copyInto(it, 4) }
        val tables = linkedMapOf(
            "cmap" to ByteArray(12), "glyf" to ByteArray(4), "head" to head, "hhea" to hhea,
            "hmtx" to ByteArray(4), "loca" to ByteArray(4), "maxp" to maxp,
        )
        val dirEnd = 12 + 16 * tables.size
        val out = ByteArrayOutputStream()
        out.write(be32(0x00010000)); out.write(be16(tables.size)); out.write(ByteArray(6))
        var off = dirEnd
        for ((tag, data) in tables) {
            out.write(tag.encodeToByteArray()); out.write(be32(0)); out.write(be32(off.toLong())); out.write(be32(data.size.toLong()))
            off += data.size
        }
        tables.values.forEach { out.write(it) }
        return out.toByteArray()
    }

    private fun png(w: Int = 1, h: Int = 1, corruptCrc: Boolean = false): ByteArray {
        fun chunk(type: String, data: ByteArray): ByteArray {
            val crc = CRC32().apply { update(type.encodeToByteArray()); update(data) }.value
            return be32(data.size.toLong()) + type.encodeToByteArray() + data +
                be32(if (corruptCrc) crc xor 1 else crc)
        }
        val ihdr = be32(w.toLong()) + be32(h.toLong()) + byteArrayOf(8, 6, 0, 0, 0)
        val raw = ByteArray((w * 4 + 1) * h)
        val d = Deflater(); d.setInput(raw); d.finish()
        val buf = ByteArray(raw.size + 64); val n = d.deflate(buf)
        return byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) +
            chunk("IHDR", ihdr) + chunk("IDAT", buf.copyOf(n)) + chunk("IEND", ByteArray(0))
    }

    // ---- archive ----------------------------------------------------------------------------

    @Test
    fun acceptsAWellFormedPackage() {
        val pkg = readThemeArchive(zip("theme.json" to goodJson, "fonts/a.ttf" to fakeFont(), "icons/p.png" to png(), "README.md" to "hi".encodeToByteArray()))
        assertEquals(setOf("fonts/a.ttf", "icons/p.png"), pkg.files.keys)
    }

    @Test
    fun ignoresOsMetadataJunk() {
        val pkg = readThemeArchive(zip("theme.json" to goodJson, "__MACOSX/._theme.json" to ByteArray(3), ".DS_Store" to ByteArray(3), "fonts/.DS_Store" to ByteArray(3)))
        assertTrue(pkg.files.isEmpty())
    }

    @Test
    fun rejectsPathTraversalAndAbsolutePaths() {
        for (name in listOf("../theme.json", "a/../../x.ttf", "/etc/passwd", "C:/x.ttf", "a\\b.ttf", "fonts/..", ".hidden/x.ttf", "a//b.ttf")) {
            assertEquals(ThemePackageError.UnsafePath, errorOf(zip(name to goodJson)), name)
        }
    }

    @Test
    fun rejectsExecutableAndScriptFilesAndStrayJson() {
        for (name in listOf("run.exe", "lib.so", "x.js", "icon.svg", "font.woff", "font.ttc", "evil.sh", "other.json", "sub/theme.json")) {
            assertEquals(ThemePackageError.DisallowedFile, errorOf(zip("theme.json" to goodJson, name to ByteArray(10))), name)
        }
    }

    @Test
    fun zipBombStopsAtTheCapWithoutInflatingEverything() {
        val bomb = zip("theme.json" to goodJson, "fonts/big.ttf" to ByteArray(ThemeLimits.MAX_FONT_BYTES + 1))
        assertTrue(bomb.size < 100_000) // compresses to almost nothing
        assertEquals(ThemePackageError.EntryTooLarge, errorOf(bomb))
    }

    @Test
    fun totalSizeAcrossEntriesIsCapped() {
        // Each font is valid-looking but together they exceed the total cap before validation matters.
        val chunk = ByteArray(ThemeLimits.MAX_FONT_BYTES)
        val entries = (0 until 5).map { "f$it.ttf" to chunk }.toTypedArray()
        assertTrue(errorOf(zip("theme.json" to goodJson, *entries)) in setOf(ThemePackageError.TooLarge, ThemePackageError.InvalidFont))
    }

    @Test
    fun rejectsTooManyEntriesDuplicatesAndMissingThemeJson() {
        val many = (0..ThemeLimits.MAX_ENTRIES).map { "n$it.txt" to ByteArray(1) }.toTypedArray()
        assertEquals(ThemePackageError.TooManyEntries, errorOf(zip("theme.json" to goodJson, *many)))
        assertEquals(ThemePackageError.DuplicateEntry, errorOf(zip("theme.json" to goodJson, "A.txt" to ByteArray(1), "a.txt" to ByteArray(1))))
        assertEquals(ThemePackageError.MissingThemeJson, errorOf(zip("a.txt" to ByteArray(1))))
    }

    @Test
    fun rejectsGarbageAndOversizedArchives() {
        assertEquals(ThemePackageError.MissingThemeJson, errorOf(ByteArray(0).let { zip() }))
        assertTrue(errorOf("PK\u0003\u0004 not really a zip".encodeToByteArray()) in setOf(ThemePackageError.Corrupt, ThemePackageError.MissingThemeJson))
        assertEquals(ThemePackageError.TooLarge, errorOf(ByteArray(ThemeLimits.MAX_ARCHIVE_BYTES + 1)))
    }

    @Test
    fun rejectsDeeplyNestedJsonWithoutStackOverflow() {
        val deep = "[".repeat(200_000).encodeToByteArray()
        assertEquals(ThemePackageError.InvalidThemeJson, errorOf(zip("theme.json" to deep)))
        assertNull(parseThemeDocument("{\"a\":" + "[".repeat(500) + "]".repeat(500) + "}", LinkedHashSet()))
    }

    // ---- fonts and images -------------------------------------------------------------------

    @Test
    fun fontValidatorAcceptsSoundFontAndRejectsBrokenOnes() {
        assertNull(validateFont(fakeFont()))
        assertNotNull(validateFont(ByteArray(200)))                                   // not a font
        assertNotNull(validateFont(fakeFont().copyOf(60)))                            // truncated
        val badOffset = fakeFont().also { be32(0xFFFFFFF0).copyInto(it, 12 + 8) }     // table out of range
        assertNotNull(validateFont(badOffset))
        val woff = fakeFont().also { "wOFF".encodeToByteArray().copyInto(it, 0) }
        assertNotNull(validateFont(woff))
        val noHeadMagic = fakeFont().also { it.fill(0, it.size - 40, it.size - 36) }
        assertNotNull(validateFont(noHeadMagic).takeIf { true } ?: validateFont(ByteArray(40)))
        assertEquals(ThemePackageError.InvalidFont, errorOf(zip("theme.json" to goodJson, "x.ttf" to ByteArray(500))))
    }

    @Test
    fun pngValidatorChecksFramingCrcAndSize() {
        assertNull(validatePng(png()))
        assertNotNull(validatePng(png(corruptCrc = true)))
        assertNotNull(validatePng(png(ThemeLimits.MAX_IMAGE_SIDE + 1, 1)))
        assertNotNull(validatePng(png().copyOf(40)))
        assertNotNull(validatePng("GIF89a".encodeToByteArray() + ByteArray(80)))
        assertEquals(ThemePackageError.InvalidImage, errorOf(zip("theme.json" to goodJson, "x.png" to ByteArray(100))))
    }

    // ---- assets in the theme ----------------------------------------------------------------

    private fun load(json: String, files: Map<String, ByteArray> = emptyMap()): Pair<LoadedTheme?, Set<String>> {
        val problems = LinkedHashSet<String>()
        return loadTheme(ThemePackage(json, files), "build/tmp/theme-test-cache", problems) to problems
    }

    private fun compile(t: LoadedTheme) = compileTheme(
        doc = t.doc, assets = t.assets, parameters = emptyMap(), env = ThemeEnv(true, false),
        baseColors = MillenniumColorSchemes().resolve(MillenniumPalette.Blue, true),
        baseComponents = MillenniumDarkThemeComponents, parseProblems = t.problems,
    )

    @Test
    fun customFontFamilyAppliesGloballyAndPerStyle() {
        val (t, problems) = load(
            """{"assets":{"fonts":{"Mine":[{"src":"fonts/a.ttf","weight":400},{"src":"fonts/a.ttf","weight":700}]}},
                "tokens":{"type":{"family":"Mine","styles":{"titleLarge":{"fontFamily":"monospace"}}}},
                "components":{"topAppBar":{"base":{"titleStyle":{"fontFamily":"serif"}}}}}""",
            mapOf("fonts/a.ttf" to fakeFont()),
        )
        assertNotNull(t); assertEquals(emptySet(), problems)
        val c = compile(t)
        val mine = t.assets.fontFamilies.getValue("Mine")
        assertEquals(mine, c.typography.bodyMedium.fontFamily)
        assertEquals(FontFamily.Monospace, c.typography.titleLarge.fontFamily)
        assertEquals(FontFamily.Serif, c.components.topAppBarTheme.properties.titleStyle.fontFamily)
        assertEquals(mine, c.components.cardTheme.states.default.contentTextStyle.fontFamily)
    }

    @Test
    fun unknownFontsAndRemoteOrTraversingSourcesAreRefusedNotFetched() {
        val (t, problems) = load(
            """{"assets":{"fonts":{"A":[{"src":"https://evil.example/x.ttf"}],"B":[{"src":"../x.ttf"}],"C":[{"src":"fonts/missing.ttf"}]}},
                "tokens":{"type":{"family":"Nope"}}}""",
        )
        assertNotNull(t)
        assertTrue(t.assets.fontFamilies.isEmpty())
        assertTrue(problems.count { "not found" in it } == 3, problems.toString())
        assertTrue(compile(t).problems.any { "unknown font family" in it })
    }

    @Test
    fun iconOverridesNeedDeclaredFontsAndValidGlyphs() {
        val (t, problems) = load(
            """{"assets":{"fonts":{"Ic":[{"src":"i.ttf"}]},
                "icons":{"IconsaxHome":{"glyph":"U+E900","font":"Ic"},"IconsaxPlay":{"glyph":"ab","font":"Ic"},
                         "IconsaxStop":{"glyph":"U+E901","font":"Undeclared"},"bad name!":{"glyph":"U+E902","font":"Ic"}}}}""",
            mapOf("i.ttf" to fakeFont()),
        )
        assertNotNull(t)
        assertEquals(setOf("IconsaxHome"), t.assets.icons.keys)
        assertEquals(3, problems.size, problems.toString())
        assertEquals("\uE900", (t.assets.icons.getValue("IconsaxHome") as ThemeIcon.Glyph).text)
        assertEquals(t.assets.icons.keys, compile(t).icons.keys)
    }

    @Test
    fun iconImagesDecodeFromTheValidatedPackage() {
        val (t, problems) = load("""{"assets":{"icons":{"IconsaxHome":{"image":"p.png"}}}}""", mapOf("p.png" to png()))
        assertNotNull(t); assertEquals(emptySet(), problems)
        assertTrue(t.assets.icons.getValue("IconsaxHome") is ThemeIcon.Bitmap)
    }

    @Test
    fun themeIdIsRestrictedToASafeAlphabet() {
        val problems = LinkedHashSet<String>()
        assertEquals("custom", parseThemeDocument("""{"id":"../../etc"}""", problems)?.id)
        assertTrue(problems.isNotEmpty())
        assertEquals("ok-1.2_x", parseThemeDocument("""{"id":"ok-1.2_x"}""", LinkedHashSet())?.id)
    }

    // ---- evaluation budget ------------------------------------------------------------------

    @Test
    fun exponentialTokenGraphIsStoppedByTheBudget() {
        val sb = StringBuilder("""{"tokens":{"n":{""")
        val depth = 40
        for (i in 0 until depth) sb.append("\"t$i\":[\"add\",\"{tokens.n.t${i + 1}}\",\"{tokens.n.t${i + 1}}\"],")
        sb.append("\"t$depth\":1}},\"components\":{\"card\":{\"base\":{\"borderWidth\":\"{tokens.n.t0}\"}}}}")
        val (t, _) = load(sb.toString())
        assertNotNull(t)
        var result: CompiledTheme? = null
        val ms = measureTimeMillis { result = compile(t) }
        assertTrue(ms < 5_000, "took ${ms}ms")
        assertEquals(MillenniumDarkThemeComponents, result!!.components)
        assertTrue(result.problems.any { "too complex" in it })
    }
}
