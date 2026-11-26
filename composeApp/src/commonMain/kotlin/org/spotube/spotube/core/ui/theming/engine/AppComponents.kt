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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import org.spotube.millennium.theme.MillenniumColors
import org.spotube.millennium.theme.millenniumBevelBrush
import org.spotube.millennium.theme.millenniumPanelBrush
import org.spotube.millennium.theme.MillenniumLightColors
import org.spotube.spotube.core.ui.theming.AppDefaultShapes
import org.spotube.spotube.core.ui.theming.AppDefaultTypography
import org.spotube.spotube.core.ui.theming.AppShapes
import org.spotube.spotube.core.ui.theming.AppTypography

/*
 * Theming for the app's OWN components (cards, players, screens...), as opposed to Millennium's.
 *
 * Millennium components are themed through the library's `*ThemeProperties`. The in-house ones use the same JSON
 * (`components.<name>` with `base` / `states` / `variants` / `rules` / `vars`, same cascade, same value types) but
 * their property classes live in this app, and each styled part is an [AppSlot]:
 *
 *   `components.mediaCard.variants.root.states.hovered.background`  <->  `AppSlots.MediaCardRoot` at [AppState.Hovered]
 *
 * Performance contract:
 *  - Every slot is resolved for ALL seven states when the theme is compiled (off the main thread, once per theme
 *    selection). A composable asks for `slot[state]` and gets an already-built immutable object: no JSON, no
 *    evaluation and no allocation while composing.
 *  - A slot the theme never mentions is shared as its defaults and costs nothing at compile time either.
 *  - Property classes are `@Immutable` data classes, so reading one never forces extra recomposition.
 *  - Only [AppState] changes (hover/press/focus) pick a different precompiled object; a composable animates between
 *    two of them by interpolating numbers/colours (see `lerp` helpers on the props), never by recompiling.
 */

/** Interaction/semantic state a slot is styled for. Names match the JSON `states.<name>` keys. */
enum class AppState {
    Default, Hovered, Focused, Pressed, Disabled, Selected, Error;

    val key: String = name.lowercase()
}

/**
 * What slot defaults may depend on: the theme's effective colours, type scale and corner shapes, plus the app-wide
 * fills ([panel], [bevel]) and whether the theme is [flat] (`tokens.surface.glow: false`). Slot defaults use these
 * instead of calling Millennium's glossy brushes, so a theme that never mentions a slot still gets a matching fill.
 */
@Immutable
class AppThemeBase(
    val colors: MillenniumColors,
    val typography: AppTypography,
    val shapes: AppShapes,
    val flat: Boolean = false,
    val panel: Brush = appPanelBrush(colors, flat, null),
    val bevel: Brush = appBevelBrush(colors, flat, null),
)

/**
 * Fill of the app's own panels, cards and rows (`tokens.app.panel`). Unset, it is Millennium's glossy panel gradient,
 * or the flat surface colour when the theme turned surface glow off.
 */
fun appPanelBrush(colors: MillenniumColors, flat: Boolean, override: Brush?): Brush =
    override ?: if (flat) SolidColor(colors.surface) else millenniumPanelBrush(colors)

/**
 * Fill of small raised pieces: chips, icon boxes, artwork placeholders (`tokens.app.bevel`). Unset, it is Millennium's
 * neutral bevel, or the flat raised colour when the theme turned surface glow off.
 */
fun appBevelBrush(colors: MillenniumColors, flat: Boolean, override: Brush?): Brush =
    override ?: if (flat) SolidColor(colors.raised) else millenniumBevelBrush(colors)

/**
 * One styled part of an in-house component, addressed in JSON as `components.[component]` (with
 * `variants.[variant]` when [variant] is set).
 *
 * [defaults] must reproduce the shipped look exactly for every state, so an unthemed app (and any existing theme)
 * is pixel-identical; [apply] reads the theme's values over it, with the same `PropReader` Millennium props use.
 */
class AppSlot<P : Any> internal constructor(
    val component: String,
    val variant: String?,
    val defaults: (AppThemeBase, AppState) -> P,
    internal val apply: (P, PropReader) -> P,
) {
    /** `component` or `component.variant`, used in problem messages. */
    val path: String get() = if (variant == null) component else "$component.$variant"
}

/** A slot resolved for every [AppState]. */
@Immutable
class AppSlotTheme<P : Any> internal constructor(private val byState: Array<Any>) {
    @Suppress("UNCHECKED_CAST")
    operator fun get(state: AppState): P = byState[state.ordinal] as P

    companion object {
        internal fun <P : Any> of(build: (AppState) -> P): AppSlotTheme<P> =
            AppSlotTheme(Array(AppState.entries.size) { build(AppState.entries[it]) })
    }
}

/** Every slot of the compiled theme. Lookup is a hash probe on an identity key. */
@Immutable
class AppComponentThemes internal constructor(private val slots: Map<AppSlot<*>, AppSlotTheme<*>>) {
    @Suppress("UNCHECKED_CAST")
    operator fun <P : Any> get(slot: AppSlot<P>): AppSlotTheme<P> =
        (slots[slot] ?: error("slot ${slot.path} is not registered in AppSlots.all")) as AppSlotTheme<P>

    companion object {
        /** The app's own look: every slot at its defaults, shared across states where the props don't depend on them. */
        fun defaults(base: AppThemeBase): AppComponentThemes =
            AppComponentThemes(AppSlots.all.associateWith { slot -> slotDefaults(slot, base) })

        private fun <P : Any> slotDefaults(slot: AppSlot<P>, base: AppThemeBase): AppSlotTheme<P> =
            AppSlotTheme.of { slot.defaults(base, it) }
    }
}

/**
 * Compiles every registered slot against the theme document. Called once from [ThemeCompiler.compile].
 *
 * `defaults` of the theme document are deliberately NOT applied to app slots: they are meant for Millennium's
 * uniform property names (`shape`, `borderWidth`...) and would silently restyle every in-house component of an
 * existing theme. A theme opts in by naming the component.
 */
internal fun ThemeCompiler.compileAppComponents(base: AppThemeBase): AppComponentThemes {
    val out = LinkedHashMap<AppSlot<*>, AppSlotTheme<*>>(AppSlots.all.size * 2)
    for (slot in AppSlots.all) out[slot] = compileSlot(slot, base)
    return AppComponentThemes(out)
}

private fun <P : Any> ThemeCompiler.compileSlot(slot: AppSlot<P>, base: AppThemeBase): AppSlotTheme<P> {
    // Untouched component: share the defaults, no evaluation at all.
    if (componentJson(slot.component) == null) return AppSlotTheme.of { slot.defaults(base, it) }
    return AppSlotTheme.of { state ->
        val merged = mergedProps(slot.component, slot.variant, state.key, useDefaults = false)
        val where = "components.${slot.path}"
        val reader = PropReader(merged.values, problems, "$where.${state.key}", fontContext)
        slot.apply(slot.defaults(base, state), reader).also { reportUnknown(where, reader, merged) }
    }
}

/** The look of an app with no theme; the `LocalAppComponents` default so previews and tests need no setup. */
val DefaultAppThemeBase = AppThemeBase(MillenniumLightColors, AppDefaultTypography, AppDefaultShapes)

/** Props of the active theme for in-house components; installed by `SpotubeTheme`. */
val LocalAppComponents = staticCompositionLocalOf { AppComponentThemes.defaults(DefaultAppThemeBase) }

/** Reads precompiled slot props: `AppComponents[AppSlots.MediaCardRoot][AppState.Hovered]`. */
object AppComponents {
    val current: AppComponentThemes @Composable get() = LocalAppComponents.current

    @Composable
    fun <P : Any> slot(slot: AppSlot<P>): AppSlotTheme<P> = LocalAppComponents.current[slot]
}
