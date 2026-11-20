/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.core.ui.component.cards

import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.geometry.Offset

import org.spotube.spotube.core.ui.theming.engine.AppComponents
import org.spotube.spotube.core.ui.theming.engine.AppSlots
import org.spotube.spotube.core.ui.theming.engine.AppState
import org.spotube.spotube.core.ui.theming.engine.ContentSlots
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composeunstyled.LocalTextStyle
import coil3.compose.AsyncImage
import org.spotube.spotube.core.ui.component.LocalAnimatedVisibilityScope
import org.spotube.spotube.core.ui.component.LocalSharedTransitionScope
import org.spotube.spotube.core.ui.component.sharedElementOrNone
import org.spotube.spotube.core.ui.misc.TextWithShimmer
import org.spotube.spotube.core.ui.misc.shimmerApply
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.spotube.spotube.resources.iconsax.IconsaxMusic
import org.spotube.spotube.resources.iconsax.IconsaxPause
import org.spotube.spotube.resources.iconsax.IconsaxPlay
import org.jetbrains.compose.resources.stringResource
import org.spotube.spotube.core.ui.theming.AppIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumListTile
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumBevelBrush
import org.spotube.millennium.theme.millenniumPanelBrush
import org.spotube.millennium.theme.millenniumSurface
import spotube.composeapp.generated.resources.*

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayableListTile(
    title: String,
    subtitle: String? = null,
    imageURL: String? = null,
    isPlaying: Boolean = false,
    onClick: (() -> Unit)? = null,
    onPlay: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    sharedElementKey: String? = null,
    playLoading: Boolean = false,
    addToQueueLoading: Boolean = false,
    imageModel: Any? = imageURL,
    imageShape: Shape? = null,
    placeholderIcon: ImageVector = Iconsax.IconsaxMusic,
    artwork: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    isBlacklisted: Boolean = false,
    compact: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onRightClick: ((Offset) -> Unit)? = null
) {
    val slot = AppComponents.slot(ContentSlots.MediaListTile)
    // Background follows "playing"; border and title colour also follow "blacklisted".
    val look = slot[if (isPlaying) AppState.Selected else AppState.Default]
    val tone = slot[if (isBlacklisted) AppState.Error else if (isPlaying) AppState.Selected else AppState.Default]
    val click = onClick ?: onPlay
    MillenniumListTile(
        modifier = if (onLongClick == null) modifier else modifier.combinedClickable(onClick = { click?.invoke() }, onLongClick = onLongClick),
        onClick = click.takeIf { onLongClick == null },
        onRightClick = onRightClick,
        selected = isPlaying || isSelected,
        background = look.background,
        shape = look.shape,
        borderColor = tone.borderColor,
        titleForeground = tone.titleForeground,
        borderWidth = look.borderWidth,
        contentPadding = PaddingValues(if (compact) look.compactPadding else look.padding),
        minimumHeight = if (compact) look.compactMinimumHeight else look.minimumHeight,
        leading = {
            MediaArtwork(
                imageModel, title,
                Modifier.size(if (compact) look.compactArtworkSize else look.artworkSize).sharedElementOrNone(
                    sharedElementKey,
                    LocalSharedTransitionScope.current,
                    LocalAnimatedVisibilityScope.current
                ),
                imageShape ?: AppComponents.slot(ContentSlots.MediaArtwork)[AppState.Default].shape, placeholderIcon, artwork
            )
        },
        title = {
            TextWithShimmer(
                title,
                style = look.titleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        subtitle = subtitle?.let { text ->
            {
                TextWithShimmer(
                    text,
                    style = look.subtitleStyle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        trailing = if (onPlay != null || onAddToQueue != null || trailingContent != null) {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(look.actionSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    trailingContent?.invoke()
                    if (onAddToQueue != null && !isPlaying) {
                        MillenniumIconButton(
                            onClick = onAddToQueue,
                            loading = addToQueueLoading,
                            modifier = Modifier.size(if (compact) look.compactActionSize else look.actionSize),
                            style = MillenniumIconButtonStyle.Secondary
                        ) {
                            AppIcon(
                                Iconsax.IconsaxAddSquare,
                                stringResource(Res.string.player_add_to_queue_content_description)
                            )
                        }
                    }
                    if (onPlay != null) {
                        MillenniumIconButton(
                            onClick = onPlay,
                            loading = playLoading,
                            modifier = Modifier.size(if (compact) look.compactActionSize else look.actionSize),
                            style = MillenniumIconButtonStyle.Primary
                        ) {
                            AppIcon(
                                if (isPlaying) Iconsax.IconsaxPause else Iconsax.IconsaxPlay,
                                stringResource(if (isPlaying) Res.string.player_pause_content_description else Res.string.player_play_content_description),
                                tint = LocalTextStyle.current.color
                            )
                        }
                    }
                }
            }
        } else null
    )
}

@Composable
internal fun MediaArtwork(
    model: Any?,
    title: String,
    modifier: Modifier,
    shape: Shape? = null,
    placeholderIcon: ImageVector = Iconsax.IconsaxMusic,
    artwork: (@Composable () -> Unit)? = null
) {
    val look = AppComponents.slot(ContentSlots.MediaArtwork)[AppState.Default]
    val shape = shape ?: look.shape
    Box(
        modifier.millenniumSurface(
            background = look.background,
            shape = shape,
            borderColor = look.borderColor,
            borderWidth = look.borderWidth
        ).clip(shape).shimmerApply(),
        contentAlignment = Alignment.Center
    ) {
        if (artwork != null) artwork()
        else if (model != null) AsyncImage(
            model,
            title,
            Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        else AppIcon(
            placeholderIcon,
            null,
            modifier = Modifier.size(look.placeholderSize),
            tint = look.placeholderTint
        )
    }
}
