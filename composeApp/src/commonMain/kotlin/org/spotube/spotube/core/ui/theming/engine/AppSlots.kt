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

package org.spotube.spotube.core.ui.theming.engine

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * The registry of themable in-house components. To make another component themable:
 *   1. add an `@Immutable` props class whose defaults reproduce today's hard-coded look,
 *   2. add an [AppSlot] below (defaults per [AppState] + an `apply` that reads each property with `PropReader`),
 *   3. list it in [AppSlots.all],
 *   4. read `AppComponents.slot(AppSlots.X)[state]` in the composable instead of literals,
 *   5. document it in themes/README.md ("In-house components").
 * Nothing else changes: compile, caching, `rules`/`vars`/`states`, parameters and warnings all come with the slot.
 */

// ---- media card (PlayableCard, AvatarCard, LikedTracksCard) ----------------------------------

/** The card surface. `hovered` also covers keyboard focus-within and `pressed`; `error` is a blacklisted item. */
@Immutable
data class MediaCardRootProps(
    val width: Dp,
    /** Drawn under the content; null is transparent (resting cards have no plate). */
    val background: Brush?,
    /** Cross-faded in over [background] by the hover/focus highlight, so brushes animate without a custom Brush. */
    val highlightBackground: Brush?,
    val shape: Shape,
    val borderColor: Color,
    val borderWidth: Dp,
    val shadowElevation: Dp,
    val innerGlowColor: Color,
    val scale: Float,
    /** Hover/focus fade in milliseconds. */
    val transitionMillis: Int,
    /** Press scale animation in milliseconds. */
    val scaleMillis: Int,
)

/** The picture area of a card and the play / add-to-queue buttons floating over it. */
@Immutable
data class MediaCardArtworkProps(
    val shape: Shape,
    /** Inset of the action buttons from the artwork's corner. */
    val actionPadding: Dp,
    val actionSize: Dp,
    val actionSpacing: Dp,
)

/** The text block under the artwork. */
@Immutable
data class MediaCardInfoProps(
    val padding: PaddingValues,
    val spacing: Dp,
    val titleStyle: TextStyle,
    val subtitleStyle: TextStyle,
)

// ---- floating player (the compact mini player above the bottom bar) ---------------------------

@Immutable
data class FloatingPlayerRootProps(
    val background: Brush,
    val shape: Shape,
    val borderColor: Color,
    val borderWidth: Dp,
    val shadowElevation: Dp,
    val contentPadding: PaddingValues,
    /** Inset from the left and right edges, to line the player up with an inset (floating) dock. Height is unaffected. */
    val horizontalMargin: Dp,
    val progressHeight: Dp,
    /** Space between the progress line and the row below it. */
    val progressSpacing: Dp,
    /** Space between the heart button and the play/pause button. */
    val controlsSpacing: Dp,
)

@Immutable
data class FloatingPlayerArtworkProps(
    val size: Dp,
    val shape: Shape,
    val background: Color,
    val placeholderSize: Dp,
    val placeholderTint: Color,
    /** Space between the artwork and the title block. */
    val spacing: Dp,
)

@Immutable
data class FloatingPlayerInfoProps(
    val titleStyle: TextStyle,
    val subtitleStyle: TextStyle,
)

object AppSlots {
    val MediaCardRoot = AppSlot(
        component = "mediaCard", variant = "root",
        defaults = { b, st ->
            val c = b.colors
            // Hover, press and focus share the lit look; `error` (blacklisted) is a red plate. Focus gets the accent ring.
            val lit = st == AppState.Hovered || st == AppState.Focused || st == AppState.Pressed
            MediaCardRootProps(
                width = 160.dp,
                background = if (st == AppState.Error) SolidColor(c.danger.copy(alpha = 0.12f)) else null,
                highlightBackground = when {
                    !lit -> null
                    b.flat -> SolidColor(c.raised)
                    else -> Brush.verticalGradient(listOf(c.raised, c.surface))
                },
                shape = RoundedCornerShape(10.dp),
                borderColor = when {
                    st == AppState.Focused -> c.accent
                    lit -> c.border
                    else -> c.border.copy(alpha = 0f)
                },
                borderWidth = if (st == AppState.Focused) 2.dp else 1.dp,
                shadowElevation = if (lit) 4.dp else 0.dp,
                innerGlowColor = if (lit) c.highlight.copy(alpha = 0.4f) else c.highlight.copy(alpha = 0f),
                scale = if (st == AppState.Pressed) 0.985f else 1f,
                transitionMillis = 140,
                scaleMillis = 100,
            )
        },
        apply = { p, r ->
            p.copy(
                width = r.dp("width", p.width),
                background = r.brush("background", p.background),
                highlightBackground = r.brush("highlightBackground", p.highlightBackground),
                shape = r.shape("shape", p.shape),
                borderColor = r.color("borderColor", p.borderColor) ?: p.borderColor,
                borderWidth = r.dp("borderWidth", p.borderWidth),
                shadowElevation = r.dp("shadowElevation", p.shadowElevation),
                innerGlowColor = r.color("innerGlowColor", p.innerGlowColor) ?: p.innerGlowColor,
                scale = r.float("scale", p.scale),
                transitionMillis = r.int("transitionMillis", p.transitionMillis).coerceIn(0, 2000),
                scaleMillis = r.int("scaleMillis", p.scaleMillis).coerceIn(0, 2000),
            )
        },
    )

    val MediaCardArtwork = AppSlot(
        component = "mediaCard", variant = "artwork",
        defaults = { _, _ ->
            MediaCardArtworkProps(shape = RoundedCornerShape(8.dp), actionPadding = 8.dp, actionSize = 34.dp, actionSpacing = 8.dp)
        },
        apply = { p, r ->
            p.copy(
                shape = r.shape("shape", p.shape),
                actionPadding = r.dp("actionPadding", p.actionPadding),
                actionSize = r.dp("actionSize", p.actionSize),
                actionSpacing = r.dp("actionSpacing", p.actionSpacing),
            )
        },
    )

    val MediaCardInfo = AppSlot(
        component = "mediaCard", variant = "info",
        defaults = { b, _ ->
            MediaCardInfoProps(
                padding = PaddingValues(12.dp),
                spacing = 4.dp,
                titleStyle = b.typography.titleSmall,
                subtitleStyle = b.typography.bodySmall.copy(color = b.colors.muted),
            )
        },
        apply = { p, r ->
            p.copy(
                padding = r.padding("padding", p.padding),
                spacing = r.dp("spacing", p.spacing),
                titleStyle = r.textStyle("titleStyle", p.titleStyle),
                subtitleStyle = r.textStyle("subtitleStyle", p.subtitleStyle),
            )
        },
    )

    val FloatingPlayerRoot = AppSlot(
        component = "floatingPlayer", variant = "root",
        defaults = { b, _ ->
            FloatingPlayerRootProps(
                background = b.panel,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                borderColor = b.colors.border,
                borderWidth = 1.dp,
                shadowElevation = 0.dp,
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalMargin = 0.dp,
                progressHeight = 2.dp,
                progressSpacing = 10.dp,
                controlsSpacing = 8.dp,
            )
        },
        apply = { p, r ->
            p.copy(
                background = r.brush("background", p.background) ?: p.background,
                shape = r.shape("shape", p.shape),
                borderColor = r.color("borderColor", p.borderColor) ?: p.borderColor,
                borderWidth = r.dp("borderWidth", p.borderWidth),
                shadowElevation = r.dp("shadowElevation", p.shadowElevation),
                contentPadding = r.padding("contentPadding", p.contentPadding),
                horizontalMargin = r.dp("horizontalMargin", p.horizontalMargin),
                progressHeight = r.dp("progressHeight", p.progressHeight),
                progressSpacing = r.dp("progressSpacing", p.progressSpacing),
                controlsSpacing = r.dp("controlsSpacing", p.controlsSpacing),
            )
        },
    )

    val FloatingPlayerArtwork = AppSlot(
        component = "floatingPlayer", variant = "artwork",
        defaults = { b, _ ->
            FloatingPlayerArtworkProps(
                size = 42.dp,
                shape = RoundedCornerShape(6.dp),
                background = b.colors.text.copy(alpha = 0.12f),
                placeholderSize = 22.dp,
                placeholderTint = b.colors.muted,
                spacing = 10.dp,
            )
        },
        apply = { p, r ->
            p.copy(
                size = r.dp("size", p.size),
                shape = r.shape("shape", p.shape),
                background = r.color("background", p.background) ?: p.background,
                placeholderSize = r.dp("placeholderSize", p.placeholderSize),
                placeholderTint = r.color("placeholderTint", p.placeholderTint) ?: p.placeholderTint,
                spacing = r.dp("spacing", p.spacing),
            )
        },
    )

    val FloatingPlayerInfo = AppSlot(
        component = "floatingPlayer", variant = "info",
        defaults = { b, _ ->
            FloatingPlayerInfoProps(
                titleStyle = b.typography.bodyMedium.copy(color = b.colors.text),
                subtitleStyle = b.typography.bodySmall.copy(color = b.colors.muted),
            )
        },
        apply = { p, r ->
            p.copy(
                titleStyle = r.textStyle("titleStyle", p.titleStyle),
                subtitleStyle = r.textStyle("subtitleStyle", p.subtitleStyle),
            )
        },
    )

    /** Every slot the compiler resolves. A slot missing here fails fast on first lookup. */
    val all: List<AppSlot<*>> by lazy {
        listOf(
            MediaCardRoot, MediaCardArtwork, MediaCardInfo,
            FloatingPlayerRoot, FloatingPlayerArtwork, FloatingPlayerInfo,
        ) + PlayersSlots.all + ContentSlots.all + ScreensSlots.all
    }
}
