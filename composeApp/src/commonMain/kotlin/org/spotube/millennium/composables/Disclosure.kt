package org.spotube.millennium.composables

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
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import org.spotube.millennium.theme.collectIsKeyboardFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.DisclosedContent
import com.composeunstyled.DisclosureButton
import com.composeunstyled.Text
import com.composeunstyled.UnstyledDisclosure
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumActionPointer
import org.spotube.millennium.theme.MillenniumPreview
import org.spotube.millennium.theme.MillenniumStyledContent
import org.spotube.millennium.theme.millenniumBevelBrush
import org.spotube.millennium.theme.millenniumSurface
import org.spotube.millennium.theme.millenniumInteractionSurface

/**
 * Visual, layout and motion properties of a [MillenniumDisclosure].
 *
 * @property background Surface fill; null uses the palette bevel brush.
 * @property foreground Colour of title, chevron and content text; null uses the palette text colour.
 * @property shape Corner shape of the whole disclosure (12dp rounded).
 * @property borderColor Border colour; null picks accent for hovered, focused and selected (expanded), danger for
 * error and the palette border colour otherwise. Colour changes animate over [animationMillis].
 * @property borderWidth Border thickness (1dp, 2dp when focused or in error).
 * @property shadowElevation Drop shadow depth (0dp, 2dp while pressed); changes animate.
 * @property innerGlowColor Inner highlight; null uses the palette highlight at 50%.
 * @property titleStyle Style of the header title (15sp SemiBold).
 * @property contentStyle Default text style supplied to the body content (14sp).
 * @property minimumHeaderHeight Minimum header height (48dp, above the 44dp target size).
 * @property headerPadding Padding around the header row (12dp).
 * @property contentPadding Padding around the expanded body (16dp).
 * @property headerContentSpacing Horizontal gap between the title and the chevron in the header (12dp).
 * @property contentSpacing Vertical spacing between body children (12dp).
 * @property animationMillis Base duration in milliseconds for colour, elevation and fade transitions (180).
 * @property alpha Whole-disclosure opacity (0.45 in the default disabled bag).
 * @see MillenniumDisclosureTheme
 * @see defaultMillenniumDisclosureTheme
 */
@Immutable
data class MillenniumDisclosureThemeProperties(
    val background: Brush? = null,
    val foreground: Color? = null,
    val shape: Shape,
    val borderColor: Color? = null,
    val borderWidth: Dp,
    val shadowElevation: Dp,
    val innerGlowColor: Color? = null,
    val titleStyle: TextStyle,
    val contentStyle: TextStyle,
    val minimumHeaderHeight: Dp,
    val headerPadding: PaddingValues,
    val contentPadding: PaddingValues,
    val headerContentSpacing: Dp,
    val contentSpacing: Dp,
    val animationMillis: Int,
    val alpha: Float = 1f,
)

/**
 * Per-state theme of [MillenniumDisclosure].
 *
 * @property states Properties per [MillenniumControlState]. [MillenniumDisclosure] resolves Pressed, keyboard
 * Focused, Hovered, Selected (expanded) or Default.
 */
@Immutable
data class MillenniumDisclosureTheme(
    val states: MillenniumControlStateProperty<MillenniumDisclosureThemeProperties>,
)

/**
 * Creates the default [MillenniumDisclosureTheme] installed as `MillenniumTheme.disclosureTheme`.
 *
 * Defaults: a 12dp rounded bevelled surface with a 48dp header, SemiBold 15sp title, 16dp body padding, a 180ms
 * base animation, a flat resting shadow that deepens slightly while pressed, and a thicker border when focused.
 *
 * @return A fresh theme instance.
 */
fun defaultMillenniumDisclosureTheme(): MillenniumDisclosureTheme {
    val base = MillenniumDisclosureThemeProperties(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        borderWidth = 1.dp,
        shadowElevation = 0.dp,
        titleStyle = TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold),
        contentStyle = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
        minimumHeaderHeight = 48.dp,
        headerPadding = PaddingValues(12.dp),
        contentPadding = PaddingValues(16.dp),
        headerContentSpacing = 12.dp,
        contentSpacing = 12.dp,
        animationMillis = 180,
    )
    return MillenniumDisclosureTheme(
        states = MillenniumControlStateProperty(
            default = base,
            hovered = base,
            focused = base.copy(borderWidth = 2.dp),
            pressed = base.copy(shadowElevation = 2.dp),
            disabled = base.copy(alpha = 0.45f),
            selected = base,
            error = base.copy(borderWidth = 2.dp),
        ),
    )
}

/**
 * A collapsible section with a clickable title header and an animated chevron.
 *
 * Built on Compose Unstyled's `UnstyledDisclosure`, which supplies the button semantics, keyboard activation and
 * expanded/collapsed accessibility state. Use it for FAQs, advanced settings and any content worth hiding by default.
 * The expanded state is owned by the caller.
 *
 * Behaviour: the header is at least 48dp tall and shows the hand cursor; the whole surface presses in slightly; the
 * chevron rotates with a spring; the body expands/collapses vertically with a fade. The focus border appears only for
 * keyboard focus. The border highlights in accent when hovered, focused or expanded. Body children inherit
 * `contentStyle` and the foreground colour.
 *
 * Example:
 * ```kotlin
 * var expanded by remember { mutableStateOf(false) }
 * MillenniumDisclosure(
 *     expanded = expanded,
 *     onExpandedChange = { expanded = it },
 *     title = "Why the glossy details?",
 * ) {
 *     MillenniumText("Familiar depth and a little shine, with modern accessibility.")
 * }
 * ```
 *
 * @param expanded Whether the body is visible (caller-owned).
 * @param onExpandedChange Called with the requested new state when the header is activated.
 * @param title Header text.
 * @param modifier Modifier applied to the whole disclosure surface.
 * @param background Overrides the fill; null uses the theme, then the palette bevel brush.
 * @param foreground Overrides title, chevron and body colour; null uses the theme, then the palette text colour.
 * @param shape Overrides the corner shape; null uses the theme.
 * @param borderColor Overrides the border colour in every state; null lets the state decide.
 * @param borderWidth Overrides the border thickness; null uses the state's theme value.
 * @param shadowElevation Overrides the shadow depth; null uses the state's theme value.
 * @param innerGlowColor Overrides the inner highlight; null uses the theme, then the palette highlight.
 * @param titleStyle Overrides the title style; null uses the theme.
 * @param contentStyle Overrides the body text style; null uses the theme.
 * @param minimumHeaderHeight Overrides the minimum header height; null uses the theme (48dp).
 * @param headerPadding Overrides the header padding; null uses the theme.
 * @param contentPadding Overrides the body padding; null uses the theme.
 * @param contentSpacing Overrides the vertical spacing between body children; null uses the theme.
 * @param animationMillis Overrides the base animation duration; null uses the theme.
 * @param alpha Overrides the opacity; null uses the state's theme value.
 * @param theme Per-state theme; defaults to `MillenniumTheme.disclosureTheme`.
 * @param content Body content, shown when [expanded] is true.
 * @see MillenniumDisclosureTheme
 * @see MillenniumIcons.Chevron
 */
@Composable
fun MillenniumDisclosure(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    background: Brush? = null,
    foreground: Color? = null,
    shape: Shape? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    shadowElevation: Dp? = null,
    innerGlowColor: Color? = null,
    titleStyle: TextStyle? = null,
    contentStyle: TextStyle? = null,
    minimumHeaderHeight: Dp? = null,
    headerPadding: PaddingValues? = null,
    contentPadding: PaddingValues? = null,
    contentSpacing: Dp? = null,
    animationMillis: Int? = null,
    alpha: Float? = null,
    theme: MillenniumDisclosureTheme = MillenniumTheme.disclosureTheme,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsKeyboardFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val state = when {
        pressed -> MillenniumControlState.Pressed
        focused -> MillenniumControlState.Focused
        hovered -> MillenniumControlState.Hovered
        expanded -> MillenniumControlState.Selected
        else -> MillenniumControlState.Default
    }
    val properties = theme.states.resolve(state)
    val colors = MillenniumTheme.colors
    val resolvedForeground = foreground ?: properties.foreground ?: colors.text
    val targetBorderColor = borderColor ?: properties.borderColor ?: when (state) {
        MillenniumControlState.Hovered, MillenniumControlState.Focused, MillenniumControlState.Selected -> colors.accent
        MillenniumControlState.Error -> colors.danger
        else -> colors.border
    }
    val resolvedAnimationMillis = animationMillis ?: properties.animationMillis
    val resolvedBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(resolvedAnimationMillis),
    )
    val resolvedElevation by animateDpAsState(
        targetValue = shadowElevation ?: properties.shadowElevation,
        animationSpec = tween(resolvedAnimationMillis),
    )
    val headerScale by animateFloatAsState(
        targetValue = if (pressed) 0.992f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
    )
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.68f,
            stiffness = Spring.StiffnessMediumLow,
        ),
    )
    UnstyledDisclosure(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier
            .graphicsLayer { scaleX = headerScale; scaleY = headerScale }
            .millenniumSurface(
                background = background ?: properties.background ?: millenniumBevelBrush(colors),
                shape = shape ?: properties.shape,
                borderColor = resolvedBorderColor,
                borderWidth = borderWidth ?: properties.borderWidth,
                shadowElevation = resolvedElevation,
                innerGlowColor = innerGlowColor ?: properties.innerGlowColor ?: colors.highlight.copy(alpha = 0.5f),
            ).alpha(alpha ?: properties.alpha),
    ) {
        Column {
            DisclosureButton(
                modifier = Modifier.millenniumActionPointer().fillMaxWidth()
                    .heightIn(min = minimumHeaderHeight ?: properties.minimumHeaderHeight)
                    .millenniumInteractionSurface(state, shape ?: properties.shape, animationMillis = resolvedAnimationMillis)
                    .padding(headerPadding ?: properties.headerPadding),
                contentPadding = PaddingValues(0.dp),
                indication = null,
                interactionSource = interaction,
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(properties.headerContentSpacing),
                ) {
                    Text(title, modifier = Modifier.weight(1f), style = titleStyle ?: properties.titleStyle, color = resolvedForeground)
                    MillenniumIcon(
                        MillenniumIcons.Chevron,
                        modifier = Modifier.rotate(arrowRotation),
                        tint = resolvedForeground,
                    )
                }
            }
            DisclosedContent(
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = 0.80f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    expandFrom = Alignment.Top,
                ) + fadeIn(animationSpec = tween(resolvedAnimationMillis, easing = FastOutSlowInEasing)),
                exit = shrinkVertically(
                    animationSpec = tween((resolvedAnimationMillis * 0.8f).toInt().coerceAtLeast(100), easing = FastOutLinearInEasing),
                    shrinkTowards = Alignment.Top,
                ) + fadeOut(animationSpec = tween((resolvedAnimationMillis * 0.7f).toInt().coerceAtLeast(90), easing = FastOutLinearInEasing)),
            ) {
                Column(
                    Modifier.padding(contentPadding ?: properties.contentPadding),
                    verticalArrangement = Arrangement.spacedBy(contentSpacing ?: properties.contentSpacing),
                ) {
                    MillenniumStyledContent(contentStyle ?: properties.contentStyle, resolvedForeground, content)
                }
            }
        }
    }
}

@Preview
@Composable
private fun MillenniumDisclosurePreview() = MillenniumPreview {
    MillenniumDisclosure(expanded = true, onExpandedChange = {}, title = "Why the glossy details?") {
        MillenniumText("Familiar depth and a little shine, with the accessibility and flexibility of modern Compose.")
    }
}
