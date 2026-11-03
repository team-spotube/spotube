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

import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
internal actual fun provideAppLocale(languageTag: String?): ProvidedValue<*> {
    // System resources are never touched by the override below, so they always report the
    // device language (also after the user changes it while the app is running).
    val locale = languageTag?.let(Locale::forLanguageTag)
        ?: Resources.getSystem().configuration.locales[0]
    // Compose resources read Locale.getDefault(); also used by non-UI code (notifications).
    Locale.setDefault(locale)

    val configuration = Configuration(LocalConfiguration.current).apply { setLocale(locale) }
    val resources = LocalContext.current.resources
    @Suppress("DEPRECATION")
    resources.updateConfiguration(configuration, resources.displayMetrics)
    return LocalConfiguration provides configuration
}
