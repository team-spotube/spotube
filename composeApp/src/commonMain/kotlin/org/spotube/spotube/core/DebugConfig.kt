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

package org.spotube.spotube.core

/**
 * Development-only switches. These are intentionally kept out of [org.spotube.spotube.modules.settings.UserSettings]
 * so they can never leak into user-facing configuration.
 */
object DebugConfig {
    /**
     * Flip to `true` while developing to always open the Welcome/Onboarding screen, even after
     * it has been completed (so you don't have to wipe app data to see it again). It still
     * dismisses for the session when you finish or skip it.
     *
     * Keep it `false` when committing.
     */
    const val FORCE_SHOW_ONBOARDING: Boolean = false

    /**
     * Flip to `true` while developing to show the support reminder dialog on the next eligible
     * app launch, ignoring both the 7-day interval and the "Support reminders" opt-out toggle.
     * It still waits until onboarding is dismissed, and only fires once per launch.
     *
     * Keep it `false` when committing.
     */
    const val FORCE_SUPPORT_REMINDER: Boolean = false

    const val FORCE_VLC_LOGS: Boolean = false
}
