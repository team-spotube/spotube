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

package org.spotube.spotube.modules.settings.logs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Severity
import com.composeunstyled.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumTextField
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.logger.AppLogBuffer
import org.spotube.spotube.core.logger.LogEntry
import org.spotube.spotube.core.server.LocalServer
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.plugin.PluginManager
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxDocumentText
import org.spotube.spotube.resources.iconsax.IconsaxFilterSearch
import org.spotube.spotube.resources.iconsax.IconsaxShare
import org.spotube.spotube.resources.iconsax.IconsaxTrash
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_logs_clear
import spotube.composeapp.generated.resources.settings_logs_copied
import spotube.composeapp.generated.resources.settings_logs_copy
import spotube.composeapp.generated.resources.settings_logs_empty
import spotube.composeapp.generated.resources.settings_logs_filter_all
import spotube.composeapp.generated.resources.settings_logs_filter_errors
import spotube.composeapp.generated.resources.settings_logs_filter_warnings
import spotube.composeapp.generated.resources.settings_logs_search_hint
import spotube.composeapp.generated.resources.settings_logs_title

private enum class LogFilter {
    ALL,
    ERRORS,
    WARNINGS
}

@Composable
fun LogsScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        ApplicationMainBar(
            backButton = true,
            title = { Text(stringResource(Res.string.settings_logs_title)) }
        )
        LogsContent(modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun LogsContent(modifier: Modifier = Modifier) {
    val logs by AppLogBuffer.logs.collectAsStateWithLifecycle()
    val shellBottomInset = LocalAppShellBottomInset.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val localServer = runCatching { koinInject<LocalServer>() }.getOrNull()
    val localServerPort by (localServer?.port?.collectAsStateWithLifecycle() ?: remember { mutableStateOf<Int?>(null) })
    val pluginManager = runCatching { koinInject<PluginManager>() }.getOrNull()
    val audioPlugin by (pluginManager?.selectedAudioPlugin?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) })
    val audioPlayerQueue = runCatching { koinInject<AudioPlayerQueue>() }.getOrNull()
    val currentQueueEntry by (audioPlayerQueue?.currentQueueEntryFlow?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) })

    val searchState = rememberTextFieldState("")
    val searchQuery = searchState.text.toString().trim().lowercase()
    var selectedFilter by remember { mutableStateOf(LogFilter.ALL) }
    var copiedFeedback by remember { mutableStateOf(false) }

    val filteredLogs = remember(logs, searchQuery, selectedFilter) {
        logs.filter { entry ->
            val matchesSeverity = when (selectedFilter) {
                LogFilter.ALL -> true
                LogFilter.ERRORS -> entry.severity == Severity.Error || entry.severity == Severity.Assert
                LogFilter.WARNINGS -> entry.severity == Severity.Warn || entry.severity == Severity.Error || entry.severity == Severity.Assert
            }
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                entry.tag.lowercase().contains(searchQuery) || entry.message.lowercase().contains(searchQuery)
            }
            matchesSeverity && matchesSearch
        }.asReversed()
    }

    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Diagnostic status overview card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppTheme.colors.surfaceVariant.copy(alpha = 0.5f))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "System Diagnostics",
                    style = AppTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Proxy Server:",
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.textMuted
                    )
                    Text(
                        text = if (localServerPort != null) "127.0.0.1:$localServerPort (Active)" else "Starting...",
                        style = AppTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (localServerPort != null) AppTheme.colors.primary else AppTheme.colors.textMuted
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Audio Plugin:",
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.textMuted
                    )
                    Text(
                        text = audioPlugin?.name ?: "NewPipe YouTube (Built-in)",
                        style = AppTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.colors.onSurface
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Current Track:",
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.textMuted
                    )
                    Text(
                        text = currentQueueEntry?.let { entry ->
                            when (entry) {
                                is org.spotube.spotube.core.audioplayer.QueueEntry.StreamingTrack -> entry.track.title
                                is org.spotube.spotube.core.audioplayer.QueueEntry.LocalTrack -> entry.name
                            }
                        } ?: "None",
                        style = AppTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.colors.onSurface
                    )
                }
            }
        }

        // Action & Filter row
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MillenniumTextField(
                state = searchState,
                placeholder = { Text(stringResource(Res.string.settings_logs_search_hint), fontSize = 12.sp) },
                leadingIcon = {
                    MillenniumIcon(
                        imageVector = Iconsax.IconsaxFilterSearch,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.weight(1f)
            )

            MillenniumButton(
                onClick = {
                    val fullLogs = AppLogBuffer.formatAllLogs()
                    clipboardManager.setText(AnnotatedString(fullLogs))
                    copiedFeedback = true
                    coroutineScope.launch {
                        delay(2000)
                        copiedFeedback = false
                    }
                },
                style = MillenniumButtonStyle.Secondary
            ) {
                MillenniumIcon(
                    imageVector = Iconsax.IconsaxShare,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (copiedFeedback) stringResource(Res.string.settings_logs_copied) else stringResource(Res.string.settings_logs_copy),
                    style = AppTheme.typography.labelSmall
                )
            }

            MillenniumIconButton(
                onClick = { AppLogBuffer.clear() },
                style = MillenniumIconButtonStyle.Secondary,
                contentDescription = stringResource(Res.string.settings_logs_clear)
            ) {
                MillenniumIcon(
                    imageVector = Iconsax.IconsaxTrash,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Filter chips row
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LogFilterChip(
                label = stringResource(Res.string.settings_logs_filter_all),
                selected = selectedFilter == LogFilter.ALL,
                count = logs.size,
                onClick = { selectedFilter = LogFilter.ALL }
            )
            LogFilterChip(
                label = stringResource(Res.string.settings_logs_filter_errors),
                selected = selectedFilter == LogFilter.ERRORS,
                count = logs.count { it.severity == Severity.Error || it.severity == Severity.Assert },
                chipColor = Color(0xFFE53935),
                onClick = { selectedFilter = LogFilter.ERRORS }
            )
            LogFilterChip(
                label = stringResource(Res.string.settings_logs_filter_warnings),
                selected = selectedFilter == LogFilter.WARNINGS,
                count = logs.count { it.severity == Severity.Warn },
                chipColor = Color(0xFFFB8C00),
                onClick = { selectedFilter = LogFilter.WARNINGS }
            )
        }

        // Logs list
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MillenniumIcon(
                        imageVector = Iconsax.IconsaxDocumentText,
                        contentDescription = null,
                        tint = AppTheme.colors.textMuted,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = stringResource(Res.string.settings_logs_empty),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.textMuted
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp + shellBottomInset),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredLogs, key = { it.id }) { logEntry ->
                    LogEntryCard(entry = logEntry)
                }
            }
        }
    }
}

@Composable
private fun LogFilterChip(
    label: String,
    selected: Boolean,
    count: Int,
    chipColor: Color = AppTheme.colors.primary,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) chipColor.copy(alpha = 0.2f) else AppTheme.colors.surfaceVariant.copy(alpha = 0.3f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = "$label ($count)",
            style = AppTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) chipColor else AppTheme.colors.onSurface
        )
    }
}

@Composable
private fun LogEntryCard(entry: LogEntry) {
    var expanded by remember { mutableStateOf(false) }

    val severityColor = when (entry.severity) {
        Severity.Error, Severity.Assert -> Color(0xFFE53935)
        Severity.Warn -> Color(0xFFFB8C00)
        Severity.Info -> AppTheme.colors.primary
        Severity.Debug -> AppTheme.colors.textMuted
        Severity.Verbose -> AppTheme.colors.textMuted.copy(alpha = 0.7f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AppTheme.colors.surfaceVariant.copy(alpha = 0.35f))
            .clickable(enabled = entry.throwable != null) { expanded = !expanded }
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Severity label
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(severityColor.copy(alpha = 0.18f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = entry.severity.name.take(4),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = severityColor
                    )
                }

                // Timestamp
                Text(
                    text = entry.formattedTime,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = AppTheme.colors.textMuted
                )

                // Tag
                Text(
                    text = "[${entry.tag}]",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.onSurface
                )
            }

            // Message
            Text(
                text = entry.message,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = AppTheme.colors.onSurface,
                lineHeight = 16.sp
            )

            // Throwable toggle / stack trace
            if (entry.throwable != null) {
                Text(
                    text = if (expanded) "▲ Hide stack trace" else "▼ Show stack trace (${entry.throwable::class.simpleName})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = severityColor
                )
                AnimatedVisibility(visible = expanded) {
                    Text(
                        text = entry.throwable.stackTraceToString(),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AppTheme.colors.textMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .padding(6.dp)
                    )
                }
            }
        }
    }
}
