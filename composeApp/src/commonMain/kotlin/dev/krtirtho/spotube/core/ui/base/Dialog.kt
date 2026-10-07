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

package dev.krtirtho.spotube.core.ui.base

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val DefaultDialogMaxWidth: Dp = 440.dp

@Composable
fun ThemedDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
    theme: BaseUITheme.DialogTheme? = null,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(280),
            ) + fadeIn(animationSpec = tween(200)),
            exit = scaleOut(
                targetScale = 0.92f,
                animationSpec = tween(180),
            ) + fadeOut(animationSpec = tween(150)),
        ) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                ThemedDialogSurface(
                    theme = theme,
                    title = title,
                    actions = actions,
                    content = content,
                )
            }
        }
    }
}

/**
 * The visual chrome of [ThemedDialog] without the platform [Dialog] window.
 *
 * Extracted so the dialog can be rendered inside previews (and reused elsewhere),
 * since the platform dialog lives in a separate window the preview canvas cannot show.
 */
@Composable
internal fun ThemedDialogSurface(
    modifier: Modifier = Modifier,
    theme: BaseUITheme.DialogTheme? = null,
    title: @Composable (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val dialogTheme = theme ?: LocalBaseUITheme.current.dialog

    Box(
        modifier = modifier
            .widthIn(max = DefaultDialogMaxWidth)
            .shadow(
                elevation = dialogTheme.shadow.elevation,
                shape = dialogTheme.shadow.let { if (it.clip) dialogTheme.shape else RoundedCornerShape(0.dp) },
                ambientColor = dialogTheme.shadow.ambientColor,
                spotColor = dialogTheme.shadow.spotColor,
            )
            .clip(dialogTheme.shape)
            .background(dialogTheme.background, dialogTheme.shape)
            .border(
                BorderStroke(dialogTheme.border.width, dialogTheme.border.color),
                dialogTheme.shape
            )
            .highlight(Color.White.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.widthIn(max = DefaultDialogMaxWidth)) {
            if (title != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp),
                ) {
                    title()
                }
                Spacer(Modifier.height(8.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = if (title == null) 24.dp else 0.dp,
                    ),
            ) {
                content()
            }

            if (actions != null) {
                Spacer(Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        actions()
                    }
                }
            } else {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@BaseUIPhonePreview
@Composable
private fun ThemedDialogPreview() {
    BaseUIPreview {
        var visible by remember { mutableStateOf(true) }

        Box(modifier = Modifier.fillMaxSize()) {
            PrimaryButton(
                onClick = { visible = true },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(24.dp),
            ) {
                Text("Open dialog")
            }

            if (visible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalBaseUITheme.current.dialog.scrim)
                        .clickable { visible = false },
                    contentAlignment = Alignment.Center,
                ) {
                    ThemedDialogSurface(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                        title = {
                            Text(
                                text = "Sign out of Spotube?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        actions = {
                            OutlineButton(onClick = { visible = false }) { Text("Cancel") }
                            PrimaryButton(onClick = { visible = false }) { Text("Sign out") }
                        },
                    ) {
                        Text(
                            text = "You can sign back in at any time. Your downloaded tracks stay on this device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
