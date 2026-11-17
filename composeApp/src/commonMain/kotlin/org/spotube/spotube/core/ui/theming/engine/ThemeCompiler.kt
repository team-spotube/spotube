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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.spotube.millennium.composables.MillenniumButtonMotion
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumButtonTheme
import org.spotube.millennium.composables.MillenniumButtonThemeProperties
import org.spotube.millennium.composables.MillenniumCardTheme
import org.spotube.millennium.composables.MillenniumCardThemeProperties
import org.spotube.millennium.composables.MillenniumListTileTheme
import org.spotube.millennium.composables.MillenniumListTileThemeProperties
import org.spotube.millennium.composables.MillenniumTopAppBarTheme
import org.spotube.millennium.composables.MillenniumTopAppBarThemeProperties
import org.spotube.millennium.theme.MillenniumColors
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumThemeComponents
import org.spotube.millennium.theme.gelBrush
import org.spotube.spotube.core.ui.theming.AppDefaultShapes
import org.spotube.spotube.core.ui.theming.AppDefaultTypography
import org.spotube.spotube.core.ui.theming.AppShapes
import org.spotube.spotube.core.ui.theming.AppTypography

/** Live facts a theme can branch on, exposed to the JSON as `ctx.env.*`. */
@Immutable
data class ThemeEnv(
    val dark: Boolean,
    /** Compact (phone-sized) window width. */
    val compact: Boolean,
    /** Extra `ctx.*` values (for example `player`); nested maps are addressed with dots. */
    val extras: Map<String, Any?> = emptyMap(),
)

@Immutable
data class CompiledTheme(
    val colors: MillenniumColors,
    val components: MillenniumThemeComponents,
    val shapes: AppShapes,
    val typography: AppTypography,
    /** App icons the theme replaces, by icon name. */
    val icons: Map<String, ThemeIcon> = emptyMap(),
    /** Everything the theme author should fix; the rest of the theme still applies. */
    val problems: List<String>,
)

/**
 * Compiles [doc] onto the app's base appearance for [env].
 *
 * Pass the already-customised [baseComponents] (what the app uses with no theme) so a theme only
 * has to state what it changes. A null [doc] returns the base untouched.
 */
fun compileTheme(
    doc: ThemeDocument?,
    assets: ThemeAssets = ThemeAssets.Empty,
    userVars: Map<String, String> = emptyMap(),
    env: ThemeEnv,
    baseColors: MillenniumColors,
    baseComponents: MillenniumThemeComponents,
    parseProblems: Set<String> = emptySet(),
): CompiledTheme {
    if (doc == null) {
        return CompiledTheme(baseColors, baseComponents, AppDefaultShapes, AppDefaultTypography, problems = parseProblems.toList())
    }
    val compiler = ThemeCompiler(doc, assets, userVars, env, baseColors, baseComponents, parseProblems)
    return try {
        compiler.compile()
    } catch (e: ThemeBudgetExceeded) {
        // A hostile or accidental exponential theme: fall back to the stock look, never hang.
        val problems = compiler.problems + "Theme is too complex to evaluate and was ignored"
        CompiledTheme(baseColors, baseComponents, AppDefaultShapes, AppDefaultTypography, problems = problems.toList())
    }
}

private val STATES = listOf("default", "hovered", "focused", "pressed", "disabled", "selected", "error")

internal class ThemeCompiler(
    val doc: ThemeDocument,
    val assets: ThemeAssets,
    userVars: Map<String, String>,
    val env: ThemeEnv,
    val baseColors: MillenniumColors,
    val baseComponents: MillenniumThemeComponents,
    parseProblems: Set<String>,
) {
    val problems = LinkedHashSet<String>(parseProblems)
    val ev = ThemeEvaluator(
        doc = doc,
        userVars = userVars.filterKeys { it.startsWith("${doc.id}.") }.mapKeys { it.key.removePrefix("${doc.id}.") },
        dark = env.dark,
        namespaces = mapOf(
            "ctx" to (mapOf("env" to mapOf("dark" to env.dark, "compact" to env.compact)) + env.extras),
            "palette" to paletteMap(baseColors),
        ),
        problems = problems,
    )

    /** `tokens.type.family`: applied to every text style a theme touches or the app defines. */
    val globalFamily: FontFamily? by lazy {
        val name = tokenGroup("type")["family"] as? String ?: return@lazy null
        resolveFontFamily(name, assets).also {
            if (it == null) problems += "tokens.type.family: unknown font family \"$name\" (declare it in assets.fonts)"
        }
    }
    val fontContext by lazy { FontContext(assets, globalFamily, problems) }

    fun compile(): CompiledTheme {
        val colors = compileColors()
        val components = compileAllComponents(baseComponents)
        return CompiledTheme(
            colors = colors,
            components = components,
            shapes = compileShapes(),
            typography = compileTypography(),
            icons = assets.icons,
            problems = problems.toList(),
        )
    }

    // ---- global tokens ----------------------------------------------------------------------

    fun compileColors(): MillenniumColors {
        val tokens = doc.tokens["color"] as? JsonObject ?: return baseColors
        fun get(name: String): Color? {
            val el = tokens[name] ?: return null
            val v = ev.eval(el)
            if (v !is Color) {
                problems += "tokens.color.$name must be a colour"
                return null
            }
            return v
        }
        return baseColors.copy(
            background = get("background") ?: baseColors.background,
            surface = get("surface") ?: baseColors.surface,
            raised = get("raised") ?: baseColors.raised,
            inset = get("inset") ?: baseColors.inset,
            text = get("text") ?: baseColors.text,
            muted = get("muted") ?: baseColors.muted,
            border = get("border") ?: baseColors.border,
            highlight = get("highlight") ?: baseColors.highlight,
            accent = get("accent") ?: baseColors.accent,
            accentTop = get("accentTop") ?: baseColors.accentTop,
            accentBottom = get("accentBottom") ?: baseColors.accentBottom,
            onAccent = get("onAccent") ?: baseColors.onAccent,
            success = get("success") ?: baseColors.success,
            danger = get("danger") ?: baseColors.danger,
            scrim = get("scrim") ?: baseColors.scrim,
        )
    }

    fun tokenGroup(name: String): Map<String, Any?> {
        val el = doc.tokens[name] ?: return emptyMap()
        return ev.eval(el) as? Map<String, Any?> ?: emptyMap<String, Any?>().also {
            problems += "tokens.$name must be an object"
        }
    }

    fun compileShapes(): AppShapes {
        val r = PropReader(tokenGroup("shape"), problems, "tokens.shape", fontContext)
        return AppShapes(
            small = r.shape("small", AppDefaultShapes.small),
            medium = r.shape("medium", AppDefaultShapes.medium),
            large = r.shape("large", AppDefaultShapes.large),
        )
    }

    fun compileTypography(): AppTypography {
        val group = tokenGroup("type")
        val r = PropReader(group, problems, "tokens.type", fontContext)
        val scale = r.float("scale", 1f).coerceIn(0.5f, 2f)
        val styles = PropReader((group["styles"] as? Map<String, Any?>) ?: emptyMap(), problems, "tokens.type.styles", fontContext)
        fun style(name: String, base: TextStyle): TextStyle {
            val scaled = base.copy(
                fontSize = base.fontSize * scale,
                lineHeight = base.lineHeight * scale,
                fontFamily = globalFamily ?: base.fontFamily,
            )
            return styles.textStyle(name, scaled)
        }
        val t = AppDefaultTypography
        return AppTypography(
            displaySmall = style("displaySmall", t.displaySmall),
            headlineLarge = style("headlineLarge", t.headlineLarge),
            headlineMedium = style("headlineMedium", t.headlineMedium),
            headlineSmall = style("headlineSmall", t.headlineSmall),
            titleLarge = style("titleLarge", t.titleLarge),
            titleMedium = style("titleMedium", t.titleMedium),
            titleSmall = style("titleSmall", t.titleSmall),
            bodyLarge = style("bodyLarge", t.bodyLarge),
            bodyMedium = style("bodyMedium", t.bodyMedium),
            bodySmall = style("bodySmall", t.bodySmall),
            labelLarge = style("labelLarge", t.labelLarge),
            labelMedium = style("labelMedium", t.labelMedium),
            labelSmall = style("labelSmall", t.labelSmall),
        )
    }

    // ---- per-component ----------------------------------------------------------------------

    fun componentJson(name: String): JsonObject? = doc.components[name] as? JsonObject

    fun scopeFor(comp: JsonObject?): ThemeScope {
        val decls = parseVarDecls(comp?.get("vars") as? JsonObject, problems, "components.vars")
        return if (decls.isEmpty()) ev.globalScope else ThemeScope(decls, ev.globalScope)
    }

    /**
     * Cascade for one component/variant/state, later wins:
     * `defaults` < `base` < `states.<state>` < `variants.<v>.base` < `variants.<v>.states.<state>` < matching `rules`.
     */
    fun mergedProps(name: String, variant: String?, state: String): MergedProps {
        val comp = componentJson(name)
        val scope = scopeFor(comp)
        ev.component = mapOf("state" to state, "variant" to variant)
        val layers = ArrayList<JsonElement?>()
        layers += doc.defaults
        layers += comp?.get("base")
        layers += (comp?.get("states") as? JsonObject)?.get(state)
        val v = variant?.let { (comp?.get("variants") as? JsonObject)?.get(it) as? JsonObject }
        layers += v?.get("base")
        layers += (v?.get("states") as? JsonObject)?.get(state)
        (comp?.get("rules") as? JsonArray)?.forEach { rule ->
            val r = rule as? JsonObject ?: return@forEach
            val whenEl = r["when"]
            if (whenEl == null || ev.truthy(whenEl, scope)) layers += r["set"]
        }
        var out: Map<String, Any?> = emptyMap()
        val explicit = HashSet<String>()
        for ((index, layer) in layers.withIndex()) {
            val obj = layer as? JsonObject ?: continue
            val m = ev.eval(obj, scope) as? Map<String, Any?> ?: continue
            out = deepMerge(out, m)
            if (index > 0) explicit += m.keys // `defaults` may name props a component lacks; everything else must exist
        }
        return MergedProps(out, explicit)
    }

    /** Warns about properties a theme sets that the component does not have (almost always a typo). */
    private fun reportUnknown(where: String, reader: PropReader, merged: MergedProps) {
        for (key in reader.unreadKeys(merged.explicit)) problems += "$where: unknown property \"$key\""
    }

    fun <T> stateProp(
        name: String,
        current: MillenniumControlStateProperty<T>,
        apply: (T, PropReader) -> T,
        variant: String? = null,
    ): MillenniumControlStateProperty<T> = MillenniumControlStateProperty.resolveWith { st ->
        val state = st.name.lowercase()
        val merged = mergedProps(name, variant, state)
        val where = "components.$name${variant?.let { ".$it" } ?: ""}"
        val reader = PropReader(merged.values, problems, "$where.$state", fontContext)
        apply(current.resolve(st), reader).also { reportUnknown(where, reader, merged) }
    }

    fun <T> singleProp(name: String, current: T, apply: (T, PropReader) -> T, variant: String? = null): T {
        val merged = mergedProps(name, variant, "default")
        val where = "components.$name${variant?.let { ".$it" } ?: ""}"
        val reader = PropReader(merged.values, problems, where, fontContext)
        return apply(current, reader).also { reportUnknown(where, reader, merged) }
    }
}

/** The merged property map of one component/variant/state, and which keys the theme set itself (not via `defaults`). */
internal class MergedProps(val values: Map<String, Any?>, val explicit: Set<String>)

private fun paletteMap(c: MillenniumColors): Map<String, Any?> = mapOf(
    "background" to c.background, "surface" to c.surface, "raised" to c.raised, "inset" to c.inset,
    "text" to c.text, "muted" to c.muted, "border" to c.border, "highlight" to c.highlight,
    "accent" to c.accent, "accentTop" to c.accentTop, "accentBottom" to c.accentBottom,
    "onAccent" to c.onAccent, "success" to c.success, "danger" to c.danger, "scrim" to c.scrim,
)

@Suppress("UNCHECKED_CAST")
private fun deepMerge(a: Map<String, Any?>, b: Map<String, Any?>): Map<String, Any?> {
    val out = LinkedHashMap(a)
    for ((k, v) in b) {
        val prev = out[k]
        out[k] = if (prev is Map<*, *> && v is Map<*, *>) deepMerge(prev as Map<String, Any?>, v as Map<String, Any?>) else v
    }
    return out
}

/**
 * Typed reads over an evaluated property map. A missing key keeps the base value; a wrong type
 * keeps it too and records a problem. An explicit JSON `null` clears nullable properties.
 */
internal class PropReader(
    source: Map<String, Any?>,
    private val problems: MutableSet<String>,
    private val where: String,
    private val fonts: FontContext? = null,
) {
    private val touched = HashSet<String>()

    /** Every read goes through here, so afterwards we know which keys the component actually has. */
    private val m: Map<String, Any?> = object : Map<String, Any?> by source {
        override fun containsKey(key: String): Boolean { touched += key; return source.containsKey(key) }
        override fun get(key: String): Any? { touched += key; return source[key] }
    }

    fun unreadKeys(candidates: Set<String>): List<String> = candidates.filter { it !in touched }.sorted()

    private fun bad(key: String, want: String) {
        problems += "$where.$key: expected $want"
    }

    @Suppress("UNCHECKED_CAST")
    fun sub(key: String): PropReader? = (m[key] as? Map<String, Any?>)?.let { PropReader(it, problems, "$where.$key", fonts) }

    fun long(key: String, d: Long): Long {
        if (!m.containsKey(key)) return d
        return (m[key] as? Double)?.toLong() ?: d.also { bad(key, "a number") }
    }

    fun motion(key: String, d: MillenniumButtonMotion): MillenniumButtonMotion {
        val it = sub(key) ?: return d
        return MillenniumButtonMotion(
            stateChangeMillis = it.int("stateChangeMillis", d.stateChangeMillis),
            hoverScale = it.float("hoverScale", d.hoverScale),
            pressedScale = it.float("pressedScale", d.pressedScale),
            scaleDampingRatio = it.float("scaleDampingRatio", d.scaleDampingRatio),
            scaleStiffness = it.float("scaleStiffness", d.scaleStiffness),
            elevationDampingRatio = it.float("elevationDampingRatio", d.elevationDampingRatio),
            elevationStiffness = it.float("elevationStiffness", d.elevationStiffness),
        )
    }

    // Nullable variants: a missing key keeps the base value, an explicit JSON null clears it.
    fun intOrNull(key: String, d: Int?): Int? {
        if (!m.containsKey(key)) return d
        val v = m[key] ?: return null
        return (v as? Double)?.toInt() ?: d.also { bad(key, "a number or null") }
    }

    fun floatOrNull(key: String, d: Float?): Float? {
        if (!m.containsKey(key)) return d
        val v = m[key] ?: return null
        return (v as? Double)?.toFloat() ?: d.also { bad(key, "a number or null") }
    }

    fun dpOrNull(key: String, d: Dp?): Dp? {
        if (!m.containsKey(key)) return d
        val v = m[key] ?: return null
        return (v as? Double)?.dp ?: d.also { bad(key, "a number of dp or null") }
    }

    fun shapeOrNull(key: String, d: Shape?): Shape? = if (m.containsKey(key) && m[key] == null) null else shape(key, d ?: RectangleShape).let { r ->
        if (!m.containsKey(key)) d else r
    }

    fun textStyleOrNull(key: String, d: TextStyle?): TextStyle? {
        if (!m.containsKey(key)) return d
        if (m[key] == null) return null
        return textStyle(key, d ?: TextStyle.Default)
    }

    fun buttonStyle(key: String, d: MillenniumButtonStyle): MillenniumButtonStyle {
        if (!m.containsKey(key)) return d
        return when ((m[key] as? String)?.lowercase()) {
            "primary" -> MillenniumButtonStyle.Primary
            "secondary" -> MillenniumButtonStyle.Secondary
            "quiet" -> MillenniumButtonStyle.Quiet
            "danger" -> MillenniumButtonStyle.Danger
            else -> d.also { bad(key, "\"primary\", \"secondary\", \"quiet\" or \"danger\"") }
        }
    }

    fun float(key: String, d: Float): Float {
        if (!m.containsKey(key)) return d
        return (m[key] as? Double)?.toFloat() ?: d.also { bad(key, "a number") }
    }

    fun int(key: String, d: Int): Int {
        if (!m.containsKey(key)) return d
        return (m[key] as? Double)?.toInt() ?: d.also { bad(key, "a number") }
    }

    fun bool(key: String, d: Boolean): Boolean {
        if (!m.containsKey(key)) return d
        return m[key] as? Boolean ?: d.also { bad(key, "true or false") }
    }

    fun dp(key: String, d: Dp): Dp {
        if (!m.containsKey(key)) return d
        return (m[key] as? Double)?.dp ?: d.also { bad(key, "a number of dp") }
    }

    fun dpSize(key: String, d: DpSize): DpSize {
        if (!m.containsKey(key)) return d
        return when (val v = m[key]) {
            is Double -> DpSize(v.dp, v.dp)
            is List<*> -> if (v.size == 2 && v.all { it is Double }) DpSize((v[0] as Double).dp, (v[1] as Double).dp) else d.also { bad(key, "[width, height]") }
            else -> d.also { bad(key, "a number or [width, height]") }
        }
    }

    fun color(key: String, d: Color?): Color? {
        if (!m.containsKey(key)) return d
        return when (val v = m[key]) {
            null -> null
            is Color -> v
            else -> d.also { bad(key, "a colour") }
        }
    }

    fun colors(key: String, d: List<Color>?): List<Color>? {
        if (!m.containsKey(key)) return d
        val v = m[key] ?: return null
        val list = (v as? List<*>)?.map { it as? Color }
        return if (list != null && list.all { it != null }) list.filterNotNull() else d.also { bad(key, "a list of colours") }
    }

    fun brush(key: String, d: Brush?): Brush? {
        if (!m.containsKey(key)) return d
        return when (val v = m[key]) {
            null -> null
            is Color -> SolidColor(v)
            is BrushSpec.Gel -> gelBrush(v.top, v.bottom)
            is BrushSpec.Linear -> LinearBrush(v.angleDegrees, v.stops.map { it.color }, resolveStopPositions(v.stops))
            is BrushSpec.Radial -> RadialBrush(v.centerX, v.centerY, v.radius, v.stops.map { it.color }, resolveStopPositions(v.stops))
            is BrushSpec.Sweep -> SweepBrush(v.centerX, v.centerY, v.stops.map { it.color }, resolveStopPositions(v.stops))
            else -> d.also { bad(key, "a colour or a gradient: [\"linear\"|\"radial\"|\"sweep\"|\"gradient\"|\"gel\", ...]") }
        }
    }

    fun shape(key: String, d: Shape): Shape {
        if (!m.containsKey(key)) return d
        return when (val v = m[key]) {
            is Double -> RoundedCornerShape(v.dp)
            "pill", "circle" -> CircleShape
            "rect", "none" -> RectangleShape
            is List<*> -> if (v.size == 4 && v.all { it is Double }) {
                RoundedCornerShape((v[0] as Double).dp, (v[1] as Double).dp, (v[2] as Double).dp, (v[3] as Double).dp)
            } else d.also { bad(key, "[topStart, topEnd, bottomEnd, bottomStart]") }
            else -> d.also { bad(key, "a radius, \"pill\", \"rect\" or four corner radii") }
        }
    }

    fun padding(key: String, d: PaddingValues): PaddingValues {
        if (!m.containsKey(key)) return d
        return when (val v = m[key]) {
            is Double -> PaddingValues(v.dp)
            is List<*> -> when {
                v.size == 2 && v.all { it is Double } -> PaddingValues(horizontal = (v[0] as Double).dp, vertical = (v[1] as Double).dp)
                v.size == 4 && v.all { it is Double } -> PaddingValues((v[0] as Double).dp, (v[1] as Double).dp, (v[2] as Double).dp, (v[3] as Double).dp)
                else -> d.also { bad(key, "[horizontal, vertical] or [start, top, end, bottom]") }
            }
            is Map<*, *> -> {
                fun n(k: String): Dp? = (v[k] as? Double)?.dp
                val h = n("horizontal"); val vt = n("vertical")
                PaddingValues(
                    start = n("start") ?: h ?: 0.dp,
                    top = n("top") ?: vt ?: 0.dp,
                    end = n("end") ?: h ?: 0.dp,
                    bottom = n("bottom") ?: vt ?: 0.dp,
                )
            }
            else -> d.also { bad(key, "a number, a list or an object") }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun textStyle(key: String, d: TextStyle): TextStyle {
        // Global family first, so a component's own `fontFamily` can still override it.
        val base = fonts?.default?.takeIf { d.fontFamily == null }?.let { d.copy(fontFamily = it) } ?: d
        if (!m.containsKey(key)) return base
        val v = m[key] as? Map<String, Any?> ?: return base.also { bad(key, "an object") }
        val r = PropReader(v, problems, "$where.$key", fonts)
        return base.copy(
            fontSize = if (v.containsKey("fontSize")) r.float("fontSize", 0f).sp else base.fontSize,
            lineHeight = if (v.containsKey("lineHeight")) r.float("lineHeight", 0f).sp else base.lineHeight,
            letterSpacing = if (v.containsKey("letterSpacing")) r.float("letterSpacing", 0f).sp else base.letterSpacing,
            fontWeight = if (v.containsKey("fontWeight")) FontWeight(r.int("fontWeight", 400).coerceIn(1, 1000)) else base.fontWeight,
            fontStyle = when (v["fontStyle"]) {
                "italic" -> FontStyle.Italic
                "normal" -> FontStyle.Normal
                else -> base.fontStyle
            },
            color = (v["color"] as? Color) ?: base.color,
            shadow = if (v.containsKey("shadow")) shadowOf(v["shadow"]) else base.shadow,
            textDecoration = when (v["textDecoration"]) {
                "underline" -> TextDecoration.Underline
                "line-through" -> TextDecoration.LineThrough
                "none" -> TextDecoration.None
                else -> base.textDecoration
            },
            fontFamily = (v["fontFamily"] as? String)?.let { name ->
                fonts?.let { resolveFontFamily(name, it.assets) }
                    ?: base.fontFamily.also { problems += "$where.$key.fontFamily: unknown font family \"$name\" (declare it in assets.fonts)" }
            } ?: base.fontFamily,
        )
    }
}

/** `{"color", "offsetX", "offsetY", "blurRadius"}` (px) for a text shadow; null/invalid clears it. */
private fun shadowOf(v: Any?): Shadow? {
    val m = v as? Map<*, *> ?: return null
    val color = m["color"] as? Color ?: return null
    return Shadow(
        color = color,
        offset = Offset(((m["offsetX"] as? Double) ?: 0.0).toFloat(), ((m["offsetY"] as? Double) ?: 0.0).toFloat()),
        blurRadius = ((m["blurRadius"] as? Double) ?: 0.0).toFloat().coerceAtLeast(0f),
    )
}

/** What [PropReader] needs to resolve `fontFamily` names. */
internal class FontContext(val assets: ThemeAssets, val default: FontFamily?, val problems: MutableSet<String>)
