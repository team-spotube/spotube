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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Slots for the large (desktop) player, the expanded (full-screen) player and the player queue. See AppSlots.kt for the recipe. */

// ---- large player -----------------------------------------------------------------------------

@Immutable
data class LargePlayerRootProps(
    /** The translucent tint over the blurred backdrop. */
    val tint: Color,
    val blurRadius: Dp,
    val contentPadding: PaddingValues,
)

@Immutable
data class LargePlayerArtworkProps(val size: Dp, val shape: Shape, val background: Color, val spacing: Dp)

@Immutable
data class LargePlayerInfoProps(val titleStyle: TextStyle, val artistStyle: TextStyle, val heartSpacing: Dp)

@Immutable
data class LargePlayerTransportProps(
    val timeStyle: TextStyle,
    val activeTint: Color,
    val inactiveTint: Color,
    /** Width of the volume slider as a fraction of its row. */
    val volumeWidthFraction: Float,
)

// ---- expanded player --------------------------------------------------------------------------

@Immutable
data class ExpandedPlayerRootProps(val contentPadding: PaddingValues, val topSpacing: Dp, val headerStyle: TextStyle)

@Immutable
data class ExpandedPlayerArtworkProps(val widthFraction: Float, val shape: Shape, val background: Color, val padding: PaddingValues)

@Immutable
data class ExpandedPlayerInfoProps(val rowPadding: PaddingValues, val titleStyle: TextStyle, val artistStyle: TextStyle)

@Immutable
data class ExpandedPlayerTransportProps(
    val seekPadding: PaddingValues,
    val timePadding: PaddingValues,
    val timeStyle: TextStyle,
    val controlsPadding: PaddingValues,
    val playIconSize: Dp,
    val activeTint: Color,
    val inactiveTint: Color,
)

@Immutable
data class ExpandedPlayerLyricsProps(
    val outerPadding: PaddingValues,
    val contentPadding: PaddingValues,
    val contentSpacing: Dp,
    val shadowElevation: Dp,
    val previewHeight: Dp,
    val titleStyle: TextStyle,
    val iconTint: Color,
)

@Immutable
data class ExpandedPlayerOptionTileProps(
    val contentPadding: PaddingValues,
    val iconSize: Dp,
    val iconTint: Color,
    val spacing: Dp,
    val labelStyle: TextStyle,
)

// ---- player queue -----------------------------------------------------------------------------

@Immutable
data class PlayerQueueRootProps(
    val contentPadding: PaddingValues,
    val spacing: Dp,
    val filterSpacing: Dp,
    val listPadding: PaddingValues,
    val titleStyle: TextStyle,
    val emptyStyle: TextStyle,
)

/** One queue row; `selected` is the currently playing track. */
@Immutable
data class PlayerQueueRowProps(
    val height: Dp,
    val handleSize: Dp,
    val handleTint: Color,
    val thumbSize: Dp,
    val thumbShape: Shape,
    val thumbBackground: Color,
    val thumbIndexStyle: TextStyle,
    val titleStyle: TextStyle,
    val subtitleStyle: TextStyle,
    val durationStyle: TextStyle,
    val trailingSpacing: Dp,
    val participantSize: Dp,
    val participantBackground: Color,
    val participantStyle: TextStyle,
)

object PlayersSlots {
    val LargePlayerRoot = AppSlot(
        component = "largePlayer", variant = "root",
        defaults = { b, _ -> LargePlayerRootProps(b.colors.surface.copy(alpha = 0.6f), 20.dp, PaddingValues(horizontal = 16.dp, vertical = 10.dp)) },
        apply = { p, r ->
            p.copy(
                tint = r.color("tint", p.tint) ?: p.tint,
                blurRadius = r.dp("blurRadius", p.blurRadius),
                contentPadding = r.padding("contentPadding", p.contentPadding),
            )
        },
    )

    val LargePlayerArtwork = AppSlot(
        component = "largePlayer", variant = "artwork",
        defaults = { b, _ -> LargePlayerArtworkProps(52.dp, RoundedCornerShape(12.dp), b.colors.raised, 12.dp) },
        apply = { p, r ->
            p.copy(
                size = r.dp("size", p.size),
                shape = r.shape("shape", p.shape),
                background = r.color("background", p.background) ?: p.background,
                spacing = r.dp("spacing", p.spacing),
            )
        },
    )

    val LargePlayerInfo = AppSlot(
        component = "largePlayer", variant = "info",
        defaults = { b, _ ->
            LargePlayerInfoProps(b.typography.bodyLarge, b.typography.bodySmall.copy(color = b.colors.muted), 10.dp)
        },
        apply = { p, r ->
            p.copy(
                titleStyle = r.textStyle("titleStyle", p.titleStyle),
                artistStyle = r.textStyle("artistStyle", p.artistStyle),
                heartSpacing = r.dp("heartSpacing", p.heartSpacing),
            )
        },
    )

    val LargePlayerTransport = AppSlot(
        component = "largePlayer", variant = "transport",
        defaults = { b, _ ->
            LargePlayerTransportProps(b.typography.labelSmall.copy(color = b.colors.muted), b.colors.accent, b.colors.muted, 0.62f)
        },
        apply = { p, r ->
            p.copy(
                timeStyle = r.textStyle("timeStyle", p.timeStyle),
                activeTint = r.color("activeTint", p.activeTint) ?: p.activeTint,
                inactiveTint = r.color("inactiveTint", p.inactiveTint) ?: p.inactiveTint,
                volumeWidthFraction = r.float("volumeWidthFraction", p.volumeWidthFraction).coerceIn(0.1f, 1f),
            )
        },
    )

    val ExpandedPlayerRoot = AppSlot(
        component = "expandedPlayer", variant = "root",
        defaults = { b, _ -> ExpandedPlayerRootProps(PaddingValues(horizontal = 16.dp), 4.dp, b.typography.titleMedium) },
        apply = { p, r ->
            p.copy(
                contentPadding = r.padding("contentPadding", p.contentPadding),
                topSpacing = r.dp("topSpacing", p.topSpacing),
                headerStyle = r.textStyle("headerStyle", p.headerStyle),
            )
        },
    )

    val ExpandedPlayerArtwork = AppSlot(
        component = "expandedPlayer", variant = "artwork",
        defaults = { b, _ -> ExpandedPlayerArtworkProps(0.84f, RoundedCornerShape(22.dp), b.colors.raised, PaddingValues(top = 20.dp, bottom = 24.dp)) },
        apply = { p, r ->
            p.copy(
                widthFraction = r.float("widthFraction", p.widthFraction).coerceIn(0.2f, 1f),
                shape = r.shape("shape", p.shape),
                background = r.color("background", p.background) ?: p.background,
                padding = r.padding("padding", p.padding),
            )
        },
    )

    val ExpandedPlayerInfo = AppSlot(
        component = "expandedPlayer", variant = "info",
        defaults = { b, _ ->
            ExpandedPlayerInfoProps(PaddingValues(horizontal = 2.dp), b.typography.headlineSmall, b.typography.bodyLarge.copy(color = b.colors.muted))
        },
        apply = { p, r ->
            p.copy(
                rowPadding = r.padding("rowPadding", p.rowPadding),
                titleStyle = r.textStyle("titleStyle", p.titleStyle),
                artistStyle = r.textStyle("artistStyle", p.artistStyle),
            )
        },
    )

    val ExpandedPlayerTransport = AppSlot(
        component = "expandedPlayer", variant = "transport",
        defaults = { b, _ ->
            ExpandedPlayerTransportProps(
                seekPadding = PaddingValues(top = 20.dp),
                timePadding = PaddingValues(top = 2.dp),
                timeStyle = b.typography.labelSmall.copy(color = b.colors.muted),
                controlsPadding = PaddingValues(top = 16.dp, bottom = 12.dp),
                playIconSize = 30.dp,
                activeTint = b.colors.accent,
                inactiveTint = b.colors.muted,
            )
        },
        apply = { p, r ->
            p.copy(
                seekPadding = r.padding("seekPadding", p.seekPadding),
                timePadding = r.padding("timePadding", p.timePadding),
                timeStyle = r.textStyle("timeStyle", p.timeStyle),
                controlsPadding = r.padding("controlsPadding", p.controlsPadding),
                playIconSize = r.dp("playIconSize", p.playIconSize),
                activeTint = r.color("activeTint", p.activeTint) ?: p.activeTint,
                inactiveTint = r.color("inactiveTint", p.inactiveTint) ?: p.inactiveTint,
            )
        },
    )

    val ExpandedPlayerLyrics = AppSlot(
        component = "expandedPlayer", variant = "lyrics",
        defaults = { b, _ ->
            ExpandedPlayerLyricsProps(
                outerPadding = PaddingValues(bottom = 16.dp),
                contentPadding = PaddingValues(12.dp),
                contentSpacing = 8.dp,
                shadowElevation = 2.dp,
                previewHeight = 120.dp,
                titleStyle = b.typography.labelMedium.copy(color = b.colors.muted),
                iconTint = b.colors.muted,
            )
        },
        apply = { p, r ->
            p.copy(
                outerPadding = r.padding("outerPadding", p.outerPadding),
                contentPadding = r.padding("contentPadding", p.contentPadding),
                contentSpacing = r.dp("contentSpacing", p.contentSpacing),
                shadowElevation = r.dp("shadowElevation", p.shadowElevation),
                previewHeight = r.dp("previewHeight", p.previewHeight),
                titleStyle = r.textStyle("titleStyle", p.titleStyle),
                iconTint = r.color("iconTint", p.iconTint) ?: p.iconTint,
            )
        },
    )

    val ExpandedPlayerOptionTile = AppSlot(
        component = "expandedPlayer", variant = "optionTile",
        defaults = { b, _ ->
            ExpandedPlayerOptionTileProps(PaddingValues(12.dp), 28.dp, b.colors.text, 8.dp, b.typography.labelSmall.copy(color = b.colors.muted))
        },
        apply = { p, r ->
            p.copy(
                contentPadding = r.padding("contentPadding", p.contentPadding),
                iconSize = r.dp("iconSize", p.iconSize),
                iconTint = r.color("iconTint", p.iconTint) ?: p.iconTint,
                spacing = r.dp("spacing", p.spacing),
                labelStyle = r.textStyle("labelStyle", p.labelStyle),
            )
        },
    )

    val PlayerQueueRoot = AppSlot(
        component = "playerQueue", variant = "root",
        defaults = { b, _ ->
            PlayerQueueRootProps(
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                spacing = 10.dp,
                filterSpacing = 5.dp,
                listPadding = PaddingValues(bottom = 8.dp),
                titleStyle = b.typography.titleLarge,
                emptyStyle = b.typography.bodyMedium.copy(color = b.colors.muted),
            )
        },
        apply = { p, r ->
            p.copy(
                contentPadding = r.padding("contentPadding", p.contentPadding),
                spacing = r.dp("spacing", p.spacing),
                filterSpacing = r.dp("filterSpacing", p.filterSpacing),
                listPadding = r.padding("listPadding", p.listPadding),
                titleStyle = r.textStyle("titleStyle", p.titleStyle),
                emptyStyle = r.textStyle("emptyStyle", p.emptyStyle),
            )
        },
    )

    val PlayerQueueRow = AppSlot(
        component = "playerQueue", variant = "row",
        defaults = { b, st ->
            PlayerQueueRowProps(
                height = 56.dp,
                handleSize = 24.dp,
                handleTint = b.colors.muted,
                thumbSize = 40.dp,
                thumbShape = b.shapes.small,
                thumbBackground = b.colors.inset,
                thumbIndexStyle = b.typography.labelMedium.copy(color = b.colors.muted),
                // The playing row and the others share the text colour today; `selected` lets a theme tell them apart.
                titleStyle = b.typography.bodyLarge.copy(color = b.colors.text),
                subtitleStyle = b.typography.bodySmall.copy(color = b.colors.muted),
                durationStyle = b.typography.bodySmall.copy(color = b.colors.muted),
                trailingSpacing = 4.dp,
                participantSize = 28.dp,
                participantBackground = b.colors.inset,
                participantStyle = b.typography.labelSmall.copy(color = b.colors.muted),
            )
        },
        apply = { p, r ->
            p.copy(
                height = r.dp("height", p.height),
                handleSize = r.dp("handleSize", p.handleSize),
                handleTint = r.color("handleTint", p.handleTint) ?: p.handleTint,
                thumbSize = r.dp("thumbSize", p.thumbSize),
                thumbShape = r.shape("thumbShape", p.thumbShape),
                thumbBackground = r.color("thumbBackground", p.thumbBackground) ?: p.thumbBackground,
                thumbIndexStyle = r.textStyle("thumbIndexStyle", p.thumbIndexStyle),
                titleStyle = r.textStyle("titleStyle", p.titleStyle),
                subtitleStyle = r.textStyle("subtitleStyle", p.subtitleStyle),
                durationStyle = r.textStyle("durationStyle", p.durationStyle),
                trailingSpacing = r.dp("trailingSpacing", p.trailingSpacing),
                participantSize = r.dp("participantSize", p.participantSize),
                participantBackground = r.color("participantBackground", p.participantBackground) ?: p.participantBackground,
                participantStyle = r.textStyle("participantStyle", p.participantStyle),
            )
        },
    )

    val all: List<AppSlot<*>> = listOf(
        LargePlayerRoot, LargePlayerArtwork, LargePlayerInfo, LargePlayerTransport,
        ExpandedPlayerRoot, ExpandedPlayerArtwork, ExpandedPlayerInfo, ExpandedPlayerTransport,
        ExpandedPlayerLyrics, ExpandedPlayerOptionTile,
        PlayerQueueRoot, PlayerQueueRow,
    )
}
