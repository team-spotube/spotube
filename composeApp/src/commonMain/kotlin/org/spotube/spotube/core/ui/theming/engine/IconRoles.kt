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

package org.spotube.spotube.core.ui.theming.engine

/**
 * Icon *roles*: the neutral vocabulary an icon pack maps. A role is the Lucide icon name (`house`, `skip-forward`,
 * `trash-2`), so any pack that offers "a home icon" can fill `house`. The app's own icons (Iconsax, Feather,
 * Millennium's checkmarks and chevrons) are tied to roles through the same table the bundled Modern theme uses, so one
 * pack mapping restyles every icon in the app, including the ones Millennium draws itself.
 */
internal val iconRoleByVectorName: Map<String, String> by lazy {
    lucideIconReplacements.mapValues { (_, v) -> kebabCase(v.name) }
}

/** `SkipForward` -> `skip-forward`, `Trash2` -> `trash-2`. */
internal fun kebabCase(pascal: String): String = buildString {
    pascal.forEachIndexed { i, c ->
        val digitStart = c.isDigit() && i > 0 && !pascal[i - 1].isDigit()
        if (i > 0 && (c.isUpperCase() || digitStart) && last() != '-') append('-')
        append(c.lowercaseChar())
    }
}
