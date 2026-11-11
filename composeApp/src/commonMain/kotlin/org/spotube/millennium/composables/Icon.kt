package org.spotube.millennium.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composeunstyled.UnstyledIcon
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.MillenniumPreview

/**
 * Visual properties of a [MillenniumIcon] in one control state.
 *
 * @property tint Tint colour; null derives it from the state (text, accent for hovered/focused/selected,
 * accent-bottom for pressed, muted for disabled, danger for error).
 * @property size Width and height of the icon (20dp by default).
 * @property alpha Opacity of the icon (0.45 when disabled by default).
 * @property tintAnimationMillis Duration in milliseconds of tint colour transitions.
 * @see MillenniumIconTheme
 * @see defaultMillenniumIconTheme
 */
@Immutable
data class MillenniumIconThemeProperties(
    val tint: Color? = null,
    val size: Dp,
    val alpha: Float,
    val tintAnimationMillis: Int,
)

/**
 * Per-state theme of [MillenniumIcon].
 *
 * @property states Properties resolved per [MillenniumControlState]; [MillenniumIcon] resolves the state passed in
 * its `state` parameter.
 */
@Immutable
data class MillenniumIconTheme(
    val states: MillenniumControlStateProperty<MillenniumIconThemeProperties>,
)

/**
 * Creates the default [MillenniumIconTheme] installed as `MillenniumTheme.iconTheme`.
 *
 * Defaults: 20dp icons, full alpha (0.45 when disabled) and a 120ms tint animation; tints come from the palette.
 *
 * @return A fresh theme instance.
 */
fun defaultMillenniumIconTheme(): MillenniumIconTheme {
    val base = MillenniumIconThemeProperties(
        size = 20.dp,
        alpha = 1f,
        tintAnimationMillis = 120,
    )
    return MillenniumIconTheme(
        states = MillenniumControlStateProperty(
            default = base,
            hovered = base,
            focused = base,
            pressed = base,
            disabled = base.copy(alpha = 0.45f),
            selected = base,
            error = base,
        ),
    )
}

/**
 * A themed vector icon built on Compose Unstyled's `UnstyledIcon`.
 *
 * Use it for glyphs inside buttons, rows and chips. The icon does not handle input itself; pass the [state] of the
 * surrounding control so the tint follows it (for example [MillenniumControlState.Selected] tints with the accent).
 * Tint changes animate over `tintAnimationMillis`.
 *
 * Accessibility: supply [contentDescription] for meaningful icons; pass null for purely decorative ones.
 *
 * Example:
 * ```kotlin
 * MillenniumIcon(MillenniumIcons.Music, contentDescription = "Music")
 * MillenniumIcon(
 *     MillenniumIcons.Check,
 *     contentDescription = null,
 *     tint = MillenniumTheme.colors.success,
 *     size = 16.dp,
 * )
 * ```
 *
 * @param imageVector The vector to draw, for example one from [MillenniumIcons] or any Compose `ImageVector`.
 * @param contentDescription Text read by screen readers; null marks the icon decorative.
 * @param modifier Modifier applied after the size and alpha are set.
 * @param tint Overrides the tint; null uses the theme, then a state-derived palette colour.
 * @param state Control state used to resolve theme properties and the default tint.
 * @param size Overrides the icon size; null uses the theme (20dp).
 * @param alpha Overrides the opacity; null uses the state's theme value.
 * @param tintAnimationMillis Overrides the tint animation duration; null uses the theme.
 * @param theme Per-state theme; defaults to `MillenniumTheme.iconTheme`.
 * @see MillenniumIcons
 * @see MillenniumIconButton
 */
@Composable
fun MillenniumIcon(
    imageVector: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    state: MillenniumControlState = MillenniumControlState.Default,
    size: Dp? = null,
    alpha: Float? = null,
    tintAnimationMillis: Int? = null,
    theme: MillenniumIconTheme = MillenniumTheme.iconTheme,
) {
    val properties = theme.states.resolve(state)
    val colors = MillenniumTheme.colors
    val targetTint = tint ?: properties.tint ?: when (state) {
        MillenniumControlState.Hovered, MillenniumControlState.Focused, MillenniumControlState.Selected -> colors.accent
        MillenniumControlState.Pressed -> colors.accentBottom
        MillenniumControlState.Disabled -> colors.muted
        MillenniumControlState.Error -> colors.danger
        MillenniumControlState.Default -> colors.text
    }
    val animatedTint = animateColorAsState(
        targetTint,
        tween(tintAnimationMillis ?: properties.tintAnimationMillis),
    ).value
    UnstyledIcon(
        imageVector,
        contentDescription,
        modifier.size(size ?: properties.size).alpha(alpha ?: properties.alpha),
        animatedTint,
    )
}

/**
 * Small, dependency-free set of stroke icons used by the gallery and by themed controls.
 *
 * Every icon is a 24dp by 24dp vector on a 24 by 24 viewport, drawn as a single 1.8-unit round-capped, round-joined
 * black stroke (no fill). The black is a placeholder: [MillenniumIcon] replaces it with its tint, so tint icons rather
 * than relying on the built-in colour. The vectors are built once, when the object is first used.
 *
 * ```kotlin
 * MillenniumIcon(MillenniumIcons.Sparkle, contentDescription = "Featured")
 * ```
 *
 * @see MillenniumIcon
 */
object MillenniumIcons {
    /**
     * A checkmark; used by checkboxes, menus and confirmation affordances.
     */
    val Check = icon("Check") { moveTo(4f, 12f); lineTo(9f, 17f); lineTo(20f, 6f) }
    /**
     * A downward chevron; rotated by disclosures, dropdowns and sidebars to indicate expansion.
     */
    val Chevron = icon("Chevron") { moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f) }
    /**
     * A four-pointed sparkle star, handy for highlights and favourites.
     */
    val Sparkle = icon("Sparkle") {
        moveTo(12f, 2f); lineTo(15f, 9f); lineTo(22f, 12f); lineTo(15f, 15f)
        lineTo(12f, 22f); lineTo(9f, 15f); lineTo(2f, 12f); lineTo(9f, 9f); close()
    }
    /**
     * A pair of beamed eighth notes, representing music or audio content.
     */
    val Music = icon("Music") {
        moveTo(9f, 18f); lineTo(9f, 5f); lineTo(20f, 3f); lineTo(20f, 16f)
        moveTo(9f, 16f); curveTo(2f, 13f, 2f, 23f, 9f, 19f)
        moveTo(20f, 14f); curveTo(13f, 11f, 13f, 21f, 20f, 17f)
    }

    /**
     * Builds a 24dp, 24 by 24 viewport vector with one rounded 1.8-unit black stroke path.
     *
     * @param name Name given to the vector.
     * @param block Path commands to draw.
     */
    private fun icon(name: String, block: PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block,
            )
        }.build()
}

@Preview
@Composable
private fun MillenniumIconPreview() = MillenniumPreview {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp),
    ) {
        MillenniumIcon(MillenniumIcons.Sparkle, contentDescription = "Sparkle", tint = MillenniumTheme.colors.accent)
        MillenniumIcon(MillenniumIcons.Music, contentDescription = "Music")
        MillenniumIcon(MillenniumIcons.Check, contentDescription = "Check", tint = MillenniumTheme.colors.success)
    }
}
