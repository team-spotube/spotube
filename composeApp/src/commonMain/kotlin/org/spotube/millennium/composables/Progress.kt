package org.spotube.millennium.composables

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composeunstyled.Indicator
import com.composeunstyled.UnstyledProgress
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.MillenniumPreview
import org.spotube.millennium.theme.gelBrush
import org.spotube.millennium.theme.millenniumBevelBrush
import org.spotube.millennium.theme.millenniumSurface

/**
 * Appearance of one control state of [MillenniumProgress] and [MillenniumIndeterminateProgress].
 *
 * Instances are normally produced by [defaultMillenniumProgressTheme] and customised with `copy`. The
 * composables currently resolve only [MillenniumControlState.Default] from the owning
 * [MillenniumProgressTheme]; the other states exist so a theme can describe them uniformly.
 *
 * @property trackBackground Brush filling the recessed track; `null` derives a sunken bevel brush from the
 * active palette.
 * @property progressBackground Brush filling the indicator; `null` derives the accent gel gradient
 * (`accentTop` to `accentBottom`) from the active palette.
 * @property shape Shape of both the track and the indicator.
 * @property height Track height in dp.
 * @property borderColor Hairline colour around the track; `null` uses the palette border colour.
 * @property borderWidth Hairline thickness in dp.
 * @property innerGlowColor Inner highlight colour of the track; `null` uses the translucent palette highlight.
 * @property determinateAnimationMillis Duration (ms) of determinate value changes. Only applied when the
 * matching `determinateAnimationMillis` argument of [MillenniumProgress] is passed explicitly (a timed tween);
 * otherwise the bar animates with its default spring.
 * @property indeterminateAnimationMillis Duration in ms of one sweep (one direction) of the indeterminate
 * block.
 * @property indeterminateFraction Width of the sweeping block as a fraction (0..1) of the track width.
 * @property alpha Overall opacity multiplier (1 = opaque).
 */
@Immutable
data class MillenniumProgressThemeProperties(
    val trackBackground: Brush? = null,
    val progressBackground: Brush? = null,
    val shape: Shape,
    val height: Dp,
    val borderColor: Color? = null,
    val borderWidth: Dp,
    val innerGlowColor: Color? = null,
    val determinateAnimationMillis: Int,
    val indeterminateAnimationMillis: Int,
    val indeterminateFraction: Float,
    val alpha: Float,
)

/**
 * Theme for the progress bars, grouping [MillenniumProgressThemeProperties] per control state.
 *
 * Installed globally as [MillenniumTheme.progressTheme] and overridable per call through the `theme`
 * parameter of [MillenniumProgress] and [MillenniumIndeterminateProgress].
 *
 * @property states State-resolved properties; both composables resolve [MillenniumControlState.Default].
 * @see defaultMillenniumProgressTheme
 */
@Immutable
data class MillenniumProgressTheme(
    val states: MillenniumControlStateProperty<MillenniumProgressThemeProperties>,
)

/**
 * Creates the default progress theme: a 12dp fully rounded sunken track with a 1dp hairline, an accent
 * gel indicator, a 30% wide indeterminate block sweeping in 1400ms, a thicker (2dp) border for focused and
 * error states and 45% opacity when disabled.
 *
 * Customise by copying a state, for example:
 * ```kotlin
 * val base = defaultMillenniumProgressTheme()
 * val thick = base.copy(
 *     states = base.states.copy(default = base.states.default.copy(height = 20.dp)),
 * )
 * ```
 *
 * @return A new [MillenniumProgressTheme] resolved lazily against the active palette.
 */
fun defaultMillenniumProgressTheme(): MillenniumProgressTheme {
    val base = MillenniumProgressThemeProperties(
        shape = RoundedCornerShape(50),
        height = 12.dp,
        borderWidth = 1.dp,
        determinateAnimationMillis = 180,
        indeterminateAnimationMillis = 1400,
        indeterminateFraction = 0.30f,
        alpha = 1f,
    )
    return MillenniumProgressTheme(
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
 * A determinate progress bar: a recessed glass track with an accent gel indicator that fills from the
 * start edge.
 *
 * Use it when the completed fraction is known; use [MillenniumIndeterminateProgress] otherwise. The bar fills
 * the available width and has a fixed [height].
 *
 * Behaviour: [progress] is clamped to 0..1 and animated with a medium-low-stiffness spring (damping 0.8), so
 * value changes glide rather than jump. State is caller-owned. Accessibility comes from Compose Unstyled's
 * `UnstyledProgress`, which applies `progressSemantics(progress, 0f..1f)`, so the animated value is exposed as
 * a progress-bar range (`progressBarRangeInfo`) to assistive technology.
 *
 * ```kotlin
 * var downloaded by remember { mutableFloatStateOf(0.4f) }
 * MillenniumProgress(
 *     progress = downloaded,
 *     modifier = Modifier.fillMaxWidth().padding(16.dp),
 * )
 * ```
 *
 * @param progress Completed fraction; values outside 0..1 are clamped.
 * @param modifier Modifier applied to the track (it already fills max width and sets the height).
 * @param trackBackground Overrides the track brush; `null` falls back to the theme, then a sunken bevel brush.
 * @param progressBackground Overrides the indicator brush; `null` falls back to the theme, then the accent gel.
 * @param shape Overrides the track and indicator shape; `null` falls back to the theme.
 * @param height Overrides the track height; `null` falls back to the theme.
 * @param borderColor Overrides the hairline colour; `null` falls back to the theme, then the palette border.
 * @param borderWidth Overrides the hairline thickness; `null` falls back to the theme.
 * @param innerGlowColor Overrides the track inner glow; `null` falls back to the theme, then the palette
 * highlight.
 * @param determinateAnimationMillis When non-null, value changes animate with a tween of this many ms instead
 * of the default spring.
 * @param alpha Overrides the overall opacity; `null` falls back to the theme.
 * @param theme Theme supplying defaults; defaults to [MillenniumTheme.progressTheme].
 * @see MillenniumIndeterminateProgress
 * @see MillenniumProgressTheme
 */
@Composable
fun MillenniumProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    trackBackground: Brush? = null,
    progressBackground: Brush? = null,
    shape: Shape? = null,
    height: Dp? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    innerGlowColor: Color? = null,
    determinateAnimationMillis: Int? = null,
    alpha: Float? = null,
    theme: MillenniumProgressTheme = MillenniumTheme.progressTheme,
) {
    val properties = theme.states.resolve(MillenniumControlState.Default)
    val colors = MillenniumTheme.colors
    val resolvedShape = shape ?: properties.shape
    val trackBackground = trackBackground ?: properties.trackBackground ?: millenniumBevelBrush(colors, sunken = true)
    val progressBackground = progressBackground ?: properties.progressBackground
        ?: gelBrush(colors.accentTop, colors.accentBottom)
    val borderColor = borderColor ?: properties.borderColor ?: colors.border
    val glowColor = innerGlowColor ?: properties.innerGlowColor ?: colors.highlight.copy(alpha = 0.45f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        // An explicit duration gives a timed tween; otherwise the default spring is used.
        animationSpec = if (determinateAnimationMillis != null) {
            tween(determinateAnimationMillis, easing = FastOutSlowInEasing)
        } else {
            spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow)
        },
    )
    UnstyledProgress(
        animatedProgress,
        modifier.fillMaxWidth().height(height ?: properties.height).millenniumSurface(
            background = trackBackground,
            shape = resolvedShape,
            borderColor = borderColor,
            borderWidth = borderWidth ?: properties.borderWidth,
            innerGlowColor = glowColor,
            bottomRimColor = colors.highlight.copy(alpha = 0.3f),
        ).alpha(alpha ?: properties.alpha),
    ) {
        Indicator(
            Modifier.fillMaxHeight().background(progressBackground, resolvedShape),
        )
    }
}

/**
 * An indeterminate progress bar: the same glass track as [MillenniumProgress] with an accent gel block that
 * sweeps back and forth.
 *
 * Use it for work of unknown duration where a bar (rather than the circular or dotted indicators in
 * `Loading.kt`) fits the layout. The bar fills the available width.
 *
 * Behaviour: an infinite transition moves the block between both ends and reverses
 * ([RepeatMode.Reverse]) using `FastOutSlowInEasing`; each sweep lasts [indeterminateAnimationMillis].
 * Accessibility: built on Compose Unstyled's value-less `UnstyledProgress`, which applies
 * `progressSemantics()` with no value, so it is announced as an indeterminate progress bar. It has no text label; wrap it with a visible or semantic label if context is
 * needed.
 *
 * ```kotlin
 * if (isSyncing) {
 *     MillenniumIndeterminateProgress(
 *         modifier = Modifier.fillMaxWidth(),
 *         indeterminateFraction = 0.25f,
 *     )
 * }
 * ```
 *
 * @param modifier Modifier applied to the track.
 * @param trackBackground Overrides the track brush; `null` falls back to the theme, then a sunken bevel brush.
 * @param progressBackground Overrides the moving block brush; `null` falls back to the theme, then the accent gel.
 * @param shape Overrides the track and block shape; `null` falls back to the theme.
 * @param height Overrides the track height; `null` falls back to the theme.
 * @param borderColor Overrides the hairline colour; `null` falls back to the theme, then the palette border.
 * @param borderWidth Overrides the hairline thickness; `null` falls back to the theme.
 * @param innerGlowColor Overrides the track inner glow; `null` falls back to the theme, then the palette
 * highlight.
 * @param indeterminateAnimationMillis Overrides the duration of one sweep in ms; `null` falls back to the theme.
 * @param indeterminateFraction Overrides the block width as a fraction of the track (clamped to 0..1); `null`
 * falls back to the theme.
 * @param alpha Overrides the overall opacity; `null` falls back to the theme.
 * @param theme Theme supplying defaults; defaults to [MillenniumTheme.progressTheme].
 * @see MillenniumProgress
 * @see MillenniumProgressTheme
 */
@Composable
fun MillenniumIndeterminateProgress(
    modifier: Modifier = Modifier,
    trackBackground: Brush? = null,
    progressBackground: Brush? = null,
    shape: Shape? = null,
    height: Dp? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    innerGlowColor: Color? = null,
    indeterminateAnimationMillis: Int? = null,
    indeterminateFraction: Float? = null,
    alpha: Float? = null,
    theme: MillenniumProgressTheme = MillenniumTheme.progressTheme,
) {
    val properties = theme.states.resolve(MillenniumControlState.Default)
    val colors = MillenniumTheme.colors
    val resolvedShape = shape ?: properties.shape
    val trackBackground = trackBackground ?: properties.trackBackground ?: millenniumBevelBrush(colors, sunken = true)
    val progressBackground = progressBackground ?: properties.progressBackground
        ?: gelBrush(colors.accentTop, colors.accentBottom)
    val borderColor = borderColor ?: properties.borderColor ?: colors.border
    val glowColor = innerGlowColor ?: properties.innerGlowColor ?: colors.highlight.copy(alpha = 0.45f)
    val transition = rememberInfiniteTransition()
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(
                durationMillis = indeterminateAnimationMillis ?: properties.indeterminateAnimationMillis,
                easing = FastOutSlowInEasing,
            ),
            RepeatMode.Reverse,
        ),
    )
    UnstyledProgress(
        modifier.fillMaxWidth().height(height ?: properties.height).millenniumSurface(
            background = trackBackground,
            shape = resolvedShape,
            borderColor = borderColor,
            borderWidth = borderWidth ?: properties.borderWidth,
            innerGlowColor = glowColor,
            bottomRimColor = colors.highlight.copy(alpha = 0.3f),
        ).alpha(alpha ?: properties.alpha),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = BiasAlignment(offset * 2f - 1f, 0f)) {
            Box(
                Modifier.fillMaxWidth((indeterminateFraction ?: properties.indeterminateFraction).coerceIn(0f, 1f))
                    .fillMaxHeight().background(progressBackground, resolvedShape),
            )
        }
    }
}

/** Preview of both progress bars. */
@Preview
@Composable
private fun MillenniumProgressPreview() = MillenniumPreview {
    MillenniumProgress(0.68f)
    MillenniumIndeterminateProgress()
}
