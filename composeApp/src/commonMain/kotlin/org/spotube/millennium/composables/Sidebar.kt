package org.spotube.millennium.composables

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumColors
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumPreview
import org.spotube.millennium.theme.MillenniumStyledContent
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumActionPointer
import org.spotube.millennium.theme.gelBrush
import org.spotube.millennium.theme.millenniumBevelBrush
import org.spotube.millennium.theme.millenniumSurface
import org.spotube.millennium.theme.millenniumInteractionSurface
import org.spotube.millennium.theme.millenniumPanelBrush

// ---------------------------------------------------------------------------
// Theme
// ---------------------------------------------------------------------------

/**
 * Raised shell that frames a sidebar: width, surface, shape and motion.
 *
 * Read from the default state of [MillenniumSidebarTheme.root]. Nullable colors and brushes derive
 * from the active palette when `null`.
 *
 * @property background Shell fill; `null` uses the shared glass panel brush.
 * @property foreground Ambient content color for all children; `null` uses the palette text color.
 * @property width Expanded width (default 264dp).
 * @property collapsedWidth Icon-rail width when collapsed (default 76dp).
 * @property shape Shell shape (default rounded 12dp on the top and bottom end corners).
 * @property borderColor Border color; `null` uses the palette border color.
 * @property borderWidth Border width (default 1dp).
 * @property shadowElevation Drop shadow elevation (default 8dp).
 * @property innerGlowColor Inner glow; `null` uses the palette highlight at 55 percent alpha.
 * @property stateAnimationMillis Reserved animation duration in milliseconds for state changes
 *   (default 180); width changes currently use a fixed spring.
 * @property alpha Overall shell opacity (default 1).
 * @see MillenniumSidebar
 */
@Immutable
data class MillenniumSidebarRootProperties(
    val background: Brush? = null,
    val foreground: Color? = null,
    val width: Dp,
    val collapsedWidth: Dp,
    val shape: Shape,
    val borderColor: Color? = null,
    val borderWidth: Dp,
    val shadowElevation: Dp,
    val innerGlowColor: Color? = null,
    val stateAnimationMillis: Int,
    val alpha: Float = 1f,
)

/**
 * Fixed top region of a sidebar.
 *
 * @property contentPadding Padding around the header content (default 14dp sides, 16dp top, 12dp
 *   bottom).
 * @property contentSpacing Vertical gap between header children (default 12dp).
 * @property minimumHeight Minimum content height (default 0dp, i.e. wrap content).
 * @property background Header band fill; `null` uses a gel gradient from the palette raised to
 *   surface colors.
 * @property dividerColor Color of the hairline under the header; `null` uses the palette border.
 * @property dividerWidth Thickness of each hairline stripe (default 1dp); 0dp hides the divider.
 * @see MillenniumSidebarHeader
 */
@Immutable
data class MillenniumSidebarHeaderProperties(
    val contentPadding: PaddingValues,
    val contentSpacing: Dp,
    val minimumHeight: Dp,
    val background: Brush? = null,
    val dividerColor: Color? = null,
    val dividerWidth: Dp,
)

/**
 * The scrollable middle region of a sidebar.
 *
 * @property contentPadding Padding inside the scroll area (default 10dp).
 * @property contentSpacing Vertical gap between groups and loose items (default 14dp).
 * @property showScrollbar Whether the themed scrollbar overlays the end edge (default true).
 * @property scrollbarEndPadding Extra end padding reserved for the scrollbar when it is shown
 *   (default 6dp).
 * @see MillenniumSidebarContent
 */
@Immutable
data class MillenniumSidebarContentProperties(
    val contentPadding: PaddingValues,
    val contentSpacing: Dp,
    val showScrollbar: Boolean,
    val scrollbarEndPadding: Dp,
)

/**
 * Fixed bottom region of a sidebar.
 *
 * @property contentPadding Padding around the footer content (default 14dp).
 * @property contentSpacing Vertical gap between footer children (default 10dp).
 * @property minimumHeight Minimum content height (default 0dp).
 * @property background Footer band fill; `null` uses a sunken bevel brush from the palette.
 * @property dividerColor Color of the hairline above the footer; `null` uses the palette border.
 * @property dividerWidth Thickness of each hairline stripe (default 1dp); 0dp hides the divider.
 * @see MillenniumSidebarFooter
 */
@Immutable
data class MillenniumSidebarFooterProperties(
    val contentPadding: PaddingValues,
    val contentSpacing: Dp,
    val minimumHeight: Dp,
    val background: Brush? = null,
    val dividerColor: Color? = null,
    val dividerWidth: Dp,
)

/**
 * A collapsible cluster of items with its own label.
 *
 * Container values (spacing, alpha) are read from the default state; label values are resolved per
 * interaction state.
 *
 * @property contentPadding Padding inside the group's collapsible content (default 2dp).
 * @property contentSpacing Vertical gap between rows inside the group content (default 4dp).
 * @property groupSpacing Vertical gap between the label and the content (default 4dp).
 * @property labelStyle Label text style (default 11sp bold with 0.8sp letter spacing).
 * @property labelForeground Label color; `null` uses the palette muted color.
 * @property labelPadding Padding inside the label button (default 10dp by 6dp).
 * @property labelMinimumHeight Minimum label height (default 44dp, the accessible target size).
 * @property labelShape Label shape (default 8dp rounded corners).
 * @property labelHoveredBackground Label fill when hovered or focused; `null` uses a translucent
 *   accent wash (pressed always uses a slightly stronger wash).
 * @property labelBorderColor Label border color; `null` uses accent for keyboard focus, danger for
 *   error, otherwise no border.
 * @property labelBorderWidth Label border width (default 1dp; keyboard focus enforces at least 2dp).
 * @property animationMillis Base duration in milliseconds of the group content open/close
 *   animation (default 180); the chevron rotation uses a fixed spring.
 * @property alpha Opacity of the whole group (the default theme dims disabled groups to 0.45).
 * @property labelBackground Label fill at rest; `null` is transparent.
 * @property labelInnerGlowColor Label inner glow; `null` draws none except for keyboard focus.
 * @see MillenniumSidebarGroup
 * @see MillenniumSidebarGroupLabel
 */
@Immutable
data class MillenniumSidebarGroupProperties(
    val contentPadding: PaddingValues,
    val contentSpacing: Dp,
    val groupSpacing: Dp,
    val labelStyle: TextStyle,
    val labelForeground: Color? = null,
    val labelPadding: PaddingValues,
    val labelMinimumHeight: Dp,
    val labelShape: Shape,
    val labelHoveredBackground: Brush? = null,
    val labelBorderColor: Color? = null,
    val labelBorderWidth: Dp,
    val animationMillis: Int,
    val alpha: Float = 1f,
    val labelBackground: Brush? = null,
    val labelInnerGlowColor: Color? = null,
)

/**
 * One selectable sidebar row, resolved per interaction state.
 *
 * @property background Row fill; `null` derives it from the state (accent gel when selected,
 *   bevels on pressed/hover/focus, transparent at rest).
 * @property foreground Label color; `null` uses on-accent when selected, otherwise palette text.
 * @property iconTint Icon tint; `null` follows the foreground when selected, otherwise accent for
 *   hover/focus/press and muted at rest or when disabled.
 * @property labelStyle Label text style (default 14sp medium; semibold when selected).
 * @property shape Row shape (default 10dp rounded corners).
 * @property contentPadding Padding inside the row (default 10dp by 8dp).
 * @property minimumHeight Minimum row height (default 44dp, the accessible target size).
 * @property iconSize Icon box size (default 18dp).
 * @property iconLabelSpacing Gap between icon, label and trailing content (default 10dp).
 * @property borderColor Border color; `null` uses accent when focused, danger for error, the accent
 *   gradient bottom when selected, the palette border when hovered or pressed, otherwise none.
 * @property borderWidth Border width (default 1dp; 2dp focused).
 * @property shadowElevation Drop shadow elevation (default 0dp; 1dp hovered, 3dp selected).
 * @property innerGlowColor Inner glow; `null` uses a 35 percent palette highlight when selected,
 *   hovered or focused, otherwise none.
 * @property selectedIndicatorColor Color of the selection indicator bar; `null` uses on-accent.
 * @property selectedIndicatorWidth Width of the selection indicator bar (default 3dp).
 * @property selectedIndicatorHeight Height of the selection indicator bar (default 18dp).
 * @property stateAnimationMillis Duration in milliseconds of color and alpha animations
 *   (default 140).
 * @property alpha Row opacity (the default theme dims disabled rows to 0.45).
 * @see MillenniumSidebarItem
 */
@Immutable
data class MillenniumSidebarItemProperties(
    val background: Brush? = null,
    val foreground: Color? = null,
    val iconTint: Color? = null,
    val labelStyle: TextStyle,
    val shape: Shape,
    val contentPadding: PaddingValues,
    val minimumHeight: Dp,
    val iconSize: Dp,
    val iconLabelSpacing: Dp,
    val borderColor: Color? = null,
    val borderWidth: Dp,
    val shadowElevation: Dp,
    val innerGlowColor: Color? = null,
    val selectedIndicatorColor: Color? = null,
    val selectedIndicatorWidth: Dp,
    val selectedIndicatorHeight: Dp,
    val stateAnimationMillis: Int,
    val alpha: Float = 1f,
)

/**
 * Compact trailing counter or status pill.
 *
 * @property background Pill fill; `null` uses a sunken bevel, or the accent gel when highlighted.
 * @property foreground Text color; `null` uses the palette muted color, or on-accent when
 *   highlighted.
 * @property textStyle Badge text style (default 11sp semibold).
 * @property shape Pill shape (default fully rounded).
 * @property contentPadding Padding inside the pill (default 8dp by 2dp).
 * @property minimumHeight Minimum pill height (default 18dp).
 * @property borderColor Border color; `null` uses the palette border, or 50 percent on-accent when
 *   highlighted.
 * @property borderWidth Border width (default 1dp).
 * @property innerGlowColor Inner glow; `null` uses the palette highlight at 25 percent alpha.
 * @see MillenniumSidebarBadge
 */
@Immutable
data class MillenniumSidebarBadgeProperties(
    val background: Brush? = null,
    val foreground: Color? = null,
    val textStyle: TextStyle,
    val shape: Shape,
    val contentPadding: PaddingValues,
    val minimumHeight: Dp,
    val borderColor: Color? = null,
    val borderWidth: Dp = 1.dp,
    val innerGlowColor: Color? = null,
)

/**
 * The collapse/expand affordance, usually placed in the header.
 *
 * These values are part of [MillenniumSidebarTheme.trigger] for theme completeness. In the current
 * implementation [MillenniumSidebarTrigger] only supplies state and does not draw a surface, so
 * these properties are not read by it; apply them to whatever button you render in its slot.
 *
 * @property foreground Intended icon color; `null` derives it from the button you render.
 * @property shape Intended button shape (default 8dp rounded corners).
 * @property size Intended button size (default 44dp, the accessible target size).
 * @property animationMillis Intended duration in milliseconds of state animations (default 160).
 * @property background Intended button fill; `null` leaves it to the rendered button.
 * @property borderColor Intended border color; `null` leaves it to the rendered button.
 * @property borderWidth Intended border width (default 1dp; 2dp focused in the default theme).
 * @property shadowElevation Intended drop shadow elevation (default 1dp; 2dp hovered, 0dp pressed).
 * @property innerGlowColor Intended inner glow; `null` leaves it to the rendered button.
 * @see MillenniumSidebarTrigger
 */
@Immutable
data class MillenniumSidebarTriggerProperties(
    val foreground: Color? = null,
    val shape: Shape,
    val size: Dp,
    val animationMillis: Int,
    val background: Brush? = null,
    val borderColor: Color? = null,
    val borderWidth: Dp = 1.dp,
    val shadowElevation: Dp = 1.dp,
    val innerGlowColor: Color? = null,
)

/**
 * Complete, independently replaceable theme for every sidebar brick, exposed as
 * [MillenniumTheme.sidebarTheme].
 *
 * Each brick reads its own property group, so you can restyle one without touching the others.
 * Container bricks (root, header, content, footer, badge) use the default state; group, item and
 * trigger resolve interaction states.
 *
 * ```kotlin
 * val base = defaultMillenniumSidebarTheme()
 * val wide = base.copy(
 *     root = MillenniumControlStateProperty.all(base.root.default.copy(width = 320.dp)),
 * )
 * MillenniumSidebar(state = state, theme = wide) { }
 * ```
 *
 * @property root Shell properties.
 * @property header Header properties.
 * @property content Scroll region properties.
 * @property footer Footer properties.
 * @property group Group and group label properties per state.
 * @property item Row properties per state.
 * @property badge Badge properties.
 * @property trigger Collapse trigger properties per state.
 */
@Immutable
data class MillenniumSidebarTheme(
    val root: MillenniumControlStateProperty<MillenniumSidebarRootProperties>,
    val header: MillenniumControlStateProperty<MillenniumSidebarHeaderProperties>,
    val content: MillenniumControlStateProperty<MillenniumSidebarContentProperties>,
    val footer: MillenniumControlStateProperty<MillenniumSidebarFooterProperties>,
    val group: MillenniumControlStateProperty<MillenniumSidebarGroupProperties>,
    val item: MillenniumControlStateProperty<MillenniumSidebarItemProperties>,
    val badge: MillenniumControlStateProperty<MillenniumSidebarBadgeProperties>,
    val trigger: MillenniumControlStateProperty<MillenniumSidebarTriggerProperties>,
)

/**
 * Builds the default [MillenniumSidebarTheme].
 *
 * Defaults: a 264dp shell collapsing to a 76dp rail, 44dp minimum targets on rows, group labels and
 * the trigger, a 3dp by 18dp selection indicator, 8dp shadow on the shell, and state variations
 * (hover lifts the row shadow to 1dp, selection to 3dp with a semibold label, focus and error widen
 * borders to 2dp, disabled fades to 0.45 alpha). All colors and brushes are `null` so they follow
 * the active palette. Customise with `copy` on the groups.
 */
fun defaultMillenniumSidebarTheme(): MillenniumSidebarTheme {
    val root = MillenniumSidebarRootProperties(
        width = 264.dp,
        collapsedWidth = 76.dp,
        shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
        borderWidth = 1.dp,
        shadowElevation = 8.dp,
        stateAnimationMillis = 180,
    )
    val header = MillenniumSidebarHeaderProperties(
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 12.dp),
        contentSpacing = 12.dp,
        minimumHeight = 0.dp,
        dividerWidth = 1.dp,
    )
    val content = MillenniumSidebarContentProperties(
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
        contentSpacing = 14.dp,
        showScrollbar = true,
        scrollbarEndPadding = 6.dp,
    )
    val footer = MillenniumSidebarFooterProperties(
        contentPadding = PaddingValues(14.dp),
        contentSpacing = 10.dp,
        minimumHeight = 0.dp,
        dividerWidth = 1.dp,
    )
    val group = MillenniumSidebarGroupProperties(
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
        contentSpacing = 4.dp,
        groupSpacing = 4.dp,
        labelStyle = TextStyle(
            fontSize = 11.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        ),
        labelPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        labelMinimumHeight = 44.dp,
        labelShape = RoundedCornerShape(8.dp),
        labelBorderWidth = 1.dp,
        animationMillis = 180,
    )
    val item = MillenniumSidebarItemProperties(
        labelStyle = TextStyle(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        minimumHeight = 44.dp,
        iconSize = 18.dp,
        iconLabelSpacing = 10.dp,
        borderWidth = 1.dp,
        shadowElevation = 0.dp,
        selectedIndicatorWidth = 3.dp,
        selectedIndicatorHeight = 18.dp,
        stateAnimationMillis = 140,
    )
    val badge = MillenniumSidebarBadgeProperties(
        textStyle = TextStyle(
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold
        ),
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        minimumHeight = 18.dp,
    )
    val trigger = MillenniumSidebarTriggerProperties(
        shape = RoundedCornerShape(8.dp),
        size = 44.dp,
        animationMillis = 160,
    )
    return MillenniumSidebarTheme(
        root = MillenniumControlStateProperty.all(root),
        header = MillenniumControlStateProperty.all(header),
        content = MillenniumControlStateProperty.all(content),
        footer = MillenniumControlStateProperty.all(footer),
        group = MillenniumControlStateProperty(
            default = group,
            hovered = group,
            focused = group.copy(labelBorderWidth = 2.dp),
            pressed = group.copy(labelBorderWidth = 1.dp),
            disabled = group.copy(alpha = 0.45f),
            selected = group,
            error = group.copy(labelBorderWidth = 2.dp),
        ),
        item = MillenniumControlStateProperty(
            default = item,
            hovered = item.copy(shadowElevation = 1.dp),
            focused = item.copy(borderWidth = 2.dp),
            pressed = item.copy(shadowElevation = 0.dp),
            disabled = item.copy(alpha = 0.45f),
            selected = item.copy(
                shadowElevation = 3.dp,
                labelStyle = item.labelStyle.copy(fontWeight = FontWeight.SemiBold)
            ),
            error = item.copy(borderWidth = 2.dp),
        ),
        badge = MillenniumControlStateProperty.all(badge),
        trigger = MillenniumControlStateProperty(
            default = trigger,
            hovered = trigger.copy(shadowElevation = 2.dp),
            focused = trigger.copy(borderWidth = 2.dp),
            pressed = trigger.copy(shadowElevation = 0.dp),
        ),
    )
}

// ---------------------------------------------------------------------------
// State & scopes
// ---------------------------------------------------------------------------

/**
 * Hoistable sidebar state: whether the rail is collapsed and which groups are
 * open. Callers may also drive every function with plain `Boolean` state.
 *
 * Both values are snapshot state, so they can be read from composition and mutated from any
 * snapshot-aware context (typically the main thread). Group expansion is keyed by a caller-chosen
 * string id; groups that were never touched use [defaultGroupExpanded]. The state is not
 * saveable on its own; wrap the fields in `rememberSaveable` if it must survive process death.
 *
 * ```kotlin
 * val sidebar = rememberMillenniumSidebarState()
 * Button(onClick = { sidebar.collapsed = !sidebar.collapsed }) { Text("Toggle rail") }
 * sidebar.collapseAll(listOf("library", "settings"))
 * ```
 *
 * @param collapsed Initial rail state; `true` shows the icon-width rail.
 * @param defaultGroupExpanded Expansion reported for groups with no recorded choice.
 * @see rememberMillenniumSidebarState
 * @see MillenniumSidebar
 */
@Stable
class MillenniumSidebarState(
    collapsed: Boolean = false,
    private val defaultGroupExpanded: Boolean = true,
) {
    /** Whether the sidebar is collapsed to its icon rail. Observable; assigning recomposes readers. */
    var collapsed: Boolean by mutableStateOf(collapsed)

    private val expandedGroups = mutableStateMapOf<String, Boolean>()

    /** Returns whether the group [id] is open, falling back to the default when unset. */
    fun isGroupExpanded(id: String): Boolean = expandedGroups[id] ?: defaultGroupExpanded

    /** Records that the group [id] is [expanded] (open) or closed. */
    fun setGroupExpanded(id: String, expanded: Boolean) {
        expandedGroups[id] = expanded
    }

    /** Flips the open/closed state of the group [id]. */
    fun toggleGroup(id: String) {
        setGroupExpanded(id, !isGroupExpanded(id))
    }

    /** Opens every group in [ids]. */
    fun expandAll(ids: Collection<String>) {
        ids.forEach { expandedGroups[it] = true }
    }

    /** Closes every group in [ids]. */
    fun collapseAll(ids: Collection<String>) {
        ids.forEach { expandedGroups[it] = false }
    }
}

/**
 * Creates and remembers a [MillenniumSidebarState]. The arguments only seed the first
 * composition; later changes to them do not reset the state.
 *
 * @param collapsed Initial collapsed state.
 * @param defaultGroupExpanded Default expansion for groups without a recorded choice.
 */
@Composable
fun rememberMillenniumSidebarState(
    collapsed: Boolean = false,
    defaultGroupExpanded: Boolean = true,
): MillenniumSidebarState = remember { MillenniumSidebarState(collapsed, defaultGroupExpanded) }

/**
 * Read by sidebar children to align themselves with the rail width.
 *
 * Created by [MillenniumSidebar] (the constructor is internal) and provided through
 * [LocalMillenniumSidebar]. A new instance is created when the collapsed flag or callback changes.
 *
 * @property collapsed Whether the sidebar is currently in rail mode.
 * @property toggleCollapsed Requests the opposite collapsed state through the caller's callback.
 */
@Stable
class MillenniumSidebarScope internal constructor(
    val collapsed: Boolean,
    val toggleCollapsed: () -> Unit,
)

/**
 * Read by group children to share one open/closed decision.
 *
 * Created by [MillenniumSidebarGroup] (internal constructor) and provided through an internal
 * composition local read by the label and content bricks.
 *
 * @property expanded Whether the group is open.
 * @property collapsible Whether the label acts as a toggle.
 * @property enabled Whether the group accepts toggling.
 * @property toggle Requests the opposite expansion through the caller's callback.
 */
@Stable
class MillenniumSidebarGroupScope internal constructor(
    val expanded: Boolean,
    val collapsible: Boolean,
    val enabled: Boolean,
    val toggle: () -> Unit,
)

/**
 * Composition local holding the nearest [MillenniumSidebarScope], or `null` outside a
 * [MillenniumSidebar]. Items, triggers and groups read it to adapt to rail mode; custom bricks may
 * read it too. It is a static local provided only by [MillenniumSidebar].
 */
val LocalMillenniumSidebar = staticCompositionLocalOf<MillenniumSidebarScope?> { null }

/** Composition local holding the nearest [MillenniumSidebarGroupScope], or `null` outside a group. */
internal val LocalMillenniumSidebarGroup =
    staticCompositionLocalOf<MillenniumSidebarGroupScope?> { null }

// ---------------------------------------------------------------------------
// Root
// ---------------------------------------------------------------------------

/**
 * A themed, caller-owned sidebar shell. Compose it from lego bricks:
 *
 * ```
 * MillenniumSidebar {
 *     MillenniumSidebarHeader { ... }        // fixed
 *     MillenniumSidebarContent { ... }       // scrolls
 *     MillenniumSidebarFooter { ... }        // fixed
 * }
 * ```
 *
 * This overload takes a hoisted [MillenniumSidebarState] and forwards to the `Boolean`-based
 * overload, writing collapse requests back into the state. The shell is a full-height [Column]
 * whose width springs between the expanded and collapsed widths. See the `Boolean` overload for the
 * full description.
 *
 * ```kotlin
 * val sidebar = rememberMillenniumSidebarState()
 * MillenniumSidebar(state = sidebar) {
 *     MillenniumSidebarHeader {
 *         MillenniumSidebarTrigger { trigger ->
 *             MillenniumIconButton(
 *                 MillenniumIcons.Chevron,
 *                 contentDescription = "Toggle sidebar",
 *                 onClick = trigger.toggle,
 *                 modifier = Modifier.rotate(trigger.rotation),
 *             )
 *         }
 *     }
 *     MillenniumSidebarContent {
 *         MillenniumSidebarGroup(id = "main", state = sidebar) {
 *             MillenniumSidebarGroupLabel("Main")
 *             MillenniumSidebarGroupContent {
 *                 MillenniumSidebarMenu {
 *                     MillenniumSidebarItem(label = "Home", selected = true, onClick = { })
 *                 }
 *             }
 *         }
 *     }
 *     MillenniumSidebarFooter { MillenniumText("v1.0") }
 * }
 * ```
 *
 * @param state Caller-owned sidebar state holding the collapsed flag and group expansion.
 * @param modifier Modifier for the shell (width and full height are applied after it).
 * @param theme Theme supplying every brick's defaults. Defaults to [MillenniumTheme.sidebarTheme].
 * @param content The bricks, in a [ColumnScope] so [MillenniumSidebarContent] can take the remaining
 *   height.
 */
@Composable
fun MillenniumSidebar(
    state: MillenniumSidebarState,
    modifier: Modifier = Modifier,
    theme: MillenniumSidebarTheme = MillenniumTheme.sidebarTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
    MillenniumSidebar(
        modifier = modifier,
        collapsed = state.collapsed,
        onCollapsedChange = { state.collapsed = it },
        theme = theme,
        content = content,
    )
}

/**
 * Caller-owned, `Boolean`-driven sidebar shell: the base overload that [MillenniumSidebar] with a
 * state object delegates to.
 *
 * The shell is a full-height [Column] drawn as a raised glass surface (panel gradient, hairline
 * border, inner glow, shadow, optional clipping by shape through the surface). Its width springs
 * between the expanded and collapsed widths whenever [collapsed] changes. It provides a
 * [MillenniumSidebarScope] through [LocalMillenniumSidebar], so descendant items, group labels and
 * triggers render in icon-rail mode (centered icons, no labels) without extra wiring, and also
 * provides the ambient foreground color. The shell itself has no semantics; the interactive rows
 * inside carry the accessibility behavior (button, selected state, 44dp targets).
 *
 * ```kotlin
 * var collapsed by remember { mutableStateOf(false) }
 * MillenniumSidebar(collapsed = collapsed, onCollapsedChange = { collapsed = it }) {
 *     MillenniumSidebarHeader { MillenniumText("Millennium") }
 *     MillenniumSidebarContent {
 *         MillenniumSidebarMenu {
 *             MillenniumSidebarItem(label = "Home", icon = MillenniumIcons.Sparkle, selected = true, onClick = { })
 *             MillenniumSidebarItem(label = "Saved", icon = MillenniumIcons.Music, onClick = { })
 *         }
 *     }
 *     MillenniumSidebarFooter { MillenniumText("Footer") }
 * }
 * ```
 *
 * @param modifier Modifier for the shell; width and full height are applied after it.
 * @param collapsed Whether the sidebar is shown as the icon rail.
 * @param onCollapsedChange Called with the requested collapsed value when a descendant trigger
 *   toggles; the caller decides whether to apply it.
 * @param background Overrides the shell fill; `null` falls back to the theme, then the glass panel
 *   brush.
 * @param foreground Overrides the ambient content color; `null` falls back to the theme, then the
 *   palette text color.
 * @param width Overrides the expanded width; `null` uses the theme.
 * @param collapsedWidth Overrides the rail width; `null` uses the theme.
 * @param shape Overrides the shell shape; `null` uses the theme.
 * @param borderColor Overrides the border color; `null` falls back to the theme, then the palette
 *   border.
 * @param borderWidth Overrides the border width; `null` uses the theme.
 * @param shadowElevation Overrides the shadow elevation; `null` uses the theme.
 * @param innerGlowColor Overrides the inner glow; `null` falls back to the theme, then a palette
 *   highlight.
 * @param stateAnimationMillis Accepted for symmetry with the theme; the width animation currently
 *   uses a fixed spring and does not read it.
 * @param alpha Overrides the shell opacity; `null` uses the theme.
 * @param theme Theme supplying defaults for every nullable override. Defaults to
 *   [MillenniumTheme.sidebarTheme].
 * @param content The bricks, in a [ColumnScope].
 * @see MillenniumSidebarState
 * @see MillenniumSidebarHeader
 * @see MillenniumSidebarContent
 * @see MillenniumSidebarFooter
 */
@Composable
fun MillenniumSidebar(
    modifier: Modifier = Modifier,
    collapsed: Boolean = false,
    onCollapsedChange: (Boolean) -> Unit = {},
    background: Brush? = null,
    foreground: Color? = null,
    width: Dp? = null,
    collapsedWidth: Dp? = null,
    shape: Shape? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    shadowElevation: Dp? = null,
    innerGlowColor: Color? = null,
    stateAnimationMillis: Int? = null,
    alpha: Float? = null,
    theme: MillenniumSidebarTheme = MillenniumTheme.sidebarTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MillenniumTheme.colors
    val properties = theme.root.resolve(MillenniumControlState.Default)
    val targetWidth =
        if (collapsed) collapsedWidth ?: properties.collapsedWidth else width ?: properties.width
    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(
            dampingRatio = 0.80f,
            stiffness = Spring.StiffnessMediumLow,
        ),
    )
    val scope = remember(collapsed, onCollapsedChange) {
        MillenniumSidebarScope(collapsed = collapsed) { onCollapsedChange(!collapsed) }
    }
    CompositionLocalProvider(LocalMillenniumSidebar provides scope) {
        Column(
            modifier = modifier
                .width(animatedWidth)
                .fillMaxHeight()
                .millenniumSurface(
                    background = background ?: properties.background
                    ?: millenniumPanelBrush(colors),
                    shape = shape ?: properties.shape,
                    borderColor = borderColor ?: properties.borderColor ?: colors.border,
                    borderWidth = borderWidth ?: properties.borderWidth,
                    shadowElevation = shadowElevation ?: properties.shadowElevation,
                    innerGlowColor = innerGlowColor ?: properties.innerGlowColor
                    ?: colors.highlight.copy(alpha = 0.55f),
                )
                .alpha(alpha ?: properties.alpha),
        ) {
            MillenniumStyledContent(
                TextStyle.Default,
                foreground ?: properties.foreground ?: colors.text
            ) { content() }
        }
    }
}

// ---------------------------------------------------------------------------
// Header / Content / Footer
// ---------------------------------------------------------------------------

/**
 * Fixed top region of a [MillenniumSidebar]: a glossy band holding brand, trigger or search content,
 * closed by a paired hairline.
 *
 * It never scrolls and takes its natural height. Children are stacked in a [Column] with themed
 * spacing.
 *
 * ```kotlin
 * MillenniumSidebarHeader {
 *     MillenniumText("Millennium", weight = FontWeight.Bold)
 *     MillenniumTextField(state = rememberTextFieldState(), placeholder = "Search")
 * }
 * ```
 *
 * @param modifier Modifier for the band.
 * @param contentPadding Overrides the padding around the content; `null` uses the theme.
 * @param contentSpacing Overrides the vertical gap between children; `null` uses the theme.
 * @param minimumHeight Overrides the minimum content height; `null` uses the theme.
 * @param background Overrides the band fill; `null` falls back to the theme, then a palette gel
 *   gradient.
 * @param dividerColor Overrides the hairline color; `null` falls back to the theme, then the
 *   palette border.
 * @param dividerWidth Overrides the hairline thickness; 0dp hides it; `null` uses the theme.
 * @param theme Theme supplying defaults for every nullable override. Defaults to
 *   [MillenniumTheme.sidebarTheme].
 * @param content Header children.
 * @see MillenniumSidebar
 * @see MillenniumSidebarContent
 * @see MillenniumSidebarFooter
 */
@Composable
fun MillenniumSidebarHeader(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues? = null,
    contentSpacing: Dp? = null,
    minimumHeight: Dp? = null,
    background: Brush? = null,
    dividerColor: Color? = null,
    dividerWidth: Dp? = null,
    theme: MillenniumSidebarTheme = MillenniumTheme.sidebarTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MillenniumTheme.colors
    val properties = theme.header.resolve(MillenniumControlState.Default)
    val resolvedBackground =
        background ?: properties.background ?: gelBrush(colors.raised, colors.surface)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(resolvedBackground),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = minimumHeight ?: properties.minimumHeight)
                .padding(contentPadding ?: properties.contentPadding),
            verticalArrangement = Arrangement.spacedBy(contentSpacing ?: properties.contentSpacing),
        ) {
            content()
        }
        SidebarHairline(
            dividerColor ?: properties.dividerColor ?: colors.border,
            dividerWidth ?: properties.dividerWidth,
        )
    }
}

/**
 * The only region of a sidebar that scrolls. Must be placed directly inside [MillenniumSidebar]
 * (it is a [ColumnScope] extension and takes the remaining height with `weight(1f)`).
 *
 * Children are stacked vertically with themed padding and spacing. When [showScrollbar] resolves to
 * true, a themed [MillenniumScrollbar] overlays the end edge and extra end padding is reserved so
 * rows are not covered. The scroll position lives in a remembered scroll state owned by this
 * brick (not hoisted).
 *
 * ```kotlin
 * MillenniumSidebarContent {
 *     MillenniumSidebarGroup(id = "library", state = sidebar) {
 *         MillenniumSidebarGroupLabel("Library")
 *         MillenniumSidebarGroupContent {
 *             MillenniumSidebarMenu {
 *                 MillenniumSidebarItem(label = "Discover", onClick = { })
 *             }
 *         }
 *     }
 *     MillenniumSidebarSeparator()
 * }
 * ```
 *
 * @param modifier Modifier for the scroll region (weight and fill width are applied after it).
 * @param contentPadding Overrides the padding inside the scroll area; `null` uses the theme.
 * @param contentSpacing Overrides the vertical gap between children; `null` uses the theme.
 * @param showScrollbar Overrides whether the scrollbar is shown; `null` uses the theme.
 * @param scrollbarEndPadding Overrides the end padding reserved for the scrollbar; `null` uses the
 *   theme.
 * @param theme Theme supplying defaults for every nullable override. Defaults to
 *   [MillenniumTheme.sidebarTheme].
 * @param content Scrollable children.
 * @see MillenniumSidebar
 * @see MillenniumScrollbar
 */
@Composable
fun ColumnScope.MillenniumSidebarContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues? = null,
    contentSpacing: Dp? = null,
    showScrollbar: Boolean? = null,
    scrollbarEndPadding: Dp? = null,
    theme: MillenniumSidebarTheme = MillenniumTheme.sidebarTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
    val properties = theme.content.resolve(MillenniumControlState.Default)
    val resolvedShowScrollbar = showScrollbar ?: properties.showScrollbar
    val scrollState = rememberScrollState()
    Box(modifier = modifier.weight(1f).fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(contentPadding ?: properties.contentPadding)
                .padding(
                    end = if (resolvedShowScrollbar) scrollbarEndPadding
                        ?: properties.scrollbarEndPadding else 0.dp
                ),
            verticalArrangement = Arrangement.spacedBy(contentSpacing ?: properties.contentSpacing),
        ) {
            content()
        }
        if (resolvedShowScrollbar) {
            MillenniumScrollbar(
                scrollState,
                Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 6.dp),
            )
        }
    }
}

/**
 * Fixed bottom region of a [MillenniumSidebar]: an inset (sunken) band opened by a paired hairline,
 * for account, settings or version information.
 *
 * It never scrolls and takes its natural height.
 *
 * ```kotlin
 * MillenniumSidebarFooter {
 *     MillenniumSidebarItem(label = "Settings", icon = MillenniumIcons.Sparkle, onClick = { })
 *     MillenniumText("Edition 001 / 2026", color = MillenniumTheme.colors.muted, size = 10.sp)
 * }
 * ```
 *
 * @param modifier Modifier for the band.
 * @param contentPadding Overrides the padding around the content; `null` uses the theme.
 * @param contentSpacing Overrides the vertical gap between children; `null` uses the theme.
 * @param minimumHeight Overrides the minimum content height; `null` uses the theme.
 * @param background Overrides the band fill; `null` falls back to the theme, then a sunken bevel.
 * @param dividerColor Overrides the hairline color; `null` falls back to the theme, then the
 *   palette border.
 * @param dividerWidth Overrides the hairline thickness; 0dp hides it; `null` uses the theme.
 * @param theme Theme supplying defaults for every nullable override. Defaults to
 *   [MillenniumTheme.sidebarTheme].
 * @param content Footer children.
 * @see MillenniumSidebar
 * @see MillenniumSidebarHeader
 */
@Composable
fun MillenniumSidebarFooter(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues? = null,
    contentSpacing: Dp? = null,
    minimumHeight: Dp? = null,
    background: Brush? = null,
    dividerColor: Color? = null,
    dividerWidth: Dp? = null,
    theme: MillenniumSidebarTheme = MillenniumTheme.sidebarTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MillenniumTheme.colors
    val properties = theme.footer.resolve(MillenniumControlState.Default)
    val resolvedBackground =
        background ?: properties.background ?: millenniumBevelBrush(colors, sunken = true)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(resolvedBackground),
    ) {
        SidebarHairline(
            dividerColor ?: properties.dividerColor ?: colors.border,
            dividerWidth ?: properties.dividerWidth,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = minimumHeight ?: properties.minimumHeight)
                .padding(contentPadding ?: properties.contentPadding),
            verticalArrangement = Arrangement.spacedBy(contentSpacing ?: properties.contentSpacing),
        ) {
            content()
        }
    }
}

/** Paired dark and light hairline used by the header and footer; draws nothing when [width] is 0. */
@Composable
private fun SidebarHairline(color: Color, width: Dp) {
    if (width > 0.dp) {
        Box(Modifier.fillMaxWidth().height(width).background(color))
        Box(
            Modifier.fillMaxWidth().height(width)
                .background(MillenniumTheme.colors.highlight.copy(alpha = 0.45f))
        )
    }
}

// ---------------------------------------------------------------------------
// Groups
// ---------------------------------------------------------------------------

/**
 * A cluster of items. When [collapsible] is true, a
 * [MillenniumSidebarGroupLabel] inside it becomes a toggle and a nested
 * [MillenniumSidebarGroupContent] animates open and shut.
 *
 * This caller-owned overload is driven by plain `expanded` and `onExpandedChange`; the
 * state-based overload keys expansion by id in a [MillenniumSidebarState]. The group renders a
 * full-width [Column] and shares its open/closed decision with its label and content bricks
 * through an internal [MillenniumSidebarGroupScope]. Non-collapsible groups ignore `expanded`
 * and always show their content.
 *
 * ```kotlin
 * var open by remember { mutableStateOf(true) }
 * MillenniumSidebarGroup(expanded = open, collapsible = true, onExpandedChange = { open = it }) {
 *     MillenniumSidebarGroupLabel("Library")
 *     MillenniumSidebarGroupContent {
 *         MillenniumSidebarMenu { MillenniumSidebarItem(label = "Saved", onClick = { }) }
 *     }
 * }
 * ```
 *
 * @param modifier Modifier for the group column (full width is applied).
 * @param expanded Whether the group is open; only meaningful when [collapsible].
 * @param collapsible Whether the label toggles the content.
 * @param enabled Whether toggling is allowed; disabled groups are dimmed.
 * @param onExpandedChange Called with the requested expansion when the label is activated.
 * @param groupSpacing Overrides the gap between label and content; `null` uses the theme.
 * @param alpha Overrides the group opacity; `null` uses the theme.
 * @param theme Theme supplying defaults. Defaults to [MillenniumTheme.sidebarTheme].
 * @param content Group bricks, normally a label and a [MillenniumSidebarGroupContent].
 * @see MillenniumSidebarGroupLabel
 * @see MillenniumSidebarGroupContent
 */
@Composable
fun MillenniumSidebarGroup(
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    collapsible: Boolean = false,
    enabled: Boolean = true,
    onExpandedChange: (Boolean) -> Unit = {},
    groupSpacing: Dp? = null,
    alpha: Float? = null,
    theme: MillenniumSidebarTheme = MillenniumTheme.sidebarTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
    SidebarGroupContainer(
        modifier,
        expanded,
        collapsible,
        enabled,
        onExpandedChange,
        groupSpacing,
        alpha,
        theme,
        content
    )
}

/**
 * Convenience overload that reads and writes group expansion from [state].
 *
 * Expansion is stored under [id] in the [MillenniumSidebarState], so it survives recomposition and
 * can be driven with [MillenniumSidebarState.expandAll] and [MillenniumSidebarState.collapseAll].
 * Unlike the `Boolean` overload, groups here are collapsible by default.
 *
 * ```kotlin
 * val sidebar = rememberMillenniumSidebarState()
 * MillenniumSidebarGroup(id = "workspace", state = sidebar) {
 *     MillenniumSidebarGroupLabel("Workspace")
 *     MillenniumSidebarGroupContent {
 *         MillenniumSidebarMenu { MillenniumSidebarItem(label = "History", onClick = { }) }
 *     }
 * }
 * ```
 *
 * @param id Stable key for this group's expansion in [state].
 * @param state The caller-owned sidebar state.
 * @param modifier Modifier for the group column.
 * @param collapsible Whether the label toggles the content (default true).
 * @param enabled Whether toggling is allowed.
 * @param groupSpacing Overrides the gap between label and content; `null` uses the theme.
 * @param alpha Overrides the group opacity; `null` uses the theme.
 * @param theme Theme supplying defaults. Defaults to [MillenniumTheme.sidebarTheme].
 * @param content Group bricks.
 */
@Composable
fun MillenniumSidebarGroup(
    id: String,
    state: MillenniumSidebarState,
    modifier: Modifier = Modifier,
    collapsible: Boolean = true,
    enabled: Boolean = true,
    groupSpacing: Dp? = null,
    alpha: Float? = null,
    theme: MillenniumSidebarTheme = MillenniumTheme.sidebarTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
    SidebarGroupContainer(
        modifier = modifier,
        expanded = state.isGroupExpanded(id),
        collapsible = collapsible,
        enabled = enabled,
        onExpandedChange = { state.setGroupExpanded(id, it) },
        groupSpacing = groupSpacing,
        alpha = alpha,
        theme = theme,
        content = content,
    )
}

/** Shared implementation of both group overloads: provides the group scope and stacks content. */
@Composable
private fun SidebarGroupContainer(
    modifier: Modifier,
    expanded: Boolean,
    collapsible: Boolean,
    enabled: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    groupSpacing: Dp?,
    alpha: Float?,
    theme: MillenniumSidebarTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
    val properties = theme.group.resolve(MillenniumControlState.Default)
    val scope = remember(expanded, collapsible, enabled, onExpandedChange) {
        MillenniumSidebarGroupScope(
            expanded = expanded,
            collapsible = collapsible,
            enabled = enabled,
            toggle = { onExpandedChange(!expanded) },
        )
    }
    CompositionLocalProvider(LocalMillenniumSidebarGroup provides scope) {
        Column(
            modifier = modifier.fillMaxWidth().alpha(alpha ?: properties.alpha),
            verticalArrangement = Arrangement.spacedBy(groupSpacing ?: properties.groupSpacing),
        ) {
            content()
        }
    }
}

/**
 * Text convenience overload of [MillenniumSidebarGroupLabel].
 *
 * Shows [label] in the themed label style. Inside a collapsible [MillenniumSidebarGroup] the label
 * becomes a toggle button with a rotating chevron; otherwise it is a passive heading.
 *
 * ```kotlin
 * MillenniumSidebarGroupLabel("Library", trailing = { MillenniumSidebarBadge("3") })
 * ```
 *
 * @param label The label text.
 * @param modifier Modifier for the label row.
 * @param leading Optional content before the text (an icon).
 * @param trailing Optional content after the text (a badge or action).
 * @param theme Theme supplying defaults. Defaults to [MillenniumTheme.sidebarTheme].
 */
@Composable
fun MillenniumSidebarGroupLabel(
    label: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    theme: MillenniumSidebarTheme = MillenniumTheme.sidebarTheme,
) {
    MillenniumSidebarGroupLabel(
        modifier = modifier,
        leading = leading,
        trailing = trailing,
        theme = theme
    ) {
        Text(label)
    }
}

/**
 * Slot-based heading for a [MillenniumSidebarGroup]; the toggle when the group is collapsible.
 *
 * It reads the enclosing group's [MillenniumSidebarGroupScope]. In a collapsible group it renders
 * an `UnstyledButton` (button semantics, keyboard activation, action cursor) with a minimum 44dp
 * height and a chevron that springs between open and closed; the focus border and glow are drawn
 * only for keyboard focus. In a non-collapsible group it is a static, transparent heading. By
 * default it has no bevel or box at rest; hover, focus and press add translucent accent washes.
 * In a collapsed rail, non-collapsible labels are omitted and collapsible ones show only the
 * chevron, so the group affordance remains.
 *
 * ```kotlin
 * MillenniumSidebarGroupLabel(leading = { MillenniumIcon(MillenniumIcons.Music) }) {
 *     Text("Playlists")
 * }
 * ```
 *
 * @param modifier Modifier for the label.
 * @param leading Optional content before the label.
 * @param trailing Optional content after the label.
 * @param labelStyle Overrides the label text style; `null` uses the theme.
 * @param labelForeground Overrides the label color; `null` falls back to the theme, then muted
 *   (text color while hovered, focused or pressed).
 * @param labelPadding Overrides the padding inside the label; `null` uses the theme.
 * @param labelMinimumHeight Overrides the minimum height; `null` uses the theme.
 * @param labelShape Overrides the shape; `null` uses the theme.
 * @param labelHoveredBackground Overrides the hover/focus fill; `null` falls back to the theme,
 *   then an accent wash.
 * @param labelBorderColor Overrides the border color; `null` falls back to the theme, then a
 *   state-derived color or none.
 * @param labelBorderWidth Overrides the border width; `null` uses the theme.
 * @param labelBackground Overrides the resting fill; `null` falls back to the theme, then transparent.
 * @param labelInnerGlowColor Overrides the inner glow; `null` falls back to the theme, then a glow
 *   only for keyboard focus.
 * @param animationMillis Accepted for symmetry; the chevron currently uses a fixed spring.
 * @param alpha Overrides the opacity; `null` uses the theme.
 * @param theme Theme supplying defaults. Defaults to [MillenniumTheme.sidebarTheme].
 * @param content The label content, in a [RowScope], styled with the label text style.
 * @see MillenniumSidebarGroup
 * @see MillenniumSidebarGroupContent
 */
@Composable
fun MillenniumSidebarGroupLabel(
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    labelStyle: TextStyle? = null,
    labelForeground: Color? = null,
    labelPadding: PaddingValues? = null,
    labelMinimumHeight: Dp? = null,
    labelShape: Shape? = null,
    labelHoveredBackground: Brush? = null,
    labelBorderColor: Color? = null,
    labelBorderWidth: Dp? = null,
    labelBackground: Brush? = null,
    labelInnerGlowColor: Color? = null,
    animationMillis: Int? = null,
    alpha: Float? = null,
    theme: MillenniumSidebarTheme = MillenniumTheme.sidebarTheme,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MillenniumTheme.colors
    val group = LocalMillenniumSidebarGroup.current
    val railCollapsed = LocalMillenniumSidebar.current?.collapsed == true
    val collapsible = group?.collapsible == true
    val expanded = group?.expanded ?: true
    val enabled = group?.enabled ?: true

    // In a collapsed rail only toggleable groups keep an affordance.
    if (railCollapsed && !collapsible) return

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsKeyboardFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val state = when {
        !enabled -> MillenniumControlState.Disabled
        pressed -> MillenniumControlState.Pressed
        focused -> MillenniumControlState.Focused
        hovered -> MillenniumControlState.Hovered
        else -> MillenniumControlState.Default
    }
    val properties = theme.group.resolve(state)
    val rowForeground = labelForeground ?: properties.labelForeground ?: when (state) {
        MillenniumControlState.Hovered, MillenniumControlState.Focused, MillenniumControlState.Pressed -> colors.text
        else -> colors.muted
    }
    val background: Brush = when (state) {
        MillenniumControlState.Hovered, MillenniumControlState.Focused ->
            labelHoveredBackground ?: properties.labelHoveredBackground ?: Brush.verticalGradient(
                listOf(colors.accent.copy(alpha = 0.12f), colors.accent.copy(alpha = 0.05f)),
            )

        MillenniumControlState.Pressed -> Brush.verticalGradient(
            listOf(colors.accent.copy(alpha = 0.18f), colors.accent.copy(alpha = 0.10f)),
        )

        else -> labelBackground ?: properties.labelBackground
        ?: Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    }
    val borderColor = labelBorderColor ?: properties.labelBorderColor ?: when (state) {
        MillenniumControlState.Focused -> colors.accent
        MillenniumControlState.Error -> colors.danger
        else -> null
    }
    val resolvedLabelBorderWidth = labelBorderWidth ?: properties.labelBorderWidth
    val borderWidth = when (state) {
        MillenniumControlState.Focused -> maxOf(resolvedLabelBorderWidth, 2.dp)
        else -> resolvedLabelBorderWidth
    }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        animationSpec = spring(
            dampingRatio = 0.68f,
            stiffness = Spring.StiffnessMediumLow,
        ),
    )

    val rowContent: @Composable RowScope.() -> Unit = {
        val rowScope = this
        if (!railCollapsed) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(6.dp))
            }
            Box(Modifier.weight(1f)) {
                MillenniumStyledContent(
                    labelStyle ?: properties.labelStyle,
                    rowForeground
                ) { rowScope.content() }
            }
            if (trailing != null) trailing()
            if (collapsible) Spacer(Modifier.width(6.dp))
        }
        if (collapsible) {
            MillenniumIcon(
                MillenniumIcons.Chevron,
                modifier = Modifier.rotate(rotation),
                tint = rowForeground,
                state = state,
            )
        }
    }

    val surface = Modifier
        .fillMaxWidth()
        .heightIn(min = labelMinimumHeight ?: properties.labelMinimumHeight)
        .alpha(alpha ?: properties.alpha)
        .millenniumSurface(
            background = background,
            shape = labelShape ?: properties.labelShape,
            borderColor = borderColor,
            borderWidth = borderWidth,
            innerGlowColor = labelInnerGlowColor ?: properties.labelInnerGlowColor
            ?: if (state == MillenniumControlState.Focused) colors.highlight.copy(alpha = 0.45f) else null,
        )
        .millenniumInteractionSurface(if (collapsible) state else MillenniumControlState.Default, labelShape ?: properties.labelShape)
        .padding(labelPadding ?: properties.labelPadding)

    if (collapsible) {
        UnstyledButton(
            onClick = { group.toggle() },
            enabled = enabled,
            interactionSource = interaction,
            indication = null,
            contentPadding = PaddingValues(0.dp),
            modifier = modifier.millenniumActionPointer(enabled).then(surface),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (railCollapsed) Arrangement.Center else Arrangement.Start,
            ) {
                rowContent()
            }
        }
    } else {
        Box(modifier.then(surface)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (railCollapsed) Arrangement.Center else Arrangement.Start,
            ) {
                rowContent()
            }
        }
    }
}

/**
 * The animated, collapsible body of a [MillenniumSidebarGroup].
 *
 * In a collapsible group the body expands with a spring plus fade and shrinks with a faster tween
 * (derived from [animationMillis]); in a non-collapsible group it is always shown without
 * animation. Place it next to a [MillenniumSidebarGroupLabel] and wrap rows in a
 * [MillenniumSidebarMenu].
 *
 * ```kotlin
 * MillenniumSidebarGroupContent {
 *     MillenniumSidebarMenu {
 *         MillenniumSidebarItem(label = "Playlists", icon = MillenniumIcons.Music, onClick = { })
 *     }
 * }
 * ```
 *
 * @param modifier Modifier for the body column.
 * @param contentPadding Overrides the padding around the body; `null` uses the theme.
 * @param contentSpacing Overrides the vertical gap between children; `null` uses the theme.
 * @param animationMillis Overrides the base open/close duration in milliseconds; `null` uses the theme.
 * @param theme Theme supplying defaults. Defaults to [MillenniumTheme.sidebarTheme].
 * @param content Body children.
 */
@Composable
fun MillenniumSidebarGroupContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues? = null,
    contentSpacing: Dp? = null,
    animationMillis: Int? = null,
    theme: MillenniumSidebarTheme = MillenniumTheme.sidebarTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
    val group = LocalMillenniumSidebarGroup.current
    val properties = theme.group.resolve(MillenniumControlState.Default)
    val expanded = group?.expanded ?: true
    val animate = group?.collapsible == true
    val resolvedAnimationMillis = animationMillis ?: properties.animationMillis

    val body: @Composable () -> Unit = {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(contentPadding ?: properties.contentPadding),
            verticalArrangement = Arrangement.spacedBy(contentSpacing ?: properties.contentSpacing),
        ) {
            content()
        }
    }

    if (animate) {
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = 0.80f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                expandFrom = Alignment.Top,
            ) + fadeIn(
                animationSpec = tween(
                    resolvedAnimationMillis,
                    easing = FastOutSlowInEasing
                )
            ),
            exit = shrinkVertically(
                animationSpec = tween(
                    (resolvedAnimationMillis * 0.8f).toInt().coerceAtLeast(100),
                    easing = FastOutLinearInEasing
                ),
                shrinkTowards = Alignment.Top,
            ) + fadeOut(
                animationSpec = tween(
                    (resolvedAnimationMillis * 0.7f).toInt().coerceAtLeast(90),
                    easing = FastOutLinearInEasing
                )
            ),
        ) {
            body()
        }
    } else {
        body()
    }
}

/**
 * Vertical stack of items with the group's spacing.
 *
 * ```kotlin
 * MillenniumSidebarMenu {
 *     MillenniumSidebarItem(label = "Discover", onClick = { }, selected = true)
 *     MillenniumSidebarItem(label = "Saved", onClick = { })
 * }
 * ```
 *
 * @param modifier Modifier for the stack (full width is applied).
 * @param contentSpacing Overrides the vertical gap between items; `null` uses the group spacing
 *   from the theme.
 * @param theme Theme supplying defaults. Defaults to [MillenniumTheme.sidebarTheme].
 * @param content The items.
 * @see MillenniumSidebarItem
 */
@Composable
fun MillenniumSidebarMenu(
    modifier: Modifier = Modifier,
    contentSpacing: Dp? = null,
    theme: MillenniumSidebarTheme = MillenniumTheme.sidebarTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
    val properties = theme.group.resolve(MillenniumControlState.Default)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(contentSpacing ?: properties.contentSpacing),
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------
// Items
// ---------------------------------------------------------------------------

/**
 * Visual facts an item exposes to a custom content slot.
 *
 * Created by the item container each time its state, foreground or tint changes (remembered on
 * those keys) and passed to content lambdas; it is immutable.
 *
 * @property selected Whether the row is the current selection (caller-owned).
 * @property collapsed Whether the enclosing sidebar is in rail mode.
 * @property state The resolved interaction state (disabled, pressed, keyboard-focused, hovered,
 *   selected or default, in that priority order).
 * @property foreground Resolved label color for the current state.
 * @property iconTint Resolved icon tint for the current state.
 */
@Immutable
data class MillenniumSidebarItemVisuals(
    val selected: Boolean,
    val collapsed: Boolean,
    val state: MillenniumControlState,
    val foreground: Color,
    val iconTint: Color,
)

/**
 * A single selectable row with an optional leading icon and trailing badge.
 *
 * Convenience overload for the common "icon, label, badge" row. Renders an `UnstyledButton` with
 * `selected` semantics and a content description (defaulting to [label]), a minimum 44dp height
 * and the action cursor. Selected rows use the accent gel with on-accent text, a bold-weight label,
 * a heavier shadow and a small indicator bar at the start, so selection is not conveyed by color
 * alone. Hover, press and keyboard focus add washes, interaction lighting and a spring scale
 * (hover scale is skipped for the selected row); the focus border appears for keyboard focus only.
 * Translucent states skip the drop shadow to avoid an inner box. In a collapsed sidebar only the
 * icon is shown, centered, and the label and badge are hidden (the content description keeps the
 * row identifiable to assistive technology). Selection and routing are caller-owned.
 *
 * ```kotlin
 * MillenniumSidebarMenu {
 *     MillenniumSidebarItem(
 *         label = "Discover",
 *         icon = MillenniumIcons.Sparkle,
 *         selected = route == Route.Discover,
 *         badge = "20",
 *         onClick = { route = Route.Discover },
 *     )
 * }
 * ```
 *
 * @param label The row text; also the default content description.
 * @param onClick Called when the row is activated.
 * @param modifier Modifier for the row.
 * @param selected Whether this row is the current destination.
 * @param enabled Whether the row accepts input; disabled rows are dimmed.
 * @param icon Optional leading icon; `null` omits it.
 * @param badge Optional trailing badge text, hidden in the collapsed rail.
 * @param contentDescription Overrides the accessibility description; `null` uses [label].
 * @param background Overrides the row fill; `null` falls back to the theme, then a state-derived brush.
 * @param foreground Overrides the label color; `null` falls back to the theme, then a state-derived color.
 * @param iconTint Overrides the icon tint; `null` falls back to the theme, then a state-derived tint.
 * @param labelStyle Overrides the label text style; `null` uses the theme.
 * @param shape Overrides the row shape; `null` uses the theme.
 * @param contentPadding Overrides the padding inside the row; `null` uses the theme.
 * @param minimumHeight Overrides the minimum height; `null` uses the theme.
 * @param iconSize Overrides the icon size; `null` uses the theme.
 * @param iconLabelSpacing Overrides the icon and label gap; `null` uses the theme.
 * @param borderColor Overrides the border color; `null` falls back to the theme, then a state-derived color.
 * @param borderWidth Overrides the border width; `null` uses the theme.
 * @param shadowElevation Overrides the target shadow elevation; `null` uses the theme.
 * @param innerGlowColor Overrides the inner glow; `null` falls back to the theme, then a palette highlight.
 * @param selectedIndicatorColor Overrides the indicator bar color; `null` falls back to the theme,
 *   then on-accent.
 * @param selectedIndicatorWidth Overrides the indicator width; `null` uses the theme.
 * @param selectedIndicatorHeight Overrides the indicator height; `null` uses the theme.
 * @param stateAnimationMillis Overrides the alpha tween duration in milliseconds; `null` uses the theme.
 * @param alpha Overrides the row opacity; `null` uses the theme.
 * @param theme Theme supplying defaults. Defaults to [MillenniumTheme.sidebarTheme].
 * @see MillenniumSidebarMenu
 * @see MillenniumSidebarBadge
 */
@Composable
fun MillenniumSidebarItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    badge: String? = null,
    contentDescription: String? = null,
    background: Brush? = null,
    foreground: Color? = null,
    iconTint: Color? = null,
    labelStyle: TextStyle? = null,
    shape: Shape? = null,
    contentPadding: PaddingValues? = null,
    minimumHeight: Dp? = null,
    iconSize: Dp? = null,
    iconLabelSpacing: Dp? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    shadowElevation: Dp? = null,
    innerGlowColor: Color? = null,
    selectedIndicatorColor: Color? = null,
    selectedIndicatorWidth: Dp? = null,
    selectedIndicatorHeight: Dp? = null,
    stateAnimationMillis: Int? = null,
    alpha: Float? = null,
    theme: MillenniumSidebarTheme = MillenniumTheme.sidebarTheme,
) {
    SidebarItemContainer(
        onClick = onClick,
        enabled = enabled,
        selected = selected,
        modifier = modifier.sidebarItemSemantics(contentDescription ?: label, selected),
        background = background,
        foreground = foreground,
        iconTint = iconTint,
        shape = shape,
        contentPadding = contentPadding,
        minimumHeight = minimumHeight,
        borderColor = borderColor,
        borderWidth = borderWidth,
        shadowElevation = shadowElevation,
        innerGlowColor = innerGlowColor,
        selectedIndicatorColor = selectedIndicatorColor,
        selectedIndicatorWidth = selectedIndicatorWidth,
        selectedIndicatorHeight = selectedIndicatorHeight,
        stateAnimationMillis = stateAnimationMillis,
        alpha = alpha,
        theme = theme,
    ) { visuals ->
        val itemProperties = theme.item.resolve(visuals.state)
        if (icon != null) {
            MillenniumIcon(
                icon,
                contentDescription = null,
                tint = visuals.iconTint,
                modifier = Modifier.size(iconSize ?: itemProperties.iconSize),
            )
            if (!visuals.collapsed) Spacer(
                Modifier.width(
                    iconLabelSpacing ?: itemProperties.iconLabelSpacing
                )
            )
        }
        if (!visuals.collapsed) {
            Box(Modifier.weight(1f)) {
                MillenniumStyledContent(
                    labelStyle ?: itemProperties.labelStyle,
                    visuals.foreground
                ) { Text(label) }
            }
            if (badge != null) {
                Spacer(Modifier.width(8.dp))
                MillenniumSidebarBadge(badge, highlighted = visuals.selected)
            }
        }
    }
}

/**
 * A row with fully custom leading, content and trailing slots.
 *
 * Same surface, states, semantics and rail behavior as the text overload, but [content] is yours: a
 * [RowScope] inside the padded row, styled with the item's label style and foreground. Unlike the
 * text overload it receives no collapsed flag; read [LocalMillenniumSidebar] to adapt to rail mode.
 * The content description is only set when provided.
 *
 * ```kotlin
 * MillenniumSidebarItem(onClick = { open(account) }, contentDescription = "Account") {
 *     MillenniumAvatar("KT", "Kingkor")
 *     Spacer(Modifier.width(10.dp))
 *     if (LocalMillenniumSidebar.current?.collapsed != true) Text("Kingkor")
 * }
 * ```
 *
 * @param onClick Called when the row is activated.
 * @param modifier Modifier for the row.
 * @param selected Whether this row is the current destination.
 * @param enabled Whether the row accepts input.
 * @param contentDescription Optional accessibility description.
 * @param background Overrides the row fill; `null` falls back to the theme, then a state-derived brush.
 * @param foreground Overrides the content color; `null` falls back to the theme, then a state-derived color.
 * @param iconTint Overrides the icon tint exposed to the container; `null` falls back to the theme.
 * @param shape Overrides the row shape; `null` uses the theme.
 * @param contentPadding Overrides the padding inside the row; `null` uses the theme.
 * @param minimumHeight Overrides the minimum height; `null` uses the theme.
 * @param borderColor Overrides the border color; `null` falls back to the theme, then a state-derived color.
 * @param borderWidth Overrides the border width; `null` uses the theme.
 * @param shadowElevation Overrides the target shadow elevation; `null` uses the theme.
 * @param innerGlowColor Overrides the inner glow; `null` falls back to the theme, then a palette highlight.
 * @param selectedIndicatorColor Overrides the indicator bar color; `null` falls back to the theme.
 * @param selectedIndicatorWidth Overrides the indicator width; `null` uses the theme.
 * @param selectedIndicatorHeight Overrides the indicator height; `null` uses the theme.
 * @param stateAnimationMillis Overrides the alpha tween duration in milliseconds; `null` uses the theme.
 * @param alpha Overrides the row opacity; `null` uses the theme.
 * @param theme Theme supplying defaults. Defaults to [MillenniumTheme.sidebarTheme].
 * @param content Row content in a [RowScope].
 */
@Composable
fun MillenniumSidebarItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    contentDescription: String? = null,
    background: Brush? = null,
    foreground: Color? = null,
    iconTint: Color? = null,
    shape: Shape? = null,
    contentPadding: PaddingValues? = null,
    minimumHeight: Dp? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    shadowElevation: Dp? = null,
    innerGlowColor: Color? = null,
    selectedIndicatorColor: Color? = null,
    selectedIndicatorWidth: Dp? = null,
    selectedIndicatorHeight: Dp? = null,
    stateAnimationMillis: Int? = null,
    alpha: Float? = null,
    theme: MillenniumSidebarTheme = MillenniumTheme.sidebarTheme,
    content: @Composable RowScope.() -> Unit,
) {
    SidebarItemContainer(
        onClick = onClick,
        enabled = enabled,
        selected = selected,
        modifier = modifier.sidebarItemSemantics(contentDescription, selected),
        background = background,
        foreground = foreground,
        iconTint = iconTint,
        shape = shape,
        contentPadding = contentPadding,
        minimumHeight = minimumHeight,
        borderColor = borderColor,
        borderWidth = borderWidth,
        shadowElevation = shadowElevation,
        innerGlowColor = innerGlowColor,
        selectedIndicatorColor = selectedIndicatorColor,
        selectedIndicatorWidth = selectedIndicatorWidth,
        selectedIndicatorHeight = selectedIndicatorHeight,
        stateAnimationMillis = stateAnimationMillis,
        alpha = alpha,
        theme = theme,
    ) { content() }
}

/**
 * Shared row implementation for both [MillenniumSidebarItem] overloads: resolves the interaction
 * state, colors, motion and surface, draws the selection indicator, and hands
 * [MillenniumSidebarItemVisuals] to [content].
 */
@Composable
private fun SidebarItemContainer(
    onClick: () -> Unit,
    enabled: Boolean,
    selected: Boolean,
    modifier: Modifier,
    background: Brush? = null,
    foreground: Color? = null,
    iconTint: Color? = null,
    shape: Shape? = null,
    contentPadding: PaddingValues? = null,
    minimumHeight: Dp? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    shadowElevation: Dp? = null,
    innerGlowColor: Color? = null,
    selectedIndicatorColor: Color? = null,
    selectedIndicatorWidth: Dp? = null,
    selectedIndicatorHeight: Dp? = null,
    stateAnimationMillis: Int? = null,
    alpha: Float? = null,
    theme: MillenniumSidebarTheme = MillenniumTheme.sidebarTheme,
    content: @Composable RowScope.(MillenniumSidebarItemVisuals) -> Unit,
) {
    val colors = MillenniumTheme.colors
    val collapsed = LocalMillenniumSidebar.current?.collapsed == true
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
    val resolvedForeground =
        foreground ?: properties.foreground ?: sidebarItemForeground(colors, selected, enabled)
    val resolvedIconTint = iconTint ?: properties.iconTint ?: when {
        selected -> resolvedForeground
        else -> when (state) {
            MillenniumControlState.Disabled -> colors.muted
            MillenniumControlState.Selected, MillenniumControlState.Hovered,
            MillenniumControlState.Focused, MillenniumControlState.Pressed,
                -> colors.accent

            else -> colors.muted
        }
    }
    val resolvedBackground = background ?: properties.background ?: if (selected) {
        gelBrush(colors.accentTop, colors.accentBottom)
    } else when (state) {
        MillenniumControlState.Selected -> gelBrush(colors.accentTop, colors.accentBottom)
        MillenniumControlState.Hovered -> Brush.verticalGradient(
            listOf(colors.accent.copy(alpha = 0.12f), colors.accent.copy(alpha = 0.05f)),
        )

        MillenniumControlState.Focused -> Brush.verticalGradient(
            listOf(colors.accent.copy(alpha = 0.14f), colors.accent.copy(alpha = 0.07f)),
        )

        MillenniumControlState.Pressed -> Brush.verticalGradient(
            listOf(colors.accent.copy(alpha = 0.18f), colors.accent.copy(alpha = 0.10f)),
        )

        MillenniumControlState.Error -> Brush.verticalGradient(
            listOf(colors.danger.copy(alpha = 0.16f), colors.danger.copy(alpha = 0.09f)),
        )

        MillenniumControlState.Disabled, MillenniumControlState.Default ->
            Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    }
    val resolvedBorderColor = borderColor ?: properties.borderColor ?: when (state) {
        MillenniumControlState.Focused -> colors.accent
        MillenniumControlState.Error -> colors.danger
        else -> if (selected) colors.accentBottom else if (hovered || pressed) colors.border else null
    }
    val resolvedIndicatorColor =
        selectedIndicatorColor ?: properties.selectedIndicatorColor ?: colors.onAccent
    val resolvedStateAnimationMillis = stateAnimationMillis ?: properties.stateAnimationMillis
    val elevation by animateDpAsState(
        shadowElevation ?: properties.shadowElevation,
        spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessMediumLow),
    )
    val animatedAlpha by animateFloatAsState(
        alpha ?: properties.alpha,
        tween(resolvedStateAnimationMillis)
    )
    val itemScale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            pressed -> 0.97f
            hovered && !selected -> 1.012f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.70f, stiffness = Spring.StiffnessMedium),
    )
    val visuals = remember(selected, collapsed, state, resolvedForeground, resolvedIconTint) {
        MillenniumSidebarItemVisuals(
            selected,
            collapsed,
            state,
            resolvedForeground,
            resolvedIconTint
        )
    }
    UnstyledButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        indication = null,
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .millenniumActionPointer(enabled)
            .fillMaxWidth()
            .graphicsLayer { scaleX = itemScale; scaleY = itemScale }
            .alpha(animatedAlpha)
            .millenniumSurface(
                background = resolvedBackground,
                shape = shape ?: properties.shape,
                borderColor = resolvedBorderColor,
                borderWidth = borderWidth ?: properties.borderWidth,
                shadowElevation = elevation,
                // Only the selected gel is opaque; hover/focus/press are translucent
                // washes, and a shadow behind them would show through as an inner box.
                opaqueFill = selected,
                innerGlowColor = innerGlowColor ?: properties.innerGlowColor
                ?: if (selected || hovered || focused) {
                    colors.highlight.copy(alpha = 0.35f)
                } else null,
            )
            .millenniumInteractionSurface(state, shape ?: properties.shape),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = minimumHeight ?: properties.minimumHeight)
                    .padding(contentPadding ?: properties.contentPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start,
            ) {
                MillenniumStyledContent(
                    properties.labelStyle,
                    resolvedForeground
                ) { content(visuals) }
            }
            if (selected && !collapsed) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .width(selectedIndicatorWidth ?: properties.selectedIndicatorWidth)
                        .height(selectedIndicatorHeight ?: properties.selectedIndicatorHeight)
                        .clip(RoundedCornerShape(50))
                        .background(resolvedIndicatorColor),
                )
            }
        }
    }
}

/**
 * Selection remains blue with a legible label during hover, focus and press.
 *
 * Returns on-accent when [selected] (even if disabled), muted when not [enabled], otherwise the
 * palette text color.
 */
internal fun sidebarItemForeground(
    colors: MillenniumColors,
    selected: Boolean,
    enabled: Boolean
): Color = when {
    selected -> colors.onAccent
    !enabled -> colors.muted
    else -> colors.text
}

/**
 * Compact trailing counter or status pill, used by [MillenniumSidebarItem] and usable anywhere.
 *
 * A small inset pill (sunken bevel, muted text) that switches to the accent gel with on-accent text
 * when [highlighted]. It has no interaction or semantics of its own; mention the count in the host
 * control's content description when it carries meaning.
 *
 * ```kotlin
 * MillenniumSidebarBadge("12")
 * MillenniumSidebarBadge("New", highlighted = true)
 * ```
 *
 * @param text The badge text.
 * @param modifier Modifier for the pill.
 * @param highlighted Whether to use the accent (selected) appearance and theme state.
 * @param background Overrides the fill; `null` falls back to the theme, then a state-derived brush.
 * @param foreground Overrides the text color; `null` falls back to the theme, then a state-derived color.
 * @param textStyle Overrides the text style; `null` uses the theme.
 * @param shape Overrides the shape; `null` uses the theme.
 * @param contentPadding Overrides the padding; `null` uses the theme.
 * @param minimumHeight Overrides the minimum height; `null` uses the theme.
 * @param borderColor Overrides the border color; `null` falls back to the theme, then a state-derived color.
 * @param borderWidth Overrides the border width; `null` uses the theme.
 * @param innerGlowColor Overrides the inner glow; `null` falls back to the theme, then a palette highlight.
 * @param theme Theme supplying defaults. Defaults to [MillenniumTheme.sidebarTheme].
 * @see MillenniumSidebarItem
 */
@Composable
fun MillenniumSidebarBadge(
    text: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    background: Brush? = null,
    foreground: Color? = null,
    textStyle: TextStyle? = null,
    shape: Shape? = null,
    contentPadding: PaddingValues? = null,
    minimumHeight: Dp? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    innerGlowColor: Color? = null,
    theme: MillenniumSidebarTheme = MillenniumTheme.sidebarTheme,
) {
    val colors = MillenniumTheme.colors
    val properties = theme.badge.resolve(
        if (highlighted) MillenniumControlState.Selected else MillenniumControlState.Default,
    )
    val resolvedBackground = background ?: properties.background ?: if (highlighted) {
        gelBrush(colors.accentTop, colors.accentBottom)
    } else {
        millenniumBevelBrush(colors, sunken = true)
    }
    val resolvedForeground =
        foreground ?: properties.foreground ?: if (highlighted) colors.onAccent else colors.muted
    Box(
        modifier = modifier
            .heightIn(min = minimumHeight ?: properties.minimumHeight)
            .millenniumSurface(
                background = resolvedBackground,
                shape = shape ?: properties.shape,
                borderColor = borderColor ?: properties.borderColor
                ?: if (highlighted) colors.onAccent.copy(alpha = 0.5f) else colors.border,
                borderWidth = borderWidth ?: properties.borderWidth,
                innerGlowColor = innerGlowColor ?: properties.innerGlowColor
                ?: colors.highlight.copy(alpha = 0.25f),
            )
            .padding(contentPadding ?: properties.contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        MillenniumStyledContent(
            textStyle ?: properties.textStyle,
            resolvedForeground
        ) { Text(text) }
    }
}

/**
 * A hairline tuned to the sidebar's rhythm, for use between bricks. Wraps [MillenniumSeparator]
 * with 4dp of vertical padding.
 *
 * ```kotlin
 * MillenniumSidebarContent {
 *     MillenniumSidebarMenu { MillenniumSidebarItem(label = "Home", onClick = { }) }
 *     MillenniumSidebarSeparator()
 *     MillenniumSidebarMenu { MillenniumSidebarItem(label = "Settings", onClick = { }) }
 * }
 * ```
 *
 * @param modifier Modifier for the separator (padding is applied after it).
 */
@Composable
fun MillenniumSidebarSeparator(modifier: Modifier = Modifier) {
    MillenniumSeparator(modifier.padding(vertical = 4.dp))
}

// ---------------------------------------------------------------------------
// Trigger
// ---------------------------------------------------------------------------

/**
 * State handed to a custom [MillenniumSidebarTrigger] content slot.
 *
 * Created by [MillenniumSidebarTrigger] and remembered on its inputs; immutable.
 *
 * @property collapsed Whether the enclosing sidebar is in rail mode (false outside a sidebar).
 * @property rotation Animated chevron angle in degrees: 90 when expanded, 270 when collapsed,
 *   springing in between. Use it with `Modifier.rotate` on a right- or down-pointing chevron.
 * @property toggle Requests the opposite collapsed state; a no-op outside a sidebar.
 */
@Immutable
data class MillenniumSidebarTriggerState(
    val collapsed: Boolean,
    val rotation: Float,
    val toggle: () -> Unit = {},
)

/**
 * Collapses or expands the enclosing [MillenniumSidebar].
 *
 * In the current source this function only reads the enclosing sidebar scope, animates a chevron
 * rotation and calls [content] with a [MillenniumSidebarTriggerState]. It does not draw a button
 * itself and has no default content, so you supply the control in the slot (another icon, an avatar,
 * a badge, a custom menu anchor, ...) and wire [MillenniumSidebarTriggerState.toggle] to its
 * `onClick`. Accessibility (role, 44dp target, keyboard-only focus ring) therefore comes from the
 * control you place there, for example [MillenniumIconButton].
 *
 * ```kotlin
 * MillenniumSidebarTrigger { trigger ->
 *     MillenniumIconButton(
 *         MillenniumIcons.Chevron,
 *         contentDescription = "Toggle sidebar",
 *         onClick = trigger.toggle,
 *         modifier = Modifier.rotate(trigger.rotation),
 *     )
 * }
 * ```
 *
 * @param content Slot receiving the [MillenniumSidebarTriggerState] (collapsed flag, animated
 *   rotation, toggle callback).
 * @see MillenniumSidebar
 * @see MillenniumSidebarState
 */
@Composable
fun MillenniumSidebarTrigger(
    content: @Composable (MillenniumSidebarTriggerState) -> Unit,
) {
    val scope = LocalMillenniumSidebar.current
    val collapsed = scope?.collapsed == true
    val rotation by animateFloatAsState(
        targetValue = if (collapsed) 270f else 90f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMediumLow,
        ),
    )
    val state = remember(collapsed, rotation, scope?.toggleCollapsed) {
        MillenniumSidebarTriggerState(
            collapsed,
            rotation,
            scope?.toggleCollapsed ?: {}
        )
    }

    content(state)
}

/** Adds the content description (when non-null) and selected flag to a row's semantics. */
private fun Modifier.sidebarItemSemantics(description: String?, isSelected: Boolean): Modifier =
    semantics {
        if (description != null) contentDescription = description
        selected = isSelected
    }

/** Design-time preview of a full sidebar with trigger, two groups and footer. */
@Preview
@Composable
private fun MillenniumSidebarPreview() = MillenniumPreview {
    val state = rememberMillenniumSidebarState()
    Row(Modifier.height(420.dp)) {
        MillenniumSidebar(state) {
            MillenniumSidebarHeader {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MillenniumText("Millennium", weight = FontWeight.Bold)
                    MillenniumSidebarTrigger {
                        MillenniumIconButton(
                            MillenniumIcons.Chevron,
                            modifier = Modifier.rotate(it.rotation),
                            contentDescription = "Toggle Sidebar",
                            onClick = { it.toggle() }
                        )
                    }
                }
            }
            MillenniumSidebarContent {
                MillenniumSidebarGroup(id = "library", state = state) {
                    MillenniumSidebarGroupLabel("Library")
                    MillenniumSidebarGroupContent {
                        MillenniumSidebarMenu {
                            MillenniumSidebarItem(
                                label = "Discover",
                                icon = MillenniumIcons.Sparkle,
                                onClick = {},
                                selected = true,
                                badge = "20",
                            )
                            MillenniumSidebarItem(
                                label = "Saved",
                                icon = MillenniumIcons.Music,
                                onClick = {})
                        }
                    }
                }
                MillenniumSidebarGroup(id = "workspace", state = state) {
                    MillenniumSidebarGroupLabel("Workspace")
                    MillenniumSidebarGroupContent {
                        MillenniumSidebarMenu {
                            MillenniumSidebarItem(
                                label = "Playlists",
                                icon = MillenniumIcons.Music,
                                onClick = {})
                            MillenniumSidebarItem(
                                label = "History",
                                icon = MillenniumIcons.Check,
                                onClick = {})
                        }
                    }
                }
            }
            MillenniumSidebarFooter {
                MillenniumText(
                    "Edition 001 / 2026",
                    color = MillenniumTheme.colors.muted,
                    size = 10.sp
                )
            }
        }
    }
}
