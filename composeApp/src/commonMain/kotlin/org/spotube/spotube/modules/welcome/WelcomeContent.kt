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

package org.spotube.spotube.modules.welcome

import org.jetbrains.compose.resources.StringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.welcome_get_started
import spotube.composeapp.generated.resources.welcome_lets_go
import spotube.composeapp.generated.resources.welcome_next
import spotube.composeapp.generated.resources.welcome_step_guide
import spotube.composeapp.generated.resources.welcome_step_plugins
import spotube.composeapp.generated.resources.welcome_step_support
import spotube.composeapp.generated.resources.welcome_step_welcome

/** Order matches the 4 onboarding pages. */
internal val welcomePrimaryLabels: List<StringResource> = listOf(
    Res.string.welcome_lets_go,
    Res.string.welcome_next,
    Res.string.welcome_next,
    Res.string.welcome_get_started)

/** Short labels for the step switcher. Order matches the 4 onboarding pages. */
internal val welcomeStepLabels: List<StringResource> = listOf(
    Res.string.welcome_step_welcome,
    Res.string.welcome_step_support,
    Res.string.welcome_step_plugins,
    Res.string.welcome_step_guide)
