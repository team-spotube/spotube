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

package org.spotube.spotube.core.ui.component

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.core.ui.theming.LocalModernFonts

/*
 * Small pieces of the Modern interface style that screens use directly. Each one falls back to
 * the Classic rendering when the Modern style is off.
 */

/**
 * Tint for an icon inside a button. Modern draws primary buttons in the text colour, so the
 * icon follows the button's label colour instead of Millennium's default (the text colour,
 * which would vanish). Classic returns null and keeps Millennium's own tint.
 */
@Composable
fun buttonIconTint(): Color? = if (AppTheme.isModern) com.composeunstyled.LocalTextStyle.current.color else null

/** "Library" -> "Library." with the full stop in the accent colour, as page titles are set. */
fun stopped(text: String, accent: Color): AnnotatedString = buildAnnotatedString {
    append(text)
    val last = text.trimEnd().lastOrNull()
    if (last != null && last.isLetterOrDigit()) {
        withStyle(SpanStyle(color = accent)) { append('.') }
    }
}

/**
 * A screen's title in the top bar. Modern sets it with an accent full stop; Classic shows the
 * plain text exactly as before.
 */
@Composable
fun PageTitle(text: String, modifier: Modifier = Modifier) {
    if (AppTheme.isModern) {
        Text(
            text = stopped(text, AppTheme.colors.primary),
            modifier = modifier,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    } else {
        Text(text, modifier = modifier)
    }
}

/** The app's name set as a wordmark in the Modern display face, with the accent stop. */
@Composable
fun ModernWordmark(modifier: Modifier = Modifier, size: Int = 26) {
    val fonts = LocalModernFonts.current
    Text(
        text = stopped("Spotube", AppTheme.colors.primary),
        modifier = modifier,
        style = TextStyle(
            fontFamily = fonts?.display,
            fontWeight = FontWeight.ExtraBold,
            fontSize = size.sp,
            lineHeight = size.sp,
            letterSpacing = (-0.02 * size).sp
        ),
        color = AppTheme.colors.onSurface
    )
}

/**
 * The artwork itself, blurred, lighting the area behind a header, then falling off into the page
 * background so the content below sits on plain graphite. Draws nothing without artwork.
 */
@Composable
fun ArtworkBackdrop(imageURL: String?, modifier: Modifier = Modifier) {
    val background = AppTheme.colors.background
    Box(modifier = modifier.clipToBounds()) {
        if (!imageURL.isNullOrBlank()) {
            AsyncImage(
                model = imageURL,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                // Drawn larger than the area and blurred past its edges, so the glow has no seams.
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = 1.4f; scaleY = 1.4f }
                    .blur(64.dp, BlurredEdgeTreatment.Unbounded)
                    .alpha(0.55f)
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to background.copy(alpha = 0.55f),
                        0.6f to background.copy(alpha = 0.15f),
                        1f to Color.Transparent
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to background.copy(alpha = 0.25f),
                        0.25f to Color.Transparent,
                        0.5f to Color.Transparent,
                        1f to background
                    )
                )
        )
    }
}

/**
 * An [ArtworkBackdrop] for the top of a scrolling screen. It sits behind the top bar and the
 * list, reaching the very top edge, and scrolls away with the list's first item.
 */
@Composable
fun ScrollingArtworkBackdrop(
    imageURL: String?,
    listState: LazyListState,
    height: Dp,
    modifier: Modifier = Modifier
) {
    if (imageURL.isNullOrBlank()) return
    ArtworkBackdrop(
        imageURL = imageURL,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            // Read in the layer, not in composition, so scrolling does not recompose.
            .graphicsLayer {
                translationY = if (listState.firstVisibleItemIndex == 0) {
                    -listState.firstVisibleItemScrollOffset.toFloat()
                } else {
                    -size.height
                }
            }
    )
}
