/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.core.ui.component.cards

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
import org.spotube.millennium.composables.MillenniumIcon
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
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 360.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(64.dp).millenniumSurface(
                    background = millenniumBevelBrush(MillenniumTheme.colors),
                    shape = RoundedCornerShape(14.dp),
                    borderColor = MillenniumTheme.colors.border,
                    borderWidth = 1.dp
                ),
                contentAlignment = Alignment.Center
            ) {
                MillenniumIcon(
                    icon,
                    null,
                    modifier = Modifier.size(30.dp),
                    tint = AppTheme.colors.onSurfaceVariant
                )
            }
            Text(title, style = AppTheme.typography.titleMedium, textAlign = TextAlign.Center)
            if (description != null) Text(
                description,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            action?.invoke()
        }
    }
}
