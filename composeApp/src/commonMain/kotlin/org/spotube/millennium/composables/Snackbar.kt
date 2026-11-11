package org.spotube.millennium.composables

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumPanelBrush
import org.spotube.millennium.theme.millenniumSurface

/**
 * How long a snackbar stays on screen. [MillenniumSnackbarHost] auto-dismisses a snackbar after [millis]
 * unless it is [Indefinite].
 *
 * @property millis Display time in milliseconds; 0 for [Indefinite] (never auto-dismissed).
 */
enum class MillenniumSnackbarDuration(val millis: Long) {
    /** Auto-dismisses after 4 seconds. */
    Short(4_000),

    /** Auto-dismisses after 10 seconds. */
    Long(10_000),

    /** Stays until dismissed by the caller or its action is performed. */
    Indefinite(0),
}

/**
 * The outcome of a shown snackbar, returned by [MillenniumSnackbarHostState.showSnackbar].
 */
enum class MillenniumSnackbarResult {
    /** The snackbar timed out or was dismissed programmatically (for example via `dismiss()`). */
    Dismissed,

    /** The user pressed the snackbar's action button. */
    ActionPerformed,
}

/**
 * Complete appearance configuration for [MillenniumSnackbarHost].
 *
 * Normally produced by [defaultMillenniumSnackbarTheme] and customised with `copy`.
 *
 * @property background Brush of the pill; `null` derives the palette panel brush (pearl/smoke glass).
 * @property foreground Message text colour; `null` uses the palette text colour.
 * @property actionForeground Action label colour; `null` uses the palette accent.
 * @property shape Shape of the pill.
 * @property margin Outer space around the pill (inside the host box).
 * @property contentPadding Space between the pill edge and its content.
 * @property borderColor Hairline colour; `null` uses the palette border colour.
 * @property borderWidth Hairline thickness in dp.
 * @property shadowElevation Shadow elevation in dp.
 * @property innerGlowColor Inner highlight colour; `null` uses the palette highlight at 40% alpha.
 * @property maxWidth Maximum pill width in dp (the pill is centered in the host).
 * @property actionSpacing Gap in dp between the message and the action button.
 * @property animationMillis Fade-in duration in ms; the exit animation uses 75% of it (minimum 120 ms).
 */
@Immutable
data class MillenniumSnackbarThemeProperties(
    val background: Brush? = null,
    val foreground: Color? = null,
    val actionForeground: Color? = null,
    val shape: Shape,
    val margin: PaddingValues,
    val contentPadding: PaddingValues,
    val borderColor: Color? = null,
    val borderWidth: Dp,
    val shadowElevation: Dp,
    val innerGlowColor: Color? = null,
    val maxWidth: Dp,
    val actionSpacing: Dp,
    val animationMillis: Int,
)

/**
 * Theme wrapper for [MillenniumSnackbarHost], installed as [MillenniumTheme.snackbarTheme].
 *
 * @property properties Snackbar appearance and animation.
 * @see defaultMillenniumSnackbarTheme
 */
@Immutable
data class MillenniumSnackbarTheme(val properties: MillenniumSnackbarThemeProperties)

/**
 * Creates the default snackbar theme: a 12dp rounded pearl/smoke glass pill, at most 600dp wide, with a 1dp
 * hairline, 10dp shadow, 16x12dp content padding, 16x10dp margin, 12dp action spacing and a 200ms
 * animation.
 *
 * Customise with `copy`:
 * ```kotlin
 * val base = defaultMillenniumSnackbarTheme()
 * val theme = base.copy(properties = base.properties.copy(maxWidth = 480.dp, animationMillis = 260))
 * ```
 *
 * @return A new [MillenniumSnackbarTheme].
 */
fun defaultMillenniumSnackbarTheme(): MillenniumSnackbarTheme = MillenniumSnackbarTheme(
    MillenniumSnackbarThemeProperties(
        shape = RoundedCornerShape(12.dp),
        margin = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        borderWidth = 1.dp,
        shadowElevation = 10.dp,
        maxWidth = 600.dp,
        actionSpacing = 12.dp,
        animationMillis = 200,
    ),
)

/**
 * A single snackbar currently shown by [MillenniumSnackbarHost].
 *
 * Created only by [MillenniumSnackbarHostState.showSnackbar]; read via
 * [MillenniumSnackbarHostState.currentSnackbarData] (for example to build a custom host).
 *
 * @property message Text shown in the pill.
 * @property actionLabel Label of the optional action button, or `null` for no action.
 * @property duration How long the snackbar stays visible.
 */
@Stable
class MillenniumSnackbarData internal constructor(
    val message: String,
    val actionLabel: String?,
    val duration: MillenniumSnackbarDuration,
    internal val continuation: CancellableContinuation<MillenniumSnackbarResult>,
) {
    /** Resolves the pending `showSnackbar` call with [MillenniumSnackbarResult.Dismissed]; no-op if already resolved. */
    fun dismiss() {
        if (continuation.isActive) continuation.resumeWith(Result.success(MillenniumSnackbarResult.Dismissed))
    }

    /** Resolves the pending `showSnackbar` call with [MillenniumSnackbarResult.ActionPerformed]; no-op if already resolved. */
    fun performAction() {
        if (continuation.isActive) continuation.resumeWith(Result.success(MillenniumSnackbarResult.ActionPerformed))
    }
}

/**
 * State holder for [MillenniumSnackbarHost]. Snackbars are shown one at a time; the
 * suspend call resolves when the snackbar is dismissed, times out, or its action runs.
 *
 * Create it in the caller (for example `remember { MillenniumSnackbarHostState() }`), pass the same instance
 * to the host and call [showSnackbar] from a coroutine. Concurrent calls queue behind a mutex and are
 * shown in order. There is no `rememberMillenniumSnackbarHostState` helper in this file.
 *
 * ```kotlin
 * val snackbars = remember { MillenniumSnackbarHostState() }
 * val scope = rememberCoroutineScope()
 * Box(Modifier.fillMaxSize()) {
 *     MillenniumButton(onClick = {
 *         scope.launch {
 *             val r = snackbars.showSnackbar("Track removed", actionLabel = "Undo")
 *             if (r == MillenniumSnackbarResult.ActionPerformed) undoRemove()
 *         }
 *     }) { MillenniumText("Remove") }
 *     MillenniumSnackbarHost(snackbars, Modifier.align(Alignment.BottomCenter))
 * }
 * ```
 */
@Stable
class MillenniumSnackbarHostState {
    private val mutex = Mutex()
    private var snackbarData by mutableStateOf<MillenniumSnackbarData?>(null)

    /** The snackbar currently being shown, or `null` when none is; observable Compose state. */
    val currentSnackbarData: MillenniumSnackbarData? get() = snackbarData

    /**
     * Shows a snackbar and suspends until it ends.
     *
     * If another snackbar is showing this call waits for it first. Cancelling the calling coroutine removes
     * the snackbar immediately.
     *
     * @param message Text to display.
     * @param actionLabel Optional action button label; `null` shows no button.
     * @param duration Display time; [MillenniumSnackbarDuration.Indefinite] waits until [dismiss] or the action.
     * @return [MillenniumSnackbarResult.ActionPerformed] if the action was pressed, otherwise
     * [MillenniumSnackbarResult.Dismissed].
     */
    suspend fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        duration: MillenniumSnackbarDuration = MillenniumSnackbarDuration.Short,
    ): MillenniumSnackbarResult = mutex.withLock {
        try {
            return suspendCancellableCoroutine { continuation ->
                snackbarData = MillenniumSnackbarData(message, actionLabel, duration, continuation)
                continuation.invokeOnCancellation { snackbarData = null }
            }
        } finally {
            snackbarData = null
        }
    }

    /** Dismisses the current snackbar, if any. */
    fun dismiss() {
        snackbarData?.dismiss()
    }
}

/**
 * Renders the active snackbar from [hostState] as a pearl/smoke glass pill with a
 * hairline, a shared shadow and a quiet accent action. Place it in the scaffold's
 * snackbar slot or align it inside a Box; it renders nothing when there is no snackbar
 * to show. Customize through [MillenniumTheme.snackbarTheme].
 *
 * Behaviour: the pill enters with a fade, a spring slide from below and a spring scale (0.92 to 1, origin at
 * the bottom centre), and exits with a shorter fade, slide and scale. The auto-dismiss timer runs in a
 * `LaunchedEffect` keyed by the current snackbar. The last shown snackbar is kept while the exit animation
 * plays. Messages and the action label are plain text and the action is a quiet [MillenniumButton], so
 * keyboard activation and a button role come from that button. The snackbar is a polite live region, so
 * screen readers announce the message when it appears.
 *
 * ```kotlin
 * val hostState = remember { MillenniumSnackbarHostState() }
 * Box(Modifier.fillMaxSize()) {
 *     MillenniumSnackbarHost(
 *         hostState = hostState,
 *         modifier = Modifier.align(Alignment.BottomCenter),
 *         maxWidth = 480.dp,
 *     )
 * }
 * ```
 *
 * @param hostState The caller-owned state whose current snackbar is rendered.
 * @param modifier Modifier applied to the full-width host box that centers the pill.
 * @param background Overrides the pill brush; `null` falls back to the theme, then the panel brush.
 * @param foreground Overrides the message colour; `null` falls back to the theme, then the palette text colour.
 * @param actionForeground Overrides the action label colour; `null` falls back to the theme, then the accent.
 * @param shape Overrides the pill shape; `null` falls back to the theme.
 * @param margin Overrides the outer margin; `null` falls back to the theme.
 * @param contentPadding Overrides the inner padding; `null` falls back to the theme.
 * @param borderColor Overrides the hairline colour; `null` falls back to the theme, then the palette border.
 * @param borderWidth Overrides the hairline thickness; `null` falls back to the theme.
 * @param shadowElevation Overrides the shadow elevation; `null` falls back to the theme.
 * @param innerGlowColor Overrides the inner glow; `null` falls back to the theme, then the palette highlight.
 * @param maxWidth Overrides the maximum pill width; `null` falls back to the theme.
 * @param actionSpacing Overrides the gap before the action; `null` falls back to the theme.
 * @param animationMillis Overrides the fade-in duration in ms; `null` falls back to the theme.
 * @param theme Theme supplying defaults; defaults to [MillenniumTheme.snackbarTheme].
 * @see MillenniumSnackbarHostState
 * @see MillenniumSnackbarTheme
 */
@Composable
fun MillenniumSnackbarHost(
    hostState: MillenniumSnackbarHostState,
    modifier: Modifier = Modifier,
    background: Brush? = null,
    foreground: Color? = null,
    actionForeground: Color? = null,
    shape: Shape? = null,
    margin: PaddingValues? = null,
    contentPadding: PaddingValues? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    shadowElevation: Dp? = null,
    innerGlowColor: Color? = null,
    maxWidth: Dp? = null,
    actionSpacing: Dp? = null,
    animationMillis: Int? = null,
    theme: MillenniumSnackbarTheme = MillenniumTheme.snackbarTheme,
) {
    val data = hostState.currentSnackbarData
    var lastData by remember { mutableStateOf<MillenniumSnackbarData?>(null) }
    if (data != null) lastData = data
    val visible = data != null
    val properties = theme.properties
    val resolvedMillis = animationMillis ?: properties.animationMillis
    val closeMillis = (resolvedMillis * 0.75f).toInt().coerceAtLeast(120)
    if (data != null) {
        LaunchedEffect(data) {
            if (data.duration != MillenniumSnackbarDuration.Indefinite) {
                delay(data.duration.millis)
                data.dismiss()
            }
        }
    }
    val colors = MillenniumTheme.colors
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(resolvedMillis, easing = FastOutSlowInEasing)) +
                slideInVertically(
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    initialOffsetY = { it / 2 },
                ) +
                scaleIn(
                    initialScale = 0.92f,
                    transformOrigin = TransformOrigin(0.5f, 1f),
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ),
            exit = fadeOut(tween(closeMillis, easing = FastOutLinearInEasing)) +
                slideOutVertically(
                    animationSpec = tween(closeMillis, easing = FastOutLinearInEasing),
                    targetOffsetY = { it / 2 },
                ) +
                scaleOut(
                    targetScale = 0.95f,
                    transformOrigin = TransformOrigin(0.5f, 1f),
                    animationSpec = tween(closeMillis, easing = FastOutLinearInEasing),
                ),
        ) {
            val showing = lastData ?: return@AnimatedVisibility
            Row(
                modifier = Modifier
                    .widthIn(max = maxWidth ?: properties.maxWidth)
                    .padding(margin ?: properties.margin)
                    // Screen readers announce the message when it appears.
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .millenniumSurface(
                        background = background ?: properties.background ?: millenniumPanelBrush(colors),
                        shape = shape ?: properties.shape,
                        borderColor = borderColor ?: properties.borderColor ?: colors.border,
                        borderWidth = borderWidth ?: properties.borderWidth,
                        shadowElevation = shadowElevation ?: properties.shadowElevation,
                        innerGlowColor = innerGlowColor ?: properties.innerGlowColor ?: colors.highlight.copy(alpha = 0.4f),
                    )
                    .padding(contentPadding ?: properties.contentPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(actionSpacing ?: properties.actionSpacing),
            ) {
                Box(Modifier.weight(1f)) {
                    MillenniumText(showing.message, color = foreground ?: properties.foreground ?: colors.text)
                }
                if (showing.actionLabel != null) {
                    MillenniumButton(
                        onClick = { showing.performAction() },
                        style = MillenniumButtonStyle.Quiet,
                    ) {
                        MillenniumText(
                            showing.actionLabel,
                            color = actionForeground ?: properties.actionForeground ?: colors.accent,
                        )
                    }
                }
            }
        }
    }
}
