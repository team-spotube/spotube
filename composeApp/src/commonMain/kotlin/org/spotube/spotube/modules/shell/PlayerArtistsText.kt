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

package org.spotube.spotube.modules.shell

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import com.composeunstyled.Text
import org.spotube.spotube.core.ui.theming.AppTheme

/**
 * Renders a track's artists on a single, ellipsized line. Artists that have an [PlayerArtist.id]
 * are tappable links that open their artist page; local tracks (no ids) render as plain text.
 */
@Composable
internal fun PlayerArtistsText(
    artists: List<PlayerArtist>,
    onArtistClick: (String) -> Unit,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Unspecified) {
    if (artists.isEmpty()) return
    val hoverColor = AppTheme.colors.primary
    val annotatedArtists = buildAnnotatedString {
        artists.forEachIndexed { index, artist ->
            if (index > 0) append(", ")
            val artistId = artist.id
            if (artistId == null) {
                append(artist.name)
            } else {
                withLink(
                    LinkAnnotation.Clickable(
                        tag = artistId,
                        styles = TextLinkStyles(
                            style = SpanStyle(textDecoration = TextDecoration.Underline),
                            hoveredStyle = SpanStyle(
                                color = hoverColor,
                                textDecoration = TextDecoration.Underline),
                        ),
                        linkInteractionListener = { onArtistClick(artistId) },
                    )
                ) {
                    append(artist.name)
                }
            }
        }
    }
    Text(
        text = annotatedArtists,
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
