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

import no.synth.kmpzip.okio.ZipInputStream
import no.synth.kmpzip.zip.ZipInputStream as ZipStream
import okio.Buffer
import okio.FileSystem
import okio.HashingSink
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

/** Caps for untrusted archives. Sizes are counted while reading, never taken from zip headers. */
data class ZipLimits(
    val maxArchiveBytes: Int = 64 * 1024 * 1024,
    val maxEntries: Int = 2_000,
    val maxEntryBytes: Long = 64L * 1024 * 1024,
    val maxTotalBytes: Long = 256L * 1024 * 1024,
)

enum class UnsafeZipReason { ArchiveTooLarge, TooManyEntries, EntryTooLarge, TotalTooLarge, UnsafePath, DuplicateEntry, Corrupt }

/** The archive is malicious, malformed or over a limit. [entry] names the offender (truncated). */
class UnsafeZipException(val reason: UnsafeZipReason, val entry: String? = null) :
    Exception("${reason.name}${entry?.let { " ($it)" } ?: ""}")

class ExtractedZipEntry(val path: String, val size: Long, val sha256: String)

private const val MAX_NAME_LENGTH = 255
private const val MAX_DEPTH = 16

/**
 * Normalizes an archive entry name to a safe relative path, or returns null when it could escape
 * the extraction directory or confuse the file system.
 *
 * Forms that are harmless once cleaned are accepted as *relative* paths, so unusual-but-honest
 * tooling keeps working: Windows `\` separators, a leading `/`, and leading `./`. (Treating a
 * leading `/` as relative is what makes it safe: the old code let it replace the base directory.)
 * Rejected: `..` or inner `.` segments, empty segments, drive letters and stream separators (`:`),
 * control characters, and absurd length or depth.
 */
fun safeZipEntryPath(raw: String): String? {
    if (raw.isEmpty() || raw.length > MAX_NAME_LENGTH) return null
    for (c in raw) if (c == ':' || c.code < 0x20 || c.code == 0x7F) return null
    var name = raw.replace('\\', '/')
    while (name.startsWith("/") || name.startsWith("./")) name = name.removePrefix("/").removePrefix("./")
    val segments = name.removeSuffix("/").split('/')
    if (name.isEmpty() || segments.size > MAX_DEPTH) return null
    if (segments.any { it.isEmpty() || it == "." || it == ".." }) return null
    return segments.joinToString("/")
}

/** Finder/Explorer metadata archivers add; harmless, so skipped instead of treated as hostile. */
private fun isOsJunk(raw: String): Boolean {
    val last = raw.removeSuffix("/").substringAfterLast('/')
    return raw.substringBefore('/') == "__MACOSX" || last == ".DS_Store" || last == "Thumbs.db"
}

private fun checkArchive(bytes: ByteArray, limits: ZipLimits) {
    if (bytes.size > limits.maxArchiveBytes) throw UnsafeZipException(UnsafeZipReason.ArchiveTooLarge)
}

private inline fun <T> guarded(block: () -> T): T = try {
    block()
} catch (e: UnsafeZipException) {
    throw e
} catch (e: Exception) {
    throw UnsafeZipException(UnsafeZipReason.Corrupt, e.message?.take(80))
}

/**
 * Extracts [bytes] into [destDir], which is created if needed, and returns what was written.
 *
 * Every entry name is normalized by [safeZipEntryPath] and the resolved target is re-checked to
 * be inside [destDir] (defence in depth), so a crafted archive cannot write elsewhere ("zip
 * slip"). Entry, total and count limits are enforced on the bytes actually decompressed, so a
 * zip bomb stops at the cap. Only regular files and directories are created.
 */
fun extractZipSafely(
    bytes: ByteArray,
    destDir: Path,
    fileSystem: FileSystem = FileSystem.SYSTEM,
    limits: ZipLimits = ZipLimits(),
    onEntry: (ExtractedZipEntry) -> Unit = {},
): List<ExtractedZipEntry> {
    checkArchive(bytes, limits)
    fileSystem.createDirectories(destDir)
    val root = destDir.normalized()
    val seen = HashSet<String>()
    val written = ArrayList<ExtractedZipEntry>()
    var entries = 0
    var total = 0L

    guarded {
        val zip = ZipInputStream(Buffer().apply { write(bytes) })
        while (true) {
            val entry = zip.nextEntry ?: break
            if (++entries > limits.maxEntries) throw UnsafeZipException(UnsafeZipReason.TooManyEntries)
            if (isOsJunk(entry.name)) continue
            val rel = safeZipEntryPath(entry.name) ?: throw UnsafeZipException(UnsafeZipReason.UnsafePath, entry.name.take(60))
            if (!seen.add(rel.lowercase())) throw UnsafeZipException(UnsafeZipReason.DuplicateEntry, rel)

            val target = (root / rel).normalized()
            if (!target.isInside(root)) throw UnsafeZipException(UnsafeZipReason.UnsafePath, rel)

            if (entry.isDirectory) {
                fileSystem.createDirectories(target)
                continue
            }
            target.parent?.let { fileSystem.createDirectories(it) }
            val hashing = HashingSink.sha256(fileSystem.sink(target))
            var size = 0L
            hashing.buffer().use { out ->
                val chunk = ByteArray(8192)
                while (true) {
                    val n = zip.read(chunk, 0, chunk.size)
                    if (n < 0) break
                    size += n
                    total += n
                    if (size > limits.maxEntryBytes) throw UnsafeZipException(UnsafeZipReason.EntryTooLarge, rel)
                    if (total > limits.maxTotalBytes) throw UnsafeZipException(UnsafeZipReason.TotalTooLarge)
                    out.write(chunk, 0, n)
                }
            }
            val result = ExtractedZipEntry(rel, size, hashing.hash.hex())
            written += result
            onEntry(result)
        }
    }
    return written
}

/**
 * Reads one file from the archive into memory without extracting anything else; null if absent.
 * [entryName] is matched against the normalized entry name, and reading stops at [maxBytes].
 */
fun readZipEntryCapped(
    bytes: ByteArray,
    entryName: String,
    maxBytes: Int,
    limits: ZipLimits = ZipLimits(),
): ByteArray? {
    checkArchive(bytes, limits)
    var entries = 0
    return guarded {
        val zip: ZipStream = ZipInputStream(Buffer().apply { write(bytes) })
        var found: ByteArray? = null
        while (found == null) {
            val entry = zip.nextEntry ?: break
            if (++entries > limits.maxEntries) throw UnsafeZipException(UnsafeZipReason.TooManyEntries)
            if (entry.isDirectory || safeZipEntryPath(entry.name) != entryName) continue
            val out = Buffer()
            val chunk = ByteArray(8192)
            var size = 0L
            while (true) {
                val n = zip.read(chunk, 0, chunk.size)
                if (n < 0) break
                size += n
                if (size > maxBytes) throw UnsafeZipException(UnsafeZipReason.EntryTooLarge, entryName)
                out.write(chunk, 0, n)
            }
            found = out.readByteArray()
        }
        found
    }
}

private fun Path.isInside(root: Path): Boolean {
    val r = root.toString().trimEnd('/', '\\')
    val me = toString()
    return me.startsWith(r + "/") || me.startsWith(r + "\\")
}
