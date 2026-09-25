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

package dev.krtirtho.spotube.modules.settings.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioFormat
import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioQuality
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import dev.krtirtho.spotube.core.ui.base.Card

private val standardLossyQualities = listOf(
    AudioQuality.Lossy(bitrate = 44_000),
    AudioQuality.Lossy(bitrate = 96_000),
    AudioQuality.Lossy(bitrate = 128_000),
    AudioQuality.Lossy(bitrate = 256_000),
)

internal val streamingFormatPresets = listOf(
    AudioFormat(
        codec = "opus",
        container = "webm",
        qualities = standardLossyQualities,
    ),
    AudioFormat(
        codec = "aac",
        container = "mp4",
        qualities = standardLossyQualities,
    ),
    AudioFormat(
        codec = "vorbis",
        container = "ogg",
        qualities = standardLossyQualities,
    ),
)

internal val downloadFormatPresets = listOf(
    AudioFormat(
        codec = "aac",
        container = "mp4",
        qualities = standardLossyQualities,
    ),
    AudioFormat(
        codec = "mp3",
        container = "mp3",
        qualities = standardLossyQualities,
    ),
    AudioFormat(
        codec = "opus",
        container = "webm",
        qualities = standardLossyQualities,
    ),
    AudioFormat(
        codec = "flac",
        container = "flac",
        qualities = listOf(
            AudioQuality.Lossless(sampleRate = 44_100, channels = 2),
            AudioQuality.Lossless(sampleRate = 48_000, channels = 2),
        ),
    ),
)

internal fun LazyListScope.settingsSectionHeader(title: StringResource) {
    item {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
internal fun SettingsItemIcon(
    imageVector: ImageVector,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = tint.copy(alpha = 0.12f),
    ) {
        Box(modifier = Modifier.padding(8.dp)) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = tint,
            )
        }
    }
}

internal fun availableAudioFormats(current: AudioFormat, presets: List<AudioFormat>): List<AudioFormat> {
    val options = mutableListOf(current)
    presets.forEach { preset ->
        val alreadyIncluded = options.any {
            it.codec.equals(preset.codec, ignoreCase = true) &&
                it.container.equals(preset.container, ignoreCase = true)
        }
        if (!alreadyIncluded) {
            options += preset
        }
    }
    return options
}

internal fun availableAudioQualities(format: AudioFormat, current: AudioQuality): List<AudioQuality> {
    val options = format.qualities.toMutableList()
    if (options.none { it == current }) {
        options += current
    }
    return options
}

internal fun AudioFormat.displayLabel(): String {
    return "${codec.uppercase()} in ${container.uppercase()}"
}

internal fun AudioFormat.preferredQuality(): AudioQuality {
    return qualities.lastOrNull() ?: AudioQuality.Lossy(bitrate = 256_000)
}

internal fun AudioFormat.resolveQuality(preferred: AudioQuality): AudioQuality {
    return qualities.firstOrNull { it == preferred } ?: preferredQuality()
}

internal fun AudioQuality.displayLabel(): String {
    return when (this) {
        is AudioQuality.Lossy -> "${bitrate / 1000} kbps"
        is AudioQuality.Lossless -> {
            val sampleRateLabel = if (sampleRate % 1000 == 0) {
                "${sampleRate / 1000}"
            } else {
                "${sampleRate / 1000.0}"
            }
            "$sampleRateLabel kHz • $channels ch"
        }
    }
}

internal fun normalizePath(path: String): String {
    return path.trim().trimEnd('/', '\\')
}

internal fun LazyListScope.settingsSectionCard(
    items: List<@Composable () -> Unit>,
) {
    item {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp)
            ) {
                items.forEachIndexed { index, content ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                    content()
                }
            }
        }
    }
}

