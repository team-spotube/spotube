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

package dev.krtirtho.spotube.modules.plugin.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.krtirtho.spotube.core.ui.component.AdaptiveDropdownBottomSheet
import dev.krtirtho.spotube.core.ui.component.AdaptiveMenuItem
import dev.krtirtho.spotube.core.ui.component.HeaderDisplayMode
import dev.krtirtho.spotube.modules.plugin.AbilitySelection
import dev.krtirtho.spotube.modules.plugin.PluginAbility
import dev.krtirtho.spotube.modules.plugin.PluginEntry
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxArrowDown4
import dev.krtirtho.spotube.resources.iconsax.IconsaxDocumentText
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusic
import dev.krtirtho.spotube.resources.iconsax.IconsaxSound
import dev.krtirtho.spotube.resources.iconsax.IconsaxTextalignLeft
import okio.Path
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_plugins_ability_audio
import spotube.composeapp.generated.resources.settings_plugins_ability_lyrics
import spotube.composeapp.generated.resources.settings_plugins_ability_metadata
import spotube.composeapp.generated.resources.settings_plugins_ability_scrobble
import spotube.composeapp.generated.resources.settings_plugins_default_ability_title
import spotube.composeapp.generated.resources.settings_plugins_no_plugins
import spotube.composeapp.generated.resources.settings_plugins_no_selection
import kotlin.random.Random

/** The accent color used for a plugin ability across the whole plugin UI. */
internal fun PluginAbility.accentColor(): Color = when (this) {
    PluginAbility.METADATA -> Color(0xFF34A853)
    PluginAbility.AUDIO -> Color(0xFF1E88E5)
    PluginAbility.LYRICS -> Color(0xFFF9A825)
    PluginAbility.SCROBBLE -> Color(0xFF8E24AA)
}

/** The icon used to represent a plugin ability. */
internal fun PluginAbility.abilityIcon(): ImageVector = when (this) {
    PluginAbility.METADATA -> Iconsax.IconsaxDocumentText
    PluginAbility.AUDIO -> Iconsax.IconsaxMusic
    PluginAbility.LYRICS -> Iconsax.IconsaxTextalignLeft
    PluginAbility.SCROBBLE -> Iconsax.IconsaxSound
}

/**
 * A vibrant, multi-hue gradient per ability, used by the Frutiger-Aero plugin tiles.
 */
internal fun PluginAbility.aeroGradient(): List<Color> = when (this) {
    PluginAbility.METADATA -> listOf(Color(0xFFA8E063), Color(0xFF56AB2F), Color(0xFF11998E))
    PluginAbility.AUDIO -> listOf(Color(0xFF56CCF2), Color(0xFF2F80ED), Color(0xFF6A11CB))
    PluginAbility.LYRICS -> listOf(Color(0xFFFFE259), Color(0xFFFFA751), Color(0xFFFF5E62))
    PluginAbility.SCROBBLE -> listOf(Color(0xFFFF6FD8), Color(0xFF9D50BB), Color(0xFF3F0D7A))
}

@Composable
internal fun PluginAbility.displayLabel(): String = when (this) {
    PluginAbility.METADATA -> stringResource(Res.string.settings_plugins_ability_metadata)
    PluginAbility.AUDIO -> stringResource(Res.string.settings_plugins_ability_audio)
    PluginAbility.LYRICS -> stringResource(Res.string.settings_plugins_ability_lyrics)
    PluginAbility.SCROBBLE -> stringResource(Res.string.settings_plugins_ability_scrobble)
}

/** A short "v1.2.3 · Author" summary shown under a plugin in the picker. */
private fun PluginEntry.metadataLabel(): String = buildString {
    append("v").append(version)
    if (author.isNotBlank()) {
        append(" · ").append(author)
    }
}

/**
 * The 2x2 grid of default-plugin selectors shown on large screens. Each tile is a
 * Frutiger-Aero style card representing one ability and its currently selected plugin.
 */
@Composable
internal fun DefaultAbilityPluginGrid(
    selections: List<AbilitySelection>,
    onSelected: (PluginAbility, PluginEntry?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        selections.chunked(2).forEach { rowSelections ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowSelections.forEach { selection ->
                    DefaultAbilityPluginCard(
                        selection = selection,
                        onSelected = { onSelected(selection.ability, it) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(2 - rowSelections.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DefaultAbilityPluginCard(
    selection: AbilitySelection,
    onSelected: (PluginEntry?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ability = selection.ability
    val selected = selection.selectedPlugin
    val noPluginsText = stringResource(Res.string.settings_plugins_no_plugins)
    val noSelectionText = stringResource(Res.string.settings_plugins_no_selection)

    val menuItems = buildList {
        if (selection.plugins.isNotEmpty()) {
            selection.plugins.forEach { plugin ->
                add(
                    AdaptiveMenuItem(
                        label = plugin.name,
                        subtitle = plugin.metadataLabel(),
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                PluginLogo(
                                    plugin = plugin,
                                    logoPath = selection.pluginLogoPaths[plugin.id],
                                    contentDescription = plugin.name,
                                    modifier = Modifier.fillMaxSize(),
                                    fallback = {
                                        Icon(
                                            imageVector = ability.abilityIcon(),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = ability.accentColor(),
                                        )
                                    },
                                )
                            }
                        },
                        onClick = { onSelected(plugin) },
                        selected = selected?.name == plugin.name,
                    )
                )
            }
        } else {
            add(
                AdaptiveMenuItem(
                    label = noPluginsText,
                    onClick = { },
                    enabled = false,
                )
            )
        }
    }

    AdaptiveDropdownBottomSheet(
        items = menuItems,
        modifier = modifier,
        headerDisplayMode = HeaderDisplayMode.OnlyInBottomSheet,
        header = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AbilityBadge(ability)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(
                            Res.string.settings_plugins_default_ability_title,
                            ability.displayLabel(),
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    selected?.let {
                        Text(
                            it.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
        trigger = { onClick ->
            AbilityPluginTile(
                ability = ability,
                selectedPlugin = selected,
                logoPath = selection.pluginLogoPaths[selected?.id],
                pluginName = selected?.name ?: noSelectionText,
                version = selected?.version,
                onClick = onClick,
            )
        },
    )
}

@Composable
private fun AbilityPluginTile(
    ability: PluginAbility,
    selectedPlugin: PluginEntry?,
    logoPath: Path?,
    pluginName: String,
    version: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = ability.accentColor()
    val gradient = ability.aeroGradient()
    val shape = RoundedCornerShape(20.dp)
    val seed = ability.ordinal + 1

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .heightIn(min = 148.dp)
            .drawWithCache {
                val background = Brush.linearGradient(
                    colors = gradient,
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                )
                onDrawBehind {
                    drawRect(background)
                    drawAeroDecorations(seed)
                    // Bottom scrim so the white text stays readable on light gradients.
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.32f)),
                            startY = size.height * 0.3f,
                            endY = size.height,
                        ),
                    )
                    // Glossy top highlight.
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.55f,
                        ),
                    )
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f))
                        .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    PluginLogo(
                        plugin = selectedPlugin,
                        logoPath = logoPath,
                        contentDescription = pluginName,
                        modifier = Modifier.fillMaxSize(),
                        fallback = {
                            Icon(
                                imageVector = ability.abilityIcon(),
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                                tint = accent,
                            )
                        },
                    )
                }
                Icon(
                    imageVector = Iconsax.IconsaxArrowDown4,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(alpha = 0.85f),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = pluginName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(ability.displayLabel())
                        if (!version.isNullOrBlank()) {
                            append(" · v").append(version)
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AbilityBadge(ability: PluginAbility) {
    val accent = ability.accentColor()
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
        color = accent.copy(alpha = 0.12f),
    ) {
        Icon(
            imageVector = ability.abilityIcon(),
            contentDescription = null,
            modifier = Modifier.padding(8.dp),
            tint = accent,
        )
    }
}

/**
 * Soft translucent bubbles and highlights behind a tile, giving the glossy
 * Frutiger-Aero look. Deterministic per tile so it does not flicker on recomposition.
 */
private fun DrawScope.drawAeroDecorations(seed: Int) {
    val random = Random(seed)
    repeat(6) {
        val center = Offset(
            x = random.nextFloat() * size.width,
            y = random.nextFloat() * size.height,
        )
        val radius = size.minDimension * (0.12f + random.nextFloat() * 0.4f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }
    repeat(3) {
        val center = Offset(
            x = random.nextFloat() * size.width,
            y = random.nextFloat() * size.height,
        )
        val radius = size.minDimension * (0.03f + random.nextFloat() * 0.05f)
        drawCircle(
            color = Color.White.copy(alpha = 0.28f),
            radius = radius,
            center = center,
        )
    }
}
