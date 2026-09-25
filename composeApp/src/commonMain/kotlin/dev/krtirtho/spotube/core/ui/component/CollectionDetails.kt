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

package dev.krtirtho.spotube.core.ui.component

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.krtirtho.spotube.core.ui.base.ButtonGroup
import dev.krtirtho.spotube.core.ui.base.ButtonGroupDivider
import dev.krtirtho.spotube.core.ui.base.Card
import dev.krtirtho.spotube.core.ui.base.GroupIconButton
import dev.krtirtho.spotube.core.ui.base.OutlineButton
import dev.krtirtho.spotube.core.ui.base.PrimaryButton
import dev.krtirtho.spotube.core.ui.misc.TextWithShimmer
import dev.krtirtho.spotube.core.ui.misc.shimmerApply
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxAddSquare
import dev.krtirtho.spotube.resources.iconsax.IconsaxEdit
import dev.krtirtho.spotube.resources.iconsax.IconsaxHeart
import dev.krtirtho.spotube.resources.iconsax.IconsaxHeart2
import dev.krtirtho.spotube.resources.iconsax.IconsaxPauseCircle
import dev.krtirtho.spotube.resources.iconsax.IconsaxPlayCircle2
import dev.krtirtho.spotube.resources.iconsax.IconsaxShuffle
import dev.krtirtho.spotube.resources.iconsax.User
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionDetails(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    imageURL: String,
    imageResource: DrawableResource? = null,
    ownerName: String,
    ownerImageURL: String?,
    onOwnerClick: () -> Unit,
    onPlay: () -> Unit,
    onShufflePlay: () -> Unit,
    onAddToQueue: () -> Unit,
    isPlaying: Boolean = false,
    /** Guest in a jam session: play/shuffle are replaced by Add to Jam. */
    isJamGuest: Boolean = false,
    onAddToJam: (() -> Unit)? = null,
    isFollowing: Boolean = false,
    onFollowClick: () -> Unit = { },
    showFollowButton: Boolean = true,
    onEdit: (() -> Unit)? = null,
    sharedElementKey: String? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isCompact = maxWidth < 600.dp

        val sharedTransitionScope = LocalSharedTransitionScope.current
        val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

        val playPauseButton = @Composable {
            if (isJamGuest) {
                if (onAddToJam != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        PrimaryButton(
                            modifier = if (isCompact) Modifier.weight(1f) else Modifier,
                            onClick = onAddToJam!!,
                        ) {
                            Icon(
                                imageVector = Iconsax.IconsaxAddSquare,
                                contentDescription = "Add to Jam",
                            )
                            TextWithShimmer(
                                text = "Add to Jam",
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val modifier = if (isCompact) {
                        Modifier.weight(1f)
                    } else {
                        Modifier
                    }

                    PrimaryButton(
                        modifier = modifier,
                        onClick = onPlay,
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Iconsax.IconsaxPauseCircle else Iconsax.IconsaxPlayCircle2,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                        )
                        TextWithShimmer(
                            text = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                    OutlineButton(
                        modifier = modifier,
                        onClick = onShufflePlay
                    ) {
                        Icon(imageVector = Iconsax.IconsaxShuffle, contentDescription = "Shuffle play")
                        TextWithShimmer(
                            text = "Shuffle",
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }

        val actions = @Composable {
            ButtonGroup {
                GroupIconButton(onClick = onAddToQueue) {
                    Icon(
                        imageVector = Iconsax.IconsaxAddSquare,
                        contentDescription = "Add to queue"
                    )
                }
                ButtonGroupDivider()
                if (showFollowButton) {
                    if (isFollowing) {
                        GroupIconButton(onClick = onFollowClick) {
                            Icon(
                                imageVector = Iconsax.IconsaxHeart2,
                                contentDescription = "Unfollow",
                            )
                        }
                    } else {
                        GroupIconButton(onClick = onFollowClick) {
                            Icon(
                                imageVector = Iconsax.IconsaxHeart,
                                contentDescription = "Follow",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (onEdit != null) {
                    ButtonGroupDivider()
                    GroupIconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Iconsax.IconsaxEdit,
                            contentDescription = "Edit playlist",
                        )
                    }
                }
            }
        }
        val ownerInfo = @Composable {
            Row(
                modifier = Modifier.clickable(onClick = onOwnerClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (ownerImageURL != null) {
                    AsyncImage(
                        model = ownerImageURL,
                        contentDescription = ownerName,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .shimmerApply(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Iconsax.User,
                        contentDescription = ownerName,
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            )
                            .padding(6.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                TextWithShimmer(
                    text = "By $ownerName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val artwork = @Composable {
            Box(
                modifier = Modifier
                    .size(if (isCompact) 100.dp else 200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .sharedElementOrNone(
                        key = sharedElementKey,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                    .shimmerApply(),
                contentAlignment = Alignment.Center,
            ) {
                if (imageURL.isNotBlank()) {
                    AsyncImage(
                        model = imageURL,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else if (imageResource != null) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(imageResource),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = title.take(1).ifBlank { "?" }.uppercase(),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }


        val metadata = @Composable {
            Column {
                TextWithShimmer(
                    text = title,
                    style = if (isCompact) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.headlineLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (description.isNotBlank()) {
                    TextWithShimmer(
                        text = description,
                        style = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            if (isCompact) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        artwork()
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            metadata()
                            ownerInfo()
                            actions()
                        }
                    }
                    playPauseButton()
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    artwork()

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        metadata()
                        ownerInfo()
                        actions()
                        playPauseButton()
                    }
                }
            }
        }
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview
fun CollectionDetailsPreview() {
    CollectionDetails(
        title = "My Playlist",
        description = "A collection of my favorite songs.",
        imageURL = "https://example.com/playlist.jpg",
        ownerName = "John Doe",
        ownerImageURL = "https://example.com/john.jpg",
        onOwnerClick = {},
        onPlay = {},
        onShufflePlay = {},
        onAddToQueue = {},
    )
}
