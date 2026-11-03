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

package org.spotube.spotube.core.ui.coverflow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import kotlin.math.abs

@Composable
internal fun CoverflowItem(
    geometry: Geometry,
    onClickHandler: () -> Unit = {},
    onSelectedHandler: (Boolean) -> Unit,
    content: @Composable () -> Unit) {
    var horizontalPosition by remember { mutableStateOf<Float?>(null) }
    val distanceToCenter = geometry.distanceToCenter(horizontalPosition ?: 0f)

    Box(
        modifier = Modifier
            .width(with(LocalDensity.current) { geometry.coverOffset.toDp() })
            .zIndex(1f - abs(distanceToCenter))
            .onGloballyPositioned { coordinates ->
                horizontalPosition = coordinates.positionInParent().x
            },
        contentAlignment = Alignment.Center) {
        if (horizontalPosition != null) {
            onSelectedHandler(geometry.isSelected(horizontalPosition ?: 0f))

            Box(
                modifier = Modifier
                    .requiredSize(with(LocalDensity.current) { geometry.coverSize.toDp() })
                    .graphicsLayer(
                        rotationY = geometry.rotation(distanceToCenter),
                        translationX = geometry.translationX(distanceToCenter),
                        scaleY = geometry.scale(distanceToCenter),
                        scaleX = geometry.scale(distanceToCenter)),
                propagateMinConstraints = true) {
                val interactionSource = remember { MutableInteractionSource() }
                if (geometry.params.mirror) {
                    Mirror(
                        modifier = Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClickHandler),
                        coverSize = geometry.coverSize) {
                        content()
                    }
                } else {
                    Box(
                        modifier = Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClickHandler)) {
                        content()
                    }
                }
            }
        }
    }
}
