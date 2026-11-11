package org.spotube.millennium.composables

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumPreview
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.gelBrush
import org.spotube.millennium.theme.millenniumBevelBrush
import org.spotube.millennium.theme.millenniumSurface

// ---------------------------------------------------------------------------
// Theme
// ---------------------------------------------------------------------------

/**
 * Shared knobs for the extra progress bars and the infinite loading indicators in this file.
 *
 * Normally produced by [defaultMillenniumLoadingTheme] and customised with `copy`. All composables here
 * resolve only [MillenniumControlState.Default] from the owning [MillenniumLoadingTheme]; individual
 * composables derive some values from these (for example half or one and a half times
 * [animationMillis]) so one setting speeds up the whole family.
 *
 * @property size Base diameter in dp of circular indicators; spinners derive theirs from it (orb: +8dp,
 * spoke: -8dp, pulse: +16dp).
 * @property thickness Ring thickness in dp of [MillenniumCircularProgress].
 * @property linearHeight Height in dp of the extra linear bars (striped, segmented, buffered, marquee, shimmer).
 * @property animationMillis Duration in ms of one loop of an infinite animation (scaled per variant).
 * @property stripeWidth Width in dp of one barber-pole stripe in [MillenniumStripedProgress].
 * @property segmentCount Default number of blocks in [MillenniumSegmentedProgress].
 * @property segmentSpacing Gap in dp between the segmented blocks.
 * @property color Replaces the palette accent when set (a gel is built from it); `null` uses the accent gel.
 * @property trackColor Ring track colour of [MillenniumCircularProgress]; `null` uses the palette inset colour.
 * @property alpha Overall opacity multiplier (1 = opaque).
 */
@Immutable
data class MillenniumLoadingThemeProperties(
    /** Default diameter of circular indicators and spinners. */
    val size: Dp,
    /** Ring thickness of the circular progress. */
    val thickness: Dp,
    /** Height of the extra linear bars (striped, segmented, buffered, marquee, shimmer). */
    val linearHeight: Dp,
    /** One loop of an infinite animation. */
    val animationMillis: Int,
    /** Width of one barber-pole stripe. */
    val stripeWidth: Dp,
    val segmentCount: Int,
    val segmentSpacing: Dp,
    /** Overrides the accent when set. */
    val color: Color? = null,
    val trackColor: Color? = null,
    val alpha: Float = 1f,
)

/**
 * Theme for the loading indicators and extra progress bars, grouping [MillenniumLoadingThemeProperties] per
 * control state and installed as [MillenniumTheme.loadingTheme].
 *
 * @property states State-resolved properties; every composable here resolves [MillenniumControlState.Default].
 * @see defaultMillenniumLoadingTheme
 */
@Immutable
data class MillenniumLoadingTheme(
    val states: MillenniumControlStateProperty<MillenniumLoadingThemeProperties>,
)

/**
 * Creates the default loading theme: 40dp indicators with a 6dp ring, 14dp tall linear bars, 12dp barber
 * stripes, 12 segments with 3dp gaps, a 1300ms base loop, the palette accent as colour and 45% opacity when
 * disabled.
 *
 * Customise with `copy`:
 * ```kotlin
 * val base = defaultMillenniumLoadingTheme()
 * val calm = base.copy(states = base.states.copy(default = base.states.default.copy(animationMillis = 2000)))
 * ```
 *
 * @return A new [MillenniumLoadingTheme].
 */
fun defaultMillenniumLoadingTheme(): MillenniumLoadingTheme {
    val base = MillenniumLoadingThemeProperties(
        size = 40.dp,
        thickness = 6.dp,
        linearHeight = 14.dp,
        animationMillis = 1300,
        stripeWidth = 12.dp,
        segmentCount = 12,
        segmentSpacing = 3.dp,
    )
    return MillenniumLoadingTheme(
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

// ---------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------

/** Internal: the accent's gel endpoints, or a gel built from a caller-supplied colour. */
private class LoadingGel(val top: Color, val bottom: Color) {
    val brush: Brush get() = gelBrush(top, bottom)
    val glow: Color get() = bottom
}

@Composable
private fun loadingGel(color: Color?, theme: MillenniumLoadingThemeProperties): LoadingGel {
    val colors = MillenniumTheme.colors
    val override = color ?: theme.color
    return if (override != null) {
        LoadingGel(lerp(override, Color.White, 0.28f), override)
    } else {
        LoadingGel(colors.accentTop, colors.accentBottom)
    }
}

@Composable
private fun loopingPhase(millis: Int, easing: androidx.compose.animation.core.Easing = LinearEasing): State<Float> =
    rememberInfiniteTransition(label = "millennium-loading").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(millis, easing = easing)),
        label = "phase",
    )

/**
 * Internal: sets `contentDescription` to [label] and `progressBarRangeInfo` to the clamped [progress]
 * (0..1), or `ProgressBarRangeInfo.Indeterminate` when [progress] is null. Used by every indicator here
 * except [MillenniumSkeleton], which is silent unless you pass a `contentDescription` (give one skeleton in a
 * group a description rather than every placeholder).
 */
private fun Modifier.loadingSemantics(progress: Float?, label: String): Modifier = semantics {
    contentDescription = label
    progressBarRangeInfo = if (progress == null) {
        ProgressBarRangeInfo.Indeterminate
    } else {
        ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)
    }
}

private fun wrap01(value: Float): Float = value - floor(value)

/** Internal: recessed track shared by every linear variant, matching [MillenniumProgress]; carries the semantics. */
@Composable
private fun LinearTrack(
    modifier: Modifier,
    height: Dp,
    shape: Shape,
    alpha: Float,
    progress: Float?,
    label: String,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MillenniumTheme.colors
    Box(
        modifier.fillMaxWidth().height(height).loadingSemantics(progress, label)
            .millenniumSurface(
                background = millenniumBevelBrush(colors, sunken = true),
                shape = shape,
                borderColor = colors.border,
                borderWidth = 1.dp,
                innerGlowColor = colors.highlight.copy(alpha = 0.45f),
                bottomRimColor = colors.highlight.copy(alpha = 0.3f),
            ).alpha(alpha),
        content = content,
    )
}

private fun DrawScope.glossBand(color: Color = Color.White) {
    // A soft reflection across the upper half, as on the gel fill.
    drawRect(
        brush = Brush.verticalGradient(
            0f to color.copy(alpha = 0.38f),
            0.5f to color.copy(alpha = 0.06f),
            0.5f to Color.Transparent,
            1f to Color.Transparent,
        ),
    )
}

// ---------------------------------------------------------------------------
// Linear progress variants
// ---------------------------------------------------------------------------

/**
 * Classic barber-pole bar: gel fill with diagonal stripes that march to the right.
 *
 * With a null [progress] the fill spans the whole track and only the stripes move (indeterminate). A given
 * [progress] animates the fill with a spring (damping 0.8) and the stripes keep marching on top, with a gloss band over the upper half.
 * Accessibility: sets `contentDescription` and `progressBarRangeInfo` (0..1 when [progress] is given, `ProgressBarRangeInfo.Indeterminate` when null).
 *
 * ```kotlin
 * MillenniumStripedProgress(
 *     progress = uploaded / total.toFloat(),
 *     modifier = Modifier.fillMaxWidth(),
 *     contentDescription = "Uploading",
 * )
 * ```
 *
 *
 * @param modifier Modifier applied to the indicator (linear bars already fill the max width).
 * @param progress Completed fraction 0..1 (clamped), or `null` for the indeterminate marching pole.
 * @param height Overrides the bar height; `null` falls back to the theme linear height.
 * @param color Overrides the accent; `null` falls back to the theme colour, then the palette accent gel.
 * @param shape Overrides the track and fill shape; `null` uses a fully rounded shape.
 * @param stripeWidth Overrides the stripe width; `null` falls back to the theme.
 * @param animationMillis Overrides the loop duration in ms; `null` falls back to half of the theme `animationMillis`.
 * @param contentDescription Accessibility label announced by screen readers; defaults to "Progress". Localise it for your app.
 * @param theme Theme supplying defaults (size, durations, tint, opacity); defaults to [MillenniumTheme.loadingTheme].
 * @see MillenniumProgress
 */
@Composable
fun MillenniumStripedProgress(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    height: Dp? = null,
    color: Color? = null,
    shape: Shape? = null,
    stripeWidth: Dp? = null,
    animationMillis: Int? = null,
    contentDescription: String = "Progress",
    theme: MillenniumLoadingTheme = MillenniumTheme.loadingTheme,
) {
    val properties = theme.states.resolve(MillenniumControlState.Default)
    val gel = loadingGel(color, properties)
    val resolvedShape = shape ?: RoundedCornerShape(50)
    val fraction by animateFloatAsState(
        targetValue = (progress ?: 1f).coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow),
    )
    val phase = loopingPhase(animationMillis ?: (properties.animationMillis / 2))
    val stripe = stripeWidth ?: properties.stripeWidth
    LinearTrack(modifier, height ?: properties.linearHeight, resolvedShape, properties.alpha, progress, contentDescription) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(fraction)
                .clip(resolvedShape)
                .background(gel.brush)
                .drawWithContent {
                    drawContent()
                    val w = stripe.toPx()
                    val period = w * 2f
                    val h = size.height
                    val stripes = Path()
                    var x = -h - period + phase.value * period
                    while (x < size.width + h) {
                        stripes.moveTo(x, h)
                        stripes.lineTo(x + w, h)
                        stripes.lineTo(x + w + h, 0f)
                        stripes.lineTo(x + h, 0f)
                        stripes.close()
                        x += period
                    }
                    drawPath(stripes, Color.White.copy(alpha = 0.26f))
                    glossBand()
                },
        )
    }
}

/**
 * Determinate gel bar with a bright glint that sweeps across the fill.
 *
 * Use it for a prominent task where plain [MillenniumProgress] feels too quiet. The fill animates with a spring and a
 * white glint loops across it using `FastOutSlowInEasing`.
 * Accessibility: sets `contentDescription` and `progressBarRangeInfo` (0..1 for [progress]); it is never
 * indeterminate because [progress] is required.
 *
 * ```kotlin
 * MillenniumShimmerProgress(
 *     progress = 0.62f,
 *     modifier = Modifier.fillMaxWidth(),
 *     contentDescription = "Installing",
 * )
 * ```
 *
 *
 * @param progress Completed fraction 0..1 (clamped).
 * @param modifier Modifier applied to the indicator (linear bars already fill the max width).
 * @param height Overrides the bar height; `null` falls back to the theme linear height.
 * @param color Overrides the accent; `null` falls back to the theme colour, then the palette accent gel.
 * @param shape Overrides the track and fill shape; `null` uses a fully rounded shape.
 * @param animationMillis Overrides the loop duration in ms; `null` falls back to one and a half times the theme `animationMillis`.
 * @param contentDescription Accessibility label announced by screen readers; defaults to "Progress". Localise it for your app.
 * @param theme Theme supplying defaults (size, durations, tint, opacity); defaults to [MillenniumTheme.loadingTheme].
 * @see MillenniumStripedProgress
 */
@Composable
fun MillenniumShimmerProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp? = null,
    color: Color? = null,
    shape: Shape? = null,
    animationMillis: Int? = null,
    contentDescription: String = "Progress",
    theme: MillenniumLoadingTheme = MillenniumTheme.loadingTheme,
) {
    val properties = theme.states.resolve(MillenniumControlState.Default)
    val gel = loadingGel(color, properties)
    val resolvedShape = shape ?: RoundedCornerShape(50)
    val fraction by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow),
    )
    val phase = loopingPhase(animationMillis ?: (properties.animationMillis * 3 / 2), FastOutSlowInEasing)
    LinearTrack(modifier, height ?: properties.linearHeight, resolvedShape, properties.alpha, progress, contentDescription) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(fraction)
                .clip(resolvedShape)
                .background(gel.brush)
                .drawWithContent {
                    drawContent()
                    val band = max(size.width * 0.4f, 48.dp.toPx())
                    val start = -band + (size.width + band) * phase.value
                    drawRect(
                        Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.5f to Color.White.copy(alpha = 0.65f),
                            1f to Color.Transparent,
                            startX = start,
                            endX = start + band,
                        ),
                    )
                    glossBand()
                },
        )
    }
}

/**
 * Windows-style progress made of separate recessed blocks.
 *
 * Determinate when [progress] is set (blocks light up one by one, the boundary block fades in proportionally); otherwise a lit group
 * of blocks chases along the track with a fading tail.
 * Accessibility: sets `contentDescription` and `progressBarRangeInfo` (0..1 when [progress] is given, `ProgressBarRangeInfo.Indeterminate` when null).
 *
 * ```kotlin
 * MillenniumSegmentedProgress(
 *     progress = 0.5f,
 *     segments = 10,
 *     contentDescription = "Scanning",
 * )
 * ```
 *
 *
 * @param modifier Modifier applied to the indicator (linear bars already fill the max width).
 * @param progress Completed fraction 0..1 (clamped), or `null` for the chasing animation.
 * @param segments Overrides the block count (at least 1); `null` falls back to the theme `segmentCount`.
 * @param height Overrides the bar height; `null` falls back to the theme linear height.
 * @param color Overrides the accent; `null` falls back to the theme colour, then the palette accent gel.
 * @param spacing Overrides the gap between blocks; `null` falls back to the theme.
 * @param animationMillis Overrides the loop duration in ms; `null` falls back to one and a half times the theme `animationMillis`.
 * @param contentDescription Accessibility label announced by screen readers; defaults to "Progress". Localise it for your app.
 * @param theme Theme supplying defaults (size, durations, tint, opacity); defaults to [MillenniumTheme.loadingTheme].
 * @see MillenniumProgress
 */
@Composable
fun MillenniumSegmentedProgress(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    segments: Int? = null,
    height: Dp? = null,
    color: Color? = null,
    spacing: Dp? = null,
    animationMillis: Int? = null,
    contentDescription: String = "Progress",
    theme: MillenniumLoadingTheme = MillenniumTheme.loadingTheme,
) {
    val properties = theme.states.resolve(MillenniumControlState.Default)
    val colors = MillenniumTheme.colors
    val gel = loadingGel(color, properties)
    val count = (segments ?: properties.segmentCount).coerceAtLeast(1)
    val blockShape = RoundedCornerShape(3.dp)
    val animated = animateFloatAsState(
        targetValue = (progress ?: 0f).coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
    )
    val phase = loopingPhase(animationMillis ?: (properties.animationMillis * 3 / 2))
    Row(
        modifier.fillMaxWidth().height(height ?: properties.linearHeight)
            .loadingSemantics(progress, contentDescription).alpha(properties.alpha),
        horizontalArrangement = Arrangement.spacedBy(spacing ?: properties.segmentSpacing),
    ) {
        repeat(count) { index ->
            Box(
                Modifier.weight(1f).fillMaxHeight().millenniumSurface(
                    background = millenniumBevelBrush(colors, sunken = true),
                    shape = blockShape,
                    borderColor = colors.border,
                    borderWidth = 1.dp,
                ),
            ) {
                Box(
                    Modifier.fillMaxSize()
                        .graphicsLayer {
                            alpha = if (progress != null) {
                                (animated.value * count - index).coerceIn(0f, 1f)
                            } else {
                                // Distance (in blocks) behind the moving head, wrapped around the track.
                                val head = phase.value * (count + 3)
                                val behind = (head - index + count + 3) % (count + 3)
                                (1f - behind / 3f).coerceIn(0f, 1f)
                            }
                        }
                        .clip(blockShape)
                        .background(gel.brush)
                        .drawWithContent { drawContent(); glossBand() },
                )
            }
        }
    }
}

/**
 * Streaming-style bar: a pale buffered region ahead of the gel playhead.
 *
 * Use it for media playback or downloads that expose both position and buffered amount. Both values animate with springs. The buffered
 * region is never shorter than [progress].
 * Accessibility: sets `contentDescription` and `progressBarRangeInfo` for [progress] only; the buffered amount is not exposed.
 *
 * ```kotlin
 * MillenniumBufferedProgress(
 *     progress = position / duration,
 *     buffered = bufferedPosition / duration,
 *     contentDescription = "Playback position",
 * )
 * ```
 *
 *
 * @param progress Played fraction 0..1 (clamped).
 * @param buffered Buffered fraction 0..1; values below [progress] are raised to it.
 * @param modifier Modifier applied to the indicator (linear bars already fill the max width).
 * @param height Overrides the bar height; `null` falls back to the theme linear height.
 * @param color Overrides the accent; `null` falls back to the theme colour, then the palette accent gel.
 * @param shape Overrides the track and fill shape; `null` uses a fully rounded shape.
 * @param contentDescription Accessibility label announced by screen readers; defaults to "Progress". Localise it for your app.
 * @param theme Theme supplying defaults (size, durations, tint, opacity); defaults to [MillenniumTheme.loadingTheme].
 * @see MillenniumShimmerProgress
 */
@Composable
fun MillenniumBufferedProgress(
    progress: Float,
    buffered: Float,
    modifier: Modifier = Modifier,
    height: Dp? = null,
    color: Color? = null,
    shape: Shape? = null,
    contentDescription: String = "Progress",
    theme: MillenniumLoadingTheme = MillenniumTheme.loadingTheme,
) {
    val properties = theme.states.resolve(MillenniumControlState.Default)
    val gel = loadingGel(color, properties)
    val resolvedShape = shape ?: RoundedCornerShape(50)
    val spec = spring<Float>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
    val played by animateFloatAsState(progress.coerceIn(0f, 1f), spec)
    val buffer by animateFloatAsState(max(buffered, progress).coerceIn(0f, 1f), spec)
    LinearTrack(modifier, height ?: properties.linearHeight, resolvedShape, properties.alpha, progress, contentDescription) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(buffer).clip(resolvedShape).background(
                Brush.verticalGradient(
                    listOf(gel.top.copy(alpha = 0.34f), gel.bottom.copy(alpha = 0.26f)),
                ),
            ),
        )
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(played).clip(resolvedShape).background(gel.brush)
                .drawWithContent { drawContent(); glossBand() },
        )
    }
}

/**
 * Two gel bars that sweep through the track at different speeds.
 *
 * An indeterminate bar for work of unknown length. Each bar uses `FastOutSlowInEasing`, the second is half a loop out of phase and narrower.
 * Accessibility: sets `contentDescription` and `ProgressBarRangeInfo.Indeterminate`, so it is announced as a busy indicator.
 *
 * ```kotlin
 * MillenniumMarqueeProgress(contentDescription = "Connecting")
 * ```
 *
 *
 * @param modifier Modifier applied to the indicator (linear bars already fill the max width).
 * @param height Overrides the bar height; `null` falls back to the theme linear height.
 * @param color Overrides the accent; `null` falls back to the theme colour, then the palette accent gel.
 * @param shape Overrides the track and fill shape; `null` uses a fully rounded shape.
 * @param animationMillis Overrides the loop duration in ms; `null` falls back to one and a half times the theme `animationMillis`.
 * @param contentDescription Accessibility label announced by screen readers; defaults to "Loading". Localise it for your app.
 * @param theme Theme supplying defaults (size, durations, tint, opacity); defaults to [MillenniumTheme.loadingTheme].
 * @see MillenniumIndeterminateProgress
 */
@Composable
fun MillenniumMarqueeProgress(
    modifier: Modifier = Modifier,
    height: Dp? = null,
    color: Color? = null,
    shape: Shape? = null,
    animationMillis: Int? = null,
    contentDescription: String = "Loading",
    theme: MillenniumLoadingTheme = MillenniumTheme.loadingTheme,
) {
    val properties = theme.states.resolve(MillenniumControlState.Default)
    val gel = loadingGel(color, properties)
    val resolvedShape = shape ?: RoundedCornerShape(50)
    val phase = loopingPhase(animationMillis ?: (properties.animationMillis * 3 / 2))
    LinearTrack(modifier, height ?: properties.linearHeight, resolvedShape, properties.alpha, null, contentDescription) {
        Canvas(Modifier.fillMaxSize()) {
            val t = phase.value
            fun bar(progress: Float, width: Float) {
                val eased = FastOutSlowInEasing.transform(progress.coerceIn(0f, 1f))
                val w = size.width * width
                val x = -w + (size.width + w) * eased
                drawRoundRect(
                    brush = gel.brush,
                    topLeft = Offset(x, 0f),
                    size = Size(w, size.height),
                    cornerRadius = CornerRadius(size.height / 2f),
                )
            }
            bar(t, 0.38f)
            bar(wrap01(t + 0.5f), 0.24f)
        }
    }
}

// ---------------------------------------------------------------------------
// Circular
// ---------------------------------------------------------------------------

/**
 * Round gel progress with a recessed ring.
 *
 * Pass a null [progress] for an endlessly spinning, breathing arc (the sweep grows and shrinks between 40 and 290 degrees while rotating);
 * otherwise the arc starts at the top and fills clockwise with a spring. A reflection arc rides the outer edge. [content] is centered inside the ring, e.g. a percentage.
 * Accessibility: sets `contentDescription` and `progressBarRangeInfo` (0..1 when [progress] is given, `ProgressBarRangeInfo.Indeterminate` when null).
 *
 * ```kotlin
 * MillenniumCircularProgress(progress = 0.7f, size = 64.dp, contentDescription = "Sync progress") {
 *     MillenniumText("70%")
 * }
 * ```
 *
 *
 * @param modifier Modifier applied to the indicator (linear bars already fill the max width).
 * @param progress Completed fraction 0..1 (clamped), or `null` for the spinning arc.
 * @param size Overrides the diameter; `null` falls back to the theme size.
 * @param thickness Overrides the ring thickness; `null` falls back to the theme.
 * @param color Overrides the accent; `null` falls back to the theme colour, then the palette accent gel.
 * @param trackColor Overrides the ring track colour; `null` falls back to the theme, then the palette inset colour.
 * @param animationMillis Overrides the loop duration in ms; `null` falls back to the theme `animationMillis` (rotation); the breathing cycle uses two thirds of it.
 * @param contentDescription Accessibility label announced by screen readers; defaults to "Progress". Localise it for your app.
 * @param theme Theme supplying defaults (size, durations, tint, opacity); defaults to [MillenniumTheme.loadingTheme].
 * @param content Optional content centered inside the ring; `null` for none.
 * @see MillenniumOrbSpinner
 */
@Composable
fun MillenniumCircularProgress(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    size: Dp? = null,
    thickness: Dp? = null,
    color: Color? = null,
    trackColor: Color? = null,
    animationMillis: Int? = null,
    contentDescription: String = "Progress",
    theme: MillenniumLoadingTheme = MillenniumTheme.loadingTheme,
    content: (@Composable BoxScope.() -> Unit)? = null,
) {
    val properties = theme.states.resolve(MillenniumControlState.Default)
    val colors = MillenniumTheme.colors
    val gel = loadingGel(color, properties)
    val resolvedTrack = trackColor ?: properties.trackColor ?: colors.inset
    val animated = animateFloatAsState(
        targetValue = (progress ?: 0f).coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow),
    )
    val rotation = loopingPhase(animationMillis ?: properties.animationMillis)
    val breathing = rememberInfiniteTransition(label = "millennium-loading-breath").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween((animationMillis ?: properties.animationMillis) * 2 / 3, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "breathing",
    )
    val borderColor = colors.border
    Box(
        modifier.size(size ?: properties.size).loadingSemantics(progress, contentDescription).alpha(properties.alpha),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = (thickness ?: properties.thickness).toPx()
            val rim = 1.dp.toPx()
            val inset = stroke / 2f + rim
            val topLeft = Offset(inset, inset)
            val arcSize = Size(this.size.width - inset * 2f, this.size.height - inset * 2f)
            // Recessed ring: hairline outer rim, inset floor.
            drawArc(borderColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke + rim * 2f))
            drawArc(resolvedTrack, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            val start: Float
            val sweep: Float
            if (progress != null) {
                start = -90f
                sweep = 360f * animated.value
            } else {
                start = rotation.value * 360f - 90f
                sweep = 40f + 250f * breathing.value
            }
            if (sweep > 0.5f) {
                drawArc(gel.brush, start, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                // Reflection riding the outer edge of the arc.
                val lift = stroke * 0.22f
                drawArc(
                    Color.White.copy(alpha = 0.42f), start, sweep, false,
                    Offset(inset - lift, inset - lift),
                    Size(arcSize.width + lift * 2f, arcSize.height + lift * 2f),
                    style = Stroke(stroke * 0.26f, cap = StrokeCap.Round),
                )
            }
        }
        if (content != null) content()
    }
}

// ---------------------------------------------------------------------------
// Spinners and loaders
// ---------------------------------------------------------------------------

/**
 * A glassy blue orb with a comet of light orbiting it, in the spirit of the Vista busy cursor.
 *
 * The orb gently pulses while the comet ring rotates once per loop. Indeterminate only.
 * Accessibility: sets `contentDescription` and `ProgressBarRangeInfo.Indeterminate`, so it is announced as a busy indicator.
 *
 * ```kotlin
 * MillenniumOrbSpinner(size = 56.dp, contentDescription = "Loading tracks")
 * ```
 *
 *
 * @param modifier Modifier applied to the indicator (linear bars already fill the max width).
 * @param size Overrides the diameter; `null` uses the theme size plus 8dp.
 * @param color Overrides the accent; `null` falls back to the theme colour, then the palette accent gel.
 * @param animationMillis Overrides the loop duration in ms; `null` falls back to the theme `animationMillis`.
 * @param contentDescription Accessibility label announced by screen readers; defaults to "Loading". Localise it for your app.
 * @param theme Theme supplying defaults (size, durations, tint, opacity); defaults to [MillenniumTheme.loadingTheme].
 * @see MillenniumSpokeSpinner
 */
@Composable
fun MillenniumOrbSpinner(
    modifier: Modifier = Modifier,
    size: Dp? = null,
    color: Color? = null,
    animationMillis: Int? = null,
    contentDescription: String = "Loading",
    theme: MillenniumLoadingTheme = MillenniumTheme.loadingTheme,
) {
    val properties = theme.states.resolve(MillenniumControlState.Default)
    val gel = loadingGel(color, properties)
    val phase = loopingPhase(animationMillis ?: properties.animationMillis)
    Canvas(
        modifier.size(size ?: (properties.size + 8.dp)).loadingSemantics(null, contentDescription).alpha(properties.alpha),
    ) {
        val c = center
        val outer = this.size.minDimension / 2f
        val stroke = outer * 0.16f
        val ringRadius = outer - stroke / 2f
        rotate(phase.value * 360f, c) {
            drawArc(
                brush = Brush.sweepGradient(
                    0f to Color.Transparent,
                    0.8f to gel.bottom,
                    1f to gel.top,
                    center = c,
                ),
                startAngle = 0f,
                sweepAngle = 290f,
                useCenter = false,
                topLeft = Offset(c.x - ringRadius, c.y - ringRadius),
                size = Size(ringRadius * 2f, ringRadius * 2f),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        val pulse = 1f + 0.035f * sin(phase.value * 4f * PI.toFloat())
        val r = (outer - stroke * 1.7f) * pulse
        val orb = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(c.x - r, c.y - r, c.x + r, c.y + r))
        }
        drawCircle(
            Brush.radialGradient(
                0f to lerp(gel.top, Color.White, 0.5f),
                0.55f to gel.top,
                1f to gel.bottom,
                center = Offset(c.x - r * 0.3f, c.y - r * 0.4f),
                radius = r * 1.5f,
            ),
            radius = r,
            center = c,
        )
        drawCircle(gel.bottom, radius = r, center = c, style = Stroke(1.dp.toPx()))
        clipPath(orb) {
            drawOval(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.78f), Color.White.copy(alpha = 0.04f)),
                    startY = c.y - r,
                    endY = c.y,
                ),
                topLeft = Offset(c.x - r * 0.68f, c.y - r * 0.95f),
                size = Size(r * 1.36f, r * 0.95f),
            )
        }
    }
}

/**
 * Classic spoke spinner: a fading tail of rounded ticks stepping around the circle.
 *
 * The head advances in discrete steps (one spoke at a time), which gives the familiar ticking look.
 * Accessibility: sets `contentDescription` and `ProgressBarRangeInfo.Indeterminate`, so it is announced as a busy indicator.
 *
 * ```kotlin
 * MillenniumSpokeSpinner(size = 24.dp, spokes = 8)
 * ```
 *
 *
 * @param modifier Modifier applied to the indicator (linear bars already fill the max width).
 * @param size Overrides the diameter; `null` uses the theme size minus 8dp.
 * @param color Overrides the accent; `null` falls back to the theme colour, then the palette accent gel.
 * @param spokes Number of ticks around the circle; defaults to 12 and is not themed. Should be at least 1.
 * @param animationMillis Overrides the loop duration in ms; `null` falls back to three quarters of the theme `animationMillis`.
 * @param contentDescription Accessibility label announced by screen readers; defaults to "Loading". Localise it for your app.
 * @param theme Theme supplying defaults (size, durations, tint, opacity); defaults to [MillenniumTheme.loadingTheme].
 * @see MillenniumOrbSpinner
 */
@Composable
fun MillenniumSpokeSpinner(
    modifier: Modifier = Modifier,
    size: Dp? = null,
    color: Color? = null,
    spokes: Int = 12,
    animationMillis: Int? = null,
    contentDescription: String = "Loading",
    theme: MillenniumLoadingTheme = MillenniumTheme.loadingTheme,
) {
    val properties = theme.states.resolve(MillenniumControlState.Default)
    val gel = loadingGel(color, properties)
    val phase = loopingPhase(animationMillis ?: (properties.animationMillis * 3 / 4))
    Canvas(
        modifier.size(size ?: (properties.size - 8.dp)).loadingSemantics(null, contentDescription).alpha(properties.alpha),
    ) {
        val head = floor(phase.value * spokes).toInt()
        val r = this.size.minDimension / 2f
        val width = r * 0.2f
        repeat(spokes) { index ->
            val behind = (head - index + spokes) % spokes
            val tone = lerp(gel.top, gel.bottom, behind / spokes.toFloat())
            rotate(index * 360f / spokes, center) {
                drawLine(
                    color = tone.copy(alpha = (1f - behind / spokes.toFloat()).coerceIn(0.14f, 1f)),
                    start = Offset(center.x, center.y - r * 0.58f),
                    end = Offset(center.x, center.y - r * 0.96f),
                    strokeWidth = width,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

/**
 * Glossy gel beads that bounce in a staggered wave.
 *
 * Each bead hops once in the first 55% of the loop and rests the remainder, staggered by 12% of the loop, with a slight squash at rest.
 * Accessibility: sets `contentDescription` and `ProgressBarRangeInfo.Indeterminate`, so it is announced as a busy indicator.
 *
 * ```kotlin
 * MillenniumDotsLoader(dotSize = 10.dp, count = 4)
 * ```
 *
 *
 * @param modifier Modifier applied to the indicator (linear bars already fill the max width).
 * @param dotSize Diameter of each bead; defaults to 12dp and is not themed.
 * @param count Number of beads; defaults to 3 and is not themed.
 * @param color Overrides the accent; `null` falls back to the theme colour, then the palette accent gel.
 * @param animationMillis Overrides the loop duration in ms; `null` falls back to two thirds of the theme `animationMillis`.
 * @param contentDescription Accessibility label announced by screen readers; defaults to "Loading". Localise it for your app.
 * @param theme Theme supplying defaults (size, durations, tint, opacity); defaults to [MillenniumTheme.loadingTheme].
 * @see MillenniumPulseLoader
 */
@Composable
fun MillenniumDotsLoader(
    modifier: Modifier = Modifier,
    dotSize: Dp = 12.dp,
    count: Int = 3,
    color: Color? = null,
    animationMillis: Int? = null,
    contentDescription: String = "Loading",
    theme: MillenniumLoadingTheme = MillenniumTheme.loadingTheme,
) {
    val properties = theme.states.resolve(MillenniumControlState.Default)
    val gel = loadingGel(color, properties)
    val phase = loopingPhase(animationMillis ?: (properties.animationMillis * 2 / 3))
    val gap = dotSize * 0.6f
    Canvas(
        modifier.size(width = dotSize * count + gap * (count - 1), height = dotSize * 2.2f)
            .loadingSemantics(null, contentDescription).alpha(properties.alpha),
    ) {
        val d = dotSize.toPx()
        val r = d / 2f
        val travel = size.height - d
        repeat(count) { index ->
            val p = wrap01(phase.value - index * 0.12f)
            // One hop in the first 55% of the loop, then rest.
            val hop = if (p < 0.55f) sin(p / 0.55f * PI.toFloat()) else 0f
            val cx = r + index * (d + gap.toPx())
            val cy = size.height - r - hop * travel
            val squash = 1f - 0.12f * (1f - hop)
            drawOval(
                brush = Brush.radialGradient(
                    0f to lerp(gel.top, Color.White, 0.55f),
                    0.5f to gel.top,
                    1f to gel.bottom,
                    center = Offset(cx - r * 0.3f, cy - r * 0.4f),
                    radius = r * 1.5f,
                ),
                topLeft = Offset(cx - r, cy - r * squash + r * (1f - squash)),
                size = Size(d, d * squash),
            )
            drawCircle(
                Color.White.copy(alpha = 0.7f), radius = r * 0.22f,
                center = Offset(cx - r * 0.3f, cy - r * 0.45f),
            )
        }
    }
}

/**
 * Equalizer bars that dance out of phase.
 *
 * Each bar's height follows a sine wave, offset per bar, between 28% and 100% of the available height.
 * Accessibility: sets `contentDescription` and `ProgressBarRangeInfo.Indeterminate`, so it is announced as a busy indicator.
 *
 * ```kotlin
 * MillenniumBarsLoader(size = 32.dp, bars = 4)
 * ```
 *
 *
 * @param modifier Modifier applied to the indicator (linear bars already fill the max width).
 * @param size Overrides the square size; `null` falls back to the theme size.
 * @param bars Number of bars; defaults to 5 and is not themed.
 * @param color Overrides the accent; `null` falls back to the theme colour, then the palette accent gel.
 * @param animationMillis Overrides the loop duration in ms; `null` falls back to five sixths of the theme `animationMillis`.
 * @param contentDescription Accessibility label announced by screen readers; defaults to "Loading". Localise it for your app.
 * @param theme Theme supplying defaults (size, durations, tint, opacity); defaults to [MillenniumTheme.loadingTheme].
 * @see MillenniumDotsLoader
 */
@Composable
fun MillenniumBarsLoader(
    modifier: Modifier = Modifier,
    size: Dp? = null,
    bars: Int = 5,
    color: Color? = null,
    animationMillis: Int? = null,
    contentDescription: String = "Loading",
    theme: MillenniumLoadingTheme = MillenniumTheme.loadingTheme,
) {
    val properties = theme.states.resolve(MillenniumControlState.Default)
    val gel = loadingGel(color, properties)
    val phase = loopingPhase(animationMillis ?: (properties.animationMillis * 5 / 6))
    Canvas(
        modifier.size(size ?: properties.size).loadingSemantics(null, contentDescription).alpha(properties.alpha),
    ) {
        val slot = this.size.width / bars
        val barWidth = slot * 0.62f
        repeat(bars) { index ->
            val wave = abs(sin((phase.value + index * 0.17f) * PI.toFloat()))
            val barHeight = this.size.height * (0.28f + 0.72f * wave)
            drawRoundRect(
                brush = gel.brush,
                topLeft = Offset(index * slot + (slot - barWidth) / 2f, this.size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

/**
 * Aqua ripples spreading from a breathing gel bead.
 *
 * Concentric rings expand and fade out of a central bead that breathes in sync, evenly staggered across the loop.
 * Accessibility: sets `contentDescription` and `ProgressBarRangeInfo.Indeterminate`, so it is announced as a busy indicator.
 *
 * ```kotlin
 * MillenniumPulseLoader(size = 72.dp, rings = 3)
 * ```
 *
 *
 * @param modifier Modifier applied to the indicator (linear bars already fill the max width).
 * @param size Overrides the diameter; `null` uses the theme size plus 16dp.
 * @param color Overrides the accent; `null` falls back to the theme colour, then the palette accent gel.
 * @param rings Number of ripples; defaults to 3 and is not themed. Should be at least 1.
 * @param animationMillis Overrides the loop duration in ms; `null` falls back to one and a half times the theme `animationMillis`.
 * @param contentDescription Accessibility label announced by screen readers; defaults to "Loading". Localise it for your app.
 * @param theme Theme supplying defaults (size, durations, tint, opacity); defaults to [MillenniumTheme.loadingTheme].
 * @see MillenniumOrbSpinner
 */
@Composable
fun MillenniumPulseLoader(
    modifier: Modifier = Modifier,
    size: Dp? = null,
    color: Color? = null,
    rings: Int = 3,
    animationMillis: Int? = null,
    contentDescription: String = "Loading",
    theme: MillenniumLoadingTheme = MillenniumTheme.loadingTheme,
) {
    val properties = theme.states.resolve(MillenniumControlState.Default)
    val gel = loadingGel(color, properties)
    val phase = loopingPhase(animationMillis ?: (properties.animationMillis * 3 / 2))
    Canvas(
        modifier.size(size ?: (properties.size + 16.dp)).loadingSemantics(null, contentDescription).alpha(properties.alpha),
    ) {
        val c = center
        val maxRadius = this.size.minDimension / 2f
        val core = maxRadius * 0.24f
        repeat(rings) { index ->
            val p = wrap01(phase.value + index / rings.toFloat())
            val radius = core + (maxRadius - core) * FastOutSlowInEasing.transform(p)
            drawCircle(
                gel.bottom.copy(alpha = (1f - p) * 0.55f),
                radius = radius,
                center = c,
                style = Stroke(2.dp.toPx() * (1f - p * 0.5f)),
            )
        }
        val breath = 1f + 0.12f * (0.5f - 0.5f * cos(phase.value * 2f * PI.toFloat() * rings))
        val r = core * breath
        drawCircle(
            Brush.radialGradient(
                0f to lerp(gel.top, Color.White, 0.5f),
                0.5f to gel.top,
                1f to gel.bottom,
                center = Offset(c.x - r * 0.3f, c.y - r * 0.4f),
                radius = r * 1.6f,
            ),
            radius = r,
            center = c,
        )
        drawCircle(Color.White.copy(alpha = 0.7f), radius = r * 0.24f, center = Offset(c.x - r * 0.3f, c.y - r * 0.42f))
    }
}

// ---------------------------------------------------------------------------
// Skeleton
// ---------------------------------------------------------------------------

/**
 * A recessed placeholder with a pearly glint sweeping across it.
 *
 * Compose several (a circle and a few bars) to mimic the shape of the content that is loading. Give it a size through [modifier]; it has
 * no intrinsic size. The glint loops with `FastOutSlowInEasing`. Accessibility: it exposes no semantics (no `contentDescription` or progress range), so announce
 * loading state elsewhere, for example on the container.
 *
 * ```kotlin
 * Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
 *     MillenniumSkeleton(Modifier.size(48.dp), shape = CircleShape)
 *     Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
 *         MillenniumSkeleton(Modifier.height(14.dp).width(160.dp))
 *         MillenniumSkeleton(Modifier.height(14.dp).width(100.dp))
 *     }
 * }
 * ```
 *
 *
 * @param modifier Modifier that must provide the size of the placeholder.
 * @param shape Clip shape of the placeholder; defaults to an 8dp rounded rectangle.
 * @param animationMillis Overrides the loop duration in ms; `null` falls back to one and a half times the theme `animationMillis`.
 * @param contentDescription Optional accessibility label. `null` (default) leaves the placeholder silent, so a
 * list of skeletons does not spam screen readers; give one skeleton in the group a description such as "Loading".
 * @param theme Theme supplying defaults (size, durations, tint, opacity); defaults to [MillenniumTheme.loadingTheme].
 * @see MillenniumStripedProgress
 */
@Composable
fun MillenniumSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    animationMillis: Int? = null,
    contentDescription: String? = null,
    theme: MillenniumLoadingTheme = MillenniumTheme.loadingTheme,
) {
    val properties = theme.states.resolve(MillenniumControlState.Default)
    val colors = MillenniumTheme.colors
    val phase = loopingPhase(animationMillis ?: (properties.animationMillis * 3 / 2), FastOutSlowInEasing)
    Box(
        modifier
            .then(if (contentDescription != null) Modifier.loadingSemantics(null, contentDescription) else Modifier)
            .clip(shape)
            .background(millenniumBevelBrush(colors, sunken = true))
            .drawWithContent {
                drawContent()
                val band = max(size.width * 0.5f, 64.dp.toPx())
                val start = -band + (size.width + band) * phase.value
                drawRect(
                    Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.5f to colors.highlight.copy(alpha = 0.75f),
                        1f to Color.Transparent,
                        startX = start,
                        endX = start + band,
                    ),
                )
            }.alpha(properties.alpha),
    )
}

/** Preview of every progress bar and loader. */
@Preview
@Composable
private fun MillenniumLoadingPreview() = MillenniumPreview {
    MillenniumStripedProgress(progress = 0.6f)
    MillenniumSegmentedProgress(progress = 0.5f)
    MillenniumMarqueeProgress()
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        MillenniumCircularProgress(progress = 0.7f)
        MillenniumOrbSpinner()
        MillenniumSpokeSpinner()
        MillenniumDotsLoader()
        MillenniumBarsLoader()
        MillenniumPulseLoader()
    }
}
