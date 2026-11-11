package org.spotube.millennium.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import org.spotube.millennium.theme.collectIsKeyboardFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.AnchorAlignment
import com.composeunstyled.AnchorSide
import com.composeunstyled.DropdownMenuPanel
import com.composeunstyled.Text
import com.composeunstyled.UnstyledDropdownMenu
import com.composeunstyled.UnstyledDropdownMenuItem
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumActionPointer
import org.spotube.millennium.theme.MillenniumPreview
import org.spotube.millennium.theme.MillenniumStyledContent
import org.spotube.millennium.theme.millenniumBevelBrush
import org.spotube.millennium.theme.millenniumSurface
import org.spotube.millennium.theme.millenniumInteractionSurface
import org.spotube.millennium.theme.millenniumPanelBrush

/**
 * Resolved visual and layout properties for one control state of [MillenniumDropdownMenu].
 *
 * The panel reads only the `default` state of [MillenniumDropdownMenuTheme.states]; rows resolve the
 * state that matches their own hover, focus, press, selected or disabled interaction. Nullable
 * colours and brushes are derived from the active palette (`MillenniumTheme.colors`) when left null.
 *
 * @property panelBackground Fill of the floating panel. Null uses `millenniumPanelBrush(colors)`.
 * @property panelShape Corner shape of the panel.
 * @property panelBorderColor Panel hairline colour. Null uses `colors.border`.
 * @property panelBorderWidth Panel hairline thickness.
 * @property panelShadowElevation Drop-shadow elevation under the panel.
 * @property panelGlowColor Inner top glow of the panel. Null uses `colors.highlight` at 50% alpha.
 * @property minimumWidth Minimum width of the panel (applied with `widthIn(min = ...)`).
 * @property sideOffset Gap between the anchor and the panel along the anchor side.
 * @property panelPadding Padding between the panel edge and the column of bricks.
 * @property itemBackground Row fill override for every row state. Null derives it per state
 * (translucent wash for selected, accent bevel when hovered/focused/pressed, danger wash for
 * destructive rows).
 * @property itemShape Corner shape of a row.
 * @property itemForeground Row text colour override. Null derives it from the state and variant.
 * @property itemTextStyle Typography of the row label (also the base for the panel content).
 * @property itemMinimumHeight Minimum row height; 44dp by default so rows are accessible touch targets.
 * @property itemPadding Padding inside a row around its leading, label and trailing parts.
 * @property itemSpacing Vertical spacing between rows and between bricks in the panel column and
 * header/footer/content/group sections.
 * @property stateAnimationMillis Duration of row colour, border and elevation transitions.
 * @property openAnimationMillis Duration of the panel fade-in when it opens.
 * @property closeAnimationMillis Duration of the panel exit transition.
 * @property enterScale Initial scale of the panel when entering (spring towards 1).
 * @property exitScale Target scale of the panel when exiting.
 * @property alpha Overall opacity of the panel (panel default state) or of a row (row state).
 * @property itemBorderColor Row border colour override. Null shows an accent or danger border only
 * while the row is active.
 * @property itemBorderWidth Row border thickness while it is shown.
 * @property itemGlowColor Row inner glow override. Null uses a soft highlight on filled rows.
 * @property itemWidth Fixed width of the panel's inner column, so row fills reach both panel edges.
 * @property maximumHeight Height cap of [MillenniumDropdownMenuContent] before it scrolls.
 * @property iconSize Size of [MillenniumDropdownMenuItemIcon] and of the checkbox/radio indicator slot.
 * @property iconLabelSpacing Horizontal gap between leading, label, shortcut, trailing and indicator.
 * @property descriptionTextStyle Typography of a row's [MillenniumDropdownMenuItem] description.
 * @property shortcutTextStyle Typography of a row's shortcut hint.
 * @property groupLabelStyle Typography of [MillenniumDropdownMenuLabel].
 * @property groupLabelPadding Padding around a group label.
 * @property sectionPadding Padding inside [MillenniumDropdownMenuHeader] and [MillenniumDropdownMenuFooter].
 *
 * @see MillenniumDropdownMenuTheme
 * @see defaultMillenniumDropdownMenuTheme
 */
@Immutable
data class MillenniumDropdownMenuThemeProperties(
    val panelBackground: Brush? = null,
    val panelShape: Shape,
    val panelBorderColor: Color? = null,
    val panelBorderWidth: Dp,
    val panelShadowElevation: Dp,
    val panelGlowColor: Color? = null,
    val minimumWidth: Dp,
    val sideOffset: Dp,
    val panelPadding: PaddingValues,
    val itemBackground: Brush? = null,
    val itemShape: Shape,
    val itemForeground: Color? = null,
    val itemTextStyle: TextStyle,
    val itemMinimumHeight: Dp,
    val itemPadding: PaddingValues,
    val itemSpacing: Dp,
    val stateAnimationMillis: Int,
    val openAnimationMillis: Int = 180,
    val closeAnimationMillis: Int = 130,
    val enterScale: Float = 0.94f,
    val exitScale: Float = 0.96f,
    val alpha: Float = 1f,
    val itemBorderColor: Color? = null,
    val itemBorderWidth: Dp = 1.dp,
    val itemGlowColor: Color? = null,
    val itemWidth: Dp = 192.dp,
    val maximumHeight: Dp = 320.dp,
    val iconSize: Dp = 18.dp,
    val iconLabelSpacing: Dp = 10.dp,
    val descriptionTextStyle: TextStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
    val shortcutTextStyle: TextStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    val groupLabelStyle: TextStyle = TextStyle(
        fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp,
    ),
    val groupLabelPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    val sectionPadding: PaddingValues = PaddingValues(0.dp),
)

/**
 * Theme of [MillenniumDropdownMenu] and all of its bricks, resolved per control state.
 *
 * Installed as `MillenniumTheme.dropdownMenuTheme`. Every dropdown brick accepts a `theme` parameter
 * that defaults to it, so one instance can restyle a single menu without touching the global theme.
 *
 * @property states State-dependent [MillenniumDropdownMenuThemeProperties]. Panels use `default`; rows
 * resolve `hovered`, `focused`, `pressed`, `disabled` or `selected` from their interaction state.
 *
 * @see defaultMillenniumDropdownMenuTheme
 */
@Immutable
data class MillenniumDropdownMenuTheme(
    val states: MillenniumControlStateProperty<MillenniumDropdownMenuThemeProperties>,
)

/**
 * Builds the stock dropdown-menu theme: a 12dp rounded pearl/smoke glass panel (200dp minimum, 8dp
 * shadow, 4dp padding), 8dp rounded rows with a 44dp minimum height, 14sp medium labels, a 192dp inner
 * column, a 320dp scroll cap, and spring-based open/close motion (180ms in, 130ms out). Hover and press
 * slightly lower the alpha, disabled drops to 45%, and error thickens the border.
 *
 * Customise by copying: `defaultMillenniumDropdownMenuTheme().let { t -> t.copy(states =
 * t.states.copy(default = t.states.default.copy(itemWidth = 240.dp))) }`.
 *
 * @return A fresh [MillenniumDropdownMenuTheme] with palette-derived colours left null.
 * @see MillenniumDropdownMenuThemeProperties
 */
fun defaultMillenniumDropdownMenuTheme(): MillenniumDropdownMenuTheme {
    val base = MillenniumDropdownMenuThemeProperties(
        panelShape = RoundedCornerShape(12.dp),
        panelBorderWidth = 1.dp,
        panelShadowElevation = 8.dp,
        minimumWidth = 200.dp,
        sideOffset = 8.dp,
        panelPadding = PaddingValues(4.dp),
        itemShape = RoundedCornerShape(8.dp),
        itemTextStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        itemMinimumHeight = 44.dp,
        itemPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        itemSpacing = 4.dp,
        stateAnimationMillis = 120,
    )
    return MillenniumDropdownMenuTheme(
        states = MillenniumControlStateProperty(
            default = base,
            hovered = base.copy(alpha = 0.97f),
            focused = base,
            pressed = base.copy(alpha = 0.90f),
            disabled = base.copy(alpha = 0.45f),
            selected = base,
            error = base.copy(panelBorderWidth = 2.dp),
        ),
    )
}


// ---------------------------------------------------------------------------
// Root
// ---------------------------------------------------------------------------

/**
 * A themed, caller-owned dropdown menu. Compose the panel from lego bricks.
 *
 * The menu is built on Compose Unstyled's `UnstyledDropdownMenu`, so open/close semantics, outside-click
 * and Escape dismissal, and the focus handling of rows come from that primitive. The panel animates with
 * an anchor-aware spring (scale, slide and expand from the trigger side) and leaves with a brisk
 * fade/shrink. [expanded] is fully caller-owned; the menu only reports requests through
 * [onExpandedChange] (outside click, Escape, or a row with `closeOnClick`).
 *
 * ```kotlin
 * var expanded by remember { mutableStateOf(false) }
 * var showLyrics by remember { mutableStateOf(true) }
 * val filter = rememberTextFieldState()
 *
 * MillenniumDropdownMenu(
 *     expanded = expanded,
 *     onExpandedChange = { expanded = it },
 *     anchor = { MillenniumButton(onClick = { expanded = true }) { Text("Track options") } },
 * ) {
 *     MillenniumDropdownMenuHeader { MillenniumDropdownMenuFilter(filter) }          // fixed
 *     MillenniumDropdownMenuContent {                                                // scrolls
 *         MillenniumDropdownMenuGroup {
 *             MillenniumDropdownMenuLabel("Account")
 *             MillenniumDropdownMenuItem("Profile", onClick = {}, icon = MillenniumIcons.Music)
 *             MillenniumDropdownMenuCheckboxItem(showLyrics, { showLyrics = it }, "Show lyrics")
 *         }
 *         MillenniumDropdownMenuSeparator()
 *         MillenniumDropdownMenuItem("Delete", onClick = {}, destructive = true)
 *     }
 *     MillenniumDropdownMenuFooter { MillenniumText("3 results") }                   // fixed
 * }
 * ```
 *
 * Every brick is optional and any composable may sit between them. Known limits: rows nested inside
 * [MillenniumDropdownMenuContent] (the scrolling region) do not register with Compose Unstyled's panel
 * focus list, so Home/End and opening by keyboard do not focus the first row (arrow movement between
 * already focused rows still works). Submenus are not implemented.
 *
 * @param expanded Whether the panel is shown. Caller-owned.
 * @param onExpandedChange Called with the requested new value when the menu wants to open or close.
 * @param modifier Modifier applied to the anchor container.
 * @param side Which side of the anchor the panel opens on; also selects the enter/exit animation direction.
 * @param alignment How the panel aligns to the anchor along the cross axis.
 * @param width Fixed width of the inner column. Null uses [MillenniumDropdownMenuThemeProperties.itemWidth].
 * @param panelBackground Panel fill override; null falls back to the theme, then `millenniumPanelBrush`.
 * @param panelShape Panel shape override; null falls back to the theme.
 * @param panelBorderColor Panel border colour override; null falls back to the theme, then `colors.border`.
 * @param panelBorderWidth Panel border thickness override; null falls back to the theme.
 * @param panelShadowElevation Panel shadow override; null falls back to the theme.
 * @param panelGlowColor Panel inner glow override; null falls back to the theme, then a soft highlight.
 * @param minimumWidth Minimum panel width override; null falls back to the theme.
 * @param sideOffset Gap between anchor and panel; null falls back to the theme.
 * @param panelPadding Padding around the bricks; null falls back to the theme.
 * @param sectionSpacing Vertical spacing between the direct children of the panel column; null falls
 * back to [MillenniumDropdownMenuThemeProperties.itemSpacing].
 * @param openAnimationMillis Fade-in duration; null falls back to the theme.
 * @param closeAnimationMillis Exit duration; null falls back to the theme.
 * @param enterScale Initial scale when entering; null falls back to the theme.
 * @param exitScale Target scale when exiting; null falls back to the theme.
 * @param alpha Panel opacity; null falls back to the theme.
 * @param theme Component theme; defaults to `MillenniumTheme.dropdownMenuTheme`. Only its `default` state
 * is used for the panel.
 * @param anchor The trigger content (usually a button) the panel is positioned against.
 * @param content The panel body, composed from the `MillenniumDropdownMenu*` bricks. Text inside inherits
 * the item text style and `colors.text`.
 * @see MillenniumDropdownMenuHeader
 * @see MillenniumDropdownMenuContent
 * @see MillenniumDropdownMenuFooter
 * @see MillenniumDropdownMenuItem
 */
@Composable
fun MillenniumDropdownMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    side: AnchorSide = AnchorSide.Bottom,
    alignment: AnchorAlignment = AnchorAlignment.Start,
    width: Dp? = null,
    panelBackground: Brush? = null,
    panelShape: Shape? = null,
    panelBorderColor: Color? = null,
    panelBorderWidth: Dp? = null,
    panelShadowElevation: Dp? = null,
    panelGlowColor: Color? = null,
    minimumWidth: Dp? = null,
    sideOffset: Dp? = null,
    panelPadding: PaddingValues? = null,
    sectionSpacing: Dp? = null,
    openAnimationMillis: Int? = null,
    closeAnimationMillis: Int? = null,
    enterScale: Float? = null,
    exitScale: Float? = null,
    alpha: Float? = null,
    theme: MillenniumDropdownMenuTheme = MillenniumTheme.dropdownMenuTheme,
    anchor: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MillenniumTheme.colors
    val panelProperties = theme.states.default
    val resolvedPanelBackground = panelBackground ?: panelProperties.panelBackground ?: millenniumPanelBrush(colors)
    val resolvedPanelBorderColor = panelBorderColor ?: panelProperties.panelBorderColor ?: colors.border
    val resolvedPanelGlowColor = panelGlowColor ?: panelProperties.panelGlowColor ?: colors.highlight.copy(alpha = 0.5f)
    val resolvedOpenMillis = openAnimationMillis ?: panelProperties.openAnimationMillis
    val resolvedCloseMillis = closeAnimationMillis ?: panelProperties.closeAnimationMillis
    val resolvedEnterScale = enterScale ?: panelProperties.enterScale
    val resolvedExitScale = exitScale ?: panelProperties.exitScale
    val transformOrigin = when (side) {
        AnchorSide.Bottom -> TransformOrigin(0.5f, 0f)
        AnchorSide.Top -> TransformOrigin(0.5f, 1f)
        AnchorSide.Start -> TransformOrigin(1f, 0.5f)
        AnchorSide.End -> TransformOrigin(0f, 0.5f)
    }
    val enterSpring = spring<Float>(dampingRatio = 0.76f, stiffness = Spring.StiffnessMediumLow)
    val enterSlide = when (side) {
        AnchorSide.Bottom -> expandVertically(
            animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow),
            expandFrom = Alignment.Top,
        ) + slideInVertically(
            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
            initialOffsetY = { -it / 6 },
        )
        AnchorSide.Top -> expandVertically(
            animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow),
            expandFrom = Alignment.Bottom,
        ) + slideInVertically(
            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
            initialOffsetY = { it / 6 },
        )
        AnchorSide.Start -> slideInHorizontally(
            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
            initialOffsetX = { it / 6 },
        )
        AnchorSide.End -> slideInHorizontally(
            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
            initialOffsetX = { -it / 6 },
        )
    }
    val exitSpec = tween<Float>(resolvedCloseMillis, easing = FastOutLinearInEasing)
    val exitSlide = when (side) {
        AnchorSide.Bottom -> shrinkVertically(
            animationSpec = tween(resolvedCloseMillis, easing = FastOutLinearInEasing),
            shrinkTowards = Alignment.Top,
        ) + slideOutVertically(
            animationSpec = tween(resolvedCloseMillis, easing = FastOutLinearInEasing),
            targetOffsetY = { -it / 8 },
        )
        AnchorSide.Top -> shrinkVertically(
            animationSpec = tween(resolvedCloseMillis, easing = FastOutLinearInEasing),
            shrinkTowards = Alignment.Bottom,
        ) + slideOutVertically(
            animationSpec = tween(resolvedCloseMillis, easing = FastOutLinearInEasing),
            targetOffsetY = { it / 8 },
        )
        AnchorSide.Start -> slideOutHorizontally(
            animationSpec = tween(resolvedCloseMillis, easing = FastOutLinearInEasing),
            targetOffsetX = { it / 8 },
        )
        AnchorSide.End -> slideOutHorizontally(
            animationSpec = tween(resolvedCloseMillis, easing = FastOutLinearInEasing),
            targetOffsetX = { -it / 8 },
        )
    }
    UnstyledDropdownMenu(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        side = side,
        alignment = alignment,
        sideOffset = sideOffset ?: panelProperties.sideOffset,
        anchor = anchor,
        panel = {
            DropdownMenuPanel(
                modifier = Modifier.widthIn(min = minimumWidth ?: panelProperties.minimumWidth).millenniumSurface(
                    background = resolvedPanelBackground,
                    shape = panelShape ?: panelProperties.panelShape,
                    borderColor = resolvedPanelBorderColor,
                    borderWidth = panelBorderWidth ?: panelProperties.panelBorderWidth,
                    shadowElevation = panelShadowElevation ?: panelProperties.panelShadowElevation,
                    innerGlowColor = resolvedPanelGlowColor,
                ).padding(panelPadding ?: panelProperties.panelPadding).alpha(alpha ?: panelProperties.alpha),
                enter = fadeIn(tween(resolvedOpenMillis, easing = FastOutSlowInEasing)) +
                    scaleIn(
                        initialScale = resolvedEnterScale,
                        transformOrigin = transformOrigin,
                        animationSpec = enterSpring,
                    ) + enterSlide,
                exit = fadeOut(tween(resolvedCloseMillis, easing = FastOutLinearInEasing)) +
                    scaleOut(
                        targetScale = resolvedExitScale,
                        transformOrigin = transformOrigin,
                        animationSpec = exitSpec,
                    ) + exitSlide,
            ) {
                Column(
                    modifier = Modifier.width(width ?: panelProperties.itemWidth),
                    verticalArrangement = Arrangement.spacedBy(sectionSpacing ?: panelProperties.itemSpacing),
                ) {
                    MillenniumStyledContent(panelProperties.itemTextStyle, colors.text) { content() }
                }
            }
        },
    )
}

/**
 * Convenience menu for a plain list of strings, built from the same bricks as the lego overload.
 *
 * Reach for the lego overload when rows need icons, groups, toggles, separators or any custom content.
 * With `filterable = true` a header with a search field is added; the query lives in internal state and
 * is cleared whenever the menu closes. Caller owns [expanded] and handles [onItemClick].
 *
 * ```kotlin
 * var expanded by remember { mutableStateOf(false) }
 * MillenniumDropdownMenu(
 *     expanded = expanded,
 *     onExpandedChange = { expanded = it },
 *     items = listOf("Low", "High", "Lossless"),
 *     onItemClick = { quality -> setQuality(quality) },
 *     filterable = true,
 *     anchor = { MillenniumButton(onClick = { expanded = true }) { Text("Quality") } },
 * )
 * ```
 *
 * @param expanded Whether the panel is shown. Caller-owned.
 * @param onExpandedChange Called when the menu requests opening or closing.
 * @param items The row labels, in display order.
 * @param onItemClick Called with the clicked label; the menu then requests closing.
 * @param modifier Modifier applied to the anchor container.
 * @param side Side of the anchor the panel opens on.
 * @param panelBackground Panel fill override; null falls back to the theme.
 * @param panelShape Panel shape override; null falls back to the theme.
 * @param panelBorderColor Panel border colour override; null falls back to the theme.
 * @param panelBorderWidth Panel border thickness override; null falls back to the theme.
 * @param panelShadowElevation Panel shadow override; null falls back to the theme.
 * @param panelGlowColor Panel inner glow override; null falls back to the theme.
 * @param minimumWidth Minimum panel width override; null falls back to the theme.
 * @param sideOffset Anchor-to-panel gap override; null falls back to the theme.
 * @param panelPadding Panel padding override; null falls back to the theme.
 * @param itemBackground Row fill override; null falls back to the per-state theme/derived fill.
 * @param itemShape Row shape override; null falls back to the theme.
 * @param itemForeground Row text colour override; null falls back to the theme/derived colour.
 * @param itemTextStyle Row label typography override; null falls back to the theme.
 * @param itemMinimumHeight Minimum row height override; null falls back to the theme (44dp).
 * @param itemPadding Row padding override; null falls back to the theme.
 * @param itemSpacing Spacing between rows (and the panel's sections); null falls back to the theme.
 * @param itemBorderColor Row border colour override; null falls back to the theme.
 * @param itemBorderWidth Row border thickness override; null falls back to the theme.
 * @param itemGlowColor Row inner glow override; null falls back to the theme.
 * @param itemWidth Fixed inner column width; null falls back to the theme.
 * @param maximumHeight Height at which the list starts to scroll; null falls back to the theme.
 * @param filterable Show a search header that narrows [items].
 * @param onFilter Custom filter producing the visible items from the query. Null uses a case-insensitive
 * substring match. Only used when [filterable] is true and the query is not blank.
 * @param filterPlaceholder Placeholder of the filter field.
 * @param emptyText Text shown when no rows remain.
 * @param stateAnimationMillis Row state transition duration; null falls back to the theme.
 * @param openAnimationMillis Panel fade-in duration; null falls back to the theme.
 * @param closeAnimationMillis Panel exit duration; null falls back to the theme.
 * @param enterScale Initial panel scale when entering; null falls back to the theme.
 * @param exitScale Target panel scale when exiting; null falls back to the theme.
 * @param alpha Panel opacity; null falls back to the theme.
 * @param theme Component theme; defaults to `MillenniumTheme.dropdownMenuTheme`.
 * @param anchor The trigger content the panel is positioned against.
 * @see MillenniumDropdownMenuItem
 */
@Composable
fun MillenniumDropdownMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<String>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    side: AnchorSide = AnchorSide.Bottom,
    panelBackground: Brush? = null,
    panelShape: Shape? = null,
    panelBorderColor: Color? = null,
    panelBorderWidth: Dp? = null,
    panelShadowElevation: Dp? = null,
    panelGlowColor: Color? = null,
    minimumWidth: Dp? = null,
    sideOffset: Dp? = null,
    panelPadding: PaddingValues? = null,
    itemBackground: Brush? = null,
    itemShape: Shape? = null,
    itemForeground: Color? = null,
    itemTextStyle: TextStyle? = null,
    itemMinimumHeight: Dp? = null,
    itemPadding: PaddingValues? = null,
    itemSpacing: Dp? = null,
    itemBorderColor: Color? = null,
    itemBorderWidth: Dp? = null,
    itemGlowColor: Color? = null,
    itemWidth: Dp? = null,
    maximumHeight: Dp? = null,
    filterable: Boolean = false,
    onFilter: ((query: String) -> List<String>)? = null,
    filterPlaceholder: String = "Filter",
    emptyText: String = "No results",
    stateAnimationMillis: Int? = null,
    openAnimationMillis: Int? = null,
    closeAnimationMillis: Int? = null,
    enterScale: Float? = null,
    exitScale: Float? = null,
    alpha: Float? = null,
    theme: MillenniumDropdownMenuTheme = MillenniumTheme.dropdownMenuTheme,
    anchor: @Composable () -> Unit,
) {
    val filterState = rememberTextFieldState()
    LaunchedEffect(expanded) { if (!expanded) filterState.clearText() }
    val query = filterState.text.toString()
    val visibleItems = when {
        !filterable || query.isBlank() -> items
        onFilter != null -> onFilter(query)
        else -> items.filter { it.contains(query.trim(), ignoreCase = true) }
    }
    MillenniumDropdownMenu(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        side = side,
        width = itemWidth,
        panelBackground = panelBackground,
        panelShape = panelShape,
        panelBorderColor = panelBorderColor,
        panelBorderWidth = panelBorderWidth,
        panelShadowElevation = panelShadowElevation,
        panelGlowColor = panelGlowColor,
        minimumWidth = minimumWidth,
        sideOffset = sideOffset,
        panelPadding = panelPadding,
        sectionSpacing = itemSpacing,
        openAnimationMillis = openAnimationMillis,
        closeAnimationMillis = closeAnimationMillis,
        enterScale = enterScale,
        exitScale = exitScale,
        alpha = alpha,
        theme = theme,
        anchor = anchor,
    ) {
        if (filterable) {
            MillenniumDropdownMenuHeader(showDivider = false, theme = theme) {
                MillenniumDropdownMenuFilter(filterState, placeholder = filterPlaceholder)
            }
        }
        MillenniumDropdownMenuContent(
            maximumHeight = maximumHeight,
            contentSpacing = itemSpacing,
            theme = theme,
        ) {
            if (visibleItems.isEmpty()) MillenniumDropdownMenuEmpty(emptyText, textStyle = itemTextStyle, theme = theme)
            visibleItems.forEach { item ->
                MillenniumDropdownMenuItem(
                    label = item,
                    onClick = { onItemClick(item) },
                    background = itemBackground,
                    foreground = itemForeground,
                    shape = itemShape,
                    textStyle = itemTextStyle,
                    minimumHeight = itemMinimumHeight,
                    contentPadding = itemPadding,
                    borderColor = itemBorderColor,
                    borderWidth = itemBorderWidth,
                    innerGlowColor = itemGlowColor,
                    stateAnimationMillis = stateAnimationMillis,
                    theme = theme,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Sections: header / content / footer
// ---------------------------------------------------------------------------

/**
 * Fixed top region of a menu (filters, titles, account summaries) that stays put while the content scrolls.
 *
 * Place it as the first child of [MillenniumDropdownMenu], before [MillenniumDropdownMenuContent].
 * Children are laid out in a column; an optional hairline separates it from the rest.
 *
 * ```kotlin
 * MillenniumDropdownMenuHeader { MillenniumDropdownMenuFilter(filterState, placeholder = "Search") }
 * ```
 *
 * @param modifier Modifier for the header column.
 * @param showDivider Draw a [MillenniumDropdownMenuSeparator] below the header.
 * @param contentPadding Padding around the content; null uses [MillenniumDropdownMenuThemeProperties.sectionPadding].
 * @param contentSpacing Spacing between children; null uses the theme's item spacing.
 * @param theme Component theme; defaults to `MillenniumTheme.dropdownMenuTheme`.
 * @param content Header content in a [ColumnScope].
 * @see MillenniumDropdownMenuFooter
 * @see MillenniumDropdownMenuFilter
 */
@Composable
fun MillenniumDropdownMenuHeader(
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    contentPadding: PaddingValues? = null,
    contentSpacing: Dp? = null,
    theme: MillenniumDropdownMenuTheme = MillenniumTheme.dropdownMenuTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
    val properties = theme.states.default
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(properties.itemSpacing)) {
        Column(
            Modifier.fillMaxWidth().padding(contentPadding ?: properties.sectionPadding),
            verticalArrangement = Arrangement.spacedBy(contentSpacing ?: properties.itemSpacing),
        ) { content() }
        if (showDivider) MillenniumDropdownMenuSeparator(theme = theme)
    }
}

/**
 * The scrollable region of a menu. Its height is capped at [maximumHeight] and a themed scrollbar
 * appears only when the rows actually overflow.
 *
 * Put the rows, groups and separators here. Limit: rows inside this scroll region do not register with
 * Compose Unstyled's panel focus list, so Home/End and keyboard-open initial focus do not land on the
 * first row.
 *
 * ```kotlin
 * MillenniumDropdownMenuContent(maximumHeight = 240.dp) {
 *     tracks.forEach { MillenniumDropdownMenuItem(it.title, onClick = { play(it) }) }
 * }
 * ```
 *
 * @param modifier Modifier for the scroll container.
 * @param maximumHeight Height cap before scrolling; null uses [MillenniumDropdownMenuThemeProperties.maximumHeight].
 * @param contentSpacing Spacing between children; null uses the theme's item spacing.
 * @param showScrollbar Whether a [MillenniumScrollbar] may be drawn on overflow (8dp end padding is
 * reserved only while it is visible).
 * @param theme Component theme; defaults to `MillenniumTheme.dropdownMenuTheme`.
 * @param content Rows and bricks in a [ColumnScope].
 * @see MillenniumDropdownMenuHeader
 * @see MillenniumDropdownMenuFooter
 */
@Composable
fun MillenniumDropdownMenuContent(
    modifier: Modifier = Modifier,
    maximumHeight: Dp? = null,
    contentSpacing: Dp? = null,
    showScrollbar: Boolean = true,
    theme: MillenniumDropdownMenuTheme = MillenniumTheme.dropdownMenuTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
    val properties = theme.states.default
    val scrollState = rememberScrollState()
    val overflowing = showScrollbar && scrollState.maxValue > 0
    Box(modifier.fillMaxWidth().heightIn(max = maximumHeight ?: properties.maximumHeight)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(scrollState)
                .padding(end = if (overflowing) 8.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(contentSpacing ?: properties.itemSpacing),
        ) { content() }
        if (overflowing) {
            MillenniumScrollbar(scrollState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }
    }
}

/**
 * Fixed bottom region of a menu (actions, counts, hints) that stays put while the content scrolls.
 *
 * Place it as the last child of [MillenniumDropdownMenu], after [MillenniumDropdownMenuContent].
 *
 * ```kotlin
 * MillenniumDropdownMenuFooter { MillenniumText("Showing all results") }
 * ```
 *
 * @param modifier Modifier for the footer column.
 * @param showDivider Draw a [MillenniumDropdownMenuSeparator] above the footer.
 * @param contentPadding Padding around the content; null uses [MillenniumDropdownMenuThemeProperties.sectionPadding].
 * @param contentSpacing Spacing between children; null uses the theme's item spacing.
 * @param theme Component theme; defaults to `MillenniumTheme.dropdownMenuTheme`.
 * @param content Footer content in a [ColumnScope].
 * @see MillenniumDropdownMenuHeader
 */
@Composable
fun MillenniumDropdownMenuFooter(
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    contentPadding: PaddingValues? = null,
    contentSpacing: Dp? = null,
    theme: MillenniumDropdownMenuTheme = MillenniumTheme.dropdownMenuTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
    val properties = theme.states.default
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(properties.itemSpacing)) {
        if (showDivider) MillenniumDropdownMenuSeparator(theme = theme)
        Column(
            Modifier.fillMaxWidth().padding(contentPadding ?: properties.sectionPadding),
            verticalArrangement = Arrangement.spacedBy(contentSpacing ?: properties.itemSpacing),
        ) { content() }
    }
}

/**
 * A search field wired to caller-owned [state], usually placed in a [MillenniumDropdownMenuHeader].
 *
 * A thin wrapper over [MillenniumTextField] that fills the available width. The caller filters its own
 * rows from `state.text`; clear the state when the menu closes if the query should not persist.
 *
 * ```kotlin
 * val filter = rememberTextFieldState()
 * MillenniumDropdownMenuHeader { MillenniumDropdownMenuFilter(filter, placeholder = "Search actions") }
 * ```
 *
 * @param state Caller-owned text state.
 * @param modifier Modifier for the field (width is forced to fill).
 * @param placeholder Hint shown while the field is empty.
 * @param enabled Whether the field accepts input.
 * @see MillenniumDropdownMenuHeader
 */
@Composable
fun MillenniumDropdownMenuFilter(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String = "Filter",
    enabled: Boolean = true,
) {
    MillenniumTextField(
        state = state,
        placeholder = placeholder,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
    )
}

// ---------------------------------------------------------------------------
// Groups, labels, separators, empty state
// ---------------------------------------------------------------------------

/**
 * A cluster of related rows, typically opened by a [MillenniumDropdownMenuLabel].
 *
 * Purely structural: a full-width column with item spacing. It does not add semantics.
 *
 * ```kotlin
 * MillenniumDropdownMenuGroup {
 *     MillenniumDropdownMenuLabel("Playback")
 *     MillenniumDropdownMenuItem("Shuffle", onClick = {})
 * }
 * ```
 *
 * @param modifier Modifier for the column.
 * @param contentSpacing Spacing between children; null uses the theme's item spacing.
 * @param theme Component theme; defaults to `MillenniumTheme.dropdownMenuTheme`.
 * @param content Group rows in a [ColumnScope].
 * @see MillenniumDropdownMenuRadioGroup
 */
@Composable
fun MillenniumDropdownMenuGroup(
    modifier: Modifier = Modifier,
    contentSpacing: Dp? = null,
    theme: MillenniumDropdownMenuTheme = MillenniumTheme.dropdownMenuTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(contentSpacing ?: theme.states.default.itemSpacing),
    ) { content() }
}

/**
 * Non-interactive heading for a group; any composable may go inside.
 *
 * Text inherits the theme's group-label style and the muted palette colour.
 *
 * ```kotlin
 * MillenniumDropdownMenuLabel { Text("Account") }
 * ```
 *
 * @param modifier Modifier for the label box.
 * @param textStyle Typography override; null uses [MillenniumDropdownMenuThemeProperties.groupLabelStyle].
 * @param foreground Colour override; null uses `colors.muted`.
 * @param contentPadding Padding override; null uses the theme's group label padding.
 * @param theme Component theme; defaults to `MillenniumTheme.dropdownMenuTheme`.
 * @param content Label content.
 * @see MillenniumDropdownMenuGroup
 */
@Composable
fun MillenniumDropdownMenuLabel(
    modifier: Modifier = Modifier,
    textStyle: TextStyle? = null,
    foreground: Color? = null,
    contentPadding: PaddingValues? = null,
    theme: MillenniumDropdownMenuTheme = MillenniumTheme.dropdownMenuTheme,
    content: @Composable () -> Unit,
) {
    val properties = theme.states.default
    Box(modifier.fillMaxWidth().padding(contentPadding ?: properties.groupLabelPadding)) {
        MillenniumStyledContent(
            textStyle ?: properties.groupLabelStyle,
            foreground ?: MillenniumTheme.colors.muted,
            content,
        )
    }
}

/**
 * String convenience overload of [MillenniumDropdownMenuLabel].
 *
 * @param text Heading text.
 * @param modifier Modifier for the label box.
 * @param textStyle Typography override; null falls back to the theme.
 * @param foreground Colour override; null uses `colors.muted`.
 * @param contentPadding Padding override; null falls back to the theme.
 * @param theme Component theme; defaults to `MillenniumTheme.dropdownMenuTheme`.
 */
@Composable
fun MillenniumDropdownMenuLabel(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle? = null,
    foreground: Color? = null,
    contentPadding: PaddingValues? = null,
    theme: MillenniumDropdownMenuTheme = MillenniumTheme.dropdownMenuTheme,
) {
    MillenniumDropdownMenuLabel(modifier, textStyle, foreground, contentPadding, theme) { Text(text) }
}

/**
 * A hairline between groups, with 2dp of vertical padding.
 *
 * Renders a [MillenniumSeparator]; the [theme] parameter is accepted for API symmetry but is not read
 * by the current implementation.
 *
 * ```kotlin
 * MillenniumDropdownMenuSeparator()
 * ```
 *
 * @param modifier Modifier for the separator.
 * @param theme Component theme; defaults to `MillenniumTheme.dropdownMenuTheme` (currently unused).
 */
@Composable
fun MillenniumDropdownMenuSeparator(
    modifier: Modifier = Modifier,
    theme: MillenniumDropdownMenuTheme = MillenniumTheme.dropdownMenuTheme,
) {
    MillenniumSeparator(modifier.padding(vertical = 2.dp))
}

/**
 * Placeholder shown when a filtered list has no rows; any composable may go inside.
 *
 * Uses the item text style in the muted colour with the item padding.
 *
 * ```kotlin
 * if (visible.isEmpty()) MillenniumDropdownMenuEmpty { Text("Nothing matches") }
 * ```
 *
 * @param modifier Modifier for the placeholder box.
 * @param textStyle Typography override; null uses the theme's item text style.
 * @param contentPadding Padding override; null uses the theme's item padding.
 * @param theme Component theme; defaults to `MillenniumTheme.dropdownMenuTheme`.
 * @param content Placeholder content.
 */
@Composable
fun MillenniumDropdownMenuEmpty(
    modifier: Modifier = Modifier,
    textStyle: TextStyle? = null,
    contentPadding: PaddingValues? = null,
    theme: MillenniumDropdownMenuTheme = MillenniumTheme.dropdownMenuTheme,
    content: @Composable () -> Unit,
) {
    val properties = theme.states.default
    Box(modifier.fillMaxWidth().padding(contentPadding ?: properties.itemPadding)) {
        MillenniumStyledContent(textStyle ?: properties.itemTextStyle, MillenniumTheme.colors.muted, content)
    }
}

/**
 * String convenience overload of [MillenniumDropdownMenuEmpty].
 *
 * @param text Placeholder text; defaults to "No results".
 * @param modifier Modifier for the placeholder box.
 * @param textStyle Typography override; null falls back to the theme.
 * @param contentPadding Padding override; null falls back to the theme.
 * @param theme Component theme; defaults to `MillenniumTheme.dropdownMenuTheme`.
 */
@Composable
fun MillenniumDropdownMenuEmpty(
    text: String = "No results",
    modifier: Modifier = Modifier,
    textStyle: TextStyle? = null,
    contentPadding: PaddingValues? = null,
    theme: MillenniumDropdownMenuTheme = MillenniumTheme.dropdownMenuTheme,
) {
    MillenniumDropdownMenuEmpty(modifier, textStyle, contentPadding, theme) { Text(text) }
}


// ---------------------------------------------------------------------------
// Items
// ---------------------------------------------------------------------------

/**
 * Visual facts a row exposes to its custom content slot.
 *
 * Created by [MillenniumDropdownMenuItemContainer] on each state change, handed to its `content`
 * lambda, and also published through [LocalMillenniumDropdownMenuItemVisuals] for descendants. Colours
 * are already animated, so custom content can read them directly.
 *
 * @property state The resolved interaction state (disabled, pressed, focused, hovered, selected or default).
 * @property selected Whether the row is marked as the current value.
 * @property destructive Whether the row uses the danger palette.
 * @property foreground Current label colour (white on filled hover rows, red when destructive).
 * @property mutedForeground Current colour for secondary text such as descriptions and shortcuts.
 * @property iconTint Current colour for icons (accent when selected, muted otherwise).
 * @see MillenniumDropdownMenuItemContainer
 */
@Immutable
data class MillenniumDropdownMenuItemVisuals(
    val state: MillenniumControlState,
    val selected: Boolean,
    val destructive: Boolean,
    val foreground: Color,
    val mutedForeground: Color,
    val iconTint: Color,
)

/**
 * Hands the resolved row colours to any composable inside a row, e.g. [MillenniumDropdownMenuItemIcon].
 *
 * Provided by [MillenniumDropdownMenuItemContainer]; the value is null outside of a row, in which case
 * consumers fall back to their own defaults. Read-only: do not provide it yourself.
 *
 * ```kotlin
 * val visuals = LocalMillenniumDropdownMenuItemVisuals.current
 * Box(Modifier.size(8.dp).background(visuals?.iconTint ?: Color.Gray, CircleShape))
 * ```
 *
 * @see MillenniumDropdownMenuItemVisuals
 */
val LocalMillenniumDropdownMenuItemVisuals = staticCompositionLocalOf<MillenniumDropdownMenuItemVisuals?> { null }

/**
 * An icon that follows the row's state colours (white on a hovered gel row, red when destructive).
 *
 * Reads [LocalMillenniumDropdownMenuItemVisuals] for its tint, so place it inside a row slot. Pass
 * [contentDescription] only when the icon carries meaning that the label does not.
 *
 * ```kotlin
 * MillenniumDropdownMenuItem(onClick = {}, leading = { MillenniumDropdownMenuItemIcon(MillenniumIcons.Music) }) {
 *     Text("Playlists")
 * }
 * ```
 *
 * @param imageVector The icon to draw.
 * @param modifier Modifier for the icon.
 * @param contentDescription Accessibility description; null marks it decorative.
 * @param tint Explicit tint; null uses the row's `iconTint`, then the icon default.
 * @param size Icon size; null uses [MillenniumDropdownMenuThemeProperties.iconSize].
 * @param theme Component theme; defaults to `MillenniumTheme.dropdownMenuTheme`.
 * @see MillenniumIcon
 */
@Composable
fun MillenniumDropdownMenuItemIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color? = null,
    size: Dp? = null,
    theme: MillenniumDropdownMenuTheme = MillenniumTheme.dropdownMenuTheme,
) {
    MillenniumIcon(
        imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint ?: LocalMillenniumDropdownMenuItemVisuals.current?.iconTint,
        size = size ?: theme.states.default.iconSize,
    )
}

/**
 * A standard row where every part is a slot.
 *
 * Layout is [leading] (icon, avatar, indicator), the [label] with an optional [description] beneath it,
 * a [shortcut] hint, a [trailing] slot, and the [selectedIndicator] shown when [selected]. Text inside
 * the slots inherits the row's style and colours. The row is a Compose Unstyled dropdown item, so it has
 * button semantics, keyboard activation and a minimum height of 44dp. Hover, focus and press show an
 * accent gel row with spring scale; the focus look appears only for keyboard focus. [destructive]
 * switches to the danger palette. Use [MillenniumDropdownMenuItemContainer] when the layout itself must
 * be custom.
 *
 * ```kotlin
 * MillenniumDropdownMenuItem(
 *     onClick = { open(profile) },
 *     leading = { MillenniumAvatar("K", "Kingkor", size = 28.dp) },
 *     description = { Text("Following") },
 *     trailing = { MillenniumSidebarBadge("3") },
 * ) { Text("Kingkor Roy") }
 * ```
 *
 * @param onClick Called when the row is activated.
 * @param modifier Modifier applied to the row surface.
 * @param enabled Disabled rows are muted, not focusable, and ignore clicks.
 * @param selected Marks the row as the current value (soft accent wash plus [selectedIndicator]).
 * @param destructive Use the danger palette (for delete-style actions).
 * @param closeOnClick Whether activating the row requests closing the menu.
 * @param leading Content before the label; a spacing gap is added when non-null.
 * @param description Secondary text under the label, in the muted colour.
 * @param shortcut Hint at the end of the row (e.g. a key combination), in the muted colour.
 * @param trailing Content after the shortcut.
 * @param selectedIndicator Content shown after the row while [selected]; defaults to a check icon, null hides it.
 * @param background Row fill override; null falls back to the theme, then a state-derived fill.
 * @param foreground Label colour override; null falls back to the theme, then a state-derived colour.
 * @param shape Row shape override; null falls back to the theme.
 * @param textStyle Label typography override; null falls back to the theme.
 * @param minimumHeight Minimum row height override; null falls back to the theme (44dp).
 * @param contentPadding Padding inside the row; null falls back to the theme.
 * @param borderColor Border colour override; null falls back to the theme, then accent/danger while active.
 * @param borderWidth Border thickness override; null falls back to the theme.
 * @param innerGlowColor Inner glow override; null falls back to the theme.
 * @param stateAnimationMillis State transition duration; null falls back to the theme.
 * @param alpha Row opacity override; null falls back to the per-state theme alpha.
 * @param theme Component theme; defaults to `MillenniumTheme.dropdownMenuTheme`.
 * @param label The label content.
 * @see MillenniumDropdownMenuItemContainer
 * @see MillenniumDropdownMenuCheckboxItem
 * @see MillenniumDropdownMenuRadioItem
 */
@Composable
fun MillenniumDropdownMenuItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    destructive: Boolean = false,
    closeOnClick: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    description: (@Composable () -> Unit)? = null,
    shortcut: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    selectedIndicator: (@Composable () -> Unit)? = { MillenniumDropdownMenuItemIcon(MillenniumIcons.Check) },
    background: Brush? = null,
    foreground: Color? = null,
    shape: Shape? = null,
    textStyle: TextStyle? = null,
    minimumHeight: Dp? = null,
    contentPadding: PaddingValues? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    innerGlowColor: Color? = null,
    stateAnimationMillis: Int? = null,
    alpha: Float? = null,
    theme: MillenniumDropdownMenuTheme = MillenniumTheme.dropdownMenuTheme,
    label: @Composable () -> Unit,
) {
    val base = theme.states.default
    MillenniumDropdownMenuItemContainer(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        selected = selected,
        destructive = destructive,
        closeOnClick = closeOnClick,
        background = background,
        foreground = foreground,
        shape = shape,
        minimumHeight = minimumHeight,
        contentPadding = contentPadding,
        borderColor = borderColor,
        borderWidth = borderWidth,
        innerGlowColor = innerGlowColor,
        stateAnimationMillis = stateAnimationMillis,
        alpha = alpha,
        theme = theme,
    ) { visuals ->
        if (leading != null) {
            leading()
            Spacer(Modifier.width(base.iconLabelSpacing))
        }
        Column(Modifier.weight(1f)) {
            MillenniumStyledContent(textStyle ?: base.itemTextStyle, visuals.foreground, label)
            if (description != null) {
                MillenniumStyledContent(base.descriptionTextStyle, visuals.mutedForeground, description)
            }
        }
        if (shortcut != null) {
            Spacer(Modifier.width(base.iconLabelSpacing))
            MillenniumStyledContent(base.shortcutTextStyle, visuals.mutedForeground, shortcut)
        }
        if (trailing != null) {
            Spacer(Modifier.width(base.iconLabelSpacing))
            trailing()
        }
        if (selected && selectedIndicator != null) {
            Spacer(Modifier.width(base.iconLabelSpacing))
            selectedIndicator()
        }
    }
}

/**
 * String/icon convenience over the slotted [MillenniumDropdownMenuItem].
 *
 * The selected indicator keeps its default check icon; use the slotted overload to change it.
 *
 * ```kotlin
 * MillenniumDropdownMenuItem("Share", onClick = {}, description = "Copy a link", icon = MillenniumIcons.Sparkle)
 * ```
 *
 * @param label Row text.
 * @param onClick Called when the row is activated.
 * @param modifier Modifier applied to the row surface.
 * @param enabled Disabled rows are muted and ignore clicks.
 * @param selected Marks the row as the current value.
 * @param destructive Use the danger palette.
 * @param closeOnClick Whether activating the row requests closing the menu.
 * @param icon Optional leading icon drawn with [MillenniumDropdownMenuItemIcon].
 * @param description Optional secondary text under the label.
 * @param shortcut Optional hint at the end of the row.
 * @param trailing Optional content after the shortcut.
 * @param background Row fill override; null falls back to the theme.
 * @param foreground Label colour override; null falls back to the theme.
 * @param shape Row shape override; null falls back to the theme.
 * @param textStyle Label typography override; null falls back to the theme.
 * @param minimumHeight Minimum height override; null falls back to the theme (44dp).
 * @param contentPadding Row padding override; null falls back to the theme.
 * @param borderColor Border colour override; null falls back to the theme.
 * @param borderWidth Border thickness override; null falls back to the theme.
 * @param innerGlowColor Inner glow override; null falls back to the theme.
 * @param stateAnimationMillis State transition duration; null falls back to the theme.
 * @param alpha Row opacity override; null falls back to the theme.
 * @param theme Component theme; defaults to `MillenniumTheme.dropdownMenuTheme`.
 */
@Composable
fun MillenniumDropdownMenuItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    destructive: Boolean = false,
    closeOnClick: Boolean = true,
    icon: ImageVector? = null,
    description: String? = null,
    shortcut: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    background: Brush? = null,
    foreground: Color? = null,
    shape: Shape? = null,
    textStyle: TextStyle? = null,
    minimumHeight: Dp? = null,
    contentPadding: PaddingValues? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    innerGlowColor: Color? = null,
    stateAnimationMillis: Int? = null,
    alpha: Float? = null,
    theme: MillenniumDropdownMenuTheme = MillenniumTheme.dropdownMenuTheme,
) {
    MillenniumDropdownMenuItem(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        selected = selected,
        destructive = destructive,
        closeOnClick = closeOnClick,
        leading = icon?.let { { MillenniumDropdownMenuItemIcon(it, theme = theme) } },
        description = description?.let { { Text(it) } },
        shortcut = shortcut?.let { { Text(it) } },
        trailing = trailing,
        background = background,
        foreground = foreground,
        shape = shape,
        textStyle = textStyle,
        minimumHeight = minimumHeight,
        contentPadding = contentPadding,
        borderColor = borderColor,
        borderWidth = borderWidth,
        innerGlowColor = innerGlowColor,
        stateAnimationMillis = stateAnimationMillis,
        alpha = alpha,
        theme = theme,
    ) { Text(label) }
}

/**
 * The raw row surface: a row with fully custom content that receives the resolved
 * [MillenniumDropdownMenuItemVisuals].
 *
 * It owns interaction (hover, keyboard-only focus ring, press), the state-derived fill, border,
 * elevation and spring scale, and the pointer cursor, and wraps a Compose Unstyled dropdown item so
 * button semantics and keyboard activation are preserved. It also provides
 * [LocalMillenniumDropdownMenuItemVisuals] to its content. Prefer [MillenniumDropdownMenuItem] unless the
 * internal layout must be custom.
 *
 * ```kotlin
 * MillenniumDropdownMenuItemContainer(onClick = { select(track) }) { visuals ->
 *     MillenniumText(track.title, color = visuals.foreground, modifier = Modifier.weight(1f))
 *     MillenniumText(track.length, color = visuals.mutedForeground)
 * }
 * ```
 *
 * @param onClick Called when the row is activated.
 * @param modifier Modifier applied to the row surface.
 * @param enabled Disabled rows are muted and ignore input.
 * @param selected Marks the row as the current value (soft accent wash).
 * @param destructive Use the danger palette.
 * @param closeOnClick Whether activating the row requests closing the menu.
 * @param background Row fill override; null falls back to the theme, then a state-derived fill.
 * @param foreground Label colour override; null falls back to the theme, then a state-derived colour.
 * @param shape Row shape override; null falls back to the theme.
 * @param minimumHeight Minimum height override; null falls back to the theme (44dp).
 * @param contentPadding Padding inside the row; null falls back to the theme.
 * @param borderColor Border colour override; null falls back to the theme.
 * @param borderWidth Border thickness override; null falls back to the theme.
 * @param innerGlowColor Inner glow override; null falls back to the theme.
 * @param stateAnimationMillis State transition duration; null falls back to the theme.
 * @param alpha Row opacity override; null falls back to the per-state theme alpha.
 * @param theme Component theme; defaults to `MillenniumTheme.dropdownMenuTheme`.
 * @param content Row content in a [RowScope], given the resolved visuals; vertically centred.
 * @see MillenniumDropdownMenuItem
 * @see MillenniumDropdownMenuItemVisuals
 */
@Composable
fun MillenniumDropdownMenuItemContainer(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    destructive: Boolean = false,
    closeOnClick: Boolean = true,
    background: Brush? = null,
    foreground: Color? = null,
    shape: Shape? = null,
    minimumHeight: Dp? = null,
    contentPadding: PaddingValues? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    innerGlowColor: Color? = null,
    stateAnimationMillis: Int? = null,
    alpha: Float? = null,
    theme: MillenniumDropdownMenuTheme = MillenniumTheme.dropdownMenuTheme,
    content: @Composable RowScope.(MillenniumDropdownMenuItemVisuals) -> Unit,
) {
    val colors = MillenniumTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsKeyboardFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val state = when {
        !enabled -> MillenniumControlState.Disabled
        pressed -> MillenniumControlState.Pressed
        focused -> MillenniumControlState.Focused
        hovered -> MillenniumControlState.Hovered
        selected -> MillenniumControlState.Selected
        else -> MillenniumControlState.Default
    }
    val properties = theme.states.resolve(state)
    val resolvedStateMillis = stateAnimationMillis ?: properties.stateAnimationMillis
    val active = enabled && (hovered || focused || pressed)
    val resolvedShape = shape ?: properties.itemShape
    val resolvedBackground = background ?: properties.itemBackground ?: when {
        active && destructive -> Brush.verticalGradient(
            listOf(colors.danger.copy(alpha = 0.18f), colors.danger.copy(alpha = 0.10f)),
        )
        active -> millenniumBevelBrush(colors, accent = true)
        selected && enabled -> Brush.verticalGradient(
            listOf(colors.accent.copy(alpha = 0.14f), colors.accent.copy(alpha = 0.07f)),
        )
        else -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    }
    val onFilled = active && !destructive
    val targetForeground = foreground ?: properties.itemForeground ?: when {
        !enabled -> colors.muted
        destructive -> colors.danger
        onFilled -> colors.onAccent
        else -> colors.text
    }
    val animatedForeground by animateColorAsState(targetForeground, tween(resolvedStateMillis))
    val mutedForeground by animateColorAsState(
        when {
            onFilled -> colors.onAccent.copy(alpha = 0.82f)
            destructive && enabled -> colors.danger.copy(alpha = 0.8f)
            else -> colors.muted
        },
        tween(resolvedStateMillis),
    )
    val iconTint by animateColorAsState(
        when {
            !enabled -> colors.muted
            destructive -> colors.danger
            onFilled -> colors.onAccent
            selected -> colors.accent
            else -> colors.muted
        },
        tween(resolvedStateMillis),
    )
    val targetBorderColor = borderColor ?: properties.itemBorderColor
        ?: if (active) (if (destructive) colors.danger else colors.accentBottom) else Color.Transparent
    val animatedBorderColor by animateColorAsState(targetBorderColor, tween(resolvedStateMillis))
    val animatedAlpha by animateFloatAsState(alpha ?: properties.alpha, tween(resolvedStateMillis))
    val itemScale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            pressed -> 0.975f
            hovered || focused -> 1.012f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
    )
    val itemElevation by animateDpAsState(
        targetValue = if (active && !pressed) 2.dp else 0.dp,
        animationSpec = tween(resolvedStateMillis),
    )
    val visuals = remember(state, selected, destructive, animatedForeground, mutedForeground, iconTint) {
        MillenniumDropdownMenuItemVisuals(
            state, selected, destructive, animatedForeground, mutedForeground, iconTint,
        )
    }
    UnstyledDropdownMenuItem(
        onClick = onClick,
        enabled = enabled,
        closeOnClick = closeOnClick,
        indication = null,
        interactionSource = interaction,
        modifier = modifier.millenniumActionPointer(enabled).fillMaxWidth()
            .heightIn(min = minimumHeight ?: properties.itemMinimumHeight)
            .graphicsLayer { scaleX = itemScale; scaleY = itemScale }
            .alpha(animatedAlpha)
            .millenniumSurface(
                resolvedBackground, resolvedShape,
                borderColor = animatedBorderColor,
                borderWidth = if (active || borderColor != null || properties.itemBorderColor != null) {
                    borderWidth ?: properties.itemBorderWidth
                } else 0.dp,
                shadowElevation = itemElevation,
                // Translucent washes must not paint a shadow behind themselves.
                opaqueFill = onFilled || background != null,
                innerGlowColor = innerGlowColor ?: properties.itemGlowColor
                    ?: if (onFilled) colors.highlight.copy(alpha = 0.35f) else null,
            )
            .millenniumInteractionSurface(state, resolvedShape, animationMillis = resolvedStateMillis),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = minimumHeight ?: properties.itemMinimumHeight)
                .padding(contentPadding ?: properties.itemPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalMillenniumDropdownMenuItemVisuals provides visuals) { content(visuals) }
        }
    }
}

// ---------------------------------------------------------------------------
// Checkbox & radio items
// ---------------------------------------------------------------------------

/**
 * A toggleable row; [label], [description] and [shortcut] are slots. Stays open by default so several
 * options can be changed in one visit.
 *
 * Exposes `Role.Checkbox` with a toggleable state to accessibility services. The [indicator] sits in a
 * fixed icon-size leading slot and pops in with a spring when [checked] becomes true. State is
 * caller-owned: the row only reports the requested value.
 *
 * ```kotlin
 * var showLyrics by remember { mutableStateOf(true) }
 * MillenniumDropdownMenuCheckboxItem(showLyrics, { showLyrics = it }, shortcut = { Text("L") }) {
 *     Text("Show lyrics")
 * }
 * ```
 *
 * @param checked Current value.
 * @param onCheckedChange Called with the negated value when the row is activated.
 * @param modifier Modifier applied to the row (semantics are merged onto it).
 * @param enabled Disabled rows are muted and ignore clicks.
 * @param closeOnClick Whether toggling also closes the menu; false by default.
 * @param indicator Mark drawn while checked; defaults to a check icon that follows row colours.
 * @param description Optional secondary text under the label.
 * @param shortcut Optional hint at the end of the row.
 * @param trailing Optional content after the shortcut.
 * @param theme Component theme; defaults to `MillenniumTheme.dropdownMenuTheme`.
 * @param label The label content.
 * @see MillenniumDropdownMenuRadioItem
 */
@Composable
fun MillenniumDropdownMenuCheckboxItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    closeOnClick: Boolean = false,
    indicator: @Composable () -> Unit = { MillenniumDropdownMenuItemIcon(MillenniumIcons.Check) },
    description: (@Composable () -> Unit)? = null,
    shortcut: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    theme: MillenniumDropdownMenuTheme = MillenniumTheme.dropdownMenuTheme,
    label: @Composable () -> Unit,
) {
    MillenniumDropdownMenuItem(
        onClick = { onCheckedChange(!checked) },
        modifier = modifier.semantics(mergeDescendants = true) {
            role = Role.Checkbox
            toggleableState = ToggleableState(checked)
        },
        enabled = enabled,
        selected = checked,
        selectedIndicator = null,
        closeOnClick = closeOnClick,
        leading = { DropdownIndicatorSlot(checked, theme.states.default.iconSize, indicator) },
        description = description,
        shortcut = shortcut,
        trailing = trailing,
        theme = theme,
        label = label,
    )
}

/**
 * String convenience overload of the slotted [MillenniumDropdownMenuCheckboxItem].
 *
 * ```kotlin
 * MillenniumDropdownMenuCheckboxItem(checked, { checked = it }, "Crossfade", shortcut = "C")
 * ```
 *
 * @param checked Current value.
 * @param onCheckedChange Called with the negated value when activated.
 * @param label Row text.
 * @param modifier Modifier applied to the row.
 * @param enabled Disabled rows are muted and ignore clicks.
 * @param closeOnClick Whether toggling also closes the menu; false by default.
 * @param description Optional secondary text.
 * @param shortcut Optional hint at the end of the row.
 * @param theme Component theme; defaults to `MillenniumTheme.dropdownMenuTheme`.
 */
@Composable
fun MillenniumDropdownMenuCheckboxItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    closeOnClick: Boolean = false,
    description: String? = null,
    shortcut: String? = null,
    theme: MillenniumDropdownMenuTheme = MillenniumTheme.dropdownMenuTheme,
) {
    MillenniumDropdownMenuCheckboxItem(
        checked, onCheckedChange, modifier, enabled, closeOnClick,
        description = description?.let { { Text(it) } },
        shortcut = shortcut?.let { { Text(it) } },
        theme = theme,
    ) { Text(label) }
}

/**
 * Internal holder that shares a radio group's selected value with its items through [LocalDropdownRadioScope].
 */
@Stable
private class DropdownRadioScope(
    val value: Any?,
    val onValueChange: (Any?) -> Unit,
)

/**
 * Provided by [MillenniumDropdownMenuRadioGroup]; null when a radio item is used outside a group.
 */
private val LocalDropdownRadioScope = staticCompositionLocalOf<DropdownRadioScope?> { null }

/**
 * Shares one selected [value] among the [MillenniumDropdownMenuRadioItem]s inside it.
 *
 * Renders a [MillenniumDropdownMenuGroup] column. Selection is caller-owned: items call
 * [onValueChange] with their own value and the caller stores it.
 *
 * ```kotlin
 * var quality by remember { mutableStateOf("High") }
 * MillenniumDropdownMenuRadioGroup(quality, { quality = it }) {
 *     listOf("Low", "High", "Lossless").forEach { MillenniumDropdownMenuRadioItem(it, it) }
 * }
 * ```
 *
 * @param T Type of the values compared and reported.
 * @param value Currently selected value.
 * @param onValueChange Called with the value of the activated item.
 * @param modifier Modifier for the group column.
 * @param theme Component theme; defaults to `MillenniumTheme.dropdownMenuTheme`.
 * @param content The radio items (and any other bricks) in a [ColumnScope].
 * @see MillenniumDropdownMenuRadioItem
 */
@Composable
fun <T> MillenniumDropdownMenuRadioGroup(
    value: T,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    theme: MillenniumDropdownMenuTheme = MillenniumTheme.dropdownMenuTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = remember(value, onValueChange) {
        @Suppress("UNCHECKED_CAST")
        DropdownRadioScope(value) { onValueChange(it as T) }
    }
    CompositionLocalProvider(LocalDropdownRadioScope provides scope) {
        MillenniumDropdownMenuGroup(modifier, theme = theme, content = content)
    }
}

/**
 * One choice in a [MillenniumDropdownMenuRadioGroup]; [label] and [description] are slots.
 *
 * Exposes `Role.RadioButton` with a selected state. The row is selected when its [value] equals the
 * group's value; outside a group it is never selected and clicks do nothing. Stays open by default.
 *
 * ```kotlin
 * MillenniumDropdownMenuRadioItem("Lossless", description = { Text("FLAC, larger files") }) { Text("Lossless") }
 * ```
 *
 * @param T Type of the value.
 * @param value The value this item represents.
 * @param modifier Modifier applied to the row.
 * @param enabled Disabled rows are muted and ignore clicks.
 * @param closeOnClick Whether choosing also closes the menu; false by default.
 * @param indicator Mark shown while selected; defaults to an 8dp dot in the row's icon tint.
 * @param description Optional secondary text under the label.
 * @param shortcut Optional hint at the end of the row.
 * @param trailing Optional content after the shortcut.
 * @param theme Component theme; defaults to `MillenniumTheme.dropdownMenuTheme`.
 * @param label The label content.
 * @see MillenniumDropdownMenuRadioGroup
 */
@Composable
fun <T> MillenniumDropdownMenuRadioItem(
    value: T,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    closeOnClick: Boolean = false,
    indicator: @Composable () -> Unit = {
        Box(
            Modifier.size(8.dp).clip(CircleShape)
                .background(LocalMillenniumDropdownMenuItemVisuals.current?.iconTint ?: MillenniumTheme.colors.accent),
        )
    },
    description: (@Composable () -> Unit)? = null,
    shortcut: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    theme: MillenniumDropdownMenuTheme = MillenniumTheme.dropdownMenuTheme,
    label: @Composable () -> Unit,
) {
    val group = LocalDropdownRadioScope.current
    val isSelected = group?.value == value
    MillenniumDropdownMenuItem(
        onClick = { group?.onValueChange(value) },
        modifier = modifier.semantics(mergeDescendants = true) {
            role = Role.RadioButton
            selected = isSelected
        },
        enabled = enabled,
        selected = isSelected,
        selectedIndicator = null,
        closeOnClick = closeOnClick,
        leading = { DropdownIndicatorSlot(isSelected, theme.states.default.iconSize, indicator) },
        description = description,
        shortcut = shortcut,
        trailing = trailing,
        theme = theme,
        label = label,
    )
}

/**
 * String convenience overload of the slotted [MillenniumDropdownMenuRadioItem].
 *
 * @param T Type of the value.
 * @param value The value this item represents.
 * @param label Row text.
 * @param modifier Modifier applied to the row.
 * @param enabled Disabled rows are muted and ignore clicks.
 * @param closeOnClick Whether choosing also closes the menu; false by default.
 * @param description Optional secondary text.
 * @param theme Component theme; defaults to `MillenniumTheme.dropdownMenuTheme`.
 */
@Composable
fun <T> MillenniumDropdownMenuRadioItem(
    value: T,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    closeOnClick: Boolean = false,
    description: String? = null,
    theme: MillenniumDropdownMenuTheme = MillenniumTheme.dropdownMenuTheme,
) {
    MillenniumDropdownMenuRadioItem(
        value, modifier, enabled, closeOnClick,
        description = description?.let { { Text(it) } },
        theme = theme,
    ) { Text(label) }
}

/**
 * Fixed-size leading box that springs its indicator in and out; keeps label alignment stable when unchecked.
 */
@Composable
private fun DropdownIndicatorSlot(visible: Boolean, size: Dp, content: @Composable () -> Unit) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), initialScale = 0.4f) + fadeIn(),
            exit = scaleOut(targetScale = 0.4f) + fadeOut(),
        ) { content() }
    }
}


/**
 * Preview showing the full lego composition (filter, groups, toggles, radios, footer).
 */
@Preview
@Composable
private fun MillenniumDropdownMenuPreview() = MillenniumPreview {
    var showLyrics by remember { mutableStateOf(true) }
    var quality by remember { mutableStateOf("High") }
    val filter = rememberTextFieldState()
    MillenniumDropdownMenu(
        expanded = true,
        onExpandedChange = {},
        anchor = { MillenniumButton(onClick = {}) { Text("Track options") } },
    ) {
        MillenniumDropdownMenuHeader { MillenniumDropdownMenuFilter(filter, placeholder = "Search actions") }
        MillenniumDropdownMenuContent {
            MillenniumDropdownMenuGroup {
                MillenniumDropdownMenuLabel { Text("Track") }
                MillenniumDropdownMenuItem("Add to playlist", onClick = {}, icon = MillenniumIcons.Music, shortcut = "Ctrl+P")
                MillenniumDropdownMenuItem("Share", onClick = {}, description = "Copy a link", icon = MillenniumIcons.Sparkle)
                MillenniumDropdownMenuItem(
                    onClick = {},
                    leading = { MillenniumAvatar("K", "Kingkor", size = 28.dp) },
                    description = { Text("Following") },
                    trailing = { MillenniumSidebarBadge("3") },
                ) { Text("Kingkor Roy", fontWeight = FontWeight.SemiBold) }
                MillenniumDropdownMenuItem("Unavailable", onClick = {}, enabled = false)
            }
            MillenniumDropdownMenuSeparator()
            MillenniumDropdownMenuGroup {
                MillenniumDropdownMenuLabel("Playback")
                MillenniumDropdownMenuCheckboxItem(showLyrics, { showLyrics = it }, shortcut = { Text("L") }) { Text("Show lyrics") }
                MillenniumDropdownMenuRadioGroup(quality, { quality = it }) {
                    listOf("Low", "High", "Lossless").forEach { MillenniumDropdownMenuRadioItem(it, it) }
                }
            }
            MillenniumDropdownMenuSeparator()
            MillenniumDropdownMenuItem("Remove from library", onClick = {}, destructive = true)
        }
        MillenniumDropdownMenuFooter { MillenniumText("Quality: $quality", color = MillenniumTheme.colors.muted, size = 11.sp) }
    }
}
