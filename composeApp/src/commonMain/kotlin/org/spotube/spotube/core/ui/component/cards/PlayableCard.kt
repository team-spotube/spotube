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


import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import com.composeunstyled.LocalTextStyle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape

import org.spotube.spotube.core.ui.theming.AppTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.spotube.spotube.core.ui.component.LocalAnimatedVisibilityScope
import org.spotube.spotube.core.ui.component.LocalSharedTransitionScope
import org.spotube.spotube.core.ui.component.sharedElementOrNone
import org.spotube.spotube.core.ui.misc.TextWithShimmer
import org.spotube.spotube.core.ui.misc.shimmerApply
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.spotube.spotube.resources.iconsax.IconsaxPause
import org.spotube.spotube.resources.iconsax.IconsaxPlay
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.player_add_to_queue_content_description
import spotube.composeapp.generated.resources.player_pause_content_description
import spotube.composeapp.generated.resources.player_play_content_description
import spotube.composeapp.generated.resources.media_preview_title
import spotube.composeapp.generated.resources.media_preview_subtitle

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
    listMode: Boolean = false,
    playLoading: Boolean = false,
    addToQueueLoading: Boolean = false
) {
    if (listMode) {
        PlayableListTile(
            title,
            subtitle,
            imageURL,
            isPlaying,
            onClick,
            onPlay,
            onAddToQueue,
            modifier,
            sharedElementKey,
            playLoading = playLoading,
            addToQueueLoading = addToQueueLoading
        )
        return
    }
    val touchInput = LocalInputModeManager.current.inputMode == InputMode.Touch
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    MediaCardSurface(
        modifier = modifier.width(160.dp),
        onClick = onClick ?: onPlay,
        interactive = onClick != null || onPlay != null || onAddToQueue != null
    ) { highlighted ->
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().shimmerApply()) {
                MediaArtwork(
                    model = imageURL,
                    title = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .sharedElementOrNone(
                            key = sharedElementKey,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        ),
                    shape = RoundedCornerShape(8.dp)
                )

                if (onPlay != null || onAddToQueue != null)
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        this@Column.AnimatedVisibility(
                            visible = highlighted || touchInput || (isPlaying && onPlay != null) ||
                                playLoading || addToQueueLoading,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                // While this collection is playing the play/pause toggle stays
                                // visible and the add-to-queue action is hidden entirely.
                                onAddToQueue?.takeIf { !isPlaying }?.let {
                                    MillenniumIconButton(
                                        onClick = onAddToQueue,
                                        loading = addToQueueLoading,
                                        modifier = Modifier.size(34.dp),
                                        style = MillenniumIconButtonStyle.Secondary
                                    ) {
                                        MillenniumIcon(
                                            imageVector = Iconsax.IconsaxAddSquare,
                                            contentDescription = stringResource(
                                                Res.string.player_add_to_queue_content_description
                                            ),
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                onPlay?.let {
                                    MillenniumIconButton(
                                        onClick = onPlay,
                                        loading = playLoading,
                                        modifier = Modifier.size(34.dp),
                                        style = MillenniumIconButtonStyle.Primary

                                    ) {
                                        MillenniumIcon(
                                            imageVector = if (isPlaying) {
                                                Iconsax.IconsaxPause
                                            } else Iconsax.IconsaxPlay,
                                            contentDescription = stringResource(
                                                if (isPlaying) {
                                                    Res.string.player_pause_content_description
                                                } else {
                                                    Res.string.player_play_content_description
                                                }
                                            ),
                                            tint = LocalTextStyle.current.color
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
                    style = AppTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                subtitle?.let {
                    TextWithShimmer(
                        text = subtitle,
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

    }
}

@PreviewLightDark
@Composable
private fun PlayableCardPreview() {
    Box {
        PlayableCard(
            title = stringResource(Res.string.media_preview_title),
            subtitle = stringResource(Res.string.media_preview_subtitle),
            imageURL = "https://placehold.co/600x400",
            isPlaying = true,
            onClick = {},
            onPlay = {},
            onAddToQueue = {}
        )
    }
}
