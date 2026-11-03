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

package org.spotube.spotube.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSUserDefaults

// Compose (and Compose resources) read NSLocale.preferredLanguages, which honours the
// app-level AppleLanguages default.
private const val APPLE_LANGUAGES_KEY = "AppleLanguages"
private const val SYSTEM_LANGUAGE = "system"

private val LocalAppLocale = staticCompositionLocalOf { SYSTEM_LANGUAGE }

@Composable
internal actual fun provideAppLocale(languageTag: String?): ProvidedValue<*> {
    val defaults = NSUserDefaults.standardUserDefaults
    if (languageTag == null) {
        defaults.removeObjectForKey(APPLE_LANGUAGES_KEY)
    } else {
        defaults.setObject(listOf(languageTag), APPLE_LANGUAGES_KEY)
    }
    return LocalAppLocale provides (languageTag ?: SYSTEM_LANGUAGE)
}
