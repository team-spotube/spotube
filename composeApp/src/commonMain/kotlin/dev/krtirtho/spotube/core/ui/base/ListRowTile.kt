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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusic

@Composable
fun ListRowTile(
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    subtitle: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val rowTheme = LocalBaseUITheme.current.listRowTile
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val background = when {
        isPressed -> rowTheme.background.pressed
        isHovered -> rowTheme.background.hovered
        isFocused -> rowTheme.background.focused
        selected -> rowTheme.background.selected
        else -> rowTheme.background.normal
    }
    val shape = when {
        isPressed -> rowTheme.shape.pressed
        isHovered -> rowTheme.shape.hovered
        isFocused -> rowTheme.shape.focused
        selected -> rowTheme.shape.selected
        else -> rowTheme.shape.normal
    }
    val borderDef = when {
        isPressed -> rowTheme.border.pressed
        isHovered -> rowTheme.border.hovered
        isFocused -> rowTheme.border.focused
        selected -> rowTheme.border.selected
        else -> rowTheme.border.normal
    }
    val shadowDef = when {
        isPressed -> rowTheme.shadow.pressed
        isHovered -> rowTheme.shadow.hovered
        isFocused -> rowTheme.shadow.focused
        selected -> rowTheme.shadow.selected
        else -> rowTheme.shadow.normal
    }

    val mod = modifier
        .fillMaxWidth()
        .shadow(
            shadowDef.elevation,
            shape,
            shadowDef.clip,
            shadowDef.ambientColor,
            shadowDef.spotColor
        )
        .clip(shape)
        .border(borderDef.width, borderDef.color, shape)
        .background(background, shape)
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
            } else Modifier
        )
        .padding(rowTheme.padding)

    Row(
        modifier = mod,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            if (title != null) {
                title()
            }
            if (subtitle != null) {
                Spacer(Modifier.width(2.dp))
                subtitle()
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

private data class ListRowPreviewTrack(
    val title: String,
    val subtitle: String,
    val duration: String,
)

private val ListRowPreviewTracks = listOf(
    ListRowPreviewTrack("Midnight City", "M83 · Hurry Up, We're Dreaming", "4:03"),
    ListRowPreviewTrack("Odoriko", "Vaundy · strobo", "3:37"),
    ListRowPreviewTrack("Instant Crush", "Daft Punk · Random Access Memories", "5:37"),
)

@BaseUIPhonePreview
@Composable
private fun ListRowTilePreview() {
    BaseUIPreview {
        var selectedIndex by remember { mutableStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ListRowPreviewTracks.forEachIndexed { index, track ->
                ListRowTile(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    leading = { ListRowTilePreviewArtwork() },
                    title = {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    subtitle = {
                        Text(
                            text = track.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailing = {
                        Text(
                            text = track.duration,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }

            ListRowTile(
                enabled = false,
                title = {
                    Text(
                        text = "Add to queue",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    )
                },
                subtitle = {
                    Text(
                        text = "Unavailable offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    )
                },
            )
        }
    }
}

@Composable
private fun ListRowTilePreviewArtwork() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Iconsax.IconsaxMusic,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}