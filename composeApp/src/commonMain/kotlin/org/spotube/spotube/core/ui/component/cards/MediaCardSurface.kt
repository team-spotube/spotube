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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumActionPointer
import org.spotube.millennium.theme.millenniumSurface

/** Interaction state is presentation-only; all navigation/playback stays in event callbacks. */
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
    val colors = MillenniumTheme.colors
    val highlight by animateFloatAsState(
        if (hovered || focused || groupFocused || pressed) 1f else 0f,
        tween(140)
    )
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, tween(100))
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .millenniumSurface(
                background = Brush.verticalGradient(
                    listOf(
                        colors.raised.copy(alpha = highlight),
                        if (isBlacklisted) colors.danger.copy(alpha = 0.12f + highlight * 0.12f)
                        else colors.surface.copy(alpha = highlight)
                    )
                ),
                shape = shape,
                borderColor = (if (focused) colors.accent else colors.border).copy(alpha = highlight),
                borderWidth = if (focused) 2.dp else 1.dp,
                innerGlowColor = colors.highlight.copy(alpha = highlight * 0.4f),
                shadowElevation = (highlight * 4f).dp
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
