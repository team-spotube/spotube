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

package dev.krtirtho.spotube.modules.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioStream
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import dev.krtirtho.spotube.core.ui.base.PrimaryButton
import dev.krtirtho.spotube.core.ui.base.SecondaryButton
import dev.krtirtho.spotube.core.ui.base.ThemedDialog
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxCheckCircle
import dev.krtirtho.spotube.resources.iconsax.IconsaxDirectboxReceive
import dev.krtirtho.spotube.core.server.CacheEntry

@Composable
fun PlayerOptionDialogs(viewModel: PlayerOptionsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isSleepTimerDialogOpen) {
        SleepTimerDialog(
            minutes = uiState.sleepTimerMinutes,
            remainingMs = uiState.sleepTimerRemainingMs,
            onMinutesChange = viewModel::updateSleepTimerMinutes,
            onStart = viewModel::startSleepTimer,
            onCancelTimer = viewModel::cancelSleepTimer,
            onDismiss = viewModel::dismissSleepTimerDialog,
        )
    }

    uiState.trackDetails?.let { details ->
        TrackDetailsDialog(
            details = details,
            onDismiss = viewModel::dismissTrackDetails,
        )
    }
}

@Composable
private fun SleepTimerDialog(
    minutes: String,
    remainingMs: Long?,
    onMinutesChange: (String) -> Unit,
    onStart: () -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit,
) {
    ThemedDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Sleep timer", style = MaterialTheme.typography.titleLarge)
        },
        actions = {
            if (remainingMs != null) {
                SecondaryButton(onClick = onCancelTimer) { Text("Cancel timer") }
            }
            SecondaryButton(onClick = onDismiss) { Text("Close") }
            PrimaryButton(
                onClick = onStart,
                enabled = minutes.toLongOrNull()?.let { it in 1L..600L } == true,
            ) {
                Text("Start")
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (remainingMs != null) {
                Text(
                    text = "Playback will pause in ${formatRemainingTime(remainingMs)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = "Pause playback after the selected number of minutes.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            OutlinedTextField(
                value = minutes,
                onValueChange = onMinutesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Minutes") },
                supportingText = { Text("Choose between 1 and 600 minutes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        }
    }
}

@Composable
private fun TrackDetailsDialog(
    details: PlayerTrackDetails,
    onDismiss: () -> Unit,
) {
    ThemedDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Track details", style = MaterialTheme.typography.titleLarge)
                Text(
                    details.track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            SecondaryButton(onClick = onDismiss) { Text("Close") }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (details.isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            if (details.localCacheEntry != null) {
                LocalCacheBanner(details.localCacheEntry)
            }
            if (details.localCacheEntry == null && details.activeStreamMatchesPreference == false) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Text(
                        "The playback URL was resolved with different streaming settings. The highlighted stream matches your current preference; the active URL refreshes on the next stream request.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }

            TrackMetadataSection(details.track)

            Text(
                "Matched audio source",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            val source = details.matchedSource
            if (details.isLoading) {
                Text("Loading matched source…", style = MaterialTheme.typography.bodySmall)
            } else if (source == null) {
                Text(
                    details.error ?: if (details.localCacheEntry != null) {
                        "A matched remote source is not needed while playing the local cached file."
                    } else {
                        "No matched source is available."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                DetailRow("Title", source.title)
                DetailRow("ID", source.id)
                DetailRow("Artist", source.artist)
                DetailRow("Album", source.album)
                DetailRow("Confidence", "${(source.confidence * 100).toInt()}%")
                DetailRow("External URI", source.externalUri)
            }

            Text(
                "Available streams",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            when {
                details.localCacheEntry != null && details.streams.isEmpty() -> Text(
                    "This track is being served from the local cache. Stream variants are not used for this playback.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                details.isLoading -> Text("Loading streams…", style = MaterialTheme.typography.bodySmall)
                details.streams.isEmpty() -> Text(
                    details.error ?: "No streams are available for this source.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> details.streams.forEach { stream ->
                    val isSelected = stream == details.preferredStream
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = streamFormatAndQuality(stream),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Iconsax.IconsaxCheckCircle,
                                        contentDescription = "Currently selected stream",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            Text(
                                "${stream.protocol} • ${stream.codec} • ${stream.container}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                stream.url,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (isSelected) {
                                Text(
                                    "Matches your streaming format & quality preference",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            details.activeStreamUrl?.let { activeUrl ->
                DetailRow("Active playback URL", activeUrl)
            }
        }
    }
}

@Composable
private fun LocalCacheBanner(entry: CacheEntry) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Iconsax.IconsaxDirectboxReceive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Served from local cache",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    "${entry.filename} • ${formatFileSize(entry.sizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun TrackMetadataSection(track: MetadataTrack) {
    Text(
        "Metadata track",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
    DetailRow("ID", track.id)
    DetailRow("Title", track.title)
    DetailRow("Artists", track.artists.joinToString { it.name })
    DetailRow("Album", track.album?.title)
    DetailRow("Album ID", track.album?.id)
    DetailRow("Album type", track.album?.albumType?.name)
    DetailRow("Album release date", track.album?.releaseDate)
    DetailRow("Album track count", track.album?.trackCount?.toString())
    DetailRow("Album genres", track.album?.genres?.joinToString())
    DetailRow("Duration", formatRemainingTime(track.durationMs))
    DetailRow("Track number", track.trackNumber?.toString())
    DetailRow("Disc number", track.discNumber?.toString())
    DetailRow("Explicit", track.explicit?.toString())
    DetailRow("Popularity", track.popularity?.toString())
    DetailRow("ISRC", track.isrcCode)
    DetailRow("External URI", track.externalUri)
    DetailRow("Thumbnail URLs", track.thumbnails?.joinToString { it.url })
}

@Composable
private fun DetailRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            label,
            modifier = Modifier.weight(0.38f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            modifier = Modifier.weight(0.62f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun streamFormatAndQuality(stream: AudioStream): String = when (stream) {
    is AudioStream.Lossy -> "${stream.bitrate / 1_000} kbps"
    is AudioStream.Lossless -> {
        val rate = if (stream.sampleRate % 1_000 == 0) {
            "${stream.sampleRate / 1_000}"
        } else {
            "${stream.sampleRate / 1_000.0}"
        }
        "$rate kHz • ${stream.channels} ch"
    }
}

private fun formatRemainingTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1_000).coerceAtLeast(0)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}

private fun formatFileSize(sizeBytes: Long): String = when {
    sizeBytes >= 1024L * 1024L -> "${sizeBytes / (1024L * 1024L)} MB"
    sizeBytes >= 1024L -> "${sizeBytes / 1024L} KB"
    else -> "$sizeBytes B"
}
