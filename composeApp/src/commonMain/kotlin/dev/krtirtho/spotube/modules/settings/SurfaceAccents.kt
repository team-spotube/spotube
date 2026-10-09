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

package dev.krtirtho.spotube.modules.settings

import androidx.compose.ui.graphics.Color
import dev.krtirtho.spotube.core.ui.base.SurfaceStyle

/**
 * Accent palettes that are known to pair well with each surface material.
 *
 * The appearance picker only offers these combinations, so a user can never end up with a
 * clashing surface + accent pair — instead of the old behaviour where any accent could be
 * applied to any surface.
 */
val SurfaceStyle.recommendedAccents: List<AccentColors>
    get() = when (this) {
        SurfaceStyle.Watery -> listOf(
            AccentColors.OCEANIC_CYAN,
            AccentColors.MIDNIGHT_BLUE,
            AccentColors.GREEN_GOBLIN,
            AccentColors.SUNSET_ORANGE,
        )

        SurfaceStyle.Icy -> listOf(
            AccentColors.MIDNIGHT_BLUE,
            AccentColors.OCEANIC_CYAN,
            AccentColors.ELECTRIC_VIOLET,
            AccentColors.ROSE_GARDEN,
        )

        SurfaceStyle.Chrome -> listOf(
            AccentColors.METALLIC_SLATE,
            AccentColors.MIDNIGHT_BLUE,
            AccentColors.SUNSET_ORANGE,
            AccentColors.GREEN_GOBLIN,
        )

        SurfaceStyle.Acrylic -> listOf(
            AccentColors.METALLIC_SLATE,
            AccentColors.ELECTRIC_VIOLET,
            AccentColors.OCEANIC_CYAN,
            AccentColors.ROSE_GARDEN,
        )

        SurfaceStyle.Glass -> listOf(
            AccentColors.GREEN_GOBLIN,
            AccentColors.ELECTRIC_VIOLET,
            AccentColors.OCEANIC_CYAN,
            AccentColors.ROSE_GARDEN,
        )
    }

/** The concrete accent colour for the current light/dark mode. */
fun AccentColors.colorFor(isLight: Boolean): Color =
    if (isLight) toLightColor() else toDarkColor()
