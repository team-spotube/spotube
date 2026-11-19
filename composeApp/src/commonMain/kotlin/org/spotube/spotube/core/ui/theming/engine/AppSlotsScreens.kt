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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Immutable

/** Props of the screen-level slots (shell, screens, settings, lyrics). See AppSlots.kt for the recipe. */

/** A scrolling screen's content area. The shell's bottom overlay inset is added on top of [bottomPadding]. */
@Immutable
data class ScreenRootProps(val topPadding: Dp, val bottomPadding: Dp, val sectionSpacing: Dp)

/** A titled row of cards (Home browse sections and alike). */
@Immutable
data class SectionHeaderProps(
    val spacing: Dp,
    val horizontalPadding: Dp,
    /** Gap between the cards of the row. */
    val itemSpacing: Dp,
    /** Vertical shift of the title toward the subtitle. */
    val titleOffset: Dp,
    val titleStyle: TextStyle,
    val subtitleStyle: TextStyle,
)

@Immutable
data class AppShellRootProps(
    /** Distance of the snackbars above the bottom overlay (players / bottom bar). */
    val snackbarOffset: Dp,
)

/** The "connected to remote / jam" banner strip. */
@Immutable
data class ConnectBannerRootProps(val padding: PaddingValues, val spacing: Dp)

@Immutable
data class ConnectBannerRowProps(
    val background: Color,
    val shape: Shape,
    val padding: PaddingValues,
    val spacing: Dp,
    val textStyle: TextStyle,
    val chevronTint: Color,
    val chevronSize: Dp,
)

/** The small live marker used by the banner and the sidebar. */
@Immutable
data class LiveDotProps(val size: Dp, val color: Color, val ringColor: Color, val ringWidth: Dp)

/** One row of the settings screens. */
@Immutable
data class SettingsRowProps(val titleStyle: TextStyle, val subtitleStyle: TextStyle)

/**
 * A synced lyrics line. States: `default` upcoming, `disabled` already sung, `selected` the current line.
 * The text colour is part of [textStyle].
 */
@Immutable
data class LyricsLineProps(
    val textStyle: TextStyle,
    val padding: PaddingValues,
    /** Gap between lines of the passive preview. */
    val spacing: Dp,
)

/** A line of the immersive (clickable) lyrics view. `selected` is the current line. */
@Immutable
data class LyricsImmersiveLineProps(
    val textStyle: TextStyle,
    val shape: Shape,
    val padding: PaddingValues,
    /** Gap between lines. */
    val spacing: Dp,
)

object ScreensSlots {
    val ScreenRoot = AppSlot(
        component = "screen", variant = "root",
        defaults = { _, _ -> ScreenRootProps(topPadding = 16.dp, bottomPadding = 16.dp, sectionSpacing = 24.dp) },
        apply = { p, r ->
            p.copy(
                topPadding = r.dp("topPadding", p.topPadding),
                bottomPadding = r.dp("bottomPadding", p.bottomPadding),
                sectionSpacing = r.dp("sectionSpacing", p.sectionSpacing),
            )
        },
    )

    val SectionHeader = AppSlot(
        component = "section", variant = "header",
        defaults = { b, _ ->
            SectionHeaderProps(
                spacing = 12.dp, horizontalPadding = 16.dp, itemSpacing = 12.dp, titleOffset = (-12).dp,
                titleStyle = b.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                subtitleStyle = b.typography.labelMedium.copy(color = b.colors.accent, fontWeight = FontWeight.Medium),
            )
        },
        apply = { p, r ->
            p.copy(
                spacing = r.dp("spacing", p.spacing),
                horizontalPadding = r.dp("horizontalPadding", p.horizontalPadding),
                itemSpacing = r.dp("itemSpacing", p.itemSpacing),
                titleOffset = r.dp("titleOffset", p.titleOffset),
                titleStyle = r.textStyle("titleStyle", p.titleStyle),
                subtitleStyle = r.textStyle("subtitleStyle", p.subtitleStyle),
            )
        },
    )

    val AppShellRoot = AppSlot(
        component = "appShell", variant = "root",
        defaults = { _, _ -> AppShellRootProps(snackbarOffset = 12.dp) },
        apply = { p, r -> p.copy(snackbarOffset = r.dp("snackbarOffset", p.snackbarOffset)) },
    )

    val ConnectBannerRoot = AppSlot(
        component = "connectBanner", variant = "root",
        defaults = { _, _ -> ConnectBannerRootProps(PaddingValues(horizontal = 16.dp, vertical = 4.dp), 6.dp) },
        apply = { p, r ->
            p.copy(padding = r.padding("padding", p.padding), spacing = r.dp("spacing", p.spacing))
        },
    )

    val ConnectBannerRow = AppSlot(
        component = "connectBanner", variant = "row",
        defaults = { b, _ ->
            ConnectBannerRowProps(
                background = b.colors.accent.copy(alpha = 0.12f),
                shape = b.shapes.medium,
                padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                spacing = 10.dp,
                textStyle = b.typography.labelLarge.copy(color = b.colors.text),
                chevronTint = b.colors.muted,
                chevronSize = 18.dp,
            )
        },
        apply = { p, r ->
            p.copy(
                background = r.color("background", p.background) ?: p.background,
                shape = r.shape("shape", p.shape),
                padding = r.padding("padding", p.padding),
                spacing = r.dp("spacing", p.spacing),
                textStyle = r.textStyle("textStyle", p.textStyle),
                chevronTint = r.color("chevronTint", p.chevronTint) ?: p.chevronTint,
                chevronSize = r.dp("chevronSize", p.chevronSize),
            )
        },
    )

    val LiveDot = AppSlot(
        component = "connectBanner", variant = "dot",
        defaults = { b, _ -> LiveDotProps(size = 10.dp, color = b.colors.success, ringColor = b.colors.surface, ringWidth = 2.dp) },
        apply = { p, r ->
            p.copy(
                size = r.dp("size", p.size),
                color = r.color("color", p.color) ?: p.color,
                ringColor = r.color("ringColor", p.ringColor) ?: p.ringColor,
                ringWidth = r.dp("ringWidth", p.ringWidth),
            )
        },
    )

    val SettingsRow = AppSlot(
        component = "settingsRow", variant = "root",
        defaults = { b, _ ->
            SettingsRowProps(
                titleStyle = b.typography.labelLarge.copy(color = b.colors.text),
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

    val LyricsLine = AppSlot(
        component = "lyricsLine", variant = "root",
        defaults = { b, st ->
            val c = b.colors
            val base = b.typography.bodyMedium
            LyricsLineProps(
                textStyle = when (st) {
                    AppState.Selected -> base.copy(fontWeight = FontWeight.Bold, color = c.accent)
                    AppState.Disabled -> base.copy(fontWeight = FontWeight.Normal, color = c.muted.copy(alpha = 0.55f))
                    else -> base.copy(fontWeight = FontWeight.Normal, color = c.muted)
                },
                padding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                spacing = 2.dp,
            )
        },
        apply = { p, r ->
            p.copy(
                textStyle = r.textStyle("textStyle", p.textStyle),
                padding = r.padding("padding", p.padding),
                spacing = r.dp("spacing", p.spacing),
            )
        },
    )

    val LyricsImmersiveLine = AppSlot(
        component = "lyricsLine", variant = "immersive",
        defaults = { b, st ->
            LyricsImmersiveLineProps(
                textStyle = b.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (st == AppState.Selected) b.colors.accent else b.colors.text,
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                padding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                spacing = 4.dp,
            )
        },
        apply = { p, r ->
            p.copy(
                textStyle = r.textStyle("textStyle", p.textStyle),
                shape = r.shape("shape", p.shape),
                padding = r.padding("padding", p.padding),
                spacing = r.dp("spacing", p.spacing),
            )
        },
    )

    val all: List<AppSlot<*>> = listOf(
        ScreenRoot, SectionHeader, AppShellRoot, ConnectBannerRoot, ConnectBannerRow, LiveDot,
        SettingsRow, LyricsLine, LyricsImmersiveLine,
    )
}
