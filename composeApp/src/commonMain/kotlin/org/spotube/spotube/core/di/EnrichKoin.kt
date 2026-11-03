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

package org.spotube.spotube.core.di

import androidx.compose.runtime.Composable
import co.touchlab.kermit.Logger
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

inline fun <reified T> KoinComponent.injectLogger(): Lazy<Logger> {
    return inject { parametersOf(T::class.simpleName ?: "Unknown") }
}

@Composable
inline fun <reified T : Any> rememberLogger(): Logger {
    val tag = T::class.simpleName ?: "Unknown"
    return koinInject { parametersOf(tag) }
}

@Composable
fun rememberLogger(tag: String): Logger {
    return koinInject { parametersOf(tag) }
}