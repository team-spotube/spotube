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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.launch
import org.spotube.spotube.core.remote.RemoteControlCommand

/**
 * Runs the widget's Pause / Next / Previous, on this device or, while remote-controlling another
 * Spotube device, on that one. Not exported: these actions must stay unreachable by other apps
 * (the provider itself has to be exported for the system's widget broadcasts).
 */
class NowPlayingWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val remoteCommand = when (action) {
            NowPlayingWidget.ACTION_REMOTE_TOGGLE -> RemoteControlCommand.TogglePlayPause
            NowPlayingWidget.ACTION_REMOTE_NEXT -> RemoteControlCommand.SkipNext
            NowPlayingWidget.ACTION_REMOTE_PREVIOUS -> RemoteControlCommand.SkipPrevious
            else -> null
        }
        val pending = goAsync()
        NowPlayingWidget.start(context)
        NowPlayingWidget.scope.launch {
            try {
                if (remoteCommand != null) NowPlayingWidget.sendRemote(remoteCommand)
                else NowPlayingWidget.handleLocal(action)
            } catch (e: Exception) {
                Log.w("NowPlayingWidget", "Widget action $action failed", e)
            } finally {
                pending.finish()
            }
        }
    }
}
