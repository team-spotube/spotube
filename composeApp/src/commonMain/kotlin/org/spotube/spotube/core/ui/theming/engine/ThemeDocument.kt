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

/**
 * The theme schema version this build implements ([semver](https://semver.org)).
 *
 * - **major**: breaking changes (a property removed or its meaning changed). A theme of another major is refused.
 * - **minor**: backwards-compatible additions (new tokens, components, properties, functions). A theme written for an
 *   older or *newer* minor of this major still loads; parts this build doesn't know are ignored, with a warning when
 *   the theme is newer.
 * - **patch**: clarifications and fixes, never visible to themes.
 *
 * History: 1.0.0 first release; 1.1.0 `assets.iconPack.images` (PNG icon packs from any icon library).
 *
 * Keep in sync with `themes/schema/theme.schema.json` (`ThemeSchemaTest` checks it) and bump it with every
 * change to the theme format.
 */
const val THEME_SCHEMA_VERSION = "1.1.0"

/** A parsed `MAJOR.MINOR.PATCH`. */
data class SchemaVersion(val major: Int, val minor: Int, val patch: Int) {
    override fun toString() = "$major.$minor.$patch"

    companion object {
        private val PATTERN = Regex("(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})")

        fun parse(text: String): SchemaVersion? = PATTERN.matchEntire(text.trim())?.destructured?.let { (a, b, c) ->
            SchemaVersion(a.toInt(), b.toInt(), c.toInt())
        }

        val Current: SchemaVersion = parse(THEME_SCHEMA_VERSION)!!
    }
}

/** How a theme's declared schema relates to what this build implements. */
internal sealed interface SchemaCompat {
    data object Ok : SchemaCompat
    /** Same major, but the theme is written for a newer minor: it loads, unknown parts are ignored. */
    data class Newer(val declared: SchemaVersion) : SchemaCompat
    data class Unsupported(val reason: String) : SchemaCompat
}

internal fun checkSchemaCompat(declared: SchemaVersion, current: SchemaVersion = SchemaVersion.Current): SchemaCompat = when {
    declared.major > current.major -> SchemaCompat.Unsupported("This theme needs a newer Spotube (it uses theme schema $declared, this app implements $current)")
    declared.major < current.major -> SchemaCompat.Unsupported("This theme uses the outdated theme schema $declared (this app implements $current); ask its author to update it")
    declared.minor > current.minor -> SchemaCompat.Newer(declared)
    else -> SchemaCompat.Ok
}

enum class ThemeVarType { Color, Number, Bool, String, Enum }

/**
 * A declared theme variable or parameter: a typed, validated value.
 *
 * `vars` (top level or per component) are the author's private token store, never shown to users: exactly one of
 * [default] (a plain value) or [value] (a derived expression) is meaningful. `parameters` are the knobs a theme
 * offers to *users* (accent, radius, density...): they always have a [default], the user's choice overrides it,
 * and the remaining fields describe the control the app shows ([label], [description], [min]/[max]/[step], named
 * [options] with optional swatch colours, colour [presets]).
 */
class ThemeVarDecl(
    val name: String,
    val type: ThemeVarType,
    val default: JsonElement?,
    val value: JsonElement?,
    val min: Double?,
    val max: Double?,
    val options: List<String>,
    val label: String? = null,
    val description: String? = null,
    val step: Double? = null,
    val optionLabels: Map<String, String> = emptyMap(),
    /** Swatch colour (`#RRGGBB`) per enum option, for colour-themed choices such as accent presets. */
    val optionColors: Map<String, String> = emptyMap(),
    /** Suggested colours for a colour parameter. */
    val presets: List<String> = emptyList(),
)

/** A parsed theme file. Everything below is still raw JSON: it is evaluated per appearance/state. */
class ThemeDocument(
    /** The theme schema version the file declares (semver). */
    val schema: String,
    val id: String,
    val name: String,
    val tokens: JsonObject,
    val vars: Map<String, ThemeVarDecl>,
    /** User-adjustable settings the theme offers (`parameters`); referenced as `{params.name}`. */
    val parameters: Map<String, ThemeVarDecl> = emptyMap(),
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
    val declared = parseDeclaredSchema(root["schema"]?.jsonPrimitiveOrNull(), problems) ?: return null
    when (val compat = checkSchemaCompat(declared)) {
        SchemaCompat.Ok -> Unit
        is SchemaCompat.Newer -> problems += "This theme is written for theme schema ${compat.declared}; this app implements ${SchemaVersion.Current}, so newer parts are ignored"
        is SchemaCompat.Unsupported -> {
            problems += compat.reason
            return null
        }
    }
    val schema = declared.toString()
    fun obj(key: String): JsonObject = (root[key] as? JsonObject) ?: JsonObject(emptyMap())
    return ThemeDocument(
        schema = schema,
        id = sanitizeThemeId(root["id"]?.jsonPrimitiveOrNull()?.contentOrNull, problems),
        name = root["name"]?.jsonPrimitiveOrNull()?.contentOrNull ?: "Custom theme",
        tokens = obj("tokens"),
        vars = parseVarDecls(root["vars"] as? JsonObject, problems, "vars"),
        parameters = parseParameterDecls(root["parameters"] as? JsonObject, problems),
        defaults = obj("defaults"),
        components = obj("components"),
        assets = obj("assets"),
        meta = parseThemeMetadata(root, problems),
        hasExplicitName = root["name"]?.jsonPrimitiveOrNull()?.contentOrNull?.isNotBlank() == true,
    )
}

/**
 * `schema` is a semver string. The pre-semver integer `1` still means 1.0.0, and a missing value is assumed to be the
 * current version so hand-written quick themes keep working (the themes repository's check still requires it).
 */
private fun parseDeclaredSchema(value: JsonPrimitive?, problems: MutableSet<String>): SchemaVersion? {
    if (value == null || value is kotlinx.serialization.json.JsonNull) {
        return SchemaVersion.Current
    }
    if (!value.isString && value.intOrNull == 1) return SchemaVersion(1, 0, 0)
    val parsed = value.contentOrNull?.let(SchemaVersion::parse)
    if (parsed == null) problems += "\"schema\" must be a semantic version such as \"${SchemaVersion.Current}\""
    return parsed
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
        val optionLabels = LinkedHashMap<String, String>()
        val optionColors = LinkedHashMap<String, String>()
        val options = (d["options"] as? JsonArray)?.mapNotNull { el ->
            when (el) {
                is JsonPrimitive -> el.contentOrNull
                is JsonObject -> (el["value"] as? JsonPrimitive)?.contentOrNull?.also { v ->
                    (el["label"] as? JsonPrimitive)?.contentOrNull?.let { optionLabels[v] = it.take(60) }
                    (el["color"] as? JsonPrimitive)?.contentOrNull?.takeIf { parseHexColor(it) != null }?.let { optionColors[v] = it }
                }
                else -> null
            }
        }.orEmpty()
        val presets = (d["presets"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf { c -> parseHexColor(c) != null } }.orEmpty().take(16)
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
            label = (d["label"] as? JsonPrimitive)?.contentOrNull?.take(60),
            description = (d["description"] as? JsonPrimitive)?.contentOrNull?.take(200),
            step = (d["step"] as? JsonPrimitive)?.doubleOrNull?.takeIf { it > 0 },
            optionLabels = optionLabels,
            optionColors = optionColors,
            presets = presets,
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

private val PARAMETER_NAME = Regex("[A-Za-z][A-Za-z0-9_]{0,31}")
private const val MAX_PARAMETERS = 24

/** Parses `parameters`: like `vars`, but every entry needs a plain default (the user's value replaces it). */
internal fun parseParameterDecls(obj: JsonObject?, problems: MutableSet<String>): Map<String, ThemeVarDecl> {
    if (obj == null) return emptyMap()
    val out = LinkedHashMap<String, ThemeVarDecl>()
    for ((name, decl) in parseVarDecls(obj, problems, "parameters")) {
        when {
            out.size >= MAX_PARAMETERS -> problems += "parameters: at most $MAX_PARAMETERS parameters are allowed"
            !PARAMETER_NAME.matches(name) -> problems += "parameters.$name: invalid name (letters, digits and _, starting with a letter)"
            decl.value != null || decl.default == null -> problems += "parameters.$name: a parameter needs a \"default\" (derived \"value\" is only for vars)"
            else -> out[name] = decl
        }
    }
    return out
}
