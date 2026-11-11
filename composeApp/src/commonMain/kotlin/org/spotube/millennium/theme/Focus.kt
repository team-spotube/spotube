package org.spotube.millennium.theme

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager

/**
 * Whether a focus ring should be shown: focus is real and the last input was the keyboard.
 *
 * Rule: focus rings are reserved for keyboard/accessibility focus. A pointer press focuses a target
 * but sets [InputMode.Touch], so it never produces a ring.
 *
 * ```kotlin
 * val ring = millenniumIsKeyboardFocusRing(focused = true, inputMode = InputMode.Touch) // false
 * ```
 *
 * @param focused Whether the control currently has focus.
 * @param inputMode The current input mode (from `LocalInputModeManager`).
 * @return `true` only when [focused] and [inputMode] is [InputMode.Keyboard].
 */
fun millenniumIsKeyboardFocusRing(focused: Boolean, inputMode: InputMode): Boolean =
    focused && inputMode == InputMode.Keyboard

/**
 * Collects focus that is eligible for a focus ring. Compose focuses a control on a
 * pointer press (mouse click, touch); that focus is intentionally reported as
 * `false` here so focus rings stay reserved for keyboard and accessibility
 * navigation. Focus gained from Tab / directional keys reports `true`.
 *
 * Precisely: the result is `focused && inputMode == InputMode.Keyboard`. A pointer press sets
 * [InputMode.Touch], so it does not produce a ring.
 *
 * ```kotlin
 * val interaction = remember { MutableInteractionSource() }
 * val showRing by interaction.collectIsKeyboardFocusedAsState()
 * Box(Modifier.focusable(interactionSource = interaction).border(if (showRing) 2.dp else 0.dp, colors.accent))
 * ```
 *
 * @receiver the interaction source of the focusable control.
 * @return a [State] that is `true` while the control has keyboard-driven focus.
 * @see millenniumIsKeyboardFocusRing
 */
@Composable
fun MutableInteractionSource.collectIsKeyboardFocusedAsState(): State<Boolean> {
    val focused = collectIsFocusedAsState()
    val inputModeManager = LocalInputModeManager.current
    return remember(this, inputModeManager) {
        derivedStateOf { focused.value && inputModeManager.inputMode == InputMode.Keyboard }
    }
}
