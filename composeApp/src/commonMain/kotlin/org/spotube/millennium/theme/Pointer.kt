package org.spotube.millennium.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon

/**
 * Shows the hand cursor over the complete action hit area, including Unstyled's inner arrow cursor
 * and content. Uses `overrideDescendants = true`, because Compose Unstyled 2.10.0's inner
 * `PointerIcon.Default` would otherwise override the hand.
 *
 * ```kotlin
 * Box(Modifier.millenniumActionPointer(enabled = !disabled).clickable { })
 * ```
 *
 * @receiver the modifier chain of the action.
 * @param enabled `true` (default) shows [PointerIcon.Hand]; `false` forces [PointerIcon.Default].
 */
fun Modifier.millenniumActionPointer(enabled: Boolean = true): Modifier =
    pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default, overrideDescendants = true)
