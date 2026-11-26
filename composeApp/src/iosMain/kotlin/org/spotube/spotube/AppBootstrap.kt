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

import com.yushosei.newpipe.extractor.NewPipe
import com.yushosei.newpipe.util.DefaultDownloaderImpl
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.spotube.spotube.core.di.initKoin
import org.spotube.spotube.core.webview.SafariUserAgent

/**
 * App-wide start-up that must not depend on the phone's Compose view existing: CarPlay can launch
 * the app on its own, with no window on the phone, and still needs dependency injection and the
 * stream extractor to play anything. Both the Compose view and CarPlay call this, in any order.
 *
 * [start] is main thread only (view controller creation and scene callbacks both are).
 */
object AppBootstrap {
    private var started = false
    private var newPipeReady = false
    private val newPipeLock = Mutex()

    fun start() {
        if (started) return
        started = true
        initKoin()
        SafariUserAgent.prime()
    }

    suspend fun ensureNewPipe() = newPipeLock.withLock {
        if (newPipeReady) return@withLock
        NewPipe.init(DefaultDownloaderImpl.initDefault())
        newPipeReady = true
    }
}
