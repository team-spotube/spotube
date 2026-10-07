/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.krtirtho.spotube.core.ui.base

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxDocumentDownload
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusicLibrary

private val ChipTabMinHeight = 36.dp

private data class ResolvedChipState(
    val colors: BaseUITheme.ButtonColors,
    val shape: Shape,
    val shadow: BaseUITheme.Shadow,
    val border: BaseUITheme.Border,
    val padding: androidx.compose.foundation.layout.PaddingValues,
)

@Composable
private fun resolveChipState(
    style: BaseUITheme.ButtonStyle,
    isPressed: Boolean,
    isHovered: Boolean,
): ResolvedChipState {
    return when {
        isPressed -> ResolvedChipState(style.colors.pressed, style.shape.pressed, style.shadow.pressed, style.border.pressed, style.padding.pressed)
        isHovered -> ResolvedChipState(style.colors.hovered, style.shape.hovered, style.shadow.hovered, style.border.hovered, style.padding.hovered)
        else -> ResolvedChipState(style.colors.normal, style.shape.normal, style.shadow.normal, style.border.normal, style.padding.normal)
    }
}

private fun Modifier.applyChipShadow(
    shadow: BaseUITheme.Shadow,
    shape: Shape,
): Modifier {
    return if (shadow.elevation > 0.dp) {
        this.shadow(
            elevation = shadow.elevation,
            shape = shape,
            ambientColor = shadow.ambientColor,
            spotColor = shadow.spotColor,
        )
    } else {
        this
    }
}

@Composable
fun ChipTab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    theme: BaseUITheme.ChipTabTheme? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val baseTheme = LocalBaseUITheme.current.chipTab
    val chipTheme = theme ?: baseTheme
    val style = if (selected) chipTheme.selected else chipTheme.unselected
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val state = resolveChipState(style, isPressed, isHovered)

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = ChipTabMinHeight)
            .hoverable(interactionSource = interactionSource, enabled = enabled)
            .applyChipShadow(state.shadow, state.shape)
            .clip(state.shape)
            .background(state.colors.background, state.shape)
            .border(BorderStroke(state.border.width, state.border.color), state.shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            )
            .highlight(state.colors.highlight)
            .padding(state.padding),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides state.colors.foreground,
            LocalTextStyle provides LocalTextStyle.current.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                content = content,
            )
        }
    }
}

@Composable
fun ChipTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    theme: BaseUITheme.ChipTabTheme? = null,
) {
    ChipTab(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        theme = theme,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
        }
        Text(
            text = text,
            maxLines = 1,
            softWrap = false,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@BaseUIPhonePreview
@Composable
private fun ChipTabsPreview() {
    BaseUIPreview {
        val tabs = listOf("All", "Songs", "Albums", "Playlists")
        var selected by remember { mutableStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tabs.forEachIndexed { index, tab ->
                    ChipTab(
                        text = tab,
                        selected = selected == index,
                        onClick = { selected = index },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipTab(
                    text = "Library",
                    selected = true,
                    onClick = {},
                    leadingIcon = {
                        Icon(
                            imageVector = Iconsax.IconsaxMusicLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
                ChipTab(
                    text = "Downloads",
                    selected = false,
                    onClick = {},
                    leadingIcon = {
                        Icon(
                            imageVector = Iconsax.IconsaxDocumentDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
                ChipTab(
                    text = "Disabled",
                    selected = false,
                    onClick = {},
                    enabled = false,
                )
            }
        }
    }
}
