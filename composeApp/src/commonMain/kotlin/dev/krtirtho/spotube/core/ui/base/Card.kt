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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusic
import dev.krtirtho.spotube.resources.iconsax.IconsaxPause
import dev.krtirtho.spotube.resources.iconsax.IconsaxPlay

@Composable
fun Card(
    modifier: Modifier = Modifier,
    theme: BaseUITheme.CardTheme? = null,
    content: @Composable () -> Unit,
) {
    val cardTheme = theme ?: LocalBaseUITheme.current.card

    Box(
        modifier = modifier
            .then(
                if (cardTheme.shadow.elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = cardTheme.shadow.elevation,
                        shape = cardTheme.shape,
                        ambientColor = cardTheme.shadow.ambientColor,
                        spotColor = cardTheme.shadow.spotColor,
                    )
                } else Modifier
            )
            .clip(cardTheme.shape)
            .background(cardTheme.background, cardTheme.shape)
            .applySurfaceMaterial(cardTheme.surfaceRole)
            .border(BorderStroke(cardTheme.border.width, cardTheme.border.color), cardTheme.shape)
            .padding(cardTheme.padding),
    ) {
        content()
    }
}

@BaseUIPhonePreview
@Composable
private fun CardPreview() {
    BaseUIPreview {
        var connected by remember { mutableStateOf(false) }
        var playing by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (connected) "Spotify connected" else "Connect a music source",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (connected) {
                            "Streaming, downloads and scrobbling are ready."
                        } else {
                            "Sign in to stream, download and scrobble from your account."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlineButton(onClick = { connected = false }) {
                            Text("Disconnect")
                        }
                        PrimaryButton(onClick = { connected = !connected }) {
                            Text(if (connected) "Connected" else "Connect")
                        }
                    }
                }
            }

            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Iconsax.IconsaxMusic,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Midnight City",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                        )
                        Text(
                            text = "M83 · Hurry Up, We're Dreaming",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    PrimaryIconButton(onClick = { playing = !playing }) {
                        Icon(
                            imageVector = if (playing) Iconsax.IconsaxPause else Iconsax.IconsaxPlay,
                            contentDescription = if (playing) "Pause" else "Play",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
