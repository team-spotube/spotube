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

package org.spotube.spotube

enum class PlatformType {
    Android, IOS, Windows, Linux, MacOS, Unknown
}

interface Platform {
    val name: String
    val type: PlatformType
}

expect fun getPlatform(): Platform

expect fun openUrlInBrowser(url: String)

fun Platform.isDesktop(): Boolean {
    return type == PlatformType.Windows ||
            type == PlatformType.Linux ||
            type == PlatformType.MacOS
}

fun Platform.isMobile(): Boolean {
    return type == PlatformType.Android ||
            type == PlatformType.IOS
}