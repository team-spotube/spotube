/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.local_tracks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.Text
import org.spotube.spotube.core.ui.component.ApplicationMainBar

@Composable
fun LocalFolderScreen(viewModel: LocalFolderViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column {
        ApplicationMainBar(
            backButton = true,
            title = { Text(state.currentFolder?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        )
        LocalTracksContent(
            state,
            onOpenFolder = {},
            onPlayTrack = viewModel::playTrack,
            onNavigateUp = onBack,
            onPlayFolder = viewModel::playFolder,
            onRequestPermission = {},
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}
