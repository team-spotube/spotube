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

package org.spotube.spotube.core.ui.component.cards

import org.spotube.spotube.core.ui.theming.engine.AppComponents
import org.spotube.spotube.core.ui.theming.engine.AppSlots
import org.spotube.spotube.core.ui.theming.engine.AppState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape

import org.spotube.spotube.core.ui.theming.AppTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.spotube.spotube.core.ui.misc.TextWithShimmer
import org.spotube.spotube.core.ui.misc.shimmerApply
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.User

@Composable
fun AvatarCard(
    title: String,
    subtitle: String? = null,
    imageURL: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isBlacklisted: Boolean = false,
    listMode: Boolean = false,
    /** Raw artwork (e.g. embedded cover bytes) that wins over [imageURL] when not null. */
    imageModel: Any? = null
) {
    val artworkModel = imageModel ?: imageURL
    if (listMode) {
        AvatarListTile(title, subtitle, imageURL, onClick, modifier, isBlacklisted, imageModel = artworkModel)
        return
    }

    val infoLook = AppComponents.slot(AppSlots.MediaCardInfo)[AppState.Default]
    MediaCardSurface(modifier, onClick, isBlacklisted = isBlacklisted) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                MediaArtwork(
                    model = artworkModel,
                    title = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .shimmerApply(),
                    shape = CircleShape,
                    placeholderIcon = Iconsax.User
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(infoLook.padding)
            ) {
                TextWithShimmer(
                    text = title,
                    style = infoLook.titleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                subtitle?.let {
                    Spacer(Modifier.height(infoLook.spacing))
                    TextWithShimmer(
                        text = subtitle,
                        style = infoLook.subtitleStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
