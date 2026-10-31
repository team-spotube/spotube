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

package org.spotube.spotube.core.jam

import android.content.res.Resources
import android.os.Build
import java.util.Locale
import java.util.TimeZone

internal actual fun jamDeviceTraits(): List<String> {
    val display = Resources.getSystem().displayMetrics
    return listOf(
        "android",
        Build.MANUFACTURER,
        Build.MODEL,
        Build.DEVICE,
        Build.VERSION.RELEASE,
        Build.VERSION.SDK_INT.toString(),
        "${minOf(display.widthPixels, display.heightPixels)}x${maxOf(display.widthPixels, display.heightPixels)}@${display.densityDpi}",
        Locale.getDefault().toLanguageTag(),
        TimeZone.getDefault().id)
}
