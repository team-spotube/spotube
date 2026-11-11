package org.spotube.millennium.theme

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import com.composeunstyled.LocalTextStyle
import com.composeunstyled.LocalContentColor
import com.composeunstyled.ProvideWindowHeightBreakpoints
import com.composeunstyled.ProvideWindowWidthBreakpoints
import com.composeunstyled.theme.ColorScheme
import com.composeunstyled.theme.ColoredIndication
import com.composeunstyled.theme.ThemeProperty
import com.composeunstyled.theme.ThemeToken
import com.composeunstyled.theme.buildThemeV2
import org.spotube.millennium.adaptive.heightBreakpoints
import org.spotube.millennium.adaptive.widthBreakpoints
import org.spotube.millennium.composables.*

/**
 * Global semantic palette roles for one appearance (a light or a dark variant of one accent family).
 *
 * There is exactly one [MillenniumColors] active at a time; read it with [MillenniumTheme.colors].
 * Component themes resolve against it and never store a copy, so swapping the palette or toggling
 * dark mode re-colours every component without touching component themes.
 *
 * Surface roles form an elevation ladder: [background] (page) < [surface] (panels, cards)
 * < [raised] (top edge of raised controls) with [inset] used for recessed areas (fields, tracks).
 *
 * ```kotlin
 * @Composable
 * fun Banner() {
 *     val c = MillenniumTheme.colors
 *     Box(Modifier.background(c.surface).border(1.dp, c.border).padding(12.dp)) {
 *         Text("Heads up", color = c.text)
 *     }
 * }
 * ```
 *
 * @property background Page/canvas colour behind everything else (the lowest surface).
 * @property surface Default fill for panels, cards, menus and sheets; text on it uses [text]/[muted].
 * @property raised Brightest neutral; the top stop of raised neutral gradients (buttons, cards).
 * @property inset Recessed neutral for wells such as text fields, slider tracks and sunken gradients.
 * @property text Primary foreground for body copy on [background], [surface], [raised] and [inset].
 * @property muted Secondary foreground (captions, descriptions, placeholders), contrast-tested on the
 *   neutral surfaces.
 * @property border Hairline outline colour for controls and panels.
 * @property highlight Specular colour for inner rims and bevel highlights (white in light themes, a
 *   mid zinc in dark themes).
 * @property accent Flat accent used where a single colour is needed (focus rings, links, tinted
 *   washes and hover/press indication). Not a gel endpoint.
 * @property accentTop Top (bright cap) endpoint of the accent gel; see [gelBrush].
 * @property accentBottom Bottom (deep lip) endpoint of the accent gel; see [gelBrush].
 * @property onAccent Label/icon colour for content drawn on the accent gel. Tests guarantee WCAG 4.5:1
 *   contrast against every gel stop between [accentTop] and [accentBottom].
 * @property success Semantic positive foreground (confirmation, valid state).
 * @property danger Semantic destructive/error foreground and the base of danger controls.
 * @property scrim Translucent overlay painted behind modal surfaces (dialogs, drawers, sheets).
 * @see MillenniumColorSchemes
 * @see MillenniumPalette
 */
@Immutable
data class MillenniumColors(
    val background: Color,
    val surface: Color,
    val raised: Color,
    val inset: Color,
    val text: Color,
    val muted: Color,
    val border: Color,
    val highlight: Color,
    val accent: Color,
    val accentTop: Color,
    val accentBottom: Color,
    val onAccent: Color,
    val success: Color,
    val danger: Color,
    val scrim: Color,
)

/**
 * Default [MillenniumPalette.Blue] colours for light appearances: pearl blue-grey surfaces, navy text
 * and a jewel-blue gel accent.
 *
 * @see MillenniumColorSchemes.light
 */
val MillenniumLightColors = MillenniumColors(
    background = Color(0xFFE4EDF3), surface = Color(0xFFF4F8FB), raised = Color.White,
    inset = Color(0xFFDCE6ED), text = Color(0xFF172C42), muted = Color(0xFF4A6075),
    border = Color(0xFF98ADBF), highlight = Color.White, accent = Color(0xFF005CB9),
    accentTop = Color(0xFF1675B8), accentBottom = Color(0xFF003B91), onAccent = Color.White,
    success = Color(0xFF257B51), danger = Color(0xFFBC334A), scrim = Color(0x99101E30),
)

/**
 * Default [MillenniumPalette.Blue] colours for dark appearances: neutral zinc surfaces, near-white
 * text and an aqua-blue accent over a deep blue gel.
 *
 * @see MillenniumColorSchemes.dark
 */
val MillenniumDarkColors = MillenniumColors(
    background = Color(0xFF09090B), surface = Color(0xFF18181B), raised = Color(0xFF27272A),
    inset = Color(0xFF111113), text = Color(0xFFFAFAFA), muted = Color(0xFFA1A1AA),
    border = Color(0xFF3F3F46), highlight = Color(0xFF71717A), accent = Color(0xFF79CFFF),
    accentTop = Color(0xFF1976B9), accentBottom = Color(0xFF002D74), onAccent = Color.White,
    success = Color(0xFF7AE2AE), danger = Color(0xFFFF98A9), scrim = Color(0xBB030912),
)

/**
 * [MillenniumPalette.Orange] colours for light appearances: untinted white/zinc-50 surfaces with a
 * bright apricot gel ([MillenniumColors.onAccent] is a dark brown for contrast on the light gel).
 *
 * @see MillenniumColorSchemes.orangeLight
 */
val MillenniumOrangeLightColors = MillenniumColors(
    background = Color(0xFFFAFAFA), surface = Color.White, raised = Color.White,
    inset = Color(0xFFF4F4F5), text = Color(0xFF27272A), muted = Color(0xFF52525B),
    border = Color(0xFFD4D4D8), highlight = Color.White, accent = Color(0xFF9A3412),
    accentTop = Color(0xFFFDBA74), accentBottom = Color(0xFFD97706), onAccent = Color(0xFF431407),
    success = Color(0xFF166534), danger = Color(0xFFB42332), scrim = Color(0x990F0906),
)

/**
 * [MillenniumPalette.Orange] colours for dark appearances: the same zinc surfaces as the blue dark
 * palette so only the accent family changes.
 *
 * @see MillenniumColorSchemes.orangeDark
 */
val MillenniumOrangeDarkColors = MillenniumColors(
    background = Color(0xFF09090B), surface = Color(0xFF18181B), raised = Color(0xFF27272A),
    inset = Color(0xFF111113), text = Color(0xFFFAFAFA), muted = Color(0xFFA1A1AA),
    border = Color(0xFF3F3F46), highlight = Color(0xFF71717A), accent = Color(0xFFFB923C),
    accentTop = Color(0xFFFDBA74), accentBottom = Color(0xFFF97316), onAccent = Color(0xFF431407),
    success = Color(0xFF86EFAC), danger = Color(0xFFFF8A80), scrim = Color(0xCC050505),
)

/**
 * [MillenniumPalette.Lime] colours for light appearances: untinted white/zinc-50 surfaces with a
 * bright citron gel and a dark green [MillenniumColors.onAccent].
 *
 * @see MillenniumColorSchemes.limeLight
 */
val MillenniumLimeLightColors = MillenniumColors(
    background = Color(0xFFFAFAFA), surface = Color.White, raised = Color.White,
    inset = Color(0xFFF4F4F5), text = Color(0xFF27272A), muted = Color(0xFF52525B),
    border = Color(0xFFD4D4D8), highlight = Color.White, accent = Color(0xFF3F6212),
    accentTop = Color(0xFFBEF264), accentBottom = Color(0xFF84CC16), onAccent = Color(0xFF1A2E05),
    success = Color(0xFF3F6212), danger = Color(0xFFB42332), scrim = Color(0x99080A06),
)

/**
 * [MillenniumPalette.Lime] colours for dark appearances: chartreuse gel highlights on the shared
 * neutral zinc surfaces.
 *
 * @see MillenniumColorSchemes.limeDark
 */
val MillenniumLimeDarkColors = MillenniumColors(
    background = Color(0xFF09090B), surface = Color(0xFF18181B), raised = Color(0xFF27272A),
    inset = Color(0xFF111113), text = Color(0xFFFAFAFA), muted = Color(0xFFA1A1AA),
    border = Color(0xFF3F3F46), highlight = Color(0xFF71717A), accent = Color(0xFFA3E635),
    accentTop = Color(0xFFBEF264), accentBottom = Color(0xFF84CC16), onAccent = Color(0xFF1A2E05),
    success = Color(0xFFA3E635), danger = Color(0xFFFF8A80), scrim = Color(0xCC050505),
)

/**
 * The single home of every palette role set: one light and one dark [MillenniumColors] per
 * [MillenniumPalette] family. [resolve] picks the active one.
 *
 * Replace individual role sets to re-skin a family without touching component themes.
 *
 * ```kotlin
 * val config = MillenniumThemeConfig(
 *     colorSchemes = MillenniumColorSchemes(
 *         light = MillenniumLightColors.copy(accent = Color(0xFF0B6BCB)),
 *     ),
 * )
 * ```
 *
 * @property light Role set for [MillenniumPalette.Blue] in light mode.
 * @property dark Role set for [MillenniumPalette.Blue] in dark mode.
 * @property orangeLight Role set for [MillenniumPalette.Orange] in light mode.
 * @property orangeDark Role set for [MillenniumPalette.Orange] in dark mode.
 * @property limeLight Role set for [MillenniumPalette.Lime] in light mode.
 * @property limeDark Role set for [MillenniumPalette.Lime] in dark mode.
 * @see MillenniumThemeConfig.colorSchemes
 */
@Immutable
data class MillenniumColorSchemes(
    val light: MillenniumColors = MillenniumLightColors,
    val dark: MillenniumColors = MillenniumDarkColors,
    val orangeLight: MillenniumColors = MillenniumOrangeLightColors,
    val orangeDark: MillenniumColors = MillenniumOrangeDarkColors,
    val limeLight: MillenniumColors = MillenniumLimeLightColors,
    val limeDark: MillenniumColors = MillenniumLimeDarkColors,
)

/**
 * Accent families that can be swapped without changing component themes. Select one with
 * [MillenniumThemeConfig.palette].
 *
 * - [Blue]: the default pearl-blue / jewel-blue family.
 * - [Orange]: neutral surfaces with an apricot gel.
 * - [Lime]: neutral surfaces with a citron gel.
 */
enum class MillenniumPalette {
    /** Default family: blue-grey pearl surfaces in light mode, zinc in dark, blue gel accent. */
    Blue,

    /** Neutral white/zinc surfaces with a bright apricot/orange gel accent. */
    Orange,

    /** Neutral white/zinc surfaces with a bright citron/lime gel accent. */
    Lime,
}

/**
 * Selects the role set for an accent family and appearance.
 *
 * ```kotlin
 * val colors = MillenniumColorSchemes().resolve(MillenniumPalette.Lime, dark = true)
 * ```
 *
 * @param palette Accent family to use.
 * @param dark `true` for the dark role set, `false` for light.
 * @return the matching [MillenniumColors] from this scheme set.
 */
fun MillenniumColorSchemes.resolve(palette: MillenniumPalette, dark: Boolean): MillenniumColors =
    when (palette) {
        MillenniumPalette.Blue -> if (dark) this.dark else light
        MillenniumPalette.Orange -> if (dark) orangeDark else orangeLight
        MillenniumPalette.Lime -> if (dark) limeDark else limeLight
    }

/**
 * The complete set of component-owned themes for one appearance (all of light, or all of dark).
 *
 * Each component theme owns its own dimensions, spacing, typography, shapes, motion and per-state
 * visuals, and is built from [MillenniumTheme.colors] rather than storing a palette. Customise one
 * component by `copy`-ing this class and swapping a single theme.
 *
 * ```kotlin
 * val base = defaultMillenniumThemeComponents()
 * val custom = base.copy(buttonTheme = base.buttonTheme.copy())
 * ```
 *
 * @property textTheme Theme for [MillenniumTextTheme]; read it with [MillenniumTheme.textTheme].
 * @property buttonTheme Theme for [MillenniumButtonTheme]; read it with [MillenniumTheme.buttonTheme].
 * @property cardTheme Theme for [MillenniumCardTheme]; read it with [MillenniumTheme.cardTheme].
 * @property checkboxTheme Theme for [MillenniumCheckboxTheme]; read it with [MillenniumTheme.checkboxTheme].
 * @property triStateCheckboxTheme Theme for [MillenniumTriStateCheckboxTheme]; read it with [MillenniumTheme.triStateCheckboxTheme].
 * @property toggleSwitchTheme Theme for [MillenniumToggleSwitchTheme]; read it with [MillenniumTheme.toggleSwitchTheme].
 * @property textFieldTheme Theme for [MillenniumTextFieldTheme]; read it with [MillenniumTheme.textFieldTheme].
 * @property sliderTheme Theme for [MillenniumSliderTheme]; read it with [MillenniumTheme.sliderTheme].
 * @property radioGroupTheme Theme for [MillenniumRadioGroupTheme]; read it with [MillenniumTheme.radioGroupTheme].
 * @property tabGroupTheme Theme for [MillenniumTabGroupTheme]; read it with [MillenniumTheme.tabGroupTheme].
 * @property avatarTheme Theme for [MillenniumAvatarTheme]; read it with [MillenniumTheme.avatarTheme].
 * @property iconTheme Theme for [MillenniumIconTheme]; read it with [MillenniumTheme.iconTheme].
 * @property disclosureTheme Theme for [MillenniumDisclosureTheme]; read it with [MillenniumTheme.disclosureTheme].
 * @property dialogTheme Theme for [MillenniumDialogTheme]; read it with [MillenniumTheme.dialogTheme].
 * @property dropdownMenuTheme Theme for [MillenniumDropdownMenuTheme]; read it with [MillenniumTheme.dropdownMenuTheme].
 * @property tooltipTheme Theme for [MillenniumTooltipTheme]; read it with [MillenniumTheme.tooltipTheme].
 * @property progressTheme Theme for [MillenniumProgressTheme]; read it with [MillenniumTheme.progressTheme].
 * @property loadingTheme Theme for [MillenniumLoadingTheme]; read it with [MillenniumTheme.loadingTheme].
 * @property scrollbarTheme Theme for [MillenniumScrollbarTheme]; read it with [MillenniumTheme.scrollbarTheme].
 * @property separatorTheme Theme for [MillenniumSeparatorTheme]; read it with [MillenniumTheme.separatorTheme].
 * @property bottomSheetTheme Theme for [MillenniumBottomSheetTheme]; read it with [MillenniumTheme.bottomSheetTheme].
 * @property modalBottomSheetTheme Theme for [MillenniumModalBottomSheetTheme]; read it with [MillenniumTheme.modalBottomSheetTheme].
 * @property drawerTheme Theme for [MillenniumDrawerTheme]; read it with [MillenniumTheme.drawerTheme].
 * @property sidebarTheme Theme for [MillenniumSidebarTheme]; read it with [MillenniumTheme.sidebarTheme].
 * @property bottomNavigationBarTheme Theme for [MillenniumBottomNavigationBarTheme]; read it with [MillenniumTheme.bottomNavigationBarTheme].
 * @property autocompleteTheme Theme for [MillenniumAutocompleteTheme]; read it with [MillenniumTheme.autocompleteTheme].
 * @property listTileTheme Theme for [MillenniumListTileTheme]; read it with [MillenniumTheme.listTileTheme].
 * @property scaffoldTheme Theme for [MillenniumScaffoldTheme]; read it with [MillenniumTheme.scaffoldTheme].
 * @property iconButtonTheme Theme for [MillenniumIconButtonTheme]; read it with [MillenniumTheme.iconButtonTheme].
 * @property topAppBarTheme Theme for [MillenniumTopAppBarTheme]; read it with [MillenniumTheme.topAppBarTheme].
 * @property segmentedButtonTheme Theme for [MillenniumSegmentedButtonTheme]; read it with [MillenniumTheme.segmentedButtonTheme].
 * @property snackbarTheme Theme for [MillenniumSnackbarTheme]; read it with [MillenniumTheme.snackbarTheme].
 * @property pullToRefreshTheme Theme for [MillenniumPullToRefreshTheme]; read it with [MillenniumTheme.pullToRefreshTheme].
 * @see MillenniumThemeConfig.light
 * @see MillenniumThemeConfig.dark
 */
@Immutable
data class MillenniumThemeComponents(
    val textTheme: MillenniumTextTheme,
    val buttonTheme: MillenniumButtonTheme,
    val cardTheme: MillenniumCardTheme,
    val checkboxTheme: MillenniumCheckboxTheme,
    val triStateCheckboxTheme: MillenniumTriStateCheckboxTheme,
    val toggleSwitchTheme: MillenniumToggleSwitchTheme,
    val textFieldTheme: MillenniumTextFieldTheme,
    val sliderTheme: MillenniumSliderTheme,
    val radioGroupTheme: MillenniumRadioGroupTheme,
    val tabGroupTheme: MillenniumTabGroupTheme,
    val avatarTheme: MillenniumAvatarTheme,
    val iconTheme: MillenniumIconTheme,
    val disclosureTheme: MillenniumDisclosureTheme,
    val dialogTheme: MillenniumDialogTheme,
    val dropdownMenuTheme: MillenniumDropdownMenuTheme,
    val tooltipTheme: MillenniumTooltipTheme,
    val progressTheme: MillenniumProgressTheme,
    val loadingTheme: MillenniumLoadingTheme = defaultMillenniumLoadingTheme(),
    val scrollbarTheme: MillenniumScrollbarTheme,
    val separatorTheme: MillenniumSeparatorTheme,
    val bottomSheetTheme: MillenniumBottomSheetTheme,
    val modalBottomSheetTheme: MillenniumModalBottomSheetTheme,
    val drawerTheme: MillenniumDrawerTheme,
    val sidebarTheme: MillenniumSidebarTheme,
    val bottomNavigationBarTheme: MillenniumBottomNavigationBarTheme = defaultMillenniumBottomNavigationBarTheme(),
    val autocompleteTheme: MillenniumAutocompleteTheme = defaultMillenniumAutocompleteTheme(),
    val listTileTheme: MillenniumListTileTheme = defaultMillenniumListTileTheme(),
    val scaffoldTheme: MillenniumScaffoldTheme = defaultMillenniumScaffoldTheme(),
    val iconButtonTheme: MillenniumIconButtonTheme = defaultMillenniumIconButtonTheme(),
    val topAppBarTheme: MillenniumTopAppBarTheme = defaultMillenniumTopAppBarTheme(),
    val segmentedButtonTheme: MillenniumSegmentedButtonTheme = defaultMillenniumSegmentedButtonTheme(),
    val snackbarTheme: MillenniumSnackbarTheme = defaultMillenniumSnackbarTheme(),
    val pullToRefreshTheme: MillenniumPullToRefreshTheme = defaultMillenniumPullToRefreshTheme(),
)

/**
 * Assembles every component's `default...Theme()` factory into one [MillenniumThemeComponents].
 * No palette values are embedded; component defaults read the active colours at composition time.
 *
 * ```kotlin
 * val components = defaultMillenniumThemeComponents()
 * ```
 *
 * @return a fresh set of default component themes.
 */
fun defaultMillenniumThemeComponents() = MillenniumThemeComponents(
    textTheme = defaultMillenniumTextTheme(),
    buttonTheme = defaultMillenniumButtonTheme(),
    cardTheme = defaultMillenniumCardTheme(),
    checkboxTheme = defaultMillenniumCheckboxTheme(),
    triStateCheckboxTheme = defaultMillenniumTriStateCheckboxTheme(),
    toggleSwitchTheme = defaultMillenniumToggleSwitchTheme(),
    textFieldTheme = defaultMillenniumTextFieldTheme(),
    sliderTheme = defaultMillenniumSliderTheme(),
    radioGroupTheme = defaultMillenniumRadioGroupTheme(),
    tabGroupTheme = defaultMillenniumTabGroupTheme(),
    avatarTheme = defaultMillenniumAvatarTheme(),
    iconTheme = defaultMillenniumIconTheme(),
    disclosureTheme = defaultMillenniumDisclosureTheme(),
    dialogTheme = defaultMillenniumDialogTheme(),
    dropdownMenuTheme = defaultMillenniumDropdownMenuTheme(),
    tooltipTheme = defaultMillenniumTooltipTheme(),
    progressTheme = defaultMillenniumProgressTheme(),
    loadingTheme = defaultMillenniumLoadingTheme(),
    scrollbarTheme = defaultMillenniumScrollbarTheme(),
    separatorTheme = defaultMillenniumSeparatorTheme(),
    bottomSheetTheme = defaultMillenniumBottomSheetTheme(),
    modalBottomSheetTheme = defaultMillenniumModalBottomSheetTheme(),
    drawerTheme = defaultMillenniumDrawerTheme(),
    sidebarTheme = defaultMillenniumSidebarTheme(),
    bottomNavigationBarTheme = defaultMillenniumBottomNavigationBarTheme(),
    autocompleteTheme = defaultMillenniumAutocompleteTheme(),
    listTileTheme = defaultMillenniumListTileTheme(),
    scaffoldTheme = defaultMillenniumScaffoldTheme(),
    iconButtonTheme = defaultMillenniumIconButtonTheme(),
    topAppBarTheme = defaultMillenniumTopAppBarTheme(),
    segmentedButtonTheme = defaultMillenniumSegmentedButtonTheme(),
    snackbarTheme = defaultMillenniumSnackbarTheme(),
    pullToRefreshTheme = defaultMillenniumPullToRefreshTheme(),
)

/** Default component themes installed for light appearances ([MillenniumThemeConfig.light]). */
val MillenniumLightThemeComponents = defaultMillenniumThemeComponents()

/** Default component themes installed for dark appearances ([MillenniumThemeConfig.dark]). */
val MillenniumDarkThemeComponents = defaultMillenniumThemeComponents()

/**
 * Configuration passed to [MillenniumTheme]: palette roles, which accent family is active, and the
 * light/dark component theme sets, each independently replaceable.
 *
 * ```kotlin
 * val config = MillenniumThemeConfig(
 *     palette = MillenniumPalette.Orange,
 *     themeTransitionMillis = 250,
 * )
 * MillenniumTheme(dark = true, config = config) { ... }
 * ```
 *
 * @property colorSchemes All palette role sets; see [MillenniumColorSchemes].
 * @property light Component themes used when `dark = false`.
 * @property dark Component themes used when `dark = true`.
 * @property themeTransitionMillis Duration in milliseconds of the colour-scheme crossfade the
 *   underlying Compose Unstyled theme performs when light/dark changes. Default 180.
 * @property extensions Free-form caller values, read with [MillenniumTheme.token].
 * @property palette Active accent family; resolved with [colorSchemes] and the `dark` flag.
 */
data class MillenniumThemeConfig(
    val colorSchemes: MillenniumColorSchemes = MillenniumColorSchemes(),
    val light: MillenniumThemeComponents = MillenniumLightThemeComponents,
    val dark: MillenniumThemeComponents = MillenniumDarkThemeComponents,
    val themeTransitionMillis: Int = 180,
    val extensions: Map<String, Any> = emptyMap(),
    val palette: MillenniumPalette = MillenniumPalette.Blue,
)

private val paletteProperty = ThemeProperty<MillenniumColors>("millennium.palette")
private val paletteToken = ThemeToken<MillenniumColors>("palette")
private val LocalMillenniumConfig = staticCompositionLocalOf { MillenniumThemeConfig() }
private val LocalMillenniumComponents = staticCompositionLocalOf { MillenniumLightThemeComponents }
private val LocalMillenniumColors = staticCompositionLocalOf { MillenniumLightColors }

/**
 * Entry point of the theming model and accessor for the active theme values.
 *
 * ## Theming model
 *
 * 1. **Palette**: [MillenniumThemeConfig.palette] picks an accent family ([MillenniumPalette]).
 * 2. **Colors**: [MillenniumColorSchemes.resolve] turns palette + `dark` into one [MillenniumColors],
 *    exposed as [colors]. Palette roles live only in [MillenniumThemeConfig.colorSchemes].
 * 3. **Component themes**: each component owns a `Millennium<Component>Theme` (grouped in
 *    [MillenniumThemeComponents], exposed as [components] and per-component accessors such as
 *    [buttonTheme]). They resolve against [colors] and never copy a palette.
 * 4. **Per-instance overrides**: every public composable takes `theme: ... = MillenniumTheme.<x>Theme`
 *    plus nullable parameters. Values resolve as `param ?: themeValue ?: fallback`.
 *
 * ## End-to-end example
 *
 * ```kotlin
 * // 1. Install the theme at the root with the Orange palette.
 * MillenniumTheme(
 *     dark = isSystemInDarkTheme(),
 *     config = MillenniumThemeConfig(palette = MillenniumPalette.Orange),
 * ) {
 *     // 2. Read palette roles anywhere below.
 *     val colors = MillenniumTheme.colors
 *     Box(Modifier.background(colors.background)) {
 *         Column {
 *             // Uses the active MillenniumTheme.buttonTheme.
 *             MillenniumButton(onClick = { }) { Text("Default") }
 *
 *             // 4. Override a single instance with nullable parameters.
 *             MillenniumButton(onClick = { }) { Text("Tweaked") }
 *         }
 *     }
 * }
 *
 * // 3. Customise a component theme: copy one state property and install it for all instances.
 * //    (Property names come from MillenniumButtonThemeProperties.)
 * val base = defaultMillenniumThemeComponents()
 * val customButton = base.buttonTheme.copy(
 *     primary = base.buttonTheme.primary.copy(default = base.buttonTheme.primary.default.copy(...)),
 * )
 * val config = MillenniumThemeConfig(light = base.copy(buttonTheme = customButton))
 * MillenniumTheme(dark = false, config = config) { ... }
 * ```
 *
 * Consult each component's `...ThemeProperties` class for the exact field names; the shapes above show
 * the mechanism (`copy` on a [MillenniumControlStateProperty], then `copy` on the aggregate), not a
 * complete list of fields.
 *
 * @see MillenniumThemeConfig
 * @see MillenniumColors
 * @see MillenniumControlStateProperty
 */
object MillenniumTheme {
    /** The [MillenniumThemeConfig] passed to the nearest enclosing [MillenniumTheme]. */
    val config: MillenniumThemeConfig @Composable get() = LocalMillenniumConfig.current
    /** The component theme set active for the current appearance (light or dark). */
    val components: MillenniumThemeComponents @Composable get() = LocalMillenniumComponents.current
    /**
     * The resolved [MillenniumColors] for the current palette and appearance. Use these roles for
     * custom drawing so it follows palette and dark-mode changes.
     */
    val colors: MillenniumColors @Composable get() = LocalMillenniumColors.current
    /** Every palette role set from [config] (not just the active one). */
    val colorSchemes: MillenniumColorSchemes @Composable get() = config.colorSchemes

    /** The active [MillenniumTextTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val textTheme: MillenniumTextTheme @Composable get() = components.textTheme
    /** The active [MillenniumButtonTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val buttonTheme: MillenniumButtonTheme @Composable get() = components.buttonTheme
    /** The active [MillenniumCardTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val cardTheme: MillenniumCardTheme @Composable get() = components.cardTheme
    /** The active [MillenniumCheckboxTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val checkboxTheme: MillenniumCheckboxTheme @Composable get() = components.checkboxTheme
    /** The active [MillenniumTriStateCheckboxTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val triStateCheckboxTheme: MillenniumTriStateCheckboxTheme @Composable get() = components.triStateCheckboxTheme
    /** The active [MillenniumToggleSwitchTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val toggleSwitchTheme: MillenniumToggleSwitchTheme @Composable get() = components.toggleSwitchTheme
    /** The active [MillenniumTextFieldTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val textFieldTheme: MillenniumTextFieldTheme @Composable get() = components.textFieldTheme
    /** The active [MillenniumSliderTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val sliderTheme: MillenniumSliderTheme @Composable get() = components.sliderTheme
    /** The active [MillenniumRadioGroupTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val radioGroupTheme: MillenniumRadioGroupTheme @Composable get() = components.radioGroupTheme
    /** The active [MillenniumTabGroupTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val tabGroupTheme: MillenniumTabGroupTheme @Composable get() = components.tabGroupTheme
    /** The active [MillenniumAvatarTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val avatarTheme: MillenniumAvatarTheme @Composable get() = components.avatarTheme
    /** The active [MillenniumIconTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val iconTheme: MillenniumIconTheme @Composable get() = components.iconTheme
    /** The active [MillenniumDisclosureTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val disclosureTheme: MillenniumDisclosureTheme @Composable get() = components.disclosureTheme
    /** The active [MillenniumDialogTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val dialogTheme: MillenniumDialogTheme @Composable get() = components.dialogTheme
    /** The active [MillenniumDropdownMenuTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val dropdownMenuTheme: MillenniumDropdownMenuTheme @Composable get() = components.dropdownMenuTheme
    /** The active [MillenniumTooltipTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val tooltipTheme: MillenniumTooltipTheme @Composable get() = components.tooltipTheme
    /** The active [MillenniumProgressTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val progressTheme: MillenniumProgressTheme @Composable get() = components.progressTheme
    /** The active [MillenniumLoadingTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val loadingTheme: MillenniumLoadingTheme @Composable get() = components.loadingTheme
    /** The active [MillenniumScrollbarTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val scrollbarTheme: MillenniumScrollbarTheme @Composable get() = components.scrollbarTheme
    /** The active [MillenniumSeparatorTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val separatorTheme: MillenniumSeparatorTheme @Composable get() = components.separatorTheme
    /** The active [MillenniumBottomSheetTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val bottomSheetTheme: MillenniumBottomSheetTheme @Composable get() = components.bottomSheetTheme
    /** The active [MillenniumModalBottomSheetTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val modalBottomSheetTheme: MillenniumModalBottomSheetTheme @Composable get() = components.modalBottomSheetTheme
    /** The active [MillenniumDrawerTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val drawerTheme: MillenniumDrawerTheme @Composable get() = components.drawerTheme
    /** The active [MillenniumSidebarTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val sidebarTheme: MillenniumSidebarTheme @Composable get() = components.sidebarTheme
    /** The active [MillenniumBottomNavigationBarTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val bottomNavigationBarTheme: MillenniumBottomNavigationBarTheme @Composable get() = components.bottomNavigationBarTheme
    /** The active [MillenniumAutocompleteTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val autocompleteTheme: MillenniumAutocompleteTheme @Composable get() = components.autocompleteTheme
    /** The active [MillenniumListTileTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val listTileTheme: MillenniumListTileTheme @Composable get() = components.listTileTheme
    /** The active [MillenniumScaffoldTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val scaffoldTheme: MillenniumScaffoldTheme @Composable get() = components.scaffoldTheme
    /** The active [MillenniumIconButtonTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val iconButtonTheme: MillenniumIconButtonTheme @Composable get() = components.iconButtonTheme
    /** The active [MillenniumTopAppBarTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val topAppBarTheme: MillenniumTopAppBarTheme @Composable get() = components.topAppBarTheme
    /** The active [MillenniumSegmentedButtonTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val segmentedButtonTheme: MillenniumSegmentedButtonTheme @Composable get() = components.segmentedButtonTheme
    /** The active [MillenniumSnackbarTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val snackbarTheme: MillenniumSnackbarTheme @Composable get() = components.snackbarTheme
    /** The active [MillenniumPullToRefreshTheme] for the current appearance; the default for the matching component's `theme` parameter. */
    val pullToRefreshTheme: MillenniumPullToRefreshTheme @Composable get() = components.pullToRefreshTheme

    /**
     * Reads a caller-defined value from [MillenniumThemeConfig.extensions].
     *
     * ```kotlin
     * val radius: Dp = MillenniumTheme.token("card.radius", 12.dp)
     * ```
     *
     * @param key Key in [MillenniumThemeConfig.extensions].
     * @param default Returned when the key is missing or the stored value is not a [T]. The cast is
     *   unchecked, so a value of another type that erases to the same class is not detected.
     * @return the stored value cast to [T], or [default].
     */
    @Composable
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> token(key: String, default: T): T = config.extensions[key] as? T ?: default
}

private object DefaultDebugIndication : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        DefaultDebugIndicationInstance(interactionSource)

    override fun hashCode(): Int = -1

    override fun equals(other: Any?) = other === this

    private class DefaultDebugIndicationInstance(private val interactionSource: InteractionSource) :
        Modifier.Node(), DrawModifierNode {
        override fun onAttach() {
            // no op
        }

        override fun ContentDrawScope.draw() {
            drawContent()
        }
    }
}

/**
 * Installs Millennium at an application or preview root: resolves the active [MillenniumColors] and
 * component themes, provides default content colour, text style and window breakpoints
 * (see [org.spotube.millennium.adaptive.WindowBreakpoints]) to [content].
 *
 * It also builds the underlying Compose Unstyled theme (named "Millennium / Aero refined") so
 * colour-scheme changes crossfade over [MillenniumThemeConfig.themeTransitionMillis].
 *
 * ```kotlin
 * MillenniumTheme(
 *     dark = false,
 *     config = MillenniumThemeConfig(palette = MillenniumPalette.Lime),
 * ) {
 *     MillenniumText("Hello", color = MillenniumTheme.colors.text)
 * }
 * ```
 *
 * @param dark Whether to use the dark appearance. Defaults to the system setting.
 * @param config Palette, component themes and extensions. The unstyled theme is remembered per
 *   [config] instance, so pass a stable (remembered or hoisted) object.
 * @param content Content that reads [MillenniumTheme].
 */
@Composable
fun MillenniumTheme(
    dark: Boolean = isSystemInDarkTheme(),
    config: MillenniumThemeConfig = MillenniumThemeConfig(),
    content: @Composable () -> Unit,
) {
    val activeComponents = if (dark) config.dark else config.light
    val activeColors = config.colorSchemes.resolve(config.palette, dark)
    val unstyledTheme = remember(config) {
        buildThemeV2 {
            name = "Millennium / Aero refined"
            colorSchemeTransitionSpec = tween(config.themeTransitionMillis)
            colorScheme(ColorScheme.Light) {
                properties[paletteProperty] =
                    mapOf(paletteToken to config.colorSchemes.resolve(config.palette, dark = false))
            }
            colorScheme(ColorScheme.Dark) {
                properties[paletteProperty] =
                    mapOf(paletteToken to config.colorSchemes.resolve(config.palette, dark = true))
            }
        }
    }
    unstyledTheme(colorScheme = if (dark) ColorScheme.Dark else ColorScheme.Light) {
        val indication: Indication = remember(activeColors) {
            ColoredIndication(
                hoveredColor = activeColors.accent.copy(alpha = 0.08f),
                pressedColor = activeColors.accent.copy(alpha = 0.10f),
                focusedColor = activeColors.accent.copy(alpha = 0.10f),
                draggedColor = activeColors.accent.copy(alpha = 0.16f),
                animationSpecEnter = tween(90),
                animationSpecExit = tween(120),
            )
        }
        val bodyStyle = activeComponents.textTheme.states.default.body
        ProvideWindowWidthBreakpoints(widthBreakpoints) {
            ProvideWindowHeightBreakpoints(heightBreakpoints) {
                CompositionLocalProvider(
                    LocalMillenniumConfig provides config,
                    LocalMillenniumComponents provides activeComponents,
                    LocalMillenniumColors provides activeColors,
                ) {
                    CompositionLocalProvider(
                        LocalIndication provides indication,
                        LocalContentColor provides activeColors.text,
                        LocalTextStyle provides bodyStyle.copy(color = activeColors.text),
                        LocalIndication provides DefaultDebugIndication,
                        content = content,
                    )
                }
            }
        }
    }
}
