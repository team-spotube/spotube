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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import okio.ByteString.Companion.toByteString
import org.jetbrains.compose.resources.decodeToImageBitmap

/** An app icon replacement defined by a theme. */
sealed interface ThemeIcon {
    /** One glyph of a bundled icon font, tinted like the stock icon. */
    data class Glyph(val text: String, val family: FontFamily) : ThemeIcon

    /** A replacement vector (built-in themes only: a package can't ship code, so this has no JSON form). */
    class Vector(val image: ImageVector) : ThemeIcon

    /** A bundled PNG. [tinted] recolours it with the icon tint (best for monochrome art). */
    class Bitmap(val image: ImageBitmap, val tinted: Boolean) : ThemeIcon
}

/** Fonts and icons a theme brings, already validated and turned into Compose objects. */
@Immutable
class ThemeAssets(
    val fontFamilies: Map<String, FontFamily> = emptyMap(),
    /** Keyed by the app's icon (ImageVector) name, for example `IconsaxHome`. */
    val icons: Map<String, ThemeIcon> = emptyMap(),
) {
    companion object {
        val Empty = ThemeAssets()
    }
}

private val GENERIC_FAMILIES = mapOf(
    "default" to FontFamily.Default,
    "sans-serif" to FontFamily.SansSerif,
    "sans" to FontFamily.SansSerif,
    "serif" to FontFamily.Serif,
    "monospace" to FontFamily.Monospace,
    "cursive" to FontFamily.Cursive,
)

/** Resolves a family name used in a theme: a built-in generic name, or one the theme declared. */
internal fun resolveFontFamily(name: String, assets: ThemeAssets): FontFamily? =
    GENERIC_FAMILIES[name.lowercase()] ?: assets.fontFamilies[name]

private val FAMILY_NAME = Regex("[A-Za-z0-9 _-]{1,40}")
private val ICON_NAME = Regex("[A-Za-z0-9_]{1,64}")

/**
 * Turns the theme's `assets` block into [ThemeAssets], looking files up *only* in the validated
 * in-memory [pkg]. A `src` is a key into that map: there is no file-system or network access, so
 * a theme cannot reference anything it did not ship.
 */
internal fun buildThemeAssets(
    assetsJson: JsonObject,
    pkg: ThemePackage,
    cacheDir: String,
    problems: MutableSet<String>,
): ThemeAssets {
    val families = LinkedHashMap<String, FontFamily>()

    (assetsJson["fonts"] as? JsonObject)?.forEach { (family, spec) ->
        if (!FAMILY_NAME.matches(family) || family.lowercase() in GENERIC_FAMILIES) {
            problems += "assets.fonts.$family: invalid family name"
            return@forEach
        }
        val faces = (spec as? JsonArray)?.mapNotNull { face ->
            val o = face as? JsonObject ?: return@mapNotNull null
            val src = (o["src"] as? JsonPrimitive)?.contentOrNull
            val bytes = src?.let { lookupAsset(it, pkg, ".ttf", ".otf") }
            if (bytes == null) {
                problems += "assets.fonts.$family: font file \"$src\" not found in the theme package"
                return@mapNotNull null
            }
            val weight = ((o["weight"] as? JsonPrimitive)?.intOrNull ?: 400).coerceIn(1, 1000)
            val style = if ((o["style"] as? JsonPrimitive)?.contentOrNull == "italic") FontStyle.Italic else FontStyle.Normal
            val hash = bytes.toByteString().sha256().hex().take(16)
            fontFromBytes("theme-$hash", bytes, FontWeight(weight), style, cacheDir)
        }.orEmpty()
        if (faces.isEmpty()) problems += "assets.fonts.$family: no usable font files" else families[family] = FontFamily(faces)
    }

    val icons = LinkedHashMap<String, ThemeIcon>()
    (assetsJson["icons"] as? JsonObject)?.forEach { (name, spec) ->
        if (!ICON_NAME.matches(name)) {
            problems += "assets.icons.$name: invalid icon name"
            return@forEach
        }
        val o = spec as? JsonObject ?: return@forEach
        val glyph = (o["glyph"] as? JsonPrimitive)?.contentOrNull
        val image = (o["image"] as? JsonPrimitive)?.contentOrNull
        when {
            glyph != null -> {
                val family = (o["font"] as? JsonPrimitive)?.contentOrNull?.let { families[it] }
                val text = parseGlyph(glyph)
                when {
                    family == null -> problems += "assets.icons.$name: \"font\" must name a family declared in assets.fonts"
                    text == null -> problems += "assets.icons.$name: \"glyph\" must be a single character or U+XXXX"
                    else -> icons[name] = ThemeIcon.Glyph(text, family)
                }
            }
            image != null -> {
                val bytes = lookupAsset(image, pkg, ".png")
                if (bytes == null) {
                    problems += "assets.icons.$name: image \"$image\" not found in the theme package"
                } else {
                    runCatching { bytes.decodeToImageBitmap() }
                        .onSuccess {
                            val tinted = (o["tint"] as? JsonPrimitive)?.booleanOrNull ?: true
                            icons[name] = ThemeIcon.Bitmap(it, tinted)
                        }
                        .onFailure { problems += "assets.icons.$name: image could not be decoded" }
                }
            }
            else -> problems += "assets.icons.$name: needs \"glyph\" + \"font\", or \"image\""
        }
    }

    // An icon pack fills every app icon tied to a role that the theme did not override one by one. Two forms:
    //  - `font` + `icons`: an icon font and a glyph per role (schema 1.0);
    //  - `images`: a PNG per role, tinted like the stock icon unless `tint` is false (schema 1.1), which works for any
    //    icon library, including stroke-based ones that cannot be turned into a font.
    (assetsJson["iconPack"] as? JsonObject)?.let { pack ->
        val byRole = HashMap<String, ThemeIcon>()

        val glyphs = (pack["icons"] as? JsonObject).orEmpty()
        if (glyphs.isNotEmpty() || pack["font"] != null) {
            val family = (pack["font"] as? JsonPrimitive)?.contentOrNull?.let { families[it] }
            if (family == null) {
                problems += "assets.iconPack: \"font\" must name a family declared in assets.fonts"
            } else {
                if (glyphs.size > 512) problems += "assets.iconPack: at most 512 icons"
                glyphs.entries.take(512).forEach { (role, g) ->
                    val text = (g as? JsonPrimitive)?.contentOrNull?.let(::parseGlyph)
                    if (text == null) problems += "assets.iconPack.icons.$role: glyph must be a single character or U+XXXX" else byRole[role] = ThemeIcon.Glyph(text, family)
                }
            }
        }

        val images = (pack["images"] as? JsonObject).orEmpty()
        if (images.isNotEmpty()) {
            val tinted = (pack["tint"] as? JsonPrimitive)?.booleanOrNull ?: true
            if (images.size > ThemeLimits.MAX_ICON_IMAGES) problems += "assets.iconPack.images: at most ${ThemeLimits.MAX_ICON_IMAGES} images"
            val decoded = HashMap<String, ImageBitmap?>()
            images.entries.take(ThemeLimits.MAX_ICON_IMAGES).forEach { (role, p) ->
                val path = (p as? JsonPrimitive)?.contentOrNull
                val bitmap = path?.let { decoded.getOrPut(it) { lookupAsset(it, pkg, ".png")?.let { bytes -> runCatching { bytes.decodeToImageBitmap() }.getOrNull() } } }
                if (bitmap == null) problems += "assets.iconPack.images.$role: \"$path\" is not a PNG in the theme package" else byRole[role] = ThemeIcon.Bitmap(bitmap, tinted)
            }
        }

        iconRoleByVectorName.forEach { (vectorName, role) ->
            val icon = byRole[role]
            if (icon != null && vectorName !in icons) icons[vectorName] = icon
        }
    }
    return ThemeAssets(families, icons)
}

/** Exact-key lookup into the package; remote URLs and anything not shipped simply do not exist. */
private fun lookupAsset(src: String, pkg: ThemePackage, vararg extensions: String): ByteArray? {
    val name = normalizeEntryName(src) ?: return null
    if (extensions.none { name.lowercase().endsWith(it) }) return null
    return pkg.files[name]
}

/** One code point: a literal character, `U+E900`, or ``-style hex. */
internal fun parseGlyph(s: String): String? {
    val hex = when {
        s.startsWith("U+", ignoreCase = true) -> s.substring(2)
        else -> null
    }
    if (hex != null) {
        val cp = hex.toIntOrNull(16) ?: return null
        return if (cp in 0x20..0x10FFFF && cp !in 0xD800..0xDFFF) codePointToString(cp) else null
    }
    val count = s.codePointCountCompat()
    return if (count == 1) s else null
}

private fun String.codePointCountCompat(): Int {
    var n = 0
    var i = 0
    while (i < length) {
        i += if (this[i].isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate()) 2 else 1
        n++
    }
    return n
}

private fun codePointToString(cp: Int): String =
    if (cp < 0x10000) cp.toChar().toString() else {
        val v = cp - 0x10000
        charArrayOf((0xD800 + (v shr 10)).toChar(), (0xDC00 + (v and 0x3FF)).toChar()).concatToString()
    }

/** A parsed, asset-resolved theme ready for [compileTheme], plus what its card shows. */
@Immutable
class LoadedTheme(
    val doc: ThemeDocument,
    val assets: ThemeAssets,
    /** Problems found while loading (before any compile). */
    val problems: Set<String>,
    /** True when the theme text mentions `ctx.item`, so per-row scopes are worth their cost. */
    val usesItemContext: Boolean,
    /** True when the theme reads `ctx.player.*` or `ctx.env.compact`, so it must be re-evaluated when those change. */
    val usesLiveContext: Boolean = false,
    val fontFileCount: Int,
    val iconCount: Int,
    val packageBytes: Int,
    val logo: ImageBitmap? = null,
    val licenseText: String? = null,
) {
    val id: String get() = doc.id
    val name: String get() = doc.name
    val meta: ThemeMetadata get() = doc.meta
}

/** Parses [pkg] and builds its assets; null (with [problems]) if `theme.json` is unusable. */
fun loadTheme(pkg: ThemePackage, cacheDir: String, problems: MutableSet<String>): LoadedTheme? {
    val doc = parseThemeDocument(pkg.json, problems) ?: return null
    val assets = buildThemeAssets(doc.assets, pkg, cacheDir, problems)
    val logo = doc.meta.logo?.let { path ->
        val bytes = lookupAsset(path, pkg, ".png")
        if (bytes == null) {
            problems += "logo \"$path\" not found in the theme package"
            null
        } else {
            runCatching { bytes.decodeToImageBitmap() }.getOrNull()
        }
    }
    return LoadedTheme(
        doc = doc,
        assets = assets,
        problems = problems.toSet(),
        usesItemContext = pkg.json.contains("ctx.item"),
        usesLiveContext = pkg.json.contains("ctx.player") || pkg.json.contains("ctx.env.compact"),
        fontFileCount = pkg.files.keys.count { it.lowercase().endsWith(".ttf") || it.lowercase().endsWith(".otf") },
        iconCount = assets.icons.size,
        packageBytes = pkg.files.values.sumOf { it.size } + pkg.json.length,
        logo = logo,
        licenseText = pkg.licenseText,
    )
}
