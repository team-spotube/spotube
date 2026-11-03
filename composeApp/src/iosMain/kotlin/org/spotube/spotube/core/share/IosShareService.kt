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

import org.spotube.spotube.core.share.ShareService
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

class IosShareService : ShareService {
    override fun share(url: String, title: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        val activityViewController = UIActivityViewController(listOf(title, url, nsUrl), null)
        rootViewController.presentViewController(activityViewController, true, null)
    }
}
