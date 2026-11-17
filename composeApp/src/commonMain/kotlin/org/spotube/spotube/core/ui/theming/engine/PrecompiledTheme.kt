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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import org.spotube.millennium.theme.MillenniumColorSchemes
import org.spotube.millennium.theme.MillenniumDarkThemeComponents
import org.spotube.millennium.theme.MillenniumLightThemeComponents
import org.spotube.millennium.theme.MillenniumPalette
import org.spotube.millennium.theme.MillenniumThemeComponents
import org.spotube.millennium.theme.resolve
import org.spotube.spotube.core.ui.theming.compactModalBottomSheetTheme
import org.spotube.spotube.modules.settings.UserSettingsSource

/** The app's own tweaks on top of Millennium's stock components for one appearance. */
internal fun appBaseComponents(dark: Boolean): MillenniumThemeComponents =
    (if (dark) MillenniumDarkThemeComponents else MillenniumLightThemeComponents).let { base ->
        base.copy(
            topAppBarTheme = org.spotube.millennium.composables.MillenniumTopAppBarTheme(
                properties = base.topAppBarTheme.properties.copy(separatorColor = androidx.compose.ui.graphics.Color.Transparent)
            ),
            modalBottomSheetTheme = compactModalBottomSheetTheme(),
        )
    }

/**
 * A theme compiled once, for both appearances, when the selection (theme or palette) changes, never during
 * composition. [SpotubeTheme] only picks the right half.
 *
 * Themes that read live context (`ctx.player.*`, `ctx.env.compact`) report [needsLiveContext] and are re-evaluated by
 * `SpotubeTheme` when that context changes; every other theme never recompiles until it is replaced. Per-row
 * `ctx.item.*` variants are compiled on demand and cached ([forItem]).
 */
@Immutable
class PrecompiledTheme internal constructor(
    val loaded: LoadedTheme?,
    val palette: MillenniumPalette,
    private val light: CompiledTheme,
    private val dark: CompiledTheme,
) {
    val needsLiveContext: Boolean get() = loaded?.usesLiveContext == true
    val usesItemContext: Boolean get() = loaded?.usesItemContext == true

    fun forAppearance(isDark: Boolean): CompiledTheme = if (isDark) dark else light

    /** Compiles with live facts (`ctx.*`); only used by themes that [needsLiveContext] or [usesItemContext]. */
    fun compileWith(isDark: Boolean, compact: Boolean, live: Map<String, Any?>, item: Map<String, Any?> = emptyMap()): CompiledTheme =
        compile(loaded, palette, isDark, ThemeEnv(isDark, compact, if (item.isEmpty()) live else live + ("item" to item)))

    private val itemCache = object : LinkedHashMap<Pair<Boolean, Map<String, Any?>>, CompiledTheme>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<Boolean, Map<String, Any?>>, CompiledTheme>) = size > 24
    }

    /** Per-row variant for `ctx.item.*`, memoized: rows with the same facts share one compiled theme. */
    fun forItem(isDark: Boolean, item: Map<String, Any?>, live: Map<String, Any?>, compact: Boolean): CompiledTheme =
        synchronized(itemCache) {
            itemCache.getOrPut(isDark to (item + live + ("compact" to compact))) { compileWith(isDark, compact, live, item) }
        }

    companion object {
        private fun compile(loaded: LoadedTheme?, palette: MillenniumPalette, isDark: Boolean, env: ThemeEnv) = compileTheme(
            doc = loaded?.doc,
            assets = loaded?.assets ?: ThemeAssets.Empty,
            env = env,
            baseColors = MillenniumColorSchemes().resolve(palette, isDark),
            baseComponents = appBaseComponents(isDark),
            parseProblems = loaded?.problems.orEmpty(),
        )

        /** Compiles [loaded] (or the plain Millennium look for null) for [palette], light and dark. */
        fun create(loaded: LoadedTheme?, palette: MillenniumPalette): PrecompiledTheme = PrecompiledTheme(
            loaded, palette,
            light = compile(loaded, palette, false, ThemeEnv(dark = false, compact = false)),
            dark = compile(loaded, palette, true, ThemeEnv(dark = true, compact = false)),
        )
    }
}

/**
 * Owns the application of the selected theme: compiles it when the selection (theme id, palette or the installed
 * set) changes, off the main thread, and publishes the result. Nothing is compiled per recomposition or per screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ThemeRuntime(settings: UserSettingsSource, themes: ThemePackageRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val precompiled: StateFlow<PrecompiledTheme> = combine(
        settings.userSettings.map { it.selectedThemeId to it.palette }.distinctUntilChanged(),
        themes.installed,
    ) { (id, palette), installed -> themes.themeFor(id, installed) to palette }
        .distinctUntilChanged { a, b -> a.first === b.first && a.second == b.second }
        .mapLatest { (loaded, palette) -> PrecompiledTheme.create(loaded, palette) }
        .stateIn(scope, SharingStarted.Eagerly, PrecompiledTheme.create(null, settings.userSettings.value.palette))
}
