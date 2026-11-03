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

package org.spotube.spotube.modules.plugin.components


import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape

import org.spotube.spotube.core.ui.theming.AppTheme

import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import okio.Path
import org.spotube.spotube.modules.plugin.BUILT_IN_PLUGINS
import org.spotube.spotube.modules.plugin.PluginAbility
import org.spotube.spotube.modules.plugin.PluginEntry
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxBox
import org.spotube.spotube.resources.iconsax.IconsaxCheckSquare
import org.spotube.spotube.resources.iconsax.IconsaxInformation
import org.spotube.spotube.resources.iconsax.IconsaxHeart
import org.spotube.spotube.resources.iconsax.IconsaxTag
import org.spotube.spotube.resources.iconsax.IconsaxTrash
import org.spotube.spotube.resources.iconsax.User
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.plugin_action_remove
import spotube.composeapp.generated.resources.plugin_action_login
import spotube.composeapp.generated.resources.plugin_action_logout
import spotube.composeapp.generated.resources.plugin_state_active
import spotube.composeapp.generated.resources.plugin_state_builtin
import spotube.composeapp.generated.resources.plugin_version_label

@Composable
internal fun PluginCard(
    plugin: PluginEntry,
    isSelected: Boolean,
    onRemove: () -> Unit,
    isLoggedIn: Boolean,
    onLogin: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    onInfo: (() -> Unit)? = null,
    onSupport: (() -> Unit)? = null,
    logoPath: Path? = null,
    isAuthInProgress: Boolean = false,
    isRemoving: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Plugin icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppTheme.colors.primary.copy(alpha = 0.1f))
            ) {
                PluginLogo(
                    plugin = plugin,
                    logoPath = logoPath,
                    contentDescription = plugin.name,
                    modifier = Modifier.fillMaxSize(),
                    fallback = {
                        Box(contentAlignment = Alignment.Center) {
                            MillenniumIcon(
                                Iconsax.IconsaxBox,
                                contentDescription = null,
                                tint = AppTheme.colors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    })
            }

            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        plugin.name,
                        style = AppTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // "Active" chip
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AppTheme.colors.primary.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                MillenniumIcon(
                                    Iconsax.IconsaxCheckSquare,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = AppTheme.colors.primary
                                )
                                Text(
                                    stringResource(Res.string.plugin_state_active),
                                    style = AppTheme.typography.labelSmall,
                                    color = AppTheme.colors.primary
                                )
                            }
                        }
                    }
                }

                if (plugin.description.isNotBlank()) {
                    Text(
                        plugin.description,
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Meta chips row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        MillenniumIcon(
                            Iconsax.User,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = AppTheme.colors.onSurfaceVariant
                        )
                        Text(
                            plugin.author,
                            style = AppTheme.typography.labelSmall,
                            color = AppTheme.colors.onSurfaceVariant
                        )
                    }
                    if (plugin !in BUILT_IN_PLUGINS)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            MillenniumIcon(
                                Iconsax.IconsaxTag,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = AppTheme.colors.onSurfaceVariant
                            )
                            Text(
                                stringResource(Res.string.plugin_version_label, plugin.version),
                                style = AppTheme.typography.labelSmall,
                                color = AppTheme.colors.onSurfaceVariant
                            )
                        }
                }

                // Ability chips
                if (plugin.abilities.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        plugin.abilities.forEach { ability ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AppTheme.colors.secondaryContainer.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    ability.displayLabel(),
                                    style = AppTheme.typography.labelSmall,
                                    color = AppTheme.colors.onSecondaryContainer,
                                    modifier = Modifier.padding(
                                        horizontal = 6.dp,
                                        vertical = 2.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.align(Alignment.Bottom),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onInfo != null) {
                        MillenniumIconButton(
                            onClick = onInfo,
                            style = MillenniumIconButtonStyle.Quiet) {
                            MillenniumIcon(
                                Iconsax.IconsaxInformation,
                                contentDescription = null,
                                tint = AppTheme.colors.onSurfaceVariant)
                        }
                    }
                    if (onSupport != null) {
                        MillenniumIconButton(
                            onClick = onSupport,
                            style = MillenniumIconButtonStyle.Quiet) {
                            MillenniumIcon(
                                Iconsax.IconsaxHeart,
                                contentDescription = null,
                                tint = AppTheme.colors.onSurfaceVariant)
                        }
                    }
                    if (plugin in BUILT_IN_PLUGINS) {
                        Text(
                            stringResource(Res.string.plugin_state_builtin),
                            style = AppTheme.typography.labelSmall,
                            color = AppTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                .border(
                                    BorderStroke(1.dp, AppTheme.colors.outlineVariant),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    } else {
                        MillenniumIconButton(
                            onClick = onRemove,
                            loading = isRemoving,
                            style = MillenniumIconButtonStyle.Quiet) {
                            MillenniumIcon(
                                Iconsax.IconsaxTrash,
                                contentDescription = stringResource(Res.string.plugin_action_remove),
                                tint = AppTheme.colors.onSurfaceVariant)
                        }
                    }
                }

                val authAction = when {
                    isLoggedIn && onLogout != null -> onLogout to Res.string.plugin_action_logout
                    !isLoggedIn && onLogin != null -> onLogin to Res.string.plugin_action_login
                    else -> null
                }
                authAction?.let { (action, label) ->
                    MillenniumButton(
                        onClick = action,
                        loading = isAuthInProgress,
                        style = MillenniumButtonStyle.Secondary) {
                        Text(
                            text = stringResource(label),
                            style = AppTheme.typography.labelLarge
                        )
                }
            }
        }
    }
}

