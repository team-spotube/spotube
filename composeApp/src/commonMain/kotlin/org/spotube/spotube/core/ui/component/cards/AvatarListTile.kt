/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.core.ui.component.cards

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.User

@Composable
fun AvatarListTile(
    title: String,
    subtitle: String? = null,
    imageURL: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isBlacklisted: Boolean = false
) {
    PlayableListTile(
        title = title,
        subtitle = subtitle,
        imageURL = imageURL,
        onClick = onClick,
        modifier = modifier,
        imageShape = CircleShape,
        placeholderIcon = Iconsax.User,
        isBlacklisted = isBlacklisted
    )
}
