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

package org.spotube.spotube.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/** Home screen widget: shows what's playing with previous / play-pause / next, no app launch needed. */
class NowPlayingWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) = NowPlayingWidget.start(context)

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) =
        NowPlayingWidget.start(context)

    override fun onDisabled(context: Context) = NowPlayingWidget.stop()
}
