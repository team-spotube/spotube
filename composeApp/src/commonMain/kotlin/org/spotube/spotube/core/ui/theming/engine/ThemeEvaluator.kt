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
import androidx.compose.ui.graphics.lerp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

/** Thrown when a theme needs more evaluation work than [ThemeLimits.MAX_EVAL_STEPS] (a DoS guard). */
internal class ThemeBudgetExceeded : RuntimeException("Theme is too complex to evaluate")

/** Variable lookup chain: a component's own `vars` shadow the global ones. */
internal class ThemeScope(val decls: Map<String, ThemeVarDecl>, val parent: ThemeScope?, val overridable: Boolean = false) {
    fun find(name: String): Pair<ThemeVarDecl, ThemeScope>? =
        decls[name]?.let { it to this } ?: parent?.find(name)
}

/**
 * Evaluates the JSON value language to plain Kotlin values (Double, Boolean, String, Color,
 * GradientSpec, GelSpec, List, Map or null).
 *
 * References are whole-string `"{namespace.path}"`: `tokens.*` (design tokens), `vars.*`
 * (declared variables), `palette.*` (the built-in colour roles) and `ctx.*` (live app state, plus
 * `ctx.state` / `ctx.variant` while a component is being compiled). Arrays whose first element is a
 * function name are calls. Nothing here can run code, and depth is bounded.
 */
internal class ThemeEvaluator(
    private val doc: ThemeDocument,
    /** The user's chosen parameter values, by parameter name (already stripped of any theme-id prefix). */
    private val userParams: Map<String, String>,
    private val dark: Boolean,
    /** Read-only namespaces such as `ctx` and `palette`. */
    private val namespaces: Map<String, Any?>,
    private val problems: MutableSet<String>,
) {
    val globalScope = ThemeScope(doc.vars, null)

    /** Parameters are the only declarations a user's value may replace. */
    private val paramScope = ThemeScope(doc.parameters, null, overridable = true)

    /** Set by the compiler while it works on one component/variant/state. */
    var component: Map<String, Any?> = emptyMap()

    private val resolvingVars = HashSet<ThemeVarDecl>()
    private val resolvingTokens = HashSet<String>()
    private var depth = 0
    private var steps = 0

    fun eval(el: JsonElement, scope: ThemeScope = globalScope): Any? {
        if (depth > MAX_DEPTH) {
            problems += "Theme expression nests deeper than $MAX_DEPTH levels"
            return null
        }
        if (++steps > ThemeLimits.MAX_EVAL_STEPS) throw ThemeBudgetExceeded()
        depth++
        try {
            return when (el) {
                is JsonNull -> null
                is JsonPrimitive -> evalPrimitive(el, scope)
                is JsonArray -> evalArray(el, scope)
                is JsonObject -> evalObject(el, scope)
            }
        } finally {
            depth--
        }
    }

    private fun evalPrimitive(p: JsonPrimitive, scope: ThemeScope): Any? {
        if (p.isString) {
            val s = p.content
            if (s.length > 2 && s.startsWith("{") && s.endsWith("}") && s.indexOf('{', 1) < 0) {
                return lookup(s.substring(1, s.length - 1).trim(), scope)
            }
            if (s.startsWith("#")) {
                return parseHexColor(s) ?: run {
                    problems += "Invalid colour \"$s\""
                    null
                }
            }
            return s
        }
        p.booleanOrNull?.let { return it }
        return p.doubleOrNull
    }

    private fun evalObject(o: JsonObject, scope: ThemeScope): Any? {
        // {"light": x, "dark": y}: pick by the active appearance.
        if (o.isNotEmpty() && o.keys.all { it == "light" || it == "dark" }) {
            val pick = o[if (dark) "dark" else "light"] ?: o[if (dark) "light" else "dark"]
            return pick?.let { eval(it, scope) }
        }
        val out = LinkedHashMap<String, Any?>()
        for ((k, v) in o) out[k] = eval(v, scope)
        return out
    }

    private fun evalArray(a: JsonArray, scope: ThemeScope): Any? {
        val head = (a.firstOrNull() as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (head != null && head in FUNCTIONS) return call(head, a.drop(1), scope)
        return a.map { eval(it, scope) }
    }

    // ---- references -------------------------------------------------------------------------

    private fun lookup(path: String, scope: ThemeScope): Any? {
        val parts = path.split('.')
        return when (parts[0]) {
            "tokens" -> token(parts.drop(1))
            "vars" -> variable(parts.drop(1).joinToString("."), scope)
            "params" -> variable(parts.drop(1).joinToString("."), paramScope)
            "ctx" -> {
                val rest = parts.drop(1)
                if (rest.size == 1 && rest[0] in component) component[rest[0]] else walk(namespaces["ctx"], rest, path, quiet = true)
            }
            else -> walk(namespaces[parts[0]], parts.drop(1), path)
        }
    }

    /** [quiet]: a missing value is normal (live context is optional) and just reads as null. */
    private fun walk(root: Any?, path: List<String>, full: String, quiet: Boolean = false): Any? {
        var cur: Any? = root
        for (seg in path) {
            cur = (cur as? Map<*, *>)?.get(seg)
            if (cur == null) {
                if (!quiet) problems += "Unknown reference {$full}"
                return null
            }
        }
        return cur
    }

    private fun token(path: List<String>): Any? {
        val full = path.joinToString(".")
        var node: JsonElement = doc.tokens
        for (seg in path) {
            node = (node as? JsonObject)?.get(seg) ?: run {
                problems += "Unknown token {tokens.$full}"
                return null
            }
        }
        if (!resolvingTokens.add(full)) {
            problems += "Token {tokens.$full} refers to itself"
            return null
        }
        try {
            return eval(node, globalScope)
        } finally {
            resolvingTokens.remove(full)
        }
    }

    private fun variable(name: String, scope: ThemeScope): Any? {
        val (decl, declScope) = scope.find(name) ?: run {
            problems += "Unknown variable {vars.$name}"
            return null
        }
        if (!resolvingVars.add(decl)) {
            problems += "Variable \"$name\" depends on itself"
            return null
        }
        try {
            decl.value?.let { return coerce(decl, eval(it, declScope)) }
            val override = if (declScope.overridable) userParams[decl.name] else null
            override?.let { raw -> fromUserString(decl, raw)?.let { return it } }
            return coerce(decl, decl.default?.let { eval(it, declScope) })
        } finally {
            resolvingVars.remove(decl)
        }
    }

    private fun fromUserString(d: ThemeVarDecl, raw: String): Any? = when (d.type) {
        ThemeVarType.Color -> parseHexColor(raw)
        ThemeVarType.Number -> raw.toDoubleOrNull()?.let { clamp(d, it) }
        ThemeVarType.Bool -> raw.toBooleanStrictOrNull()
        ThemeVarType.String -> raw
        ThemeVarType.Enum -> raw.takeIf { it in d.options }
    }

    private fun clamp(d: ThemeVarDecl, v: Double): Double =
        v.coerceIn(d.min ?: Double.NEGATIVE_INFINITY, d.max ?: Double.POSITIVE_INFINITY)

    private fun coerce(d: ThemeVarDecl, v: Any?): Any? {
        val ok = when (d.type) {
            ThemeVarType.Color -> v is Color
            ThemeVarType.Number -> v is Double
            ThemeVarType.Bool -> v is Boolean
            ThemeVarType.String -> v is String
            ThemeVarType.Enum -> v is String && v in d.options
        }
        if (!ok) {
            problems += "Variable \"${d.name}\" has a value that is not a valid ${d.type.name.lowercase()}"
            return null
        }
        return if (v is Double) clamp(d, v) else v
    }

    // ---- functions --------------------------------------------------------------------------

    private fun call(name: String, args: List<JsonElement>, scope: ThemeScope): Any? {
        fun arg(i: Int): Any? = args.getOrNull(i)?.let { eval(it, scope) }
        fun num(i: Int): Double? = (arg(i) as? Double)
        fun col(i: Int): Color? = arg(i) as? Color
        fun bad(): Any? {
            problems += "Bad arguments for [\"$name\", ...]"
            return null
        }
        return when (name) {
            "if" -> {
                val c = args.getOrNull(0) ?: return bad()
                val branch = if (truthy(c, scope)) args.getOrNull(1) else args.getOrNull(2)
                branch?.let { eval(it, scope) }
            }
            "match" -> {
                val key = arg(0)?.let(::keyOf)
                val table = args.getOrNull(1) as? JsonObject ?: return bad()
                (table[key] ?: table["default"])?.let { eval(it, scope) }
            }
            "alpha" -> {
                val c = col(0); val a = num(1)
                if (c == null || a == null) bad() else c.copy(alpha = a.toFloat().coerceIn(0f, 1f))
            }
            "lighten" -> {
                val c = col(0); val t = num(1)
                if (c == null || t == null) bad() else lerp(c, Color.White, t.toFloat().coerceIn(0f, 1f)).copy(alpha = c.alpha)
            }
            "darken" -> {
                val c = col(0); val t = num(1)
                if (c == null || t == null) bad() else lerp(c, Color.Black, t.toFloat().coerceIn(0f, 1f)).copy(alpha = c.alpha)
            }
            "mix" -> {
                val a = col(0); val b = col(1); val t = num(2) ?: 0.5
                if (a == null || b == null) bad() else lerp(a, b, t.toFloat().coerceIn(0f, 1f))
            }
            "gradient" -> stopsOf(args, 0, scope)?.let { BrushSpec.Linear(180f, it) } ?: bad()
            "linear" -> {
                val angle = num(0)
                val stops = stopsOf(args, 1, scope)
                if (angle == null || stops == null) bad() else BrushSpec.Linear(angle.toFloat(), stops)
            }
            "radial" -> {
                val cx = num(0); val cy = num(1); val r = num(2)
                val stops = stopsOf(args, 3, scope)
                if (cx == null || cy == null || r == null || stops == null) bad()
                else BrushSpec.Radial(cx.toFloat(), cy.toFloat(), r.toFloat(), stops)
            }
            "sweep" -> {
                val cx = num(0); val cy = num(1)
                val stops = stopsOf(args, 2, scope)
                if (cx == null || cy == null || stops == null) bad() else BrushSpec.Sweep(cx.toFloat(), cy.toFloat(), stops)
            }
            "gel" -> {
                val top = col(0); val bottom = col(1)
                if (top == null || bottom == null) bad() else BrushSpec.Gel(top, bottom)
            }
            "add", "sub", "mul", "div", "min", "max" -> {
                val a = num(0); val b = num(1)
                if (a == null || b == null) bad() else when (name) {
                    "add" -> a + b
                    "sub" -> a - b
                    "mul" -> a * b
                    "div" -> if (b == 0.0) bad() else a / b
                    "min" -> minOf(a, b)
                    else -> maxOf(a, b)
                }
            }
            "clamp" -> {
                val v = num(0); val lo = num(1); val hi = num(2)
                if (v == null || lo == null || hi == null || lo > hi) bad() else v.coerceIn(lo, hi)
            }
            else -> bad()
        }
    }

    /** Stops from [from] on: each a colour or `[colour, position]`; at least two. */
    private fun stopsOf(args: List<JsonElement>, from: Int, scope: ThemeScope): List<BrushStop>? {
        val stops = args.drop(from).map { el ->
            when (val v = eval(el, scope)) {
                is Color -> BrushStop(v)
                is List<*> -> {
                    val c = v.getOrNull(0) as? Color
                    val p = (v.getOrNull(1) as? Double)?.toFloat()
                    if (c == null) return null else BrushStop(c, p)
                }
                else -> return null
            }
        }
        return stops.takeIf { it.size >= 2 }
    }

    private fun keyOf(v: Any): String = when (v) {
        is Double -> if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
        else -> v.toString()
    }

    // ---- conditions -------------------------------------------------------------------------

    /** Truthiness of [el]: an object is a condition, anything else is evaluated and tested. */
    fun truthy(el: JsonElement, scope: ThemeScope = globalScope): Boolean =
        if (el is JsonObject) condition(el, scope) else truthy(eval(el, scope))

    private fun truthy(v: Any?): Boolean = when (v) {
        null -> false
        is Boolean -> v
        is Double -> v != 0.0
        is String -> v.isNotEmpty()
        else -> true
    }

    /** Every key must hold (implicit AND). */
    fun condition(c: JsonObject, scope: ThemeScope = globalScope): Boolean {
        for ((key, v) in c) {
            val ok = when (key) {
                "all" -> (v as? JsonArray)?.all { truthy(it, scope) } ?: unknownCond(key)
                "any" -> (v as? JsonArray)?.any { truthy(it, scope) } ?: unknownCond(key)
                "not" -> !truthy(v, scope)
                "state" -> matchesNames(v, component["state"])
                "variant" -> matchesNames(v, component["variant"])
                "var" -> compare(c, scope)
                "eq", "ne", "in", "gt", "gte", "lt", "lte" -> true // consumed by "var"
                else -> unknownCond(key)
            }
            if (!ok) return false
        }
        return true
    }

    private fun unknownCond(key: String): Boolean {
        problems += "Unknown or malformed condition \"$key\""
        return false
    }

    private fun matchesNames(v: JsonElement, actual: Any?): Boolean {
        val want = when (v) {
            is JsonArray -> v.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            is JsonPrimitive -> listOfNotNull(v.contentOrNull)
            else -> emptyList()
        }
        return actual is String && want.any { it.equals(actual, ignoreCase = true) }
    }

    private fun compare(c: JsonObject, scope: ThemeScope): Boolean {
        val ref = (c["var"] as? JsonPrimitive)?.contentOrNull ?: return unknownCond("var")
        val path = ref.removePrefix("{").removeSuffix("}").trim()
        val actual = lookup(path, scope)
        var any = false
        var ok = true
        fun operand(k: String): Any? = c[k]?.let { eval(it, scope) }
        c["eq"]?.let { any = true; ok = ok && looseEquals(actual, operand("eq")) }
        c["ne"]?.let { any = true; ok = ok && !looseEquals(actual, operand("ne")) }
        c["in"]?.let {
            any = true
            val list = operand("in") as? List<*> ?: emptyList<Any?>()
            ok = ok && list.any { looseEquals(actual, it) }
        }
        for ((k, test) in listOf<Pair<String, (Double, Double) -> Boolean>>(
            "gt" to { a, b -> a > b }, "gte" to { a, b -> a >= b },
            "lt" to { a, b -> a < b }, "lte" to { a, b -> a <= b },
        )) {
            if (c[k] == null) continue
            any = true
            val a = actual as? Double
            val b = operand(k) as? Double
            ok = ok && a != null && b != null && test(a, b)
        }
        return if (any) ok else truthy(actual)
    }

    private fun looseEquals(a: Any?, b: Any?): Boolean = when {
        a is Double && b is Double -> a == b
        a is String && b is String -> a.equals(b, ignoreCase = true)
        else -> a == b
    }

    companion object {
        const val MAX_DEPTH = 48
        val FUNCTIONS = setOf(
            "if", "match", "alpha", "lighten", "darken", "mix", "gradient", "linear", "radial", "sweep", "gel",
            "add", "sub", "mul", "div", "min", "max", "clamp",
        )
    }
}
