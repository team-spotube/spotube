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

package dev.krtirtho.spotube.core.ui.component.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import dev.krtirtho.spotube.core.ui.base.BaseScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.krtirtho.spotube.core.ui.base.BaseUITheme
import dev.krtirtho.spotube.core.ui.base.LocalBaseUITheme
import dev.krtirtho.spotube.core.ui.base.PrimaryIconButton
import dev.krtirtho.spotube.core.ui.base.SecondaryIconButton
import dev.krtirtho.spotube.core.ui.component.LocalAnimatedVisibilityScope
import dev.krtirtho.spotube.core.ui.component.LocalSharedTransitionScope
import dev.krtirtho.spotube.core.ui.component.sharedElementOrNone
import dev.krtirtho.spotube.core.ui.misc.TextWithShimmer
import dev.krtirtho.spotube.core.ui.misc.shimmerApply
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxAddSquare
import dev.krtirtho.spotube.resources.iconsax.IconsaxPause
import dev.krtirtho.spotube.resources.iconsax.IconsaxPlay

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayableCard(
    title: String,
    subtitle: String? = null,
    imageURL: String? = null,
    isPlaying: Boolean = false,
    onClick: (() -> Unit)? = null,
    onPlay: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    sharedElementKey: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = { onClick?.invoke() }, enabled = true)
            .width(160.dp)
            .hoverable(interactionSource = interactionSource),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().shimmerApply()) {
                AsyncImage(
                    model = imageURL,
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .sharedElementOrNone(
                            key = sharedElementKey,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                        ),
                    contentScale = ContentScale.Crop,
                )

                if (onPlay != null || onAddToQueue != null)
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        val isHovered by interactionSource.collectIsHoveredAsState()
                        this@Column.AnimatedVisibility(
                            visible = isHovered,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                onAddToQueue?.let {
                                    SecondaryIconButton(
                                        onClick = onAddToQueue,
                                        modifier = Modifier.size(30.dp),
                                        theme = LocalBaseUITheme.current.iconButtons.secondary.copy(
                                            shape = BaseUITheme.InteractionState.fromSingleValue(
                                                CircleShape
                                            )
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Iconsax.IconsaxAddSquare,
                                            contentDescription = "Add to queue",
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                onPlay?.let {
                                    PrimaryIconButton(
                                        onClick = onPlay,
                                        modifier = Modifier.size(30.dp),
                                        theme = LocalBaseUITheme.current.iconButtons.secondary.copy(
                                            shape = BaseUITheme.InteractionState.fromSingleValue(
                                                CircleShape
                                            )
                                        )

                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) {
                                                Iconsax.IconsaxPause
                                            } else Iconsax.IconsaxPlay,
                                            contentDescription = "Play",
                                        )
                                    }
                                }
                            }
                        }
                    }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                TextWithShimmer(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                subtitle?.let {
                    TextWithShimmer(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

    }
}


@PreviewLightDark
@Composable
private fun PlayableCardPreview() {
    BaseScaffold {
        PlayableCard(
            title = "Sample Title",
            subtitle = "Sample Subtitle",
            imageURL = "https://placehold.co/600x400",
            isPlaying = true,
            onClick = {},
            onPlay = {},
            onAddToQueue = {}
        )
    }
}
