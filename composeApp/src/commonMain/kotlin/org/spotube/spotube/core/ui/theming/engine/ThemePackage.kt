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

import no.synth.kmpzip.okio.ZipInputStream
import no.synth.kmpzip.zip.ZipInputStream as ZipStream
import okio.Buffer

/**
 * Hard limits for theme packages. A theme is untrusted input (anyone can share one), so every
 * dimension an attacker controls is capped *before* the data is interpreted.
 */
object ThemeLimits {
    const val MAX_ARCHIVE_BYTES = 16 * 1024 * 1024
    const val MAX_ENTRIES = 128
    const val MAX_TOTAL_UNCOMPRESSED = 32L * 1024 * 1024
    const val MAX_JSON_BYTES = 512 * 1024
    const val MAX_FONT_BYTES = 8 * 1024 * 1024
    const val MAX_IMAGE_BYTES = 256 * 1024
    const val MAX_TEXT_BYTES = 64 * 1024
    const val MAX_NAME_LENGTH = 120
    const val MAX_PATH_DEPTH = 4
    const val MAX_JSON_NESTING = 64
    const val MAX_IMAGE_SIDE = 512
    const val MAX_FONT_FILES = 16
    const val MAX_ICON_IMAGES = 128

    /** Upper bound on expression evaluations per compile; stops exponential `{ref}` blow-ups. */
    const val MAX_EVAL_STEPS = 500_000
}

enum class ThemePackageError {
    TooLarge, Corrupt, TooManyEntries, UnsafePath, DisallowedFile, DuplicateEntry,
    EntryTooLarge, Encrypted, MissingThemeJson, InvalidThemeJson, InvalidFont, InvalidImage, MissingMetadata,
}

class ThemePackageException(val error: ThemePackageError, val detail: String? = null) :
    Exception("${error.name}${detail?.let { ": $it" } ?: ""}")

/** A validated theme: its `theme.json` text plus validated asset files keyed by normalized path. */
class ThemePackage(val json: String, val files: Map<String, ByteArray>, val licenseText: String? = null)

private val IGNORED_TEXT_NAMES = setOf("license", "licence", "readme", "notice")
private val LICENSE_NAMES = setOf("license", "licence", "license.txt", "license.md", "licence.txt", "licence.md")

private fun isLicenseName(name: String) = name.lowercase() in LICENSE_NAMES

/**
 * Reads a theme `.zip` entirely in memory and validates it.
 *
 * Nothing is ever extracted to disk under an entry name, so path traversal ("zip slip") has no
 * target. Sizes are counted while *reading*, never trusted from the headers, so a zip bomb stops
 * at the cap. Only `theme.json` (root), `.ttf`/`.otf` fonts, `.png` images and inert text files
 * are accepted; anything else rejects the whole archive.
 */
fun readThemeArchive(bytes: ByteArray): ThemePackage {
    if (bytes.size > ThemeLimits.MAX_ARCHIVE_BYTES) throw ThemePackageException(ThemePackageError.TooLarge)

    val files = LinkedHashMap<String, ByteArray>()
    val seen = HashSet<String>()
    var json: String? = null
    var licenseText: String? = null
    var entries = 0
    var total = 0L
    var fonts = 0
    var images = 0

    try {
        val zip = ZipInputStream(Buffer().apply { write(bytes) })
        while (true) {
            val entry = zip.nextEntry ?: break
            if (++entries > ThemeLimits.MAX_ENTRIES) throw ThemePackageException(ThemePackageError.TooManyEntries)
            if (isOsJunk(entry.name)) continue // Finder/Explorer metadata; never read or stored
            val name = normalizeEntryName(entry.name)
                ?: throw ThemePackageException(ThemePackageError.UnsafePath, entry.name.take(60))
            if (!seen.add(name.lowercase())) throw ThemePackageException(ThemePackageError.DuplicateEntry, name)
            if (entry.isDirectory) continue

            val kind = classify(name) ?: throw ThemePackageException(ThemePackageError.DisallowedFile, name)
            val cap = when (kind) {
                EntryKind.Json -> ThemeLimits.MAX_JSON_BYTES
                EntryKind.Font -> ThemeLimits.MAX_FONT_BYTES
                EntryKind.Image -> ThemeLimits.MAX_IMAGE_BYTES
                EntryKind.Text -> ThemeLimits.MAX_TEXT_BYTES
            }
            val data = readCapped(zip, cap, name)
            total += data.size
            if (total > ThemeLimits.MAX_TOTAL_UNCOMPRESSED) throw ThemePackageException(ThemePackageError.TooLarge)

            when (kind) {
                EntryKind.Json -> json = decodeThemeJson(data)
                EntryKind.Font -> {
                    if (++fonts > ThemeLimits.MAX_FONT_FILES) throw ThemePackageException(ThemePackageError.TooManyEntries, "fonts")
                    validateFont(data)?.let { throw ThemePackageException(ThemePackageError.InvalidFont, "$name: $it") }
                    files[name] = data
                }
                EntryKind.Image -> {
                    if (++images > ThemeLimits.MAX_ICON_IMAGES) throw ThemePackageException(ThemePackageError.TooManyEntries, "images")
                    validatePng(data)?.let { throw ThemePackageException(ThemePackageError.InvalidImage, "$name: $it") }
                    files[name] = data
                }
                EntryKind.Text -> if (isLicenseName(name)) licenseText = data.decodeToString().take(ThemeLimits.MAX_TEXT_BYTES) // other text is read to honour the caps, then discarded
            }
        }
    } catch (e: ThemePackageException) {
        throw e
    } catch (e: Exception) {
        // Truncated or malformed zip, unsupported compression, encryption, ...
        throw ThemePackageException(ThemePackageError.Corrupt, e.message?.take(80))
    }
    return ThemePackage(json ?: throw ThemePackageException(ThemePackageError.MissingThemeJson), files, licenseText)
}

private enum class EntryKind { Json, Font, Image, Text }

private fun classify(name: String): EntryKind? {
    val lower = name.lowercase()
    val ext = lower.substringAfterLast('.', "")
    return when {
        name == "theme.json" -> EntryKind.Json
        ext == "ttf" || ext == "otf" -> EntryKind.Font
        ext == "png" -> EntryKind.Image
        ext == "txt" || ext == "md" -> EntryKind.Text
        '.' !in lower && lower in IGNORED_TEXT_NAMES -> EntryKind.Text
        else -> null
    }
}

private fun decodeThemeJson(data: ByteArray): String {
    val text = try {
        data.decodeToString(throwOnInvalidSequence = true)
    } catch (e: Exception) {
        throw ThemePackageException(ThemePackageError.InvalidThemeJson, "not UTF-8")
    }
    if (jsonNestingExceeds(text, ThemeLimits.MAX_JSON_NESTING)) {
        throw ThemePackageException(ThemePackageError.InvalidThemeJson, "nested too deeply")
    }
    return text
}

private fun readCapped(zip: ZipStream, cap: Int, name: String): ByteArray {
    val out = Buffer()
    val chunk = ByteArray(8192)
    var size = 0L
    while (true) {
        val n = zip.read(chunk, 0, chunk.size)
        if (n < 0) break
        size += n
        if (size > cap) throw ThemePackageException(ThemePackageError.EntryTooLarge, name)
        out.write(chunk, 0, n)
    }
    return out.readByteArray()
}

/**
 * Normalizes an archive entry name, or returns null if it could escape or confuse a path:
 * absolute paths, `..`, backslashes, drive letters, control characters, odd characters, overly
 * long or deep names.
 */
internal fun normalizeEntryName(raw: String): String? {
    if (raw.isEmpty() || raw.length > ThemeLimits.MAX_NAME_LENGTH) return null
    if (raw.startsWith("/")) return null
    for (c in raw) {
        val ok = c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '.' || c == '_' || c == '-' || c == '/' || c == ' '
        if (!ok) return null
    }
    val trimmed = raw.removeSuffix("/")
    val segments = trimmed.split('/')
    if (segments.size > ThemeLimits.MAX_PATH_DEPTH) return null
    if (segments.any { it.isEmpty() || it.startsWith(".") || it.startsWith(" ") || it.endsWith(" ") || it.endsWith(".") }) return null
    return trimmed
}

/** Metadata files that macOS/Windows archivers add; skipped rather than treated as hostile. */
private fun isOsJunk(raw: String): Boolean {
    val first = raw.substringBefore('/')
    val last = raw.substringAfterLast('/')
    return first == "__MACOSX" || last == ".DS_Store" || last == "Thumbs.db" || last == "desktop.ini"
}

/** True if bracket/brace nesting outside string literals goes deeper than [max]. */
internal fun jsonNestingExceeds(text: String, max: Int): Boolean {
    var depth = 0
    var inString = false
    var escaped = false
    for (c in text) {
        if (inString) {
            when {
                escaped -> escaped = false
                c == '\\' -> escaped = true
                c == '"' -> inString = false
            }
        } else when (c) {
            '"' -> inString = true
            '[', '{' -> if (++depth > max) return true
            ']', '}' -> depth--
        }
    }
    return false
}
