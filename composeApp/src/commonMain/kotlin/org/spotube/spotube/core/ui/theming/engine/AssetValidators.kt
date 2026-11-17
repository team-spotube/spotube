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

/**
 * Structural validation of untrusted asset files, in pure Kotlin before any native decoder sees
 * them. This cuts the attack surface (truncated tables, out-of-range offsets, absurd sizes), but a
 * font or image parser in the OS/Skia is still native code: that is why installs need explicit
 * user confirmation and why themes can't pull files from the network.
 */

private fun ByteArray.u8(o: Int): Int = this[o].toInt() and 0xFF
private fun ByteArray.u16(o: Int): Int = (u8(o) shl 8) or u8(o + 1)
private fun ByteArray.u32(o: Int): Long =
    (u8(o).toLong() shl 24) or (u8(o + 1).toLong() shl 16) or (u8(o + 2).toLong() shl 8) or u8(o + 3).toLong()

private val REQUIRED_FONT_TABLES = listOf("head", "hhea", "maxp", "cmap", "hmtx")

/** Returns null when [b] is a structurally sound TrueType/OpenType font, else a short reason. */
internal fun validateFont(b: ByteArray): String? {
    if (b.size < 12 + 16) return "too small"
    val magic = b.u32(0)
    // 0x00010000 TrueType, 'OTTO' CFF, 'true' Apple TrueType. Collections (ttcf) and WOFF are refused.
    if (magic != 0x00010000L && magic != 0x4F54544FL && magic != 0x74727565L) return "not a TrueType/OpenType font"
    val numTables = b.u16(4)
    if (numTables !in 1..64) return "bad table count"
    val dirEnd = 12 + 16 * numTables
    if (dirEnd > b.size) return "truncated table directory"

    val tables = HashMap<String, Pair<Int, Int>>() // tag -> (offset, length)
    for (i in 0 until numTables) {
        val rec = 12 + 16 * i
        val tag = buildString { for (k in 0 until 4) append(b.u8(rec + k).toChar()) }
        if (tag.any { it.code !in 0x20..0x7E }) return "bad table tag"
        val off = b.u32(rec + 8)
        val len = b.u32(rec + 12)
        if (off < dirEnd || len < 0 || off + len > b.size) return "table '$tag' out of range"
        if (tables.put(tag, off.toInt() to len.toInt()) != null) return "duplicate table '$tag'"
    }
    for (t in REQUIRED_FONT_TABLES) if (t !in tables) return "missing '$t' table"
    // Embedded SVG documents would route through a second, much larger parser.
    if ("SVG " in tables) return "SVG glyphs are not allowed"

    val (headOff, headLen) = tables.getValue("head")
    if (headLen < 54 || b.u32(headOff + 12) != 0x5F0F3CF5L) return "bad head table"
    val upem = b.u16(headOff + 18)
    if (upem !in 16..16384) return "bad unitsPerEm"
    val locaFormat = b.u16(headOff + 50)

    val (maxpOff, maxpLen) = tables.getValue("maxp")
    if (maxpLen < 6) return "bad maxp table"
    val numGlyphs = b.u16(maxpOff + 4)
    if (numGlyphs < 1) return "no glyphs"

    val hasGlyf = "glyf" in tables
    val hasCff = "CFF " in tables || "CFF2" in tables
    if (!hasGlyf && !hasCff) return "no outlines"
    if (hasGlyf) {
        val loca = tables["loca"] ?: return "missing 'loca' table"
        val need = (numGlyphs + 1) * (if (locaFormat == 0) 2 else 4)
        if (loca.second < need) return "'loca' too short"
    }
    val (hheaOff, hheaLen) = tables.getValue("hhea")
    if (hheaLen < 36) return "bad hhea table"
    val metrics = b.u16(hheaOff + 34)
    if (metrics < 1 || metrics > numGlyphs) return "bad hmtx metric count"
    return null
}

private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

private val CRC_TABLE = IntArray(256) { n ->
    var c = n
    repeat(8) { c = if (c and 1 != 0) (c ushr 1) xor 0xEDB88320.toInt() else c ushr 1 }
    c
}

private fun crc32(b: ByteArray, from: Int, to: Int): Int {
    var c = -1
    for (i in from until to) c = CRC_TABLE[(c xor b.u8(i)) and 0xFF] xor (c ushr 8)
    return c.inv()
}

/** Returns null for a sane, small PNG with valid chunk framing and CRCs, else a short reason. */
internal fun validatePng(b: ByteArray): String? {
    if (b.size < 8 + 25 + 12) return "too small"
    for (i in 0 until 8) if (b[i] != PNG_SIGNATURE[i]) return "not a PNG"
    var pos = 8
    var chunks = 0
    var sawIdat = false
    var sawIend = false
    while (pos + 12 <= b.size) {
        if (sawIend) return "data after IEND"
        if (++chunks > 256) return "too many chunks"
        val len = b.u32(pos)
        if (len > b.size) return "bad chunk length"
        val dataStart = pos + 8
        val end = dataStart + len.toInt()
        if (end + 4 > b.size) return "truncated chunk"
        val type = buildString { for (k in 0 until 4) append(b.u8(pos + 4 + k).toChar()) }
        if (type.any { it !in 'a'..'z' && it !in 'A'..'Z' }) return "bad chunk type"
        if (crc32(b, pos + 4, end) != b.u32(end).toInt()) return "bad CRC in $type"
        if (chunks == 1) {
            if (type != "IHDR" || len != 13L) return "missing IHDR"
            val w = b.u32(dataStart)
            val h = b.u32(dataStart + 4)
            if (w < 1 || h < 1 || w > ThemeLimits.MAX_IMAGE_SIDE || h > ThemeLimits.MAX_IMAGE_SIDE) {
                return "image larger than ${ThemeLimits.MAX_IMAGE_SIDE}px"
            }
            if (b.u8(dataStart + 8) !in listOf(1, 2, 4, 8, 16)) return "bad bit depth"
            if (b.u8(dataStart + 9) !in listOf(0, 2, 3, 4, 6)) return "bad colour type"
        }
        if (type == "IDAT") sawIdat = true
        if (type == "IEND") sawIend = true
        pos = end + 4
    }
    if (!sawIdat || !sawIend || pos != b.size) return "incomplete PNG"
    return null
}
