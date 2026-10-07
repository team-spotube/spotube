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

package dev.krtirtho.spotube.core.ui.component

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import dev.krtirtho.spotube.core.ui.base.TextField
import dev.krtirtho.spotube.core.ui.base.DropdownMenu
import dev.krtirtho.spotube.core.ui.base.DropdownMenuItem
import dev.krtirtho.spotube.core.ui.base.DropdownMenuDivider
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.Iconsax3DotsMore
import dev.krtirtho.spotube.resources.iconsax.IconsaxFilterSearch
import dev.krtirtho.spotube.resources.iconsax.IconsaxHeart
import dev.krtirtho.spotube.resources.iconsax.IconsaxShare
import dev.krtirtho.spotube.resources.iconsax.IconsaxTrash

data class AdaptiveMenuItem(
    val icon: ImageVector? = null,
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val dividerBefore: Boolean = false,
    val subtitle: String? = null,
    val leadingContent: (@Composable () -> Unit)? = null,
)

enum class HeaderDisplayMode {
    Always,
    OnlyInDropdown,
    OnlyInBottomSheet,
}

@Composable
fun AdaptiveDropdownBottomSheet(
    items: List<AdaptiveMenuItem>,
    trigger: @Composable (onClick: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    breakpointDp: Float = 600f,
    menuMinWidth: Dp = 200.dp,
    header: @Composable (() -> Unit)? = null,
    headerDisplayMode: HeaderDisplayMode = HeaderDisplayMode.Always,
    filter: ((AdaptiveMenuItem, String) -> Boolean)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isLargeScreen = adaptiveInfo.windowSizeClass.minWidthDp >= breakpointDp

    Box(modifier = modifier) {
        trigger { expanded = true }

        if (isLargeScreen) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                if (header != null && (headerDisplayMode == HeaderDisplayMode.Always || headerDisplayMode == HeaderDisplayMode.OnlyInDropdown)) {
                    header()
                }

                var query by remember { mutableStateOf("") }
                if (filter != null) {
                    TextField(
                        value = query,
                        onValueChange = {
                            query = it
                        },
                        placeholder = { Text("Search...") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Iconsax.IconsaxFilterSearch,
                                contentDescription = "Search",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                items.forEach { item ->
                    if (query.isNotBlank() && filter != null && !filter(item, query)) {
                        return@forEach
                    }
                    if (item.dividerBefore) {
                        DropdownMenuDivider()
                    }
                    DropdownMenuItem(
                        text = item.label,
                        onClick = {
                            item.onClick()
                            expanded = false
                        },
                        enabled = item.enabled,
                        selected = item.selected,
                        leadingIcon = item.icon,
                        leadingContent = item.leadingContent,
                        subtitle = item.subtitle,
                    )
                }
            }
        } else {
            if (expanded) {
                AdaptiveBottomSheetContent(
                    onDismiss = { expanded = false },
                    items = items,
                    header = if (header != null && (headerDisplayMode == HeaderDisplayMode.Always || headerDisplayMode == HeaderDisplayMode.OnlyInBottomSheet)) {
                        header
                    } else {
                        null
                    },
                    filter = filter,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdaptiveBottomSheetContent(
    onDismiss: () -> Unit,
    items: List<AdaptiveMenuItem>,
    header: @Composable (() -> Unit)?,
    filter: ((AdaptiveMenuItem, String) -> Boolean)? = null,
) {
    val hasSelection = items.any { it.selected }
    var query by remember { mutableStateOf("") }
    val filteredItems = if (filter != null && query.isNotBlank()) {
        items.filter { filter(it, query) }
    } else {
        items
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            header?.invoke()

            if (filter != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = query,
                    onValueChange = {
                        query = it
                    },
                    placeholder = { Text("Search...") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Iconsax.IconsaxFilterSearch,
                            contentDescription = "Search",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(
                    count = filteredItems.size,
                ) { index ->
                    val item = filteredItems[index]

                    if (item.dividerBefore) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable(
                                enabled = item.enabled,
                                onClick = {
                                    item.onClick()
                                    onDismiss()
                                },
                            )
                            .background(
                                color = if (item.selected) {
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                                } else {
                                    Color.Transparent
                                },
                                shape = MaterialTheme.shapes.medium,
                            )
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        when {
                            item.leadingContent != null -> item.leadingContent.invoke()
                            item.icon != null -> Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = if (item.enabled) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                },
                            )

                            item.selected -> Icon(
                                imageVector = FeatherIcons.Check,
                                contentDescription = "Selected",
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            hasSelection -> Spacer(modifier = Modifier.size(22.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (item.enabled) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                },
                            )
                            item.subtitle?.let { subtitle ->
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (item.selected && (item.icon != null || item.leadingContent != null)) {
                            Icon(
                                imageVector = FeatherIcons.Check,
                                contentDescription = "Selected",
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AdaptiveDropdownBottomSheetPreview() {
    Scaffold {
        AdaptiveDropdownBottomSheet(
            items = listOf(
                AdaptiveMenuItem(
                    icon = Iconsax.IconsaxHeart,
                    label = "Favorite",
                    onClick = {},
                ),
                AdaptiveMenuItem(
                    icon = Iconsax.IconsaxShare,
                    label = "Share",
                    onClick = {},
                ),
                AdaptiveMenuItem(
                    icon = Iconsax.IconsaxTrash,
                    label = "Delete",
                    onClick = {},
                    enabled = false,
                ),
            ),
            trigger = { onClick ->
                IconButton(
                    onClick = onClick,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape,
                        )
                        .clip(CircleShape),
                ) {
                    Icon(
                        imageVector = Iconsax.Iconsax3DotsMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )
    }
}
