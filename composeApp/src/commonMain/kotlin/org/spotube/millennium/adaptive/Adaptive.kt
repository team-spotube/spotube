package org.spotube.millennium.adaptive

import androidx.compose.ui.unit.dp
import com.composeunstyled.HeightBreakpoint
import com.composeunstyled.WidthBreakpoint
import com.composeunstyled.WindowHeightBreakpoints
import com.composeunstyled.WindowWidthBreakpoints

/**
 * Named window-size buckets installed by [org.spotube.millennium.theme.MillenniumTheme] through
 * Compose Unstyled's `ProvideWindowWidthBreakpoints` and `ProvideWindowHeightBreakpoints`.
 *
 * Width buckets start at: [Compact] 0dp, [Medium] 600dp, [Expanded] 840dp. Height buckets start at:
 * [Short] 0dp, [Tall] 720dp.
 *
 * ```kotlin
 * MillenniumTheme {
 *     val width = currentWindowWidth() // Compose Unstyled API; compare with WindowBreakpoints.Medium
 * }
 * ```
 */
object WindowBreakpoints {
    /** Width bucket from 0dp (phones in portrait). */
    val Compact = WidthBreakpoint("compact")
    /** Width bucket from 600dp. */
    val Medium = WidthBreakpoint("medium")
    /** Width bucket from 840dp. */
    val Expanded = WidthBreakpoint("expanded")
    /** Height bucket from 0dp. */
    val Short = HeightBreakpoint("short")
    /** Height bucket from 720dp. */
    val Tall = HeightBreakpoint("tall")
}

/** Width breakpoint table provided by the theme: compact 0dp, medium 600dp, expanded 840dp. */
internal val widthBreakpoints = WindowWidthBreakpoints {
    WindowBreakpoints.Compact startsAt 0.dp
    WindowBreakpoints.Medium startsAt 600.dp
    WindowBreakpoints.Expanded startsAt 840.dp
}


/** Height breakpoint table provided by the theme: short 0dp, tall 720dp. */
internal val heightBreakpoints = WindowHeightBreakpoints {
    WindowBreakpoints.Short startsAt 0.dp
    WindowBreakpoints.Tall startsAt 720.dp
}
