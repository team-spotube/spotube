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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.spotube.millennium.theme.millenniumBevelBrush
import org.spotube.millennium.theme.millenniumPanelBrush

/** Slots for the app's content components: list tiles, track list, collection header, carousel, error states. */

// ---- media list tile (PlayableListTile, AvatarListTile) -------------------------------------

/** `selected` is the playing item, `error` a blacklisted one. */
@Immutable
data class MediaListTileProps(
    /** Null keeps Millennium's list tile look. */
    val background: Brush?,
    val shape: Shape,
    val borderColor: Color?,
    val titleForeground: Color?,
    val borderWidth: Dp,
    val padding: Dp,
    val compactPadding: Dp,
    val minimumHeight: Dp,
    val compactMinimumHeight: Dp,
    val artworkSize: Dp,
    val compactArtworkSize: Dp,
    val actionSize: Dp,
    val compactActionSize: Dp,
    val actionSpacing: Dp,
    val titleStyle: TextStyle,
    val subtitleStyle: TextStyle,
)

/** The square thumbnail used by list tiles (and any card that shows artwork with a placeholder). */
@Immutable
data class MediaArtworkProps(
    val background: Brush,
    val shape: Shape,
    val borderColor: Color,
    val borderWidth: Dp,
    val placeholderSize: Dp,
    val placeholderTint: Color,
)

/** Empty / "nothing here" state of a screen or list. */
@Immutable
data class EmptyStateProps(
    val padding: Dp,
    val maxWidth: Dp,
    val spacing: Dp,
    val iconBoxSize: Dp,
    val iconBoxShape: Shape,
    val iconBoxBackground: Brush,
    val iconBoxBorderColor: Color,
    val iconBoxBorderWidth: Dp,
    val iconSize: Dp,
    val iconTint: Color,
    val titleStyle: TextStyle,
    val descriptionStyle: TextStyle,
)

// ---- track list -----------------------------------------------------------------------------

/** The card around the rows, plus loading/error/empty feedback rows. */
@Immutable
data class TrackListCardProps(
    val outerPadding: Dp,
    val innerPadding: Dp,
    /** Horizontal inset of the separators in the footer (shimmer / error rows). */
    val separatorInset: Dp,
    val feedbackPadding: PaddingValues,
    val feedbackStyle: TextStyle,
    val errorColor: Color,
)

/** One track row. `selected` covers the current track and bulk-selected rows; `error` a blacklisted track. */
@Immutable
data class TrackRowProps(
    /** Null keeps Millennium's list tile look. */
    val background: Brush?,
    val selectedBackground: Brush?,
    val titleStyle: TextStyle,
    val leadingSpacing: Dp,
    val indexWidth: Dp,
    val indexStyle: TextStyle,
    val artworkSize: Dp,
    val artworkShape: Shape,
    /** Dim layer drawn over the artwork while hovered or current. */
    val artworkOverlay: Color,
    val overlayIconSize: Dp,
    val overlayIconTint: Color,
    val artistSpacing: Dp,
    val artistStyle: TextStyle,
    val artistSeparatorStyle: TextStyle,
    val trailingSpacing: Dp,
    val albumStyle: TextStyle,
    val albumMaxWidth: Dp,
    val durationStyle: TextStyle,
    val durationWidth: Dp,
)

/** The bottom sheet / menu of a track's actions. */
@Immutable
data class TrackOptionsProps(
    val sheetPadding: PaddingValues,
    val sheetSpacing: Dp,
    val listPadding: PaddingValues,
    val itemSpacing: Dp,
    val itemPadding: PaddingValues,
    val itemGap: Dp,
    val iconSize: Dp,
    val labelStyle: TextStyle,
    val disabledAlpha: Float,
    val headerPadding: PaddingValues,
    val headerSpacing: Dp,
    val headerArtworkSize: Dp,
    val headerArtworkShape: Shape,
    val headerTitleStyle: TextStyle,
    val headerArtistStyle: TextStyle,
    val headerAlbumStyle: TextStyle,
)

// ---- collection header (album / playlist / saved tracks) ---------------------------------------

@Immutable
data class CollectionHeaderProps(
    val outerPadding: Dp,
    val padding: Dp,
    val compactPadding: Dp,
    val spacing: Dp,
    val compactSpacing: Dp,
    /** Gap between the stacked text, owner and actions. */
    val infoSpacing: Dp,
    val compactInfoSpacing: Dp,
    val buttonSpacing: Dp,
    val artworkSize: Dp,
    val compactArtworkSize: Dp,
    val artworkShape: Shape,
    val placeholderStyle: TextStyle,
    val titleStyle: TextStyle,
    val compactTitleStyle: TextStyle,
    val descriptionStyle: TextStyle,
    val compactDescriptionStyle: TextStyle,
    val ownerStyle: TextStyle,
    val ownerAvatarSize: Dp,
    val ownerSpacing: Dp,
    val ownerPlaceholderBackground: Color,
    val ownerPlaceholderTint: Color,
    val actionSeparatorHeight: Dp,
    /** The slim title shown in the top bar once the header scrolls away. */
    val collapsedArtworkSize: Dp,
    val collapsedArtworkShape: Shape,
    val collapsedSpacing: Dp,
    val collapsedTitleStyle: TextStyle,
    val collapsedPlaceholderStyle: TextStyle,
)

// ---- featured carousel -------------------------------------------------------------------------

@Immutable
data class FeaturedCarouselProps(
    val height: Dp,
    val compactHeight: Dp,
    /** Coverflow geometry. */
    val itemOffset: Float,
    val itemAngle: Float,
    val itemShift: Float,
    val itemZoom: Float,
    val cardShape: Shape,
    /** Darkening of the cards that are not centered. */
    val dimAlpha: Float,
    val scrimHeight: Dp,
    val scrimColor: Color,
    val contentPadding: Dp,
    val textSpacing: Dp,
    val titleStyle: TextStyle,
    val subtitleStyle: TextStyle,
    val buttonSize: Dp,
    val buttonTint: Color,
    val buttonBlur: Dp,
    val buttonGlassTint: Color,
    val fadeMillis: Int,
)

// ---- error / login states ---------------------------------------------------------------------

@Immutable
data class ErrorDisplayProps(
    val padding: Dp,
    val spacing: Dp,
    val iconSize: Dp,
    val iconTint: Color,
    val titleStyle: TextStyle,
    val descriptionStyle: TextStyle,
    val detailsHeaderPadding: PaddingValues,
    val detailsHeaderStyle: TextStyle,
    val detailsActionSize: Dp,
    val detailsIconSize: Dp,
    val logStyle: TextStyle,
    val logPadding: Dp,
    val logMaxLines: Int,
    val retryHeight: Dp,
    val retryIconSize: Dp,
)

@Immutable
data class LoginRequiredProps(
    val padding: PaddingValues,
    val spacing: Dp,
    val badgeSize: Dp,
    val badgeShape: Shape,
    val badgeBackground: Color,
    val iconSize: Dp,
    val iconTint: Color,
    val titleStyle: TextStyle,
    val descriptionStyle: TextStyle,
    val buttonHeight: Dp,
    val buttonIconSize: Dp,
)

object ContentSlots {
    val MediaListTile = AppSlot(
        component = "mediaListTile", variant = null,
        defaults = { b, st ->
            val c = b.colors
            MediaListTileProps(
                background = if (st == AppState.Selected) null else millenniumPanelBrush(c),
                shape = RoundedCornerShape(8.dp),
                borderColor = when (st) {
                    AppState.Error -> c.danger.copy(alpha = 0.5f)
                    AppState.Selected -> null
                    else -> c.border
                },
                titleForeground = if (st == AppState.Error) c.danger else null,
                borderWidth = 1.dp,
                padding = 12.dp, compactPadding = 8.dp,
                minimumHeight = 76.dp, compactMinimumHeight = 52.dp,
                artworkSize = 52.dp, compactArtworkSize = 36.dp,
                actionSize = 40.dp, compactActionSize = 32.dp,
                actionSpacing = 6.dp,
                titleStyle = b.typography.titleSmall,
                subtitleStyle = b.typography.bodySmall,
            )
        },
        apply = { p, r ->
            p.copy(
                background = r.brush("background", p.background),
                shape = r.shape("shape", p.shape),
                borderColor = r.color("borderColor", p.borderColor),
                titleForeground = r.color("titleForeground", p.titleForeground),
                borderWidth = r.dp("borderWidth", p.borderWidth),
                padding = r.dp("padding", p.padding),
                compactPadding = r.dp("compactPadding", p.compactPadding),
                minimumHeight = r.dp("minimumHeight", p.minimumHeight),
                compactMinimumHeight = r.dp("compactMinimumHeight", p.compactMinimumHeight),
                artworkSize = r.dp("artworkSize", p.artworkSize),
                compactArtworkSize = r.dp("compactArtworkSize", p.compactArtworkSize),
                actionSize = r.dp("actionSize", p.actionSize),
                compactActionSize = r.dp("compactActionSize", p.compactActionSize),
                actionSpacing = r.dp("actionSpacing", p.actionSpacing),
                titleStyle = r.textStyle("titleStyle", p.titleStyle),
                subtitleStyle = r.textStyle("subtitleStyle", p.subtitleStyle),
            )
        },
    )

    val MediaArtwork = AppSlot(
        component = "mediaArtwork", variant = null,
        defaults = { b, _ ->
            MediaArtworkProps(
                background = millenniumBevelBrush(b.colors),
                shape = RoundedCornerShape(6.dp),
                borderColor = b.colors.border,
                borderWidth = 1.dp,
                placeholderSize = 24.dp,
                placeholderTint = b.colors.muted,
            )
        },
        apply = { p, r ->
            p.copy(
                background = r.brush("background", p.background) ?: p.background,
                shape = r.shape("shape", p.shape),
                borderColor = r.color("borderColor", p.borderColor) ?: p.borderColor,
                borderWidth = r.dp("borderWidth", p.borderWidth),
                placeholderSize = r.dp("placeholderSize", p.placeholderSize),
                placeholderTint = r.color("placeholderTint", p.placeholderTint) ?: p.placeholderTint,
            )
        },
    )

    val EmptyState = AppSlot(
        component = "emptyState", variant = null,
        defaults = { b, _ ->
            EmptyStateProps(
                padding = 24.dp, maxWidth = 360.dp, spacing = 12.dp,
                iconBoxSize = 64.dp,
                iconBoxShape = RoundedCornerShape(14.dp),
                iconBoxBackground = millenniumBevelBrush(b.colors),
                iconBoxBorderColor = b.colors.border,
                iconBoxBorderWidth = 1.dp,
                iconSize = 30.dp,
                iconTint = b.colors.muted,
                titleStyle = b.typography.titleMedium,
                descriptionStyle = b.typography.bodySmall.copy(color = b.colors.muted),
            )
        },
        apply = { p, r ->
            p.copy(
                padding = r.dp("padding", p.padding),
                maxWidth = r.dp("maxWidth", p.maxWidth),
                spacing = r.dp("spacing", p.spacing),
                iconBoxSize = r.dp("iconBoxSize", p.iconBoxSize),
                iconBoxShape = r.shape("iconBoxShape", p.iconBoxShape),
                iconBoxBackground = r.brush("iconBoxBackground", p.iconBoxBackground) ?: p.iconBoxBackground,
                iconBoxBorderColor = r.color("iconBoxBorderColor", p.iconBoxBorderColor) ?: p.iconBoxBorderColor,
                iconBoxBorderWidth = r.dp("iconBoxBorderWidth", p.iconBoxBorderWidth),
                iconSize = r.dp("iconSize", p.iconSize),
                iconTint = r.color("iconTint", p.iconTint) ?: p.iconTint,
                titleStyle = r.textStyle("titleStyle", p.titleStyle),
                descriptionStyle = r.textStyle("descriptionStyle", p.descriptionStyle),
            )
        },
    )

    val TrackListCard = AppSlot(
        component = "trackList", variant = "card",
        defaults = { b, _ ->
            TrackListCardProps(
                outerPadding = 8.dp, innerPadding = 12.dp, separatorInset = 8.dp,
                feedbackPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                feedbackStyle = b.typography.bodyMedium.copy(color = b.colors.muted),
                errorColor = b.colors.danger,
            )
        },
        apply = { p, r ->
            p.copy(
                outerPadding = r.dp("outerPadding", p.outerPadding),
                innerPadding = r.dp("innerPadding", p.innerPadding),
                separatorInset = r.dp("separatorInset", p.separatorInset),
                feedbackPadding = r.padding("feedbackPadding", p.feedbackPadding),
                feedbackStyle = r.textStyle("feedbackStyle", p.feedbackStyle),
                errorColor = r.color("errorColor", p.errorColor) ?: p.errorColor,
            )
        },
    )

    val TrackRow = AppSlot(
        component = "trackList", variant = "row",
        defaults = { b, _ ->
            val t = b.typography
            val c = b.colors
            TrackRowProps(
                background = null, selectedBackground = null,
                titleStyle = t.bodyLarge,
                leadingSpacing = 10.dp, indexWidth = 30.dp,
                indexStyle = t.labelMedium.copy(color = c.muted),
                artworkSize = 44.dp, artworkShape = b.shapes.small,
                artworkOverlay = Color.Black.copy(alpha = 0.10f),
                overlayIconSize = 18.dp, overlayIconTint = Color.White,
                artistSpacing = 6.dp,
                artistStyle = t.bodySmall.copy(color = c.accent),
                artistSeparatorStyle = t.bodySmall.copy(color = c.muted),
                trailingSpacing = 10.dp,
                albumStyle = t.bodyMedium.copy(color = c.accent),
                albumMaxWidth = 220.dp,
                durationStyle = t.bodySmall.copy(color = c.muted),
                durationWidth = 56.dp,
            )
        },
        apply = { p, r ->
            p.copy(
                background = r.brush("background", p.background),
                selectedBackground = r.brush("selectedBackground", p.selectedBackground),
                titleStyle = r.textStyle("titleStyle", p.titleStyle),
                leadingSpacing = r.dp("leadingSpacing", p.leadingSpacing),
                indexWidth = r.dp("indexWidth", p.indexWidth),
                indexStyle = r.textStyle("indexStyle", p.indexStyle),
                artworkSize = r.dp("artworkSize", p.artworkSize),
                artworkShape = r.shape("artworkShape", p.artworkShape),
                artworkOverlay = r.color("artworkOverlay", p.artworkOverlay) ?: p.artworkOverlay,
                overlayIconSize = r.dp("overlayIconSize", p.overlayIconSize),
                overlayIconTint = r.color("overlayIconTint", p.overlayIconTint) ?: p.overlayIconTint,
                artistSpacing = r.dp("artistSpacing", p.artistSpacing),
                artistStyle = r.textStyle("artistStyle", p.artistStyle),
                artistSeparatorStyle = r.textStyle("artistSeparatorStyle", p.artistSeparatorStyle),
                trailingSpacing = r.dp("trailingSpacing", p.trailingSpacing),
                albumStyle = r.textStyle("albumStyle", p.albumStyle),
                albumMaxWidth = r.dp("albumMaxWidth", p.albumMaxWidth),
                durationStyle = r.textStyle("durationStyle", p.durationStyle),
                durationWidth = r.dp("durationWidth", p.durationWidth),
            )
        },
    )

    val TrackOptions = AppSlot(
        component = "trackOptions", variant = null,
        defaults = { b, _ ->
            val t = b.typography
            val c = b.colors
            TrackOptionsProps(
                sheetPadding = PaddingValues(top = 10.dp, bottom = 8.dp),
                sheetSpacing = 4.dp,
                listPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                itemSpacing = 2.dp,
                itemPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                itemGap = 12.dp, iconSize = 22.dp,
                labelStyle = t.bodyLarge.copy(color = c.text),
                disabledAlpha = 0.38f,
                headerPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                headerSpacing = 12.dp,
                headerArtworkSize = 56.dp, headerArtworkShape = b.shapes.small,
                headerTitleStyle = t.titleMedium,
                headerArtistStyle = t.bodyMedium.copy(color = c.muted),
                headerAlbumStyle = t.bodySmall.copy(color = c.muted),
            )
        },
        apply = { p, r ->
            p.copy(
                sheetPadding = r.padding("sheetPadding", p.sheetPadding),
                sheetSpacing = r.dp("sheetSpacing", p.sheetSpacing),
                listPadding = r.padding("listPadding", p.listPadding),
                itemSpacing = r.dp("itemSpacing", p.itemSpacing),
                itemPadding = r.padding("itemPadding", p.itemPadding),
                itemGap = r.dp("itemGap", p.itemGap),
                iconSize = r.dp("iconSize", p.iconSize),
                labelStyle = r.textStyle("labelStyle", p.labelStyle),
                disabledAlpha = r.float("disabledAlpha", p.disabledAlpha).coerceIn(0f, 1f),
                headerPadding = r.padding("headerPadding", p.headerPadding),
                headerSpacing = r.dp("headerSpacing", p.headerSpacing),
                headerArtworkSize = r.dp("headerArtworkSize", p.headerArtworkSize),
                headerArtworkShape = r.shape("headerArtworkShape", p.headerArtworkShape),
                headerTitleStyle = r.textStyle("headerTitleStyle", p.headerTitleStyle),
                headerArtistStyle = r.textStyle("headerArtistStyle", p.headerArtistStyle),
                headerAlbumStyle = r.textStyle("headerAlbumStyle", p.headerAlbumStyle),
            )
        },
    )

    val CollectionHeader = AppSlot(
        component = "collectionHeader", variant = null,
        defaults = { b, _ ->
            val t = b.typography
            val c = b.colors
            CollectionHeaderProps(
                outerPadding = 8.dp, padding = 16.dp, compactPadding = 14.dp,
                spacing = 16.dp, compactSpacing = 12.dp,
                infoSpacing = 8.dp, compactInfoSpacing = 6.dp,
                buttonSpacing = 6.dp,
                artworkSize = 200.dp, compactArtworkSize = 100.dp,
                artworkShape = RoundedCornerShape(12.dp),
                placeholderStyle = t.displaySmall.copy(color = c.muted),
                titleStyle = t.headlineLarge, compactTitleStyle = t.bodyLarge,
                descriptionStyle = t.bodyMedium.copy(color = c.muted),
                compactDescriptionStyle = t.labelSmall.copy(color = c.muted),
                ownerStyle = t.labelSmall.copy(color = c.muted),
                ownerAvatarSize = 16.dp, ownerSpacing = 10.dp,
                ownerPlaceholderBackground = c.raised, ownerPlaceholderTint = c.text,
                actionSeparatorHeight = 24.dp,
                collapsedArtworkSize = 36.dp,
                collapsedArtworkShape = RoundedCornerShape(6.dp),
                collapsedSpacing = 10.dp,
                collapsedTitleStyle = t.titleSmall,
                collapsedPlaceholderStyle = t.labelMedium.copy(color = c.muted),
            )
        },
        apply = { p, r ->
            p.copy(
                outerPadding = r.dp("outerPadding", p.outerPadding),
                padding = r.dp("padding", p.padding),
                compactPadding = r.dp("compactPadding", p.compactPadding),
                spacing = r.dp("spacing", p.spacing),
                compactSpacing = r.dp("compactSpacing", p.compactSpacing),
                infoSpacing = r.dp("infoSpacing", p.infoSpacing),
                compactInfoSpacing = r.dp("compactInfoSpacing", p.compactInfoSpacing),
                buttonSpacing = r.dp("buttonSpacing", p.buttonSpacing),
                artworkSize = r.dp("artworkSize", p.artworkSize),
                compactArtworkSize = r.dp("compactArtworkSize", p.compactArtworkSize),
                artworkShape = r.shape("artworkShape", p.artworkShape),
                placeholderStyle = r.textStyle("placeholderStyle", p.placeholderStyle),
                titleStyle = r.textStyle("titleStyle", p.titleStyle),
                compactTitleStyle = r.textStyle("compactTitleStyle", p.compactTitleStyle),
                descriptionStyle = r.textStyle("descriptionStyle", p.descriptionStyle),
                compactDescriptionStyle = r.textStyle("compactDescriptionStyle", p.compactDescriptionStyle),
                ownerStyle = r.textStyle("ownerStyle", p.ownerStyle),
                ownerAvatarSize = r.dp("ownerAvatarSize", p.ownerAvatarSize),
                ownerSpacing = r.dp("ownerSpacing", p.ownerSpacing),
                ownerPlaceholderBackground = r.color("ownerPlaceholderBackground", p.ownerPlaceholderBackground) ?: p.ownerPlaceholderBackground,
                ownerPlaceholderTint = r.color("ownerPlaceholderTint", p.ownerPlaceholderTint) ?: p.ownerPlaceholderTint,
                actionSeparatorHeight = r.dp("actionSeparatorHeight", p.actionSeparatorHeight),
                collapsedArtworkSize = r.dp("collapsedArtworkSize", p.collapsedArtworkSize),
                collapsedArtworkShape = r.shape("collapsedArtworkShape", p.collapsedArtworkShape),
                collapsedSpacing = r.dp("collapsedSpacing", p.collapsedSpacing),
                collapsedTitleStyle = r.textStyle("collapsedTitleStyle", p.collapsedTitleStyle),
                collapsedPlaceholderStyle = r.textStyle("collapsedPlaceholderStyle", p.collapsedPlaceholderStyle),
            )
        },
    )

    val FeaturedCarousel = AppSlot(
        component = "featuredCarousel", variant = null,
        defaults = { b, _ ->
            val t = b.typography
            FeaturedCarouselProps(
                height = 300.dp, compactHeight = 200.dp,
                itemOffset = 0.38f, itemAngle = 12f, itemShift = 0.255f, itemZoom = 0.7864f,
                cardShape = RoundedCornerShape(24.dp),
                dimAlpha = 0.5f,
                scrimHeight = 140.dp,
                scrimColor = Color.Black.copy(alpha = 0.9f),
                contentPadding = 16.dp, textSpacing = 4.dp,
                titleStyle = t.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                subtitleStyle = t.bodySmall.copy(color = Color.White.copy(alpha = 0.85f)),
                buttonSize = 44.dp, buttonTint = Color.White,
                buttonBlur = 20.dp, buttonGlassTint = Color.White.copy(alpha = 0.1f),
                fadeMillis = 300,
            )
        },
        apply = { p, r ->
            p.copy(
                height = r.dp("height", p.height),
                compactHeight = r.dp("compactHeight", p.compactHeight),
                itemOffset = r.float("itemOffset", p.itemOffset),
                itemAngle = r.float("itemAngle", p.itemAngle),
                itemShift = r.float("itemShift", p.itemShift),
                itemZoom = r.float("itemZoom", p.itemZoom),
                cardShape = r.shape("cardShape", p.cardShape),
                dimAlpha = r.float("dimAlpha", p.dimAlpha).coerceIn(0f, 1f),
                scrimHeight = r.dp("scrimHeight", p.scrimHeight),
                scrimColor = r.color("scrimColor", p.scrimColor) ?: p.scrimColor,
                contentPadding = r.dp("contentPadding", p.contentPadding),
                textSpacing = r.dp("textSpacing", p.textSpacing),
                titleStyle = r.textStyle("titleStyle", p.titleStyle),
                subtitleStyle = r.textStyle("subtitleStyle", p.subtitleStyle),
                buttonSize = r.dp("buttonSize", p.buttonSize),
                buttonTint = r.color("buttonTint", p.buttonTint) ?: p.buttonTint,
                buttonBlur = r.dp("buttonBlur", p.buttonBlur),
                buttonGlassTint = r.color("buttonGlassTint", p.buttonGlassTint) ?: p.buttonGlassTint,
                fadeMillis = r.int("fadeMillis", p.fadeMillis).coerceIn(0, 2000),
            )
        },
    )

    val ErrorDisplay = AppSlot(
        component = "errorDisplay", variant = null,
        defaults = { b, _ ->
            val t = b.typography
            val c = b.colors
            ErrorDisplayProps(
                padding = 16.dp, spacing = 20.dp,
                iconSize = 64.dp, iconTint = c.danger.copy(alpha = 0.7f),
                titleStyle = t.headlineSmall.copy(fontWeight = FontWeight.Bold, color = c.text),
                descriptionStyle = t.bodyMedium.copy(color = c.muted, lineHeight = t.bodyMedium.lineHeight * 1.2),
                detailsHeaderPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                detailsHeaderStyle = t.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = c.muted),
                detailsActionSize = 32.dp, detailsIconSize = 16.dp,
                logStyle = t.bodySmall.copy(fontFamily = FontFamily.Monospace, color = c.muted),
                logPadding = 12.dp, logMaxLines = 5,
                retryHeight = 40.dp, retryIconSize = 18.dp,
            )
        },
        apply = { p, r ->
            p.copy(
                padding = r.dp("padding", p.padding),
                spacing = r.dp("spacing", p.spacing),
                iconSize = r.dp("iconSize", p.iconSize),
                iconTint = r.color("iconTint", p.iconTint) ?: p.iconTint,
                titleStyle = r.textStyle("titleStyle", p.titleStyle),
                descriptionStyle = r.textStyle("descriptionStyle", p.descriptionStyle),
                detailsHeaderPadding = r.padding("detailsHeaderPadding", p.detailsHeaderPadding),
                detailsHeaderStyle = r.textStyle("detailsHeaderStyle", p.detailsHeaderStyle),
                detailsActionSize = r.dp("detailsActionSize", p.detailsActionSize),
                detailsIconSize = r.dp("detailsIconSize", p.detailsIconSize),
                logStyle = r.textStyle("logStyle", p.logStyle),
                logPadding = r.dp("logPadding", p.logPadding),
                logMaxLines = r.int("logMaxLines", p.logMaxLines).coerceIn(1, 100),
                retryHeight = r.dp("retryHeight", p.retryHeight),
                retryIconSize = r.dp("retryIconSize", p.retryIconSize),
            )
        },
    )

    val LoginRequired = AppSlot(
        component = "loginRequired", variant = null,
        defaults = { b, _ ->
            val t = b.typography
            val c = b.colors
            LoginRequiredProps(
                padding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
                spacing = 16.dp,
                badgeSize = 88.dp, badgeShape = CircleShape,
                badgeBackground = c.accent.copy(alpha = 0.14f),
                iconSize = 40.dp, iconTint = c.accent,
                titleStyle = t.headlineSmall.copy(fontWeight = FontWeight.Bold, color = c.text),
                descriptionStyle = t.bodyMedium.copy(color = c.muted),
                buttonHeight = 44.dp, buttonIconSize = 18.dp,
            )
        },
        apply = { p, r ->
            p.copy(
                padding = r.padding("padding", p.padding),
                spacing = r.dp("spacing", p.spacing),
                badgeSize = r.dp("badgeSize", p.badgeSize),
                badgeShape = r.shape("badgeShape", p.badgeShape),
                badgeBackground = r.color("badgeBackground", p.badgeBackground) ?: p.badgeBackground,
                iconSize = r.dp("iconSize", p.iconSize),
                iconTint = r.color("iconTint", p.iconTint) ?: p.iconTint,
                titleStyle = r.textStyle("titleStyle", p.titleStyle),
                descriptionStyle = r.textStyle("descriptionStyle", p.descriptionStyle),
                buttonHeight = r.dp("buttonHeight", p.buttonHeight),
                buttonIconSize = r.dp("buttonIconSize", p.buttonIconSize),
            )
        },
    )

    val all: List<AppSlot<*>> = listOf(
        MediaListTile, MediaArtwork, EmptyState, TrackListCard, TrackRow, TrackOptions,
        CollectionHeader, FeaturedCarousel, ErrorDisplay, LoginRequired,
    )
}
