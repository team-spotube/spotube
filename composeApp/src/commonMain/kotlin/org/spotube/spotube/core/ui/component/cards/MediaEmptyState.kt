/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.core.ui.component.cards

import org.spotube.spotube.core.ui.theming.engine.AppComponents
import org.spotube.spotube.core.ui.theming.engine.AppSlots
import org.spotube.spotube.core.ui.theming.engine.AppState
import org.spotube.spotube.core.ui.theming.engine.ContentSlots
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.core.ui.theming.AppIcon
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumBevelBrush
import org.spotube.millennium.theme.millenniumSurface

@Composable
internal fun MediaEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    val look = AppComponents.slot(ContentSlots.EmptyState)[AppState.Default]
    Box(modifier.fillMaxSize().padding(look.padding), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = look.maxWidth), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(look.spacing)
        ) {
            Box(
                Modifier.size(look.iconBoxSize).millenniumSurface(
                    background = look.iconBoxBackground,
                    shape = look.iconBoxShape,
                    borderColor = look.iconBoxBorderColor,
                    borderWidth = look.iconBoxBorderWidth
                ),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    icon,
                    null,
                    modifier = Modifier.size(look.iconSize),
                    tint = look.iconTint
                )
            }
            Text(title, style = look.titleStyle, textAlign = TextAlign.Center)
            if (description != null) Text(
                description,
                style = look.descriptionStyle,
                textAlign = TextAlign.Center
            )
            action?.invoke()
        }
    }
}
