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

package org.spotube.spotube.core.ui.component


import org.spotube.millennium.composables.MillenniumDialogHeader
import org.spotube.millennium.composables.MillenniumDialogTitle
import org.spotube.millennium.composables.MillenniumDialogClose
import org.spotube.millennium.composables.MillenniumDialogBody
import org.spotube.millennium.composables.MillenniumDialogFooter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumCard
import org.spotube.millennium.composables.MillenniumDialog
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll

import org.spotube.spotube.core.ui.theming.AppTheme

import com.composeunstyled.Text

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.spotube.spotube.modules.plugin.MetadataAuthViewModel
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.AlertCircle
import compose.icons.feathericons.Copy
import compose.icons.feathericons.Maximize2
import compose.icons.feathericons.RefreshCw
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.*

@Composable
fun ErrorDisplay(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    authViewModel: MetadataAuthViewModel = koinViewModel()) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    if (authState.loginRequired) {
        LoginRequiredDisplay(
            pluginName = authState.pluginName,
            onLogin = authViewModel::login,
            isLoggingIn = authState.isLoggingIn,
            modifier = modifier)
        return
    }
    var showFullLog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        MillenniumIcon(
            imageVector = FeatherIcons.AlertCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = AppTheme.colors.error.copy(alpha = 0.7f))

        Text(
            text = stringResource(Res.string.error_title),
            style = AppTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.onSurface,
            textAlign = TextAlign.Center)

        Text(
            text = stringResource(Res.string.error_description),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = AppTheme.typography.bodyMedium.lineHeight * 1.2)

        MillenniumCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.error_details),
                        style = AppTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.onSurfaceVariant)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        MillenniumIconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(errorMessage))
                            },
                            modifier = Modifier.size(32.dp),
                            style = MillenniumIconButtonStyle.Quiet) {
                            MillenniumIcon(
                                imageVector = FeatherIcons.Copy,
                                contentDescription = stringResource(Res.string.error_copy_to_clipboard),
                                modifier = Modifier.size(16.dp))
                        }

                        MillenniumIconButton(
                            onClick = { showFullLog = true },
                            modifier = Modifier.size(32.dp),
                            style = MillenniumIconButtonStyle.Quiet) {
                            MillenniumIcon(
                                imageVector = FeatherIcons.Maximize2,
                                contentDescription = stringResource(Res.string.error_view_full_log),
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }

                MillenniumCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)) {
                    SelectionContainer {
                        Text(
                            text = errorMessage,
                            style = AppTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace),
                            color = AppTheme.colors.onSurfaceVariant,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MillenniumButton(
                onClick = onRetry,
                modifier = Modifier.height(40.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MillenniumIcon(
                        imageVector = FeatherIcons.RefreshCw,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp).size(18.dp))
                    Text(stringResource(Res.string.common_retry))
                }
            }
        }
    }

    if (showFullLog) {
        ErrorLogDialog(
            errorMessage = errorMessage,
            onDismiss = { showFullLog = false })
    }
}

@Composable
private fun ErrorLogDialog(
    errorMessage: String,
    onDismiss: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current

    MillenniumDialog(visible = true, onDismissRequest = onDismiss) {
        MillenniumDialogHeader {
            MillenniumDialogTitle(
                text = stringResource(Res.string.error_full_log_title),
                textStyle = AppTheme.typography.titleMedium)
            MillenniumIconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(errorMessage))
                },
                style = MillenniumIconButtonStyle.Quiet) {
                MillenniumIcon(
                    imageVector = FeatherIcons.Copy,
                    contentDescription = stringResource(Res.string.error_copy_to_clipboard))
            }
            MillenniumDialogClose()
        }
        MillenniumDialogBody(scrollable = false) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .clip(AppTheme.shapes.small)
                    .background(AppTheme.colors.surfaceContainerHighest.copy(alpha = 0.3f))) {
                SelectionContainer {
                    Text(
                        text = errorMessage,
                        style = AppTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace),
                        color = AppTheme.colors.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp))
                }
            }
        }
        MillenniumDialogFooter {
            MillenniumButton(
                onClick = onDismiss,
                style = MillenniumButtonStyle.Secondary) {
                Text(stringResource(Res.string.common_close))
            }
        }
    }
}

@Preview
@Composable
private fun ErrorLogDialogPreview() {
    Box(modifier = Modifier.fillMaxSize().background(AppTheme.colors.background)) {
        ErrorLogDialog(
            errorMessage = "This is an error message",
            onDismiss = {})
    }
}
