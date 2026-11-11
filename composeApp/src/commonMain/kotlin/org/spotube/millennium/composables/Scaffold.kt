package org.spotube.millennium.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import org.spotube.millennium.theme.MillenniumPreview
import org.spotube.millennium.theme.MillenniumStyledContent
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumPanelBrush

/**
 * Appearance and layout of the integrated top bar drawn by [MillenniumScaffold].
 *
 * Nullable colors and brushes derive from the active palette when `null`. Adjust with `copy`.
 *
 * @property titleStyle Text style of the title slot (default 16sp semibold).
 * @property titleForeground Title color; `null` uses the palette's text color.
 * @property topBarBackground Fill of the top bar; `null` uses the shared glass panel brush.
 * @property topBarPadding Padding around the leading/title/actions row.
 * @property topBarMinimumHeight Minimum height of the content row, excluding the top inset and the
 *   hairline separator.
 * @property leadingSpacing Gap between the leading slot and the title; only applied when a leading
 *   slot is supplied.
 * @property actionsSpacing Gap before the actions row and between sibling actions.
 * @see MillenniumScaffold
 * @see defaultMillenniumScaffoldTheme
 */
@Immutable
data class MillenniumScaffoldThemeProperties(
    val titleStyle: TextStyle,
    val titleForeground: Color? = null,
    val topBarBackground: Brush? = null,
    val topBarPadding: PaddingValues,
    val topBarMinimumHeight: Dp,
    val leadingSpacing: Dp,
    val actionsSpacing: Dp,
)

/**
 * Theme wrapper for [MillenniumScaffold], exposed as [MillenniumTheme.scaffoldTheme].
 *
 * ```kotlin
 * val tall = MillenniumScaffoldTheme(
 *     defaultMillenniumScaffoldTheme().properties.copy(topBarMinimumHeight = 72.dp),
 * )
 * MillenniumScaffold(title = { Text("Home") }, theme = tall) { padding ->
 *     Column(Modifier.padding(padding)) { MillenniumText("Content") }
 * }
 * ```
 *
 * @property properties The resolved top-bar appearance and layout values.
 * @see MillenniumScaffoldThemeProperties
 */
@Immutable
data class MillenniumScaffoldTheme(
    val properties: MillenniumScaffoldThemeProperties,
)

/**
 * Builds the default [MillenniumScaffoldTheme].
 *
 * Defaults: 16sp semibold title, 14dp by 10dp bar padding, 56dp minimum bar height, 10dp leading
 * gap and 8dp actions gap. The title color and bar background are `null`, so they follow the
 * active palette (palette text and glass panel brush). Customise with `copy`.
 */
fun defaultMillenniumScaffoldTheme(): MillenniumScaffoldTheme = MillenniumScaffoldTheme(
    MillenniumScaffoldThemeProperties(
        titleStyle = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
        topBarPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        topBarMinimumHeight = 56.dp,
        leadingSpacing = 10.dp,
        actionsSpacing = 8.dp,
    ),
)

/**
 * Defaults used by [MillenniumScaffold]; pass them (or a derived value) explicitly to override.
 *
 * ```kotlin
 * MillenniumScaffold(
 *     title = { Text("Embedded") },
 *     windowInsets = MillenniumScaffoldDefaults.windowInsets.exclude(WindowInsets.ime),
 * ) { padding -> }
 * ```
 */
object MillenniumScaffoldDefaults {
    /**
     * System bars, display cutouts and the IME combined (`WindowInsets.safeDrawing`). Read in a
     * composition because the insets depend on the current window.
     */
    val windowInsets: WindowInsets
        @Composable get() = WindowInsets.safeDrawing
}

/**
 * Hoists the extra space that nested [MillenniumScaffoldPadding] calls reserve
 * around a [MillenniumScaffold]'s content. Keep the state with
 * [rememberMillenniumScaffoldState] or let the scaffold create one.
 *
 * The scaffold provides its state to its subtree through an internal composition local;
 * [MillenniumScaffoldPadding] registers and unregisters entries while composing. Registrations are
 * backed by snapshot state, so [padding] may be read from composition or layout and triggers
 * recomposition when entries change. Hoist the state yourself when code outside the scaffold
 * (for example a player controller) needs to observe the reserved space.
 *
 * ```kotlin
 * val scaffoldState = rememberMillenniumScaffoldState()
 * MillenniumScaffold(title = { Text("Player") }, state = scaffoldState) { innerPadding ->
 *     MillenniumText("Reserved: ${scaffoldState.padding.calculateBottomPadding()}")
 * }
 * ```
 *
 * @see MillenniumScaffold
 * @see MillenniumScaffoldPadding
 */
@Stable
class MillenniumScaffoldState {
    private val registered = mutableStateMapOf<Any, PaddingValues>()

    /**
     * Sum of the extra padding reserved by nested [MillenniumScaffoldPadding] calls (zero when
     * none are composed). Start and end are resolved against the layout direction when read.
     */
    val padding: PaddingValues
        get() = registered.values.fold(PaddingValues(0.dp)) { acc, value -> acc + value }

    /** Records or updates the padding reserved by [key]; a no-op when it is unchanged. */
    internal fun register(key: Any, padding: PaddingValues) {
        if (registered[key] != padding) registered[key] = padding
    }

    /** Releases whatever padding [key] reserved. */
    internal fun unregister(key: Any) {
        registered.remove(key)
    }
}

/**
 * Creates and remembers a [MillenniumScaffoldState]. The state is not saveable: reserved padding is
 * re-registered by the composables that declare it each time they enter the composition.
 *
 * @see MillenniumScaffold
 */
@Composable
fun rememberMillenniumScaffoldState(): MillenniumScaffoldState = remember { MillenniumScaffoldState() }

/**
 * Provided by [MillenniumScaffold] to its subtree and read by [MillenniumScaffoldPadding]; `null`
 * outside a scaffold, in which case reservations are silently ignored.
 */
private val LocalMillenniumScaffoldState = staticCompositionLocalOf<MillenniumScaffoldState?> { null }

/**
 * Reserves [padding] around the enclosing [MillenniumScaffold]'s content while
 * [content] is composed. Wrap a fixed composable (for example a bottom bar or a
 * player dock) so scrolling content stays clear of it; the reserved space is added
 * to the scaffold's `innerPadding` and released automatically when [content] leaves
 * the composition.
 *
 * It adds no layout of its own: [content] is composed in place. When used outside a
 * [MillenniumScaffold] it has no effect. The registration is keyed per call site and refreshed
 * whenever [padding] changes.
 *
 * ```kotlin
 * MillenniumScaffold(title = { Text("Music") }) { innerPadding ->
 *     LazyColumn(contentPadding = innerPadding) { items(tracks) { TrackRow(it) } }
 *     MillenniumScaffoldPadding(PaddingValues(bottom = 72.dp)) {
 *         PlayerDock(Modifier.height(72.dp))
 *     }
 * }
 * ```
 *
 * @param padding Extra space to reserve around the scaffold content while [content] is composed.
 * @param content The fixed composable that needs the space.
 * @see MillenniumScaffold
 * @see MillenniumScaffoldState
 */
@Composable
fun MillenniumScaffoldPadding(
    padding: PaddingValues,
    content: @Composable () -> Unit,
) {
    val state = LocalMillenniumScaffoldState.current
    if (state != null) {
        val key = remember { Any() }
        DisposableEffect(key, padding) {
            state.register(key, padding)
            onDispose { state.unregister(key) }
        }
    }
    content()
}

/**
 * A top-app-bar scaffold with `leading`, `title` and `actions` slots, safe-area
 * handling and an optional bottom bar.
 *
 * [content] receives `innerPadding`: the space used by the top bar, the bottom bar
 * and the system-safe insets. Apply it (for example `Modifier.padding(innerPadding)`)
 * to keep content clear of system overlays and the scaffold's fixed regions. Nested
 * [MillenniumScaffoldPadding] calls add to this value, so a fixed composable anywhere
 * in the subtree can reserve its own space.
 *
 * Structure: a full-size [Box] painted with the container color; the content fills it (consuming the
 * insets already covered by `innerPadding`), the top bar is aligned to the top, and the optional
 * bottom bar to the bottom. The top bar and bottom bar are measured with `onSizeChanged`, so
 * `innerPadding` is one frame late on first composition and then stable. Content therefore draws
 * behind the bars (useful for translucent glass); apply `innerPadding` to keep it clear. The top bar
 * applies the top and horizontal insets, the bottom bar the bottom and horizontal ones. The top bar
 * ends with a [MillenniumSeparator] hairline. The scaffold adds no semantics of its own; the bars are
 * plain layouts whose interactive children (buttons, tabs) carry the accessibility behavior, and
 * 44dp targets come from those components.
 *
 * ```kotlin
 * var tab by remember { mutableStateOf(0) }
 * MillenniumScaffold(
 *     title = { Text("Millennium") },
 *     leading = { MillenniumIcon(MillenniumIcons.Sparkle) },
 *     actions = {
 *         MillenniumButton(onClick = { save() }, style = MillenniumButtonStyle.Quiet) { Text("Save") }
 *     },
 *     bottomBar = {
 *         MillenniumBottomNavigationBar {
 *             MillenniumBottomNavigationItem(
 *                 selected = tab == 0,
 *                 onClick = { tab = 0 },
 *                 icon = { MillenniumIcon(MillenniumIcons.Sparkle) },
 *                 label = { Text("Home") },
 *             )
 *         }
 *     },
 * ) { innerPadding ->
 *     Column(Modifier.padding(innerPadding).fillMaxSize()) { MillenniumText("Hello") }
 * }
 * ```
 *
 * @param title The title slot, styled with the theme title style and color.
 * @param modifier Modifier applied to the outer container.
 * @param leading Optional slot before the title (navigation button or logo); `null` omits it and
 *   its spacing.
 * @param actions Optional trailing slot in a [Row]; `null` omits it and its spacing.
 * @param bottomBar Optional fixed bottom content, for example [MillenniumBottomNavigationBar].
 *   Its height is added to `innerPadding`; `null` means only the bottom inset is reserved.
 * @param windowInsets Insets the scaffold respects. Defaults to
 *   [MillenniumScaffoldDefaults.windowInsets]; pass `WindowInsets(0, 0, 0, 0)` when embedding.
 * @param containerColor Background of the whole scaffold; [Color.Unspecified] (default) uses the
 *   palette background.
 * @param state Hoisted reservation state; defaults to a remembered one. Provide your own to
 *   observe reserved padding from outside.
 * @param titleStyle Overrides the theme title style; `null` uses the theme.
 * @param titleForeground Overrides the title color; `null` falls back to the theme, then the palette
 *   text color.
 * @param topBarBackground Overrides the top bar fill; `null` falls back to the theme, then the glass
 *   panel brush.
 * @param topBarPadding Overrides the top bar row padding; `null` uses the theme.
 * @param topBarMinimumHeight Overrides the minimum bar row height; `null` uses the theme.
 * @param leadingSpacing Overrides the leading gap; `null` uses the theme.
 * @param actionsSpacing Overrides the actions gap; `null` uses the theme.
 * @param theme Theme supplying defaults for every nullable override. Defaults to
 *   [MillenniumTheme.scaffoldTheme].
 * @param content Page content; receives the `innerPadding` described above.
 * @see MillenniumScaffoldPadding
 * @see MillenniumScaffoldState
 * @see MillenniumTopAppBar
 * @see MillenniumBottomNavigationBar
 */
@Composable
fun MillenniumScaffold(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    windowInsets: WindowInsets = MillenniumScaffoldDefaults.windowInsets,
    containerColor: Color = Color.Unspecified,
    state: MillenniumScaffoldState = rememberMillenniumScaffoldState(),
    titleStyle: TextStyle? = null,
    titleForeground: Color? = null,
    topBarBackground: Brush? = null,
    topBarPadding: PaddingValues? = null,
    topBarMinimumHeight: Dp? = null,
    leadingSpacing: Dp? = null,
    actionsSpacing: Dp? = null,
    theme: MillenniumScaffoldTheme = MillenniumTheme.scaffoldTheme,
    content: @Composable (PaddingValues) -> Unit,
) {
    val colors = MillenniumTheme.colors
    val base = theme.properties
    val properties = base.copy(
        titleStyle = titleStyle ?: base.titleStyle,
        titleForeground = titleForeground ?: base.titleForeground,
        topBarBackground = topBarBackground ?: base.topBarBackground,
        topBarPadding = topBarPadding ?: base.topBarPadding,
        topBarMinimumHeight = topBarMinimumHeight ?: base.topBarMinimumHeight,
        leadingSpacing = leadingSpacing ?: base.leadingSpacing,
        actionsSpacing = actionsSpacing ?: base.actionsSpacing,
    )
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val insets = windowInsets.asPaddingValues()
    var topBarSize by remember { mutableStateOf(IntSize.Zero) }
    var bottomBarSize by remember { mutableStateOf(IntSize.Zero) }
    val innerPadding = millenniumScaffoldInnerPadding(
        insets = insets,
        topBarHeight = with(density) { topBarSize.height.toDp() },
        bottomBarHeight = if (bottomBar != null) with(density) { bottomBarSize.height.toDp() } else null,
        extraPadding = state.padding,
        layoutDirection = layoutDirection,
    )
    Box(
        modifier
            .fillMaxSize()
            .background(if (containerColor == Color.Unspecified) colors.background else containerColor),
    ) {
        CompositionLocalProvider(LocalMillenniumScaffoldState provides state) {
            Box(Modifier.fillMaxSize().consumeWindowInsets(innerPadding)) {
                content(innerPadding)
            }
            ScaffoldTopBar(
                title = title,
                leading = leading,
                actions = actions,
                properties = properties,
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .onSizeChanged { topBarSize = it },
            )
            if (bottomBar != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .onSizeChanged { bottomBarSize = it },
                ) {
                    Box(
                        Modifier.windowInsetsPadding(
                            windowInsets.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                        ),
                    ) { bottomBar() }
                }
            }
        }
    }
}

/** The scaffold's integrated top bar: glass fill, leading/title/actions row and a separator. */
@Composable
private fun ScaffoldTopBar(
    title: @Composable () -> Unit,
    leading: (@Composable () -> Unit)?,
    actions: (@Composable RowScope.() -> Unit)?,
    properties: MillenniumScaffoldThemeProperties,
    windowInsets: WindowInsets,
    modifier: Modifier = Modifier,
) {
    val colors = MillenniumTheme.colors
    Column(
        modifier
            .background(properties.topBarBackground ?: millenniumPanelBrush(colors))
            .windowInsetsPadding(windowInsets),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = properties.topBarMinimumHeight)
                .padding(properties.topBarPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(properties.leadingSpacing))
            }
            Box(Modifier.weight(1f)) {
                MillenniumStyledContent(properties.titleStyle, properties.titleForeground ?: colors.text) { title() }
            }
            if (actions != null) {
                Spacer(Modifier.width(properties.actionsSpacing))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(properties.actionsSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) { actions() }
            }
        }
        MillenniumSeparator()
    }
}

/**
 * Combines safe-area insets, bar heights and reserved padding into content padding.
 *
 * Top is the larger of the top bar height and the top inset (the bar already includes the inset),
 * plus the reserved top padding. Bottom is the larger of the bottom bar height and the bottom
 * inset, or just the inset when [bottomBarHeight] is `null`, plus the reserved bottom padding.
 * Start and end are the inset plus the reserved value, resolved for [layoutDirection].
 *
 * @param insets Safe-area insets as padding.
 * @param topBarHeight Measured top bar height.
 * @param bottomBarHeight Measured bottom bar height, or `null` when there is no bottom bar.
 * @param extraPadding Sum of padding reserved through [MillenniumScaffoldPadding].
 * @param layoutDirection Used to resolve start and end.
 */
internal fun millenniumScaffoldInnerPadding(
    insets: PaddingValues,
    topBarHeight: Dp,
    bottomBarHeight: Dp?,
    extraPadding: PaddingValues,
    layoutDirection: LayoutDirection,
): PaddingValues {
    val top = maxOf(topBarHeight, insets.calculateTopPadding()) + extraPadding.calculateTopPadding()
    val bottomInset = insets.calculateBottomPadding()
    val bottom = (if (bottomBarHeight != null) maxOf(bottomBarHeight, bottomInset) else bottomInset) +
        extraPadding.calculateBottomPadding()
    return PaddingValues(
        start = insets.calculateStartPadding(layoutDirection) + extraPadding.calculateStartPadding(layoutDirection),
        top = top,
        end = insets.calculateEndPadding(layoutDirection) + extraPadding.calculateEndPadding(layoutDirection),
        bottom = bottom,
    )
}

/** Adds two padding values, resolving start/end against the ambient layout direction. */
internal operator fun PaddingValues.plus(other: PaddingValues): PaddingValues = SummedPaddingValues(this, other)

/** Lazily sums two [PaddingValues] side by side, so layout-direction resolution stays correct. */
private class SummedPaddingValues(
    private val first: PaddingValues,
    private val second: PaddingValues,
) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        first.calculateLeftPadding(layoutDirection) + second.calculateLeftPadding(layoutDirection)

    override fun calculateTopPadding(): Dp = first.calculateTopPadding() + second.calculateTopPadding()

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        first.calculateRightPadding(layoutDirection) + second.calculateRightPadding(layoutDirection)

    override fun calculateBottomPadding(): Dp = first.calculateBottomPadding() + second.calculateBottomPadding()
}

/** Design-time preview of [MillenniumScaffold] with leading, title and one action. */
@Preview
@Composable
private fun MillenniumScaffoldPreview() = MillenniumPreview {
    MillenniumScaffold(
        title = { Text("Millennium") },
        leading = { MillenniumIcon(MillenniumIcons.Sparkle) },
        actions = { MillenniumButton(onClick = {}, style = MillenniumButtonStyle.Quiet) { Text("Save") } },
    ) { innerPadding ->
        Column(
            Modifier.padding(innerPadding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MillenniumText("Safe areas and bars are handled for you.")
        }
    }
}
