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

package org.spotube.spotube.core.share

import android.content.Context
import android.content.Intent
import androidx.core.app.ShareCompat
import org.spotube.spotube.core.share.ShareService

class AndroidShareService(private val context: Context) : ShareService {
    override fun share(url: String, title: String) {
        // context is the application context (Koin), which can only start activities in a new task
        val chooser = ShareCompat.IntentBuilder(context)
            .setType("text/plain")
            .setSubject(title)
            .setText(url)
            .createChooserIntent()
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
