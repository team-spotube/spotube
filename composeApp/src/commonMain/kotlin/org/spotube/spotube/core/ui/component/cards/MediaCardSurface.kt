/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.core.ui.component.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.lerp
import androidx.compose.foundation.layout.width
import org.spotube.spotube.core.ui.theming.engine.AppComponents
import org.spotube.spotube.core.ui.theming.engine.AppSlots
import org.spotube.spotube.core.ui.theming.engine.AppState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import org.spotube.millennium.theme.millenniumActionPointer
import org.spotube.millennium.theme.millenniumSurface

/**
 * Interaction state is presentation-only; all navigation/playback stays in event callbacks.
 *
 * The look comes from the precompiled `mediaCard.root` slot (see `AppSlots.MediaCardRoot`): the resting props are
 * [AppState.Default] (or [AppState.Error] when blacklisted) and the lit props are Hovered/Focused/Pressed. A single
 * animated fraction interpolates between the two, so no theme work happens per frame.
 */
@Composable
internal fun MediaCardSurface(
    modifier: Modifier,
    onClick: (() -> Unit)?,
    interactive: Boolean = onClick != null,
    isBlacklisted: Boolean = false,
    content: @Composable (highlighted: Boolean) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    var groupFocused by remember { mutableStateOf(false) }
    val slot = AppComponents.slot(AppSlots.MediaCardRoot)
    val rest = slot[if (isBlacklisted) AppState.Error else AppState.Default]
    val active = when {
        pressed -> AppState.Pressed
        focused || groupFocused -> AppState.Focused
        hovered -> AppState.Hovered
        else -> null
    }
    // Keep the last lit look while fading out, otherwise the fade would snap to the resting values.
    val lit = remember { LastLit() }
    if (active != null) lit.state = active
    val hot = slot[lit.state]
    val highlight by animateFloatAsState(if (active != null) 1f else 0f, tween(hot.transitionMillis))
    val scale by animateFloatAsState(if (active == AppState.Pressed) hot.scale else rest.scale, tween(rest.scaleMillis))
    val shape = rest.shape
    Box(
        modifier.width(rest.width)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .millenniumSurface(
                background = rest.background ?: Transparent,
                shape = shape,
                borderColor = lerp(rest.borderColor, hot.borderColor, highlight),
                borderWidth = lerp(rest.borderWidth, hot.borderWidth, highlight),
                innerGlowColor = lerp(rest.innerGlowColor, hot.innerGlowColor, highlight),
                shadowElevation = lerp(rest.shadowElevation, hot.shadowElevation, highlight)
            )
            .then(
                // The lit plate fades in over the resting one; skipped entirely while fully rested.
                if (hot.highlightBackground != null && highlight > 0f) {
                    val brush = hot.highlightBackground
                    Modifier.drawBehind { drawRect(brush, alpha = highlight) }
                } else Modifier
            )
            .clip(shape)
            .onFocusChanged { groupFocused = it.hasFocus }
            .focusGroup()
            .hoverable(interaction, enabled = interactive)
            .then(
                if (onClick != null) Modifier.millenniumActionPointer().clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClick = onClick
                ) else Modifier
            )
    ) { content(hovered || focused || groupFocused) }
}

private class LastLit { var state: AppState = AppState.Hovered }

private val Transparent = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Transparent)
