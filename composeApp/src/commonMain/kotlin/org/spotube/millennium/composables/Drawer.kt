package org.spotube.millennium.composables

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composeunstyled.DrawerPlacement
import com.composeunstyled.DrawerSnapPoint
import com.composeunstyled.DrawerSnapPoints
import com.composeunstyled.Overlay
import com.composeunstyled.Panel
import com.composeunstyled.UnstyledDrawer
import com.composeunstyled.UnstyledDrawerState
import com.composeunstyled.Viewport
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumActionPointer
import org.spotube.millennium.theme.MillenniumPreview
import org.spotube.millennium.theme.MillenniumStyledContent
import org.spotube.millennium.theme.millenniumBevelBrush
import org.spotube.millennium.theme.millenniumSurface
import org.spotube.millennium.theme.millenniumPanelBrush

/**
 * Appearance of one control state of [MillenniumDrawer].
 *
 * Normally produced by [defaultMillenniumDrawerTheme] and customised with `copy`. The composable resolves
 * only [MillenniumControlState.Default] from the owning [MillenniumDrawerTheme].
 *
 * @property background Brush of the panel; `null` derives the palette panel brush (pearl/smoke glass).
 * @property foreground Default content colour; `null` uses the palette text colour.
 * @property shape Shape of the panel. The default rounds the end (right) corners; for other placements the
 * rounded corners are mirrored automatically so they always face away from the screen edge.
 * @property maximumWidth Maximum panel width in dp.
 * @property contentPadding Padding inside the panel (applied before the system bars padding).
 * @property contentSpacing Vertical gap in dp between content children.
 * @property borderColor Hairline colour; `null` uses the palette border colour.
 * @property borderWidth Hairline thickness in dp.
 * @property shadowElevation Shadow elevation in dp.
 * @property innerGlowColor Inner highlight colour; `null` uses the palette highlight at 55% alpha.
 * @property scrimColor Colour of the dimming scrim; `null` uses the palette scrim colour.
 * @property animationMillis Scrim fade-in duration in ms (default 220); the fade-out uses 80% of it
 * (minimum 120 ms). Panel motion is configured by [rememberMillenniumDrawerState] instead.
 * @property alpha Overall opacity multiplier (1 = opaque).
 */
@Immutable
data class MillenniumDrawerThemeProperties(
    val background: Brush? = null,
    val foreground: Color? = null,
    val shape: Shape,
    val maximumWidth: Dp,
    val contentPadding: PaddingValues,
    val contentSpacing: Dp,
    val borderColor: Color? = null,
    val borderWidth: Dp,
    val shadowElevation: Dp,
    val innerGlowColor: Color? = null,
    val scrimColor: Color? = null,
    val animationMillis: Int = 220,
    val alpha: Float,
)

/**
 * Theme for [MillenniumDrawer], grouping [MillenniumDrawerThemeProperties] per control state and installed
 * as [MillenniumTheme.drawerTheme].
 *
 * @property states State-resolved properties; the drawer resolves [MillenniumControlState.Default].
 * @see defaultMillenniumDrawerTheme
 */
@Immutable
data class MillenniumDrawerTheme(
    val states: MillenniumControlStateProperty<MillenniumDrawerThemeProperties>,
)

/**
 * Creates the default drawer theme: a pearl/smoke glass panel at most 320dp wide with 18dp rounded end
 * corners, 24dp padding, 20dp spacing, a 1dp hairline, a 12dp shadow and a 220ms scrim fade. Focused and
 * error states use a 2dp border, pressed lowers the shadow to 8dp and disabled fades to 55%.
 *
 * Customise with `copy`:
 * ```kotlin
 * val base = defaultMillenniumDrawerTheme()
 * val theme = base.copy(states = base.states.copy(default = base.states.default.copy(maximumWidth = 360.dp)))
 * ```
 *
 * @return A new [MillenniumDrawerTheme].
 */
fun defaultMillenniumDrawerTheme(): MillenniumDrawerTheme {
    val base = MillenniumDrawerThemeProperties(
        shape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp),
        maximumWidth = 320.dp,
        contentPadding = PaddingValues(24.dp),
        contentSpacing = 20.dp,
        borderWidth = 1.dp,
        shadowElevation = 12.dp,
        alpha = 1f,
    )
    return MillenniumDrawerTheme(
        states = MillenniumControlStateProperty(
            default = base,
            hovered = base,
            focused = base.copy(borderWidth = 2.dp),
            pressed = base.copy(shadowElevation = 8.dp),
            disabled = base.copy(alpha = 0.55f),
            selected = base,
            error = base.copy(borderWidth = 2.dp),
        ),
    )
}

/**
 * The two resting values of a [MillenniumDrawer], used as the type argument of the Compose Unstyled drawer
 * state created by [rememberMillenniumDrawerState]. Set `state.targetValue` to switch between them.
 */
enum class MillenniumDrawerValue {
    /** The drawer is off screen (snap point zero). */
    Closed,

    /** The drawer is fully shown at its content size. */
    Open,
}

/**
 * Remembers the caller-owned state of a [MillenniumDrawer], initially [MillenniumDrawerValue.Closed].
 *
 * Snap points are `Closed at DrawerSnapPoint.Zero` and `Open at DrawerSnapPoint.ContentSize`. Opening
 * animates with a spring (damping 0.82, medium-low stiffness) and dismissing with a 190ms
 * `FastOutLinearInEasing` tween. Create it where the drawer is hosted and share it with whatever opens it.
 *
 * ```kotlin
 * val drawerState = rememberMillenniumDrawerState()
 * Box(Modifier.fillMaxSize()) {
 *     MillenniumButton(onClick = { drawerState.targetValue = MillenniumDrawerValue.Open }) {
 *         MillenniumText("Menu")
 *     }
 *     MillenniumDrawer(drawerState) { MillenniumText("Library") }
 * }
 * ```
 *
 * @return A remembered [UnstyledDrawerState] keyed by [MillenniumDrawerValue].
 * @see MillenniumDrawer
 */
@Composable
fun rememberMillenniumDrawerState(): UnstyledDrawerState<MillenniumDrawerValue> = remember {
    UnstyledDrawerState(
        initialValue = MillenniumDrawerValue.Closed,
        snapPoints = DrawerSnapPoints {
            MillenniumDrawerValue.Closed at DrawerSnapPoint.Zero
            MillenniumDrawerValue.Open at DrawerSnapPoint.ContentSize
        },
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        dismissAnimationSpec = tween(durationMillis = 190, easing = FastOutLinearInEasing),
    )
}

/**
 * A modal edge drawer in pearl/smoke glass with a dimming scrim, for navigation or secondary tasks.
 *
 * Use it for off-canvas navigation or settings that temporarily take over an edge of the screen. The
 * [content] is laid out in a padded column inside the panel and inherits the resolved foreground colour.
 *
 * Behaviour: wraps Compose Unstyled's `UnstyledDrawer` with `DrawerPresentation.Modal`; open or closed is
 * controlled by the caller-owned [state] (see [rememberMillenniumDrawerState]). The scrim is a Compose
 * Unstyled `Overlay` with the action cursor that fades in over [animationMillis] and out over 80% of it
 * (minimum 120 ms). No dismissal flags are passed, so Compose Unstyled's defaults apply: Back and Escape
 * dismiss a visible drawer and an outside click on the scrim dismisses it. The column also pads for system
 * bars.
 *
 * Hosting constraint: the drawer lays out a Compose Unstyled `Viewport` with `fillMaxSize()`, which needs
 * finite maximum width and height constraints. Place it at a screen-sized root (for example directly in the
 * window content or a full-size `Box`), not inside a vertically or horizontally scrolling container.
 *
 * ```kotlin
 * val drawerState = rememberMillenniumDrawerState()
 * Box(Modifier.fillMaxSize()) {
 *     AppContent(onMenu = { drawerState.targetValue = MillenniumDrawerValue.Open })
 *     MillenniumDrawer(drawerState, placement = DrawerPlacement.Start) {
 *         MillenniumText("Navigation", weight = FontWeight.SemiBold)
 *         MillenniumText("Library", color = MillenniumTheme.colors.muted)
 *     }
 * }
 * ```
 *
 * @param state Caller-owned drawer state from [rememberMillenniumDrawerState].
 * @param placement Edge the drawer slides from; defaults to [DrawerPlacement.Start]. The theme's rounded shape
 * is mirrored for the other edges unless you pass an explicit [shape].
 * @param contentPadding Overrides the inner padding; `null` falls back to the theme.
 * @param background Overrides the panel brush; `null` falls back to the theme, then the panel brush.
 * @param foreground Overrides the content colour; `null` falls back to the theme, then the palette text colour.
 * @param shape Overrides the panel shape; `null` falls back to the theme.
 * @param maximumWidth Overrides the maximum panel width; `null` falls back to the theme.
 * @param contentSpacing Overrides the vertical spacing; `null` falls back to the theme.
 * @param borderColor Overrides the hairline colour; `null` falls back to the theme, then the palette border.
 * @param borderWidth Overrides the hairline thickness; `null` falls back to the theme.
 * @param shadowElevation Overrides the shadow elevation; `null` falls back to the theme.
 * @param innerGlowColor Overrides the inner glow; `null` falls back to the theme, then the palette highlight.
 * @param scrimColor Overrides the scrim colour; `null` falls back to the theme, then the palette scrim colour.
 * @param animationMillis Overrides the scrim fade-in duration in ms; `null` falls back to the theme.
 * @param alpha Overrides the overall opacity; `null` falls back to the theme.
 * @param theme Theme supplying defaults; defaults to [MillenniumTheme.drawerTheme].
 * @param content Drawer content, laid out in a [ColumnScope] inside the panel.
 * @see rememberMillenniumDrawerState
 * @see MillenniumDrawerTheme
 */
@Composable
fun MillenniumDrawer(
    state: UnstyledDrawerState<MillenniumDrawerValue>,
    placement: DrawerPlacement = DrawerPlacement.Start,
    contentPadding: PaddingValues? = null,
    background: Brush? = null,
    foreground: Color? = null,
    shape: Shape? = null,
    maximumWidth: Dp? = null,
    contentSpacing: Dp? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    shadowElevation: Dp? = null,
    innerGlowColor: Color? = null,
    scrimColor: Color? = null,
    animationMillis: Int? = null,
    alpha: Float? = null,
    theme: MillenniumDrawerTheme = MillenniumTheme.drawerTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MillenniumTheme.colors
    val properties = theme.states.resolve(MillenniumControlState.Default)
    val resolvedBackground = background ?: properties.background ?: millenniumPanelBrush(colors)
    val resolvedForeground = foreground ?: properties.foreground ?: colors.text
    val resolvedBorderColor = borderColor ?: properties.borderColor ?: colors.border
    val resolvedGlowColor = innerGlowColor ?: properties.innerGlowColor ?: colors.highlight.copy(alpha = 0.55f)
    val resolvedScrimColor = scrimColor ?: properties.scrimColor ?: colors.scrim
    val resolvedAnimationMillis = animationMillis ?: properties.animationMillis
    UnstyledDrawer(
        state = state,
        placement = placement,
        presentation = com.composeunstyled.DrawerPresentation.Modal,
        overlay = {
            Overlay(
                modifier = Modifier.millenniumActionPointer(),
                enter = fadeIn(tween(resolvedAnimationMillis, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween((resolvedAnimationMillis * 0.8f).toInt().coerceAtLeast(120), easing = FastOutLinearInEasing)),
            ) {
                Box(Modifier.fillMaxSize().background(resolvedScrimColor))
            }
        },
    ) {
        Viewport(Modifier.fillMaxSize()) {
            Panel(
                Modifier.widthIn(max = maximumWidth ?: properties.maximumWidth).fillMaxWidth().fillMaxHeight()
                    .millenniumSurface(
                        background = resolvedBackground,
                        shape = shape ?: placementShape(properties.shape, placement),
                        borderColor = resolvedBorderColor,
                        borderWidth = borderWidth ?: properties.borderWidth,
                        shadowElevation = shadowElevation ?: properties.shadowElevation,
                        innerGlowColor = resolvedGlowColor,
                    ).alpha(alpha ?: properties.alpha),
            ) {
                Column(
                    Modifier.padding(contentPadding ?: properties.contentPadding).systemBarsPadding(),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                        contentSpacing ?: properties.contentSpacing,
                    ),
                ) {
                    MillenniumStyledContent(TextStyle(color = resolvedForeground), resolvedForeground) {
                        content()
                    }
                }
            }
        }
    }
}

/** Preview of an opened drawer. */
/**
 * Mirrors the theme's end-rounded panel shape for the edge the drawer slides from: the rounded
 * corners always face away from the screen edge. Non-rounded custom shapes pass through unchanged.
 */
private fun placementShape(shape: Shape, placement: DrawerPlacement): Shape {
    val rounded = shape as? RoundedCornerShape ?: return shape
    return when (placement) {
        DrawerPlacement.End -> RoundedCornerShape(
            topStart = rounded.topEnd, topEnd = rounded.topStart,
            bottomEnd = rounded.bottomStart, bottomStart = rounded.bottomEnd,
        )
        DrawerPlacement.Top -> RoundedCornerShape(
            topStart = CornerSize(0.dp), topEnd = CornerSize(0.dp),
            bottomEnd = rounded.topEnd, bottomStart = rounded.topEnd,
        )
        DrawerPlacement.Bottom -> RoundedCornerShape(
            topStart = rounded.topEnd, topEnd = rounded.topEnd,
            bottomEnd = CornerSize(0.dp), bottomStart = CornerSize(0.dp),
        )
        else -> shape
    }
}

@Preview
@Composable
private fun MillenniumDrawerPreview() = MillenniumPreview {
    val state = rememberMillenniumDrawerState()
    LaunchedEffect(state) { state.targetValue = MillenniumDrawerValue.Open }
    MillenniumDrawer(state) {
        MillenniumText("A little more space", weight = FontWeight.SemiBold)
        MillenniumText("A softly raised edge drawer.", color = MillenniumTheme.colors.muted)
    }
}
