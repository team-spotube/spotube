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
import androidx.compose.runtime.SideEffect
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import org.jetbrains.compose.resources.rememberResourceEnvironment
import kotlin.concurrent.Volatile
import org.jetbrains.compose.resources.getPluralString as composeGetPluralString
import org.jetbrains.compose.resources.getString as composeGetString
import org.jetbrains.compose.resources.getStringArray as composeGetStringArray

/*
 * Non-composable string lookups (ViewModels, services, snackbars). Use these instead of
 * org.jetbrains.compose.resources.getString/getPluralString/getStringArray.
 *
 * The library versions call getSystemResourceEnvironment() on every lookup. On desktop that
 * also asks Skiko for the system theme, and on Linux Skiko answers with a blocking D-Bus call to
 * the desktop portal on whatever thread is asking. Run from a background coroutine, that call
 * segfaulted the GraalVM build on GNOME (#3183). Strings never depend on the theme, so these
 * reuse the environment the UI already computed (on the UI thread, for stringResource) and only
 * fall back to the library's lookup before anything has been composed.
 */

@Volatile
private var composedEnvironment: ResourceEnvironment? = null

/** Publishes the UI's resource environment for [getString] and friends. Called by [AppLocaleProvider]. */
@Composable
internal fun PublishResourceEnvironment() {
    val environment = rememberResourceEnvironment()
    SideEffect { composedEnvironment = environment }
}

private fun resourceEnvironment(): ResourceEnvironment =
    composedEnvironment ?: getSystemResourceEnvironment()

suspend fun getString(resource: StringResource): String =
    composeGetString(resourceEnvironment(), resource)

suspend fun getString(resource: StringResource, vararg formatArgs: Any): String =
    composeGetString(resourceEnvironment(), resource, *formatArgs)

suspend fun getPluralString(resource: PluralStringResource, quantity: Int): String =
    composeGetPluralString(resourceEnvironment(), resource, quantity)

suspend fun getPluralString(resource: PluralStringResource, quantity: Int, vararg formatArgs: Any): String =
    composeGetPluralString(resourceEnvironment(), resource, quantity, *formatArgs)

suspend fun getStringArray(resource: StringArrayResource): List<String> =
    composeGetStringArray(resourceEnvironment(), resource)
