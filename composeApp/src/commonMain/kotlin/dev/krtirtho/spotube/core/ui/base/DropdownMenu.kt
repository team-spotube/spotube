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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxAddSquare
import dev.krtirtho.spotube.resources.iconsax.IconsaxHeart
import dev.krtirtho.spotube.resources.iconsax.IconsaxPlay
import dev.krtirtho.spotube.resources.iconsax.IconsaxShare
import dev.krtirtho.spotube.resources.iconsax.IconsaxTrash

@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 4.dp),
    theme: BaseUITheme.DropdownMenuTheme? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val menuTheme = theme ?: LocalBaseUITheme.current.dropdownMenu

    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        modifier = modifier.dropdownMenuContainer(menuTheme),
    ) {
        content()
    }
}

/**
 * The container styling of [DropdownMenu], split out so it can be reused by the
 * material3 popup and by previews, which cannot render the popup window itself.
 */
internal fun Modifier.dropdownMenuContainer(theme: BaseUITheme.DropdownMenuTheme): Modifier =
    this
        .widthIn(min = 180.dp)
        .shadow(
            elevation = theme.shadow.elevation,
            shape = theme.shape,
            ambientColor = theme.shadow.ambientColor,
            spotColor = theme.shadow.spotColor,
        )
        .clip(theme.shape)
        .background(theme.background, theme.shape)
        .border(
            BorderStroke(theme.border.width, theme.border.color),
            theme.shape,
        )

@Composable
fun DropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    selected: Boolean = false,
    theme: BaseUITheme.DropdownMenuTheme? = null,
) {
    val menuTheme = theme ?: LocalBaseUITheme.current.dropdownMenu
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(menuTheme.shape)
            .hoverable(interactionSource)
            .then(
                if (isHovered && enabled) {
                    Modifier.background(menuTheme.itemHoverBackground)
                } else {
                    Modifier
                }
            )
            .then(
                if (isHovered && enabled) {
                    Modifier.highlight(menuTheme.itemHighlight)
                } else {
                    Modifier
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(menuTheme.itemPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (enabled) {
                    menuTheme.itemForeground
                } else {
                    menuTheme.itemForeground.copy(alpha = 0.38f)
                },
            )
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) {
                menuTheme.itemForeground
            } else {
                menuTheme.itemForeground.copy(alpha = 0.38f)
            },
            modifier = Modifier.weight(1f),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
fun DropdownMenuDivider(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        )
    }
}

@BaseUIPhonePreview
@Composable
private fun DropdownMenuPreview() {
    BaseUIPreview {
        var expanded by remember { mutableStateOf(true) }
        var lastAction by remember { mutableStateOf("None") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlineButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Hide menu" else "Show menu")
            }

            if (expanded) {
                Column(
                    modifier = Modifier.dropdownMenuContainer(
                        LocalBaseUITheme.current.dropdownMenu,
                    ),
                ) {
                    DropdownMenuItem(
                        text = "Play next",
                        leadingIcon = Iconsax.IconsaxPlay,
                        onClick = {
                            lastAction = "Play next"
                            expanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = "Add to queue",
                        leadingIcon = Iconsax.IconsaxAddSquare,
                        onClick = {
                            lastAction = "Add to queue"
                            expanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = "Add to favorites",
                        leadingIcon = Iconsax.IconsaxHeart,
                        selected = lastAction == "Add to favorites",
                        onClick = {
                            lastAction = "Add to favorites"
                            expanded = false
                        },
                    )
                    DropdownMenuDivider()
                    DropdownMenuItem(
                        text = "Share",
                        leadingIcon = Iconsax.IconsaxShare,
                        onClick = {
                            lastAction = "Share"
                            expanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = "Remove from playlist",
                        leadingIcon = Iconsax.IconsaxTrash,
                        enabled = false,
                        onClick = {},
                    )
                }
            }

            Text(
                text = "Last action: $lastAction",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
