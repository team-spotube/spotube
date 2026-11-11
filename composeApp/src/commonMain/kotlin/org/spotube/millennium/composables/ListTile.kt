package org.spotube.millennium.composables

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.hoverable
import org.spotube.millennium.theme.collectIsKeyboardFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.composeunstyled.Text
import com.composeunstyled.UnstyledButton
import org.spotube.millennium.theme.MillenniumColors
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumPreview
import org.spotube.millennium.theme.MillenniumStyledContent
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumActionPointer
import org.spotube.millennium.theme.millenniumSelectedGelBrush
import org.spotube.millennium.theme.millenniumSurface
import org.spotube.millennium.theme.millenniumInteractionSurface

/**
 * Complete, state-specific list-tile appearance and layout configuration.
 *
 * One instance describes the tile in a single [MillenniumControlState]; [MillenniumListTileTheme]
 * maps states to instances. Nullable colors and brushes derive from the active palette when `null`.
 *
 * @property titleStyle Ambient text style of the title slot (default 14sp medium).
 * @property subtitleStyle Text style of the subtitle slot (default 12sp normal).
 * @property titleForeground Title color when not selected; `null` uses palette text (muted when
 *   disabled).
 * @property subtitleForeground Subtitle color when not selected; `null` uses the palette muted color.
 * @property selectedTitleForeground Title color when selected; `null` falls back to
 *   [titleForeground], then the palette text color.
 * @property selectedSubtitleForeground Subtitle color when selected; `null` falls back to
 *   [subtitleForeground], then palette text at 82 percent alpha.
 * @property background Fill override for non-selected states; `null` derives accent washes (hover,
 *   focus, press), a danger wash (error) or transparency.
 * @property selectedBackground Fill override while selected; `null` uses the surface-blended gel
 *   from [selectedGelStrength].
 * @property selectedGelStrength How strongly the selected fill blends the gel toward the row surface
 *   (0 is the surface, 1 the full gel; default 0.35). Hover and focus add 0.08, press adds 0.15 and
 *   disabled scales it by 0.6.
 * @property shape Tile shape (default 10dp rounded corners).
 * @property contentPadding Padding inside the tile (default 14dp horizontal, 10dp vertical).
 * @property minimumHeight Minimum tile height (default 56dp, above the 44dp target size).
 * @property leadingSpacing Gap between the leading slot and the text column.
 * @property trailingSpacing Gap between the text column and the trailing slot.
 * @property titleSpacing Vertical gap between title and subtitle.
 * @property borderWidth Border width (default 0dp; the default theme uses 2dp for focused and error).
 * @property borderColor Border color; `null` uses danger for error, accent for focus, a 45 percent
 *   accent for selected, the palette border for hover or press, otherwise none.
 * @property innerGlowColor Inner glow; `null` uses a 25 percent palette highlight when selected,
 *   hovered or focused, otherwise none.
 * @property alpha Overall opacity (the default theme dims disabled tiles to 0.45).
 * @property stateAnimationMillis Duration in milliseconds of the alpha and interaction-lighting
 *   animations (default 140).
 * @see MillenniumListTile
 */
@Immutable
data class MillenniumListTileThemeProperties(
    val titleStyle: TextStyle,
    val subtitleStyle: TextStyle,
    val titleForeground: Color? = null,
    val subtitleForeground: Color? = null,
    val selectedTitleForeground: Color? = null,
    val selectedSubtitleForeground: Color? = null,
    val background: Brush? = null,
    val selectedBackground: Brush? = null,
    /** How strongly the selected fill blends the gel toward the row surface. */
    val selectedGelStrength: Float = 0.35f,
    val shape: Shape,
    val contentPadding: PaddingValues,
    val minimumHeight: Dp,
    val leadingSpacing: Dp,
    val trailingSpacing: Dp,
    val titleSpacing: Dp,
    val borderWidth: Dp,
    val borderColor: Color? = null,
    val innerGlowColor: Color? = null,
    val alpha: Float = 1f,
    val stateAnimationMillis: Int = 140,
)

/**
 * Theme for [MillenniumListTile], exposed as [MillenniumTheme.listTileTheme].
 *
 * ```kotlin
 * val base = defaultMillenniumListTileTheme()
 * val roomy = base.copy(
 *     states = MillenniumControlStateProperty.all(
 *         base.states.default.copy(minimumHeight = 72.dp),
 *     ),
 * )
 * MillenniumListTile(title = { Text("Roomy") }, theme = roomy)
 * ```
 *
 * @property states Tile properties per control state: default, hovered, focused, pressed, disabled,
 *   selected and error.
 */
@Immutable
data class MillenniumListTileTheme(
    val states: MillenniumControlStateProperty<MillenniumListTileThemeProperties>,
)

/**
 * Builds the default [MillenniumListTileTheme].
 *
 * All states share the same geometry and typography; only focused and error widen the border to
 * 2dp and disabled fades to 0.45 alpha. Colors are left `null` so they follow the active palette.
 * Customise with `copy` on the state properties.
 */
fun defaultMillenniumListTileTheme(): MillenniumListTileTheme {
    val base = MillenniumListTileThemeProperties(
        titleStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        subtitleStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        minimumHeight = 56.dp,
        leadingSpacing = 12.dp,
        trailingSpacing = 12.dp,
        titleSpacing = 2.dp,
        borderWidth = 0.dp,
    )
    return MillenniumListTileTheme(
        states = MillenniumControlStateProperty(
            default = base,
            hovered = base,
            focused = base.copy(borderWidth = 2.dp),
            pressed = base,
            disabled = base.copy(alpha = 0.45f),
            selected = base,
            error = base.copy(borderWidth = 2.dp),
        ),
    )
}

/**
 * Resolves the visual state of a list tile from its interaction inputs.
 *
 * Priority: disabled, pressed, focused, hovered (the last three only when [clickable]), selected,
 * default. Non-interactive tiles therefore never show interaction states.
 */
internal fun listTileControlState(
    enabled: Boolean,
    clickable: Boolean,
    pressed: Boolean,
    focused: Boolean,
    hovered: Boolean,
    selected: Boolean,
): MillenniumControlState = when {
    !enabled -> MillenniumControlState.Disabled
    clickable && pressed -> MillenniumControlState.Pressed
    clickable && focused -> MillenniumControlState.Focused
    clickable && hovered -> MillenniumControlState.Hovered
    selected -> MillenniumControlState.Selected
    else -> MillenniumControlState.Default
}

/**
 * Surface for a list tile; selected rows use a quieter, surface-blended gel.
 *
 * Selected rows blend the gel by [selectedGelStrength], raised for pressed/hover/focus and lowered
 * when disabled. Unselected rows get translucent accent washes for hover, focus and press, a danger
 * wash for error, and are transparent otherwise.
 */
internal fun listTileBackground(
    colors: MillenniumColors,
    selected: Boolean,
    state: MillenniumControlState,
    selectedGelStrength: Float,
): Brush {
    if (selected) {
        val strength = when (state) {
            MillenniumControlState.Pressed -> selectedGelStrength + 0.15f
            MillenniumControlState.Hovered, MillenniumControlState.Focused -> selectedGelStrength + 0.08f
            MillenniumControlState.Disabled -> selectedGelStrength * 0.6f
            else -> selectedGelStrength
        }
        return millenniumSelectedGelBrush(colors, strength)
    }
    return when (state) {
        MillenniumControlState.Hovered -> Brush.verticalGradient(
            listOf(colors.accent.copy(alpha = 0.10f), colors.accent.copy(alpha = 0.05f)),
        )
        MillenniumControlState.Focused -> Brush.verticalGradient(
            listOf(colors.accent.copy(alpha = 0.12f), colors.accent.copy(alpha = 0.06f)),
        )
        MillenniumControlState.Pressed -> Brush.verticalGradient(
            listOf(colors.accent.copy(alpha = 0.16f), colors.accent.copy(alpha = 0.09f)),
        )
        MillenniumControlState.Error -> Brush.verticalGradient(
            listOf(colors.danger.copy(alpha = 0.14f), colors.danger.copy(alpha = 0.08f)),
        )
        else -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    }
}

/**
 * A flexible list row with leading, title, optional subtitle and trailing slots.
 *
 * The row becomes an accessible, keyboard-focusable button when [onClick] is non-null,
 * and shows a [selected] state whose fill is a lighter, surface-blended gel rather than
 * a primary button's full-strength surface. [onRightClick] (desktop/pointer) receives the
 * position of a secondary click measured relative to the tile.
 *
 * Layout is a full-width row: optional [leading], a weighted column with [title] and optional
 * [subtitle], and optional [trailing], at least 56dp tall by default. Three modes exist: with
 * [onClick] the tile is an `UnstyledButton` (Role.Button semantics, Tab focus, Space/Enter activation,
 * action cursor); with only [onRightClick] it is a pointer-only surface that still shows hover and
 * press feedback; with neither it is a static surface with no interaction states. Interaction
 * feedback comes from Millennium's surface (accent washes, interaction lighting, a spring scale on
 * hover and press) rather than a ripple. The focus border appears only for keyboard focus
 * (pointer clicks do not draw a ring). Selection is signalled by the gel fill plus an accent border;
 * the tile also exposes `selected` semantics so assistive technology announces the state. Disabled tiles are dimmed
 * and suppress both click callbacks. Text slots inherit the themed style, so plain text composables
 * pick up the right color and size, and the subtitle slot uses the subtitle style.
 *
 * ```kotlin
 * var selectedId by remember { mutableStateOf<Int?>(null) }
 * Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
 *     tracks.forEach { track ->
 *         MillenniumListTile(
 *             selected = selectedId == track.id,
 *             onClick = { selectedId = track.id },
 *             onRightClick = { offset -> menu.openAt(track, offset) },
 *             leading = { MillenniumAvatar("MC", track.title) },
 *             title = { Text(track.title) },
 *             subtitle = { Text(track.artist) },
 *             trailing = { MillenniumText(track.duration) },
 *         )
 *     }
 * }
 * ```
 *
 * @param title The primary text slot; inherits the themed title style and color.
 * @param modifier Modifier applied to the tile surface.
 * @param leading Optional leading content (avatar, icon); `null` omits it and its spacing.
 * @param subtitle Optional secondary line under the title.
 * @param trailing Optional trailing content (duration, chevron, switch).
 * @param selected Whether the tile is the current selection (caller-owned).
 * @param enabled Whether the tile accepts input; disabled tiles are dimmed and inert.
 * @param onClick Primary action; non-null makes the tile an accessible button. `null` leaves it
 *   non-clickable unless [onRightClick] is set.
 * @param onRightClick Called on a secondary press with the pointer position relative to the tile;
 *   the event is consumed. Suppressed while disabled.
 * @param titleStyle Overrides the theme title style; `null` uses the theme.
 * @param subtitleStyle Overrides the theme subtitle style; `null` uses the theme.
 * @param titleForeground Overrides the title color when not selected; `null` falls back to the theme,
 *   then the palette text color.
 * @param subtitleForeground Overrides the subtitle color when not selected; `null` falls back to the
 *   theme, then the palette muted color.
 * @param selectedTitleForeground Overrides the title color when selected; `null` falls back to the
 *   theme, then the unselected title color.
 * @param selectedSubtitleForeground Overrides the subtitle color when selected; `null` falls back to
 *   the theme, then the unselected subtitle color, then palette text at 82 percent alpha.
 * @param background Overrides the fill for every state; `null` falls back to the state-derived brush.
 * @param selectedBackground Overrides the fill while selected; `null` falls back to the theme, then
 *   the surface-blended gel.
 * @param selectedGelStrength Overrides how strongly the selected gel is applied; `null` uses the theme.
 * @param shape Overrides the tile shape; `null` uses the theme.
 * @param contentPadding Overrides the inner padding; `null` uses the theme.
 * @param minimumHeight Overrides the minimum height; `null` uses the theme.
 * @param leadingSpacing Overrides the leading gap; `null` uses the theme.
 * @param trailingSpacing Overrides the trailing gap; `null` uses the theme.
 * @param titleSpacing Overrides the title/subtitle gap; `null` uses the theme.
 * @param borderWidth Overrides the border width; `null` uses the theme.
 * @param borderColor Overrides the border color; `null` falls back to the theme, then a state-derived
 *   color (or none).
 * @param innerGlowColor Overrides the inner glow; `null` falls back to the theme, then a palette
 *   highlight for selected/hovered/focused.
 * @param alpha Overrides the overall opacity; `null` uses the theme.
 * @param stateAnimationMillis Overrides the animation duration in milliseconds; `null` uses the theme.
 * @param theme Theme supplying defaults for every nullable override. Defaults to
 *   [MillenniumTheme.listTileTheme].
 * @see MillenniumSidebarItem
 * @see MillenniumListTileTheme
 */
@Composable
fun MillenniumListTile(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    subtitle: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onRightClick: ((Offset) -> Unit)? = null,
    titleStyle: TextStyle? = null,
    subtitleStyle: TextStyle? = null,
    titleForeground: Color? = null,
    subtitleForeground: Color? = null,
    selectedTitleForeground: Color? = null,
    selectedSubtitleForeground: Color? = null,
    background: Brush? = null,
    selectedBackground: Brush? = null,
    selectedGelStrength: Float? = null,
    shape: Shape? = null,
    contentPadding: PaddingValues? = null,
    minimumHeight: Dp? = null,
    leadingSpacing: Dp? = null,
    trailingSpacing: Dp? = null,
    titleSpacing: Dp? = null,
    borderWidth: Dp? = null,
    borderColor: Color? = null,
    innerGlowColor: Color? = null,
    alpha: Float? = null,
    stateAnimationMillis: Int? = null,
    theme: MillenniumListTileTheme = MillenniumTheme.listTileTheme,
) {
    val colors = MillenniumTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsKeyboardFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val hasOnClick = onClick != null
    val clickable = (hasOnClick || onRightClick != null) && enabled
    val state = listTileControlState(
        enabled = enabled,
        clickable = clickable,
        pressed = pressed,
        focused = focused,
        hovered = hovered,
        selected = selected,
    )
    val properties = theme.states.resolve(state)
    val resolvedProperties = properties.copy(
        titleStyle = titleStyle ?: properties.titleStyle,
        subtitleStyle = subtitleStyle ?: properties.subtitleStyle,
        shape = shape ?: properties.shape,
        contentPadding = contentPadding ?: properties.contentPadding,
        minimumHeight = minimumHeight ?: properties.minimumHeight,
        leadingSpacing = leadingSpacing ?: properties.leadingSpacing,
        trailingSpacing = trailingSpacing ?: properties.trailingSpacing,
        titleSpacing = titleSpacing ?: properties.titleSpacing,
        borderWidth = borderWidth ?: properties.borderWidth,
    )
    val resolvedBackground = background
        ?: (if (selected) selectedBackground ?: properties.selectedBackground else null)
        ?: listTileBackground(
            colors,
            selected,
            state,
            selectedGelStrength ?: properties.selectedGelStrength,
        )
    val resolvedTitleForeground = if (selected) {
        selectedTitleForeground ?: properties.selectedTitleForeground
            ?: titleForeground ?: properties.titleForeground ?: colors.text
    } else {
        titleForeground ?: properties.titleForeground ?: if (enabled) colors.text else colors.muted
    }
    val resolvedSubtitleForeground = if (selected) {
        selectedSubtitleForeground ?: properties.selectedSubtitleForeground
            ?: subtitleForeground ?: properties.subtitleForeground ?: colors.text.copy(alpha = 0.82f)
    } else {
        subtitleForeground ?: properties.subtitleForeground ?: colors.muted
    }
    val resolvedBorderColor = borderColor ?: properties.borderColor ?: when {
        state == MillenniumControlState.Error -> colors.danger
        state == MillenniumControlState.Focused -> colors.accent
        selected -> colors.accent.copy(alpha = 0.45f)
        state == MillenniumControlState.Hovered || state == MillenniumControlState.Pressed -> colors.border
        else -> null
    }
    val animatedAlpha by animateFloatAsState(
        alpha ?: properties.alpha,
        tween(stateAnimationMillis ?: properties.stateAnimationMillis),
    )
    val rowScale by animateFloatAsState(
        targetValue = when {
            !clickable -> 1f
            pressed -> 0.985f
            hovered -> 1.006f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
    )
    val surfaceModifier = modifier
        .semantics { this.selected = selected }
        .millenniumActionPointer(enabled && (hasOnClick || onRightClick != null))
        .hoverable(interaction, enabled = clickable && !hasOnClick)
        .fillMaxWidth()
        .graphicsLayer { scaleX = rowScale; scaleY = rowScale }
        .alpha(animatedAlpha)
        .millenniumSurface(
            background = resolvedBackground,
            shape = resolvedProperties.shape,
            borderColor = resolvedBorderColor,
            borderWidth = resolvedProperties.borderWidth,
            innerGlowColor = innerGlowColor ?: resolvedProperties.innerGlowColor
                ?: if (selected || state == MillenniumControlState.Hovered || state == MillenniumControlState.Focused) {
                    colors.highlight.copy(alpha = 0.25f)
                } else {
                    null
                },
        )
        .millenniumInteractionSurface(state, resolvedProperties.shape, animationMillis = stateAnimationMillis ?: properties.stateAnimationMillis)
        .then(secondaryClickModifier(if (enabled) onRightClick else null, interaction))
    val row: @Composable () -> Unit = {
        ListTileRow(
            properties = resolvedProperties,
            titleForeground = resolvedTitleForeground,
            subtitleForeground = resolvedSubtitleForeground,
            leading = leading,
            title = title,
            subtitle = subtitle,
            trailing = trailing,
        )
    }
    if (onClick != null) {
        UnstyledButton(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interaction,
            indication = null,
            contentPadding = PaddingValues(0.dp),
            modifier = surfaceModifier,
        ) { row() }
    } else {
        Box(surfaceModifier) { row() }
    }
}

/**
 * Pointer-only modifier that reports secondary presses through [onRightClick] and mirrors them as
 * press interactions on [interaction] so the tile shows press feedback. Returns an empty modifier
 * when [onRightClick] is `null`.
 */
@Composable
private fun secondaryClickModifier(onRightClick: ((Offset) -> Unit)?, interaction: MutableInteractionSource): Modifier {
    if (onRightClick == null) return Modifier
    val current by rememberUpdatedState(onRightClick)
    return Modifier.pointerInput(Unit) {
        var press: PressInteraction.Press? = null
        try {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        val position = event.changes.firstOrNull()?.position ?: Offset.Zero
                        press?.let { interaction.tryEmit(PressInteraction.Cancel(it)) }
                        press = PressInteraction.Press(position).also { interaction.tryEmit(it) }
                        current(position)
                        event.changes.forEach { it.consume() }
                    } else if (event.type == PointerEventType.Release && !event.buttons.isSecondaryPressed) {
                        press?.let { interaction.tryEmit(PressInteraction.Release(it)) }
                        press = null
                    }
                }
            }
        } finally {
            press?.let { interaction.tryEmit(PressInteraction.Cancel(it)) }
        }
    }
}

/** Lays out the leading, title/subtitle column and trailing slots inside a styled-content scope. */
@Composable
private fun ListTileRow(
    properties: MillenniumListTileThemeProperties,
    titleForeground: Color,
    subtitleForeground: Color,
    leading: (@Composable () -> Unit)?,
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
) {
    MillenniumStyledContent(properties.titleStyle, titleForeground) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = properties.minimumHeight)
                .padding(properties.contentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(properties.leadingSpacing))
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(properties.titleSpacing),
            ) {
                title()
                if (subtitle != null) {
                    MillenniumStyledContent(properties.subtitleStyle, subtitleForeground) { subtitle() }
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(properties.trailingSpacing))
                trailing()
            }
        }
    }
}

/** Design-time preview with a static tile and a selected, clickable tile. */
@Preview
@Composable
private fun MillenniumListTilePreview() = MillenniumPreview {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MillenniumListTile(
            title = { Text("Midnight City") },
            leading = { MillenniumAvatar("MC", "Midnight City") },
            subtitle = { Text("M83 · Hurry Up, We're Dreaming") },
            trailing = { MillenniumText("4:03") },
        )
        MillenniumListTile(
            selected = true,
            onClick = {},
            title = { Text("Selected row") },
            subtitle = { Text("A lighter pour of the same gel.") },
            trailing = { MillenniumIcon(MillenniumIcons.Chevron) },
        )
    }
}
