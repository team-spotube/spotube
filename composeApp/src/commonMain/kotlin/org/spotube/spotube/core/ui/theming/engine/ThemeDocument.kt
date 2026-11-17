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

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/** Highest `schema` version this build understands. Newer files still load, with a warning. */
const val THEME_SCHEMA_VERSION = 1

enum class ThemeVarType { Color, Number, Bool, String, Enum }

/**
 * A declared theme variable (`vars` at the top level or inside a component): a typed, validated
 * token that theme authors use to keep colours, radii and the like in one place.
 *
 * Exactly one of [default] (a plain value) or [value] (a derived expression) is meaningful. Variables are
 * an authoring tool: they are never shown to users.
 */
class ThemeVarDecl(
    val name: String,
    val type: ThemeVarType,
    val default: JsonElement?,
    val value: JsonElement?,
    val min: Double?,
    val max: Double?,
    val options: List<String>,
)

/** A parsed theme file. Everything below is still raw JSON: it is evaluated per appearance/state. */
class ThemeDocument(
    val schema: Int,
    val id: String,
    val name: String,
    val tokens: JsonObject,
    val vars: Map<String, ThemeVarDecl>,
    /** Props applied to every component that has them (the "global" mode). */
    val defaults: JsonObject,
    val components: JsonObject,
    /** `assets` block: font families and icon replacements (resolved by [buildThemeAssets]). */
    val assets: JsonObject = JsonObject(emptyMap()),
    val meta: ThemeMetadata = ThemeMetadata(),
    /** False when `theme.json` had no `name` (the display name then defaults to "Custom theme"). */
    val hasExplicitName: Boolean = true,
) {
}

private val themeJson = Json { isLenient = false; ignoreUnknownKeys = true }

/** Parses theme [text]; problems are appended to [problems] and a null result means "unusable". */
fun parseThemeDocument(text: String, problems: MutableSet<String>): ThemeDocument? {
    if (jsonNestingExceeds(text, ThemeLimits.MAX_JSON_NESTING)) {
        problems += "Theme is nested too deeply"
        return null
    }
    val root = try {
        themeJson.parseToJsonElement(text) as? JsonObject
    } catch (e: Exception) {
        problems += "Theme is not valid JSON: ${e.message?.lineSequence()?.firstOrNull()}"
        return null
    }
    if (root == null) {
        problems += "Theme root must be a JSON object"
        return null
    }
    val schema = root["schema"]?.jsonPrimitiveOrNull()?.intOrNull ?: THEME_SCHEMA_VERSION
    if (schema > THEME_SCHEMA_VERSION) {
        problems += "Theme uses schema $schema; this app understands $THEME_SCHEMA_VERSION, unknown parts are ignored"
    }
    fun obj(key: String): JsonObject = (root[key] as? JsonObject) ?: JsonObject(emptyMap())
    return ThemeDocument(
        schema = schema,
        id = sanitizeThemeId(root["id"]?.jsonPrimitiveOrNull()?.contentOrNull, problems),
        name = root["name"]?.jsonPrimitiveOrNull()?.contentOrNull ?: "Custom theme",
        tokens = obj("tokens"),
        vars = parseVarDecls(root["vars"] as? JsonObject, problems, "vars"),
        defaults = obj("defaults"),
        components = obj("components"),
        assets = obj("assets"),
        meta = parseThemeMetadata(root, problems),
        hasExplicitName = root["name"]?.jsonPrimitiveOrNull()?.contentOrNull?.isNotBlank() == true,
    )
}

/**
 * Who made a theme and where to find them, shown on the theme's card. Mirrors `plugin.json`.
 * Every link is validated to a plain `http(s)` URL (or an email for [contact]) before it can be opened.
 */
data class ThemeMetadata(
    val version: String? = null,
    val description: String? = null,
    val author: String? = null,
    /** An `http(s)` URL or an email address (stored as `mailto:`). */
    val contact: String? = null,
    val repository: String? = null,
    /** Where to report problems (issue tracker). */
    val bugs: String? = null,
    val homepage: String? = null,
    /** SPDX identifier or short license name; the full text can ship as `LICENSE`. */
    val license: String? = null,
    /** Path of a `.png` inside the package. */
    val logo: String? = null,
) {
    /** Fields every installable theme must declare, like a plugin manifest. */
    fun missingRequired(name: String?): String? = when {
        name.isNullOrBlank() -> "name"
        version.isNullOrBlank() -> "version"
        author.isNullOrBlank() -> "author"
        else -> null
    }
}

private val VERSION = Regex("[0-9A-Za-z.+_-]{1,32}")
private val EMAIL = Regex("[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,190}\\.[A-Za-z]{2,24}")

private fun parseThemeMetadata(root: JsonObject, problems: MutableSet<String>): ThemeMetadata {
    fun str(key: String, max: Int): String? =
        (root[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }?.take(max)
    fun link(key: String): String? = str(key, 300)?.let { raw ->
        safeWebUrl(raw) ?: null.also { problems += "$key must be an http(s) URL" }
    }
    val version = str("version", 32)?.takeIf { VERSION.matches(it) || false.also { _ -> problems += "version has unsupported characters" } }
    val contactRaw = str("contact", 200)
    val contact = contactRaw?.let { c ->
        when {
            EMAIL.matches(c) -> "mailto:$c"
            else -> safeWebUrl(c) ?: null.also { problems += "contact must be an email address or an http(s) URL" }
        }
    }
    return ThemeMetadata(
        version = version,
        description = str("description", 500),
        author = str("author", 80),
        contact = contact,
        repository = link("repository"),
        bugs = link("bugs"),
        homepage = link("homepage"),
        license = str("license", 60),
        logo = str("logo", ThemeLimits.MAX_NAME_LENGTH),
    )
}

/** Only `http`/`https` URLs with a host, no whitespace or control characters: never `file:`, `javascript:` etc. */
internal fun safeWebUrl(raw: String): String? {
    if (raw.length > 300 || raw.any { it.isWhitespace() || it.code < 0x20 || it.code == 0x7F }) return null
    val lower = raw.lowercase()
    val rest = when {
        lower.startsWith("https://") -> raw.substring(8)
        lower.startsWith("http://") -> raw.substring(7)
        else -> return null
    }
    val host = rest.substringBefore('/').substringBefore('?').substringBefore('#').substringAfterLast('@')
    return if (host.isNotEmpty() && host.any { it.isLetterOrDigit() }) raw else null
}

private val THEME_ID = Regex("[A-Za-z0-9._-]{1,48}")

/** The id becomes part of settings keys, so it is restricted to a safe alphabet. */
private fun sanitizeThemeId(raw: String?, problems: MutableSet<String>): String = when {
    raw == null -> "custom"
    THEME_ID.matches(raw) -> raw
    else -> {
        problems += "Theme id must be 1-48 characters of letters, digits, '.', '_' or '-'; using \"custom\""
        "custom"
    }
}

internal fun parseVarDecls(
    obj: JsonObject?,
    problems: MutableSet<String>,
    where: String,
): Map<String, ThemeVarDecl> {
    if (obj == null) return emptyMap()
    val out = LinkedHashMap<String, ThemeVarDecl>()
    for ((name, el) in obj) {
        val d = el as? JsonObject
        if (d == null) {
            problems += "$where.$name: a variable must be an object"
            continue
        }
        val type = when (d["type"]?.jsonPrimitiveOrNull()?.contentOrNull?.lowercase()) {
            "color" -> ThemeVarType.Color
            "number", "dp" -> ThemeVarType.Number
            "bool", "boolean" -> ThemeVarType.Bool
            "string" -> ThemeVarType.String
            "enum" -> ThemeVarType.Enum
            else -> {
                problems += "$where.$name: unknown or missing type"
                continue
            }
        }
        val options = (d["options"] as? JsonArray)?.mapNotNull { it.jsonPrimitiveOrNull()?.contentOrNull }.orEmpty()
        if (type == ThemeVarType.Enum && options.isEmpty()) {
            problems += "$where.$name: an enum needs options"
            continue
        }
        if (d["default"] == null && d["value"] == null) {
            problems += "$where.$name: needs a default or a value"
            continue
        }
        out[name] = ThemeVarDecl(
            name = name,
            type = type,
            default = d["default"],
            value = d["value"],
            min = d["min"]?.jsonPrimitiveOrNull()?.doubleOrNull,
            max = d["max"]?.jsonPrimitiveOrNull()?.doubleOrNull,
            options = options,
        )
    }
    return out
}

internal fun JsonElement.jsonPrimitiveOrNull(): JsonPrimitive? = this as? JsonPrimitive

/** `#RGB`, `#RRGGBB` or `#RRGGBBAA` (alpha last, as in CSS). */
internal fun parseHexColor(s: String): Color? {
    if (!s.startsWith("#")) return null
    val h = s.substring(1)
    val v = h.toLongOrNull(16) ?: return null
    return when (h.length) {
        3 -> Color(
            red = ((v shr 8) and 0xF).toInt() * 17,
            green = ((v shr 4) and 0xF).toInt() * 17,
            blue = (v and 0xF).toInt() * 17,
        )
        6 -> Color(0xFF000000L or v)
        8 -> Color(
            red = ((v shr 24) and 0xFF).toInt(),
            green = ((v shr 16) and 0xFF).toInt(),
            blue = ((v shr 8) and 0xFF).toInt(),
            alpha = (v and 0xFF).toInt(),
        )
        else -> null
    }
}
