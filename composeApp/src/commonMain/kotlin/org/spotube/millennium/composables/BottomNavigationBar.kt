package org.spotube.millennium.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import org.spotube.millennium.theme.collectIsKeyboardFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import com.composeunstyled.UnstyledButton
import org.spotube.millennium.theme.*

/**
 * Appearance of the [MillenniumBottomNavigationBar] chrome (the container behind the items).
 *
 * Nullable colors and brushes derive from the active palette when `null`. The bar currently reads
 * only the default state of [MillenniumBottomNavigationBarTheme.states].
 *
 * @property background Container fill; `null` uses a gel gradient from the palette's raised to
 *   surface colors.
 * @property foreground Ambient content color for the items; `null` uses the palette text color.
 * @property shape Container shape; defaults to 16dp rounded top corners.
 * @property contentPadding Padding between the container edge and the items (default 8dp).
 * @property itemSpacing Horizontal gap between items (default 6dp).
 * @property minimumHeight Minimum container height before insets are added to the padding
 *   (default 76dp).
 * @property borderColor Hairline border color; `null` uses the palette border color.
 * @property borderWidth Hairline border width (default 1dp).
 * @property shadowElevation Drop shadow elevation (default 8dp).
 * @property innerGlowColor Inner top glow; `null` uses the palette highlight at 55 percent alpha.
 * @see MillenniumBottomNavigationBar
 * @see defaultMillenniumBottomNavigationBarTheme
 */
@Immutable
data class MillenniumBottomNavigationBarThemeProperties(
    val background: Brush? = null,
    val foreground: Color? = null,
    val shape: Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    val contentPadding: PaddingValues = PaddingValues(8.dp),
    val itemSpacing: Dp = 6.dp,
    val minimumHeight: Dp = 76.dp,
    val borderColor: Color? = null,
    val borderWidth: Dp = 1.dp,
    val shadowElevation: Dp = 8.dp,
    val innerGlowColor: Color? = null,
)

/**
 * Appearance of a single [MillenniumBottomNavigationItem] in one interaction state.
 *
 * Nullable colors and brushes are derived from the palette and the item state when `null`: selected
 * items get the accent gel with the on-accent foreground, pressed items a sunken bevel, hovered or
 * keyboard-focused items a raised bevel, and resting items are transparent.
 *
 * @property background Item fill; `null` derives it from the state as described above.
 * @property foreground Icon and label color; `null` uses on-accent when selected, muted when
 *   disabled, otherwise the palette text color.
 * @property shape Item shape (default 10dp rounded corners).
 * @property contentPadding Padding inside the item (default 8dp horizontal and vertical).
 * @property minimumHeight Minimum item height (default 60dp, above the 44dp target size).
 * @property minimumWidth Minimum item width (default 48dp); items are otherwise equal-width.
 * @property iconSize Size of the icon box in the slot-based overload (default 22dp).
 * @property iconLabelSpacing Gap between the icon and the label (default 4dp).
 * @property labelStyle Ambient text style of the item content (default 12sp medium).
 * @property borderColor Border color; `null` uses accent when focused, the accent gradient bottom
 *   when selected, the palette border when hovered or pressed, otherwise transparent.
 * @property borderWidth Border width (default 1dp; the default theme uses 2dp when focused).
 * @property shadowElevation Animated drop shadow elevation (default 0dp).
 * @property innerGlowColor Inner glow; `null` uses a 35 percent palette highlight when selected,
 *   hovered or focused, otherwise none.
 * @property alpha Overall item opacity (the default theme dims disabled items to 0.45).
 * @property animationMillis Duration in milliseconds of the foreground, border and alpha tweens
 *   (default 150).
 * @see MillenniumBottomNavigationItem
 */
@Immutable
data class MillenniumBottomNavigationItemThemeProperties(
    val background: Brush? = null,
    val foreground: Color? = null,
    val shape: Shape = RoundedCornerShape(10.dp),
    val contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    val minimumHeight: Dp = 60.dp,
    val minimumWidth: Dp = 48.dp,
    val iconSize: Dp = 22.dp,
    val iconLabelSpacing: Dp = 4.dp,
    val labelStyle: TextStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    val borderColor: Color? = null,
    val borderWidth: Dp = 1.dp,
    val shadowElevation: Dp = 0.dp,
    val innerGlowColor: Color? = null,
    val alpha: Float = 1f,
    val animationMillis: Int = 150,
)

/**
 * Theme for the bottom navigation bar and its items, exposed as
 * [MillenniumTheme.bottomNavigationBarTheme].
 *
 * ```kotlin
 * val base = defaultMillenniumBottomNavigationBarTheme()
 * val taller = base.copy(
 *     item = base.item.copy(default = base.item.default.copy(minimumHeight = 68.dp)),
 * )
 * MillenniumBottomNavigationBar(theme = taller) { }
 * ```
 *
 * @property states Chrome properties per control state (the bar reads the default state).
 * @property item Item properties per control state: default, hovered, focused, pressed, disabled
 *   and selected.
 */
@Immutable
data class MillenniumBottomNavigationBarTheme(
    val states: MillenniumControlStateProperty<MillenniumBottomNavigationBarThemeProperties>,
    val item: MillenniumControlStateProperty<MillenniumBottomNavigationItemThemeProperties>,
)

/**
 * Builds the default [MillenniumBottomNavigationBarTheme].
 *
 * The chrome uses the same properties in every state. Items differ per state: hovered raises the
 * shadow to 1dp, focused widens the border to 2dp, pressed drops the shadow to 0dp, disabled fades
 * to 0.45 alpha, and selected adds a 3dp shadow with a semibold label. Customise with `copy` on
 * [MillenniumBottomNavigationBarTheme] and its state properties.
 */
fun defaultMillenniumBottomNavigationBarTheme(): MillenniumBottomNavigationBarTheme {
    val item = MillenniumBottomNavigationItemThemeProperties()
    return MillenniumBottomNavigationBarTheme(
        states = MillenniumControlStateProperty.all(MillenniumBottomNavigationBarThemeProperties()),
        item = MillenniumControlStateProperty(
            default = item,
            hovered = item.copy(shadowElevation = 1.dp),
            focused = item.copy(borderWidth = 2.dp),
            pressed = item.copy(shadowElevation = 0.dp),
            disabled = item.copy(alpha = 0.45f),
            selected = item.copy(shadowElevation = 3.dp, labelStyle = item.labelStyle.copy(fontWeight = FontWeight.SemiBold)),
        ),
    )
}

/**
 * Pearl/smoked-glass navigation chrome. Place outside the scrolling body in a
 * Column to keep it fixed at the bottom. Selection and routing belong to callers.
 * Typically holds 3–5 items; this is not a horizontally scrolling menu.
 * Pass WindowInsets(0, 0, 0, 0) when a parent already handles system insets.
 *
 * The bar is a full-width [Row] drawn as a raised glass surface (gel gradient, hairline border,
 * inner glow, shadow) that pads itself by [windowInsets] and wraps its children in
 * `selectableGroup()`, so assistive technology reports them as one mutually exclusive set. Items
 * are laid out by [MillenniumBottomNavigationItem] with equal weight. The bar has no state of its
 * own; it only provides the ambient foreground color to its content.
 *
 * ```kotlin
 * var current by remember { mutableStateOf("home") }
 * MillenniumBottomNavigationBar {
 *     MillenniumBottomNavigationItem(
 *         selected = current == "home",
 *         onClick = { current = "home" },
 *         contentDescription = "Home",
 *         icon = { MillenniumIcon(MillenniumIcons.Sparkle) },
 *         label = { Text("Home") },
 *     )
 *     MillenniumBottomNavigationItem(
 *         selected = current == "library",
 *         onClick = { current = "library" },
 *         icon = { MillenniumIcon(MillenniumIcons.Music) },
 *         label = { Text("Library") },
 *         badge = { MillenniumSidebarBadge("3") },
 *     )
 * }
 * ```
 *
 * @param modifier Modifier for the container.
 * @param windowInsets Insets added as padding inside the surface. Defaults to
 *   `WindowInsets.navigationBars`; pass `WindowInsets(0, 0, 0, 0)` to embed.
 * @param background Overrides the container fill; `null` falls back to the theme, then a palette
 *   gel gradient.
 * @param foreground Overrides the ambient content color; `null` falls back to the theme, then the
 *   palette text color.
 * @param shape Overrides the container shape; `null` uses the theme.
 * @param contentPadding Overrides the padding around items; `null` uses the theme.
 * @param itemSpacing Overrides the gap between items; `null` uses the theme.
 * @param minimumHeight Overrides the minimum height; `null` uses the theme.
 * @param borderColor Overrides the border color; `null` falls back to the theme, then the palette
 *   border color.
 * @param borderWidth Overrides the border width; `null` uses the theme.
 * @param shadowElevation Overrides the shadow elevation; `null` uses the theme.
 * @param innerGlowColor Overrides the inner glow; `null` falls back to the theme, then a palette
 *   highlight.
 * @param theme Theme supplying defaults for every nullable override. Defaults to
 *   [MillenniumTheme.bottomNavigationBarTheme].
 * @param content The destinations, normally several [MillenniumBottomNavigationItem] calls.
 * @see MillenniumBottomNavigationItem
 * @see MillenniumScaffold
 */
@Composable
fun MillenniumBottomNavigationBar(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.navigationBars,
    background: Brush? = null,
    foreground: Color? = null,
    shape: Shape? = null,
    contentPadding: PaddingValues? = null,
    itemSpacing: Dp? = null,
    minimumHeight: Dp? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    shadowElevation: Dp? = null,
    innerGlowColor: Color? = null,
    theme: MillenniumBottomNavigationBarTheme = MillenniumTheme.bottomNavigationBarTheme,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MillenniumTheme.colors
    val properties = theme.states.default
    Row(
        modifier = modifier.fillMaxWidth()
            .millenniumSurface(
                background = background ?: properties.background ?: gelBrush(colors.raised, colors.surface),
                shape = shape ?: properties.shape,
                borderColor = borderColor ?: properties.borderColor ?: colors.border,
                borderWidth = borderWidth ?: properties.borderWidth,
                shadowElevation = shadowElevation ?: properties.shadowElevation,
                innerGlowColor = innerGlowColor ?: properties.innerGlowColor ?: colors.highlight.copy(alpha = 0.55f),
            )
            .windowInsetsPadding(windowInsets)
            .heightIn(min = minimumHeight ?: properties.minimumHeight)
            .padding(contentPadding ?: properties.contentPadding)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing ?: properties.itemSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MillenniumStyledContent(TextStyle.Default, foreground ?: properties.foreground ?: colors.text) { content() }
    }
}

/**
 * Palette-aware facts for custom icon, label and badge content.
 *
 * Created by the custom-content [MillenniumBottomNavigationItem] on each composition and passed to
 * its `content` lambda; it is immutable and should not be retained across compositions.
 *
 * @property selected Whether the destination is the current one (caller-owned).
 * @property enabled Whether the item accepts input.
 * @property state The resolved interaction state (disabled, pressed, focused, hovered, selected or
 *   default, in that priority order). Focused reflects keyboard focus only.
 * @property foreground The animated foreground color; use it to tint custom icons and badges.
 */
@Immutable
data class MillenniumBottomNavigationItemVisuals(
    val selected: Boolean,
    val enabled: Boolean,
    val state: MillenniumControlState,
    val foreground: Color,
)

/**
 * Equal-width destination with independently composable icon, label and badge.
 *
 * Convenience overload built on the custom-content overload: the [icon] is centered in a box of the
 * themed icon size, the optional [badge] is aligned to that box's top end, and the optional [label]
 * is stacked beneath. Selection, routing and the badge count are caller-owned. Behavior (states,
 * motion, accessibility) is described on the custom-content overload.
 *
 * ```kotlin
 * MillenniumBottomNavigationBar {
 *     MillenniumBottomNavigationItem(
 *         selected = tab == Tab.Inbox,
 *         onClick = { tab = Tab.Inbox },
 *         contentDescription = "Inbox, 2 unread",
 *         icon = { MillenniumIcon(MillenniumIcons.Music) },
 *         label = { Text("Inbox") },
 *         badge = { MillenniumSidebarBadge("2") },
 *     )
 * }
 * ```
 *
 * @param selected Whether this destination is current; exposed as selected semantics.
 * @param onClick Called when the item is activated.
 * @param modifier Modifier applied to the item (weight and size modifiers are added internally).
 * @param enabled Whether the item accepts input; disabled items are dimmed.
 * @param contentDescription Optional accessibility description; set it when the label is omitted or
 *   the badge carries meaning.
 * @param iconSize Overrides the icon box size; `null` uses the theme's item icon size.
 * @param theme Theme supplying item properties. Defaults to
 *   [MillenniumTheme.bottomNavigationBarTheme].
 * @param icon The icon content; follows the item foreground color.
 * @param label Optional label shown below the icon.
 * @param badge Optional badge overlaid on the icon's top end.
 * @see MillenniumBottomNavigationBar
 */
@Composable
fun RowScope.MillenniumBottomNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    iconSize: Dp? = null,
    theme: MillenniumBottomNavigationBarTheme = MillenniumTheme.bottomNavigationBarTheme,
    icon: @Composable () -> Unit,
    label: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
) {
    MillenniumBottomNavigationItem(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentDescription = contentDescription,
        theme = theme,
    ) { visuals ->
        val properties = theme.item.resolve(visuals.state)
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(iconSize ?: properties.iconSize), contentAlignment = Alignment.Center) { icon() }
            if (badge != null) {
                Box(Modifier.align(Alignment.TopEnd)) { badge() }
            }
        }
        if (label != null) label()
    }
}

/**
 * Fully custom item body. Content inherits the state-specific foreground/style.
 *
 * Renders a [UnstyledButton] with `Role.Tab`, `selected` semantics and an optional content
 * description, so selection and activation are announced by assistive technology and handled by
 * keyboard (Tab to focus, Space or Enter to activate). The item takes an equal share of the bar's
 * width, with at least 60dp height by default (above the 44dp minimum target). Indication is
 * disabled in favor of Millennium's own surface: selected items use the accent gel with the
 * on-accent foreground, hover and keyboard focus raise a bevel, press sinks it, and a focus border
 * is drawn only for keyboard focus (pointer clicks do not show a ring). Hover and press scale the
 * item with a spring (hover is skipped for the selected item); foreground, border and alpha
 * animate over `animationMillis`; elevation animates with a spring. Color is never the only
 * selection cue: selected items also gain a heavier shadow and, in the default theme, a semibold
 * label. The pointer shows the action cursor when enabled.
 *
 * ```kotlin
 * MillenniumBottomNavigationBar {
 *     MillenniumBottomNavigationItem(
 *         selected = tab == 1,
 *         onClick = { tab = 1 },
 *         contentDescription = "Now playing",
 *     ) { visuals ->
 *         MillenniumIcon(MillenniumIcons.Music, tint = visuals.foreground)
 *         if (visuals.selected) Text("Playing")
 *     }
 * }
 * ```
 *
 * @param selected Whether this destination is current (caller-owned).
 * @param onClick Called when the item is activated.
 * @param modifier Modifier applied to the item before the internal weight and size modifiers.
 * @param enabled Whether the item accepts input; disabled items use the disabled state properties.
 * @param contentDescription Optional accessibility description added to the item semantics.
 * @param background Overrides the item fill; `null` falls back to the theme, then a state-derived
 *   brush (accent gel when selected, bevels for pressed/hover/focus, transparent otherwise).
 * @param foreground Overrides the foreground color; `null` falls back to the theme, then a
 *   state-derived color.
 * @param shape Overrides the item shape; `null` uses the theme.
 * @param contentPadding Overrides the padding inside the button; `null` uses the theme.
 * @param minimumHeight Overrides the minimum height; `null` uses the theme.
 * @param minimumWidth Overrides the minimum width; `null` uses the theme.
 * @param iconLabelSpacing Overrides the vertical gap between content rows; `null` uses the theme.
 * @param labelStyle Overrides the ambient text style; `null` uses the theme.
 * @param borderColor Overrides the border color (before animation); `null` falls back to the theme,
 *   then a state-derived color.
 * @param borderWidth Overrides the border width; `null` uses the theme.
 * @param shadowElevation Overrides the target shadow elevation; `null` uses the theme.
 * @param innerGlowColor Overrides the inner glow; `null` falls back to the theme, then a palette
 *   highlight for selected/hovered/focused states.
 * @param alpha Overrides the item opacity; `null` uses the theme.
 * @param animationMillis Overrides the tween duration in milliseconds; `null` uses the theme.
 * @param theme Theme supplying defaults for every nullable override. Defaults to
 *   [MillenniumTheme.bottomNavigationBarTheme].
 * @param content Item body in a centered [ColumnScope], receiving [MillenniumBottomNavigationItemVisuals].
 * @see MillenniumBottomNavigationBar
 * @see MillenniumBottomNavigationItemVisuals
 */
@Composable
fun RowScope.MillenniumBottomNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    background: Brush? = null,
    foreground: Color? = null,
    shape: Shape? = null,
    contentPadding: PaddingValues? = null,
    minimumHeight: Dp? = null,
    minimumWidth: Dp? = null,
    iconLabelSpacing: Dp? = null,
    labelStyle: TextStyle? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    shadowElevation: Dp? = null,
    innerGlowColor: Color? = null,
    alpha: Float? = null,
    animationMillis: Int? = null,
    theme: MillenniumBottomNavigationBarTheme = MillenniumTheme.bottomNavigationBarTheme,
    content: @Composable ColumnScope.(MillenniumBottomNavigationItemVisuals) -> Unit,
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
    val properties = theme.item.resolve(state)
    val targetForeground = foreground ?: properties.foreground
        ?: bottomNavigationItemForeground(colors, selected, enabled)
    val resolvedAnimationMillis = animationMillis ?: properties.animationMillis
    val resolvedForeground by animateColorAsState(
        targetValue = targetForeground,
        animationSpec = tween(resolvedAnimationMillis),
    )
    val resolvedBackground = background ?: properties.background ?: when {
        selected -> gelBrush(colors.accentTop, colors.accentBottom)
        pressed -> millenniumBevelBrush(colors, sunken = true)
        hovered || focused -> millenniumBevelBrush(colors)
        else -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    }
    val elevation by animateDpAsState(
        shadowElevation ?: properties.shadowElevation,
        spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessMediumLow),
    )
    val animatedAlpha by animateFloatAsState(alpha ?: properties.alpha, tween(resolvedAnimationMillis))
    val itemScale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            pressed -> 0.96f
            hovered && !selected -> 1.02f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMedium),
    )
    val targetBorderColor = borderColor ?: properties.borderColor ?: when {
        focused -> colors.accent
        selected -> colors.accentBottom
        hovered || pressed -> colors.border
        else -> Color.Transparent
    }
    val animatedBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(resolvedAnimationMillis),
    )
    UnstyledButton(
        onClick = onClick,
        enabled = enabled,
        role = Role.Tab,
        interactionSource = interaction,
        indication = null,
        contentPadding = contentPadding ?: properties.contentPadding,
        modifier = modifier.millenniumActionPointer(enabled).weight(1f)
            .heightIn(min = minimumHeight ?: properties.minimumHeight)
            .widthIn(min = minimumWidth ?: properties.minimumWidth)
            .semantics {
                this.selected = selected
                if (contentDescription != null) this.contentDescription = contentDescription
            }
            .graphicsLayer { scaleX = itemScale; scaleY = itemScale }
            .alpha(animatedAlpha)
            .millenniumSurface(
                background = resolvedBackground,
                shape = shape ?: properties.shape,
                borderColor = animatedBorderColor,
                borderWidth = borderWidth ?: properties.borderWidth,
                shadowElevation = elevation,
                innerGlowColor = innerGlowColor ?: properties.innerGlowColor ?: if (selected || hovered || focused) {
                    colors.highlight.copy(alpha = 0.35f)
                } else null,
            )
            .millenniumInteractionSurface(state, shape ?: properties.shape, animationMillis = resolvedAnimationMillis),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(iconLabelSpacing ?: properties.iconLabelSpacing),
        ) {
            MillenniumStyledContent(labelStyle ?: properties.labelStyle, resolvedForeground) {
                content(MillenniumBottomNavigationItemVisuals(selected, enabled, state, resolvedForeground))
            }
        }
    }
}

/**
 * Default item foreground: on-accent when selected, muted when disabled, otherwise palette text.
 * Selected takes priority over disabled.
 */
internal fun bottomNavigationItemForeground(colors: MillenniumColors, selected: Boolean, enabled: Boolean): Color = when {
    selected -> colors.onAccent
    !enabled -> colors.muted
    else -> colors.text
}

/** Design-time preview of a three-item [MillenniumBottomNavigationBar]. */
@Preview
@Composable
private fun MillenniumBottomNavigationBarPreview() = MillenniumPreview {
    MillenniumBottomNavigationBar(windowInsets = WindowInsets(0, 0, 0, 0)) {
        listOf("Discover", "Library", "Saved").forEachIndexed { index, label ->
            MillenniumBottomNavigationItem(
                selected = index == 0,
                onClick = {},
                contentDescription = label,
                icon = {
                    com.composeunstyled.UnstyledIcon(
                        if (index == 0) MillenniumIcons.Sparkle else MillenniumIcons.Music,
                        contentDescription = null,
                        tint = com.composeunstyled.LocalTextStyle.current.color,
                    )
                },
                label = { Text(label) },
            )
        }
    }
}
