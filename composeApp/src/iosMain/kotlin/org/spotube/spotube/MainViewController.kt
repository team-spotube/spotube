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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import com.yushosei.newpipe.extractor.NewPipe
import com.yushosei.newpipe.util.DefaultDownloaderImpl
import org.spotube.spotube.core.di.initKoin
import org.spotube.spotube.core.webview.SafariUserAgent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

fun MainViewController() = ComposeUIViewController(configure = {
    initKoin()
    SafariUserAgent.prime()
}) { InitNewPipe { App() } }

@Composable
fun InitNewPipe(
    content: @Composable () -> Unit
) {
    var isInitialized by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        NewPipe.init(DefaultDownloaderImpl.initDefault())
        isInitialized = true
    }

    if (isInitialized) {
        content()
    }
}