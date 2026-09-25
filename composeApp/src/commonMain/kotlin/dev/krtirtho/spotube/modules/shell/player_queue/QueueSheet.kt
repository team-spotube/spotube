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

package dev.krtirtho.spotube.modules.shell.player_queue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.animateDp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.krtirtho.spotube.modules.shell.LocalAppShellBottomInset

private val SlidingSheetBreakpoint = 840.dp

@Composable
fun QueueSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        if (maxWidth >= SlidingSheetBreakpoint) {
            SlidingQueueSheet(isVisible, onDismiss, content)
        } else {
            BottomQueueSheet(isVisible, onDismiss, content)
        }
    }
}

@Composable
private fun SlidingQueueSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isVisible,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = 12.dp,
                    end = 12.dp,
                    bottom = 12.dp + LocalAppShellBottomInset.current,
                ),
            enter = slideInHorizontally { fullWidth -> fullWidth / 2 } + fadeIn(),
            exit = slideOutHorizontally { fullWidth -> fullWidth / 2 } + fadeOut(),
        ) {
            val animatedShadowElevation = transition.animateDp(label = "queueShadow") { state ->
                if (state == EnterExitState.Visible) 20.dp else 0.dp
            }
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(460.dp),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 2.dp,
                shadowElevation = animatedShadowElevation.value,
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomQueueSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
        ) {
            content()
        }
    }
}
