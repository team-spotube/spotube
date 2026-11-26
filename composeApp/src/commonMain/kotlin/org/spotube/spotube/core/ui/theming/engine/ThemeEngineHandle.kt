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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

/**
 * What a subtree needs to recompile the theme with item-specific `ctx.item.*` values.
 *
 * Only themes that mention `ctx.item` pay for this: [usesItemContext] is false otherwise and
 * [ThemeItemScope] is a plain pass-through.
 */
@Immutable
class ThemeEngineHandle(
    val usesItemContext: Boolean,
    val compile: (Map<String, Any?>) -> CompiledTheme,
    val install: @Composable (CompiledTheme, @Composable () -> Unit) -> Unit,
)

val LocalThemeEngine = compositionLocalOf<ThemeEngineHandle?> { null }

/**
 * Scopes per-item state to [content] so a theme can style one row differently, for example
 * `{"when": {"var": "ctx.item.current"}, "set": {...}}` on `listTile`.
 *
 * Wrap list rows with it and pass plain facts: `mapOf("current" to true, "liked" to false)`.
 */
@Composable
fun ThemeItemScope(item: Map<String, Any?>, content: @Composable () -> Unit) {
    val engine = LocalThemeEngine.current
    if (engine == null || !engine.usesItemContext) {
        content()
        return
    }
    val compiled = remember(engine, item) { engine.compile(item) }
    engine.install(compiled, content)
}
