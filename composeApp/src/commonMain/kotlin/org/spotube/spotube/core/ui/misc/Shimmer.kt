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

package org.spotube.spotube.core.ui.misc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.TextAutoSize
import com.composeunstyled.LocalTextStyle
import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumBevelBrush

val LocalSkeletonLoading = compositionLocalOf { false }

@Composable
fun SkeletonTree(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Intercept clicks and touches globally when loading
    val interactionModifier = if (isLoading) {
        modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    event.changes.forEach { it.consume() }
                }
            }
        }
    } else {
        modifier
    }

    CompositionLocalProvider(LocalSkeletonLoading provides isLoading) {
        if (isLoading) {
            // Valentinilk's synchronized hardware-accelerated shimmer loop
            Box(modifier = interactionModifier.shimmer(rememberShimmer(shimmerBounds = ShimmerBounds.View))) {
                content()
            }
        } else {
            Box(modifier = interactionModifier) {
                content()
            }
        }
    }
}

fun Modifier.shimmerApply(
    color: Color? = null // defaults to Millennium's recessed skeleton fill
): Modifier = composed {
    val isTreeLoading = LocalSkeletonLoading.current
    val skeletonBrush = millenniumBevelBrush(MillenniumTheme.colors, sunken = true)

    when {
        !isTreeLoading -> this
        color != null -> this.background(color)
        else -> this.background(skeletonBrush)
    }
}

@Composable
fun TextWithShimmer(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    autoSize: TextAutoSize? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current) {
    // While a skeleton tree is loading, the placeholder glyphs must not be readable:
    // only the shimmer bars (from `shimmerApply`) should show through.
    val isLoadingSkeleton = LocalSkeletonLoading.current
    Text(
        text = text,
        color = if (isLoadingSkeleton) Color.Transparent else color,
        modifier = Modifier.shimmerApply().then(modifier),
        autoSize = autoSize,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign ?: TextAlign.Start,
        lineHeight = lineHeight,
        overflow = overflow,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}