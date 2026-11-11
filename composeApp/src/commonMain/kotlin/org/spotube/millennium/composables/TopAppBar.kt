package org.spotube.millennium.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.spotube.millennium.theme.MillenniumPreview
import org.spotube.millennium.theme.MillenniumStyledContent
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumPanelBrush

/**
 * Complete, theme-owned appearance and layout configuration for [MillenniumTopAppBar].
 *
 * Every nullable color or brush is derived from the active palette when left `null`. Use `copy`
 * to adjust a single value and wrap the result in [MillenniumTopAppBarTheme].
 *
 * @property titleStyle Text style of the title. It is also the ambient style for the navigation
 *   and actions slots, so icons-with-text in those slots inherit its size and weight.
 * @property subtitleStyle Text style of the optional subtitle line.
 * @property titleForeground Title color; `null` uses the palette's primary text color.
 * @property subtitleForeground Subtitle color; `null` uses the palette's muted text color.
 * @property navigationForeground Ambient content color of the navigation slot; `null` uses the
 *   palette's text color.
 * @property actionForeground Ambient content color of the actions row; `null` uses the palette's
 *   text color.
 * @property background Fill of the whole bar; `null` paints the shared pearl/smoked glass panel
 *   brush ([millenniumPanelBrush]).
 * @property contentPadding Padding between the bar's edges and its navigation/title/actions row.
 * @property minimumHeight Minimum height of the content row (excluding window insets and the
 *   1dp hairline). The bar grows when its content is taller.
 * @property navigationSpacing Horizontal gap between the navigation slot and the title column.
 * @property actionSpacing Horizontal gap between sibling actions.
 * @property separatorColor Color of the 1dp hairline along the bottom edge; `null` uses the
 *   palette's border color.
 * @see MillenniumTopAppBar
 * @see defaultMillenniumTopAppBarTheme
 */
@Immutable
data class MillenniumTopAppBarThemeProperties(
    val titleStyle: TextStyle,
    val subtitleStyle: TextStyle,
    val titleForeground: Color? = null,
    val subtitleForeground: Color? = null,
    val navigationForeground: Color? = null,
    val actionForeground: Color? = null,
    val background: Brush? = null,
    val contentPadding: PaddingValues,
    val minimumHeight: Dp,
    val navigationSpacing: Dp,
    val actionSpacing: Dp,
    val separatorColor: Color? = null,
)

/**
 * Theme wrapper for [MillenniumTopAppBar], exposed as [MillenniumTheme.topAppBarTheme].
 *
 * Pass a custom instance through the `theme` parameter of one bar, or install it globally
 * through the [MillenniumTheme] configuration.
 *
 * ```kotlin
 * val dense = defaultMillenniumTopAppBarTheme().let {
 *     MillenniumTopAppBarTheme(it.properties.copy(minimumHeight = 44.dp))
 * }
 * MillenniumTopAppBar(title = { MillenniumText("Dense") }, theme = dense)
 * ```
 *
 * @property properties The resolved appearance and layout values.
 * @see MillenniumTopAppBarThemeProperties
 */
@Immutable
data class MillenniumTopAppBarTheme(
    val properties: MillenniumTopAppBarThemeProperties,
)

/**
 * Builds the default [MillenniumTopAppBarTheme].
 *
 * Defaults: 18sp semibold title, 12sp subtitle, 14dp by 8dp content padding, a 56dp minimum
 * row height, 10dp navigation gap, 4dp action gap, and every color and the background brush left
 * `null` so they follow the active light or dark palette (glass panel fill, palette text,
 * muted subtitle and a border-colored hairline). Customise with `copy` on
 * [MillenniumTopAppBarTheme.properties].
 */
fun defaultMillenniumTopAppBarTheme(): MillenniumTopAppBarTheme = MillenniumTopAppBarTheme(
    MillenniumTopAppBarThemeProperties(
        titleStyle = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        subtitleStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        minimumHeight = 56.dp,
        navigationSpacing = 10.dp,
        actionSpacing = 4.dp,
    ),
)

/**
 * A pearl/smoked-glass app bar with a title, optional subtitle, a navigation slot and an
 * actions row, finished with a hairline. Colors, typography, spacing, the glass fill and
 * the separator all resolve from [MillenniumTheme.topAppBarTheme], so it matches
 * [MillenniumScaffold]'s integrated bar and the rest of the system.
 *
 * Layout is a single row: the [navigationIcon] slot, a weighted column holding the [title] and the
 * optional [subtitle], and the [actions] row, followed by a 1dp separator. The bar fills the
 * available width, is at least `minimumHeight` tall and does not consume system insets unless you
 * pass [windowInsets]. It is purely presentational: it has no state and does not scroll or
 * collapse. Slots are wrapped in `MillenniumStyledContent`, so plain text inherits the themed style
 * and color, while [MillenniumIconButton] and other controls keep their own colors. The title and
 * action buttons are ordinary focusable controls, so keyboard focus and 44dp targets come from the
 * components you place in the slots (for example [MillenniumIconButton]); the bar adds no
 * semantics of its own.
 *
 * ```kotlin
 * MillenniumTopAppBar(
 *     title = { MillenniumText("Library") },
 *     subtitle = { MillenniumText("128 tracks") },
 *     navigationIcon = {
 *         MillenniumIconButton(MillenniumIcons.Chevron, "Back", onClick = { navigator.back() },
 *             style = MillenniumIconButtonStyle.Quiet)
 *     },
 *     actions = {
 *         MillenniumIconButton(MillenniumIcons.Sparkle, "Favorite", onClick = { favorite() },
 *             style = MillenniumIconButtonStyle.Quiet)
 *     },
 *     windowInsets = WindowInsets.statusBars,
 * )
 * ```
 *
 * @param title The title content. Defaults to empty.
 * @param modifier Modifier applied to the outer column (fill width, background, insets).
 * @param subtitle Optional second line under the title; `null` omits it.
 * @param navigationIcon Leading slot, typically a back or menu button. Defaults to empty, but the
 *   [navigationSpacing] gap is still reserved.
 * @param actions Trailing slot laid out in a [Row] with [actionSpacing] between items.
 * @param windowInsets Insets applied as padding inside the background. Defaults to zero so the bar
 *   can be embedded; pass `WindowInsets.statusBars` or `safeDrawing` when it sits at the screen top.
 * @param titleStyle Overrides the theme title style (also the ambient style of the navigation and
 *   action slots); `null` uses the theme.
 * @param subtitleStyle Overrides the theme subtitle style; `null` uses the theme.
 * @param titleForeground Overrides the title color; `null` falls back to the theme, then the
 *   palette text color.
 * @param subtitleForeground Overrides the subtitle color; `null` falls back to the theme, then the
 *   palette muted color.
 * @param navigationForeground Overrides the navigation slot color; `null` falls back to the theme,
 *   then the palette text color.
 * @param actionForeground Overrides the actions color; `null` falls back to the theme, then the
 *   palette text color.
 * @param background Overrides the bar fill; `null` falls back to the theme, then the glass panel
 *   brush.
 * @param contentPadding Overrides the row padding; `null` uses the theme.
 * @param minimumHeight Overrides the minimum row height; `null` uses the theme.
 * @param navigationSpacing Overrides the gap after the navigation slot; `null` uses the theme.
 * @param actionSpacing Overrides the gap between actions; `null` uses the theme.
 * @param separatorColor Overrides the bottom hairline color; `null` falls back to the theme, then
 *   the palette border color.
 * @param theme Theme supplying defaults for every nullable override above. Defaults to
 *   [MillenniumTheme.topAppBarTheme].
 * @see MillenniumScaffold
 * @see MillenniumIconButton
 * @see MillenniumTopAppBarTheme
 */
@Composable
fun MillenniumTopAppBar(
    title: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = WindowInsets(0, 0, 0, 0),
    titleStyle: TextStyle? = null,
    subtitleStyle: TextStyle? = null,
    titleForeground: Color? = null,
    subtitleForeground: Color? = null,
    navigationForeground: Color? = null,
    actionForeground: Color? = null,
    background: Brush? = null,
    contentPadding: PaddingValues? = null,
    minimumHeight: Dp? = null,
    navigationSpacing: Dp? = null,
    actionSpacing: Dp? = null,
    separatorColor: Color? = null,
    theme: MillenniumTopAppBarTheme = MillenniumTheme.topAppBarTheme,
) {
    val colors = MillenniumTheme.colors
    val properties = theme.properties
    val resolvedTitleStyle = titleStyle ?: properties.titleStyle
    Column(
        modifier
            .fillMaxWidth()
            .background(background ?: properties.background ?: millenniumPanelBrush(colors))
            .windowInsetsPadding(windowInsets),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = minimumHeight ?: properties.minimumHeight)
                .padding(contentPadding ?: properties.contentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MillenniumStyledContent(
                resolvedTitleStyle,
                navigationForeground ?: properties.navigationForeground ?: colors.text,
            ) { navigationIcon() }
            Spacer(Modifier.width(navigationSpacing ?: properties.navigationSpacing))
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                MillenniumStyledContent(
                    resolvedTitleStyle,
                    titleForeground ?: properties.titleForeground ?: colors.text,
                ) { title() }
                if (subtitle != null) {
                    MillenniumStyledContent(
                        subtitleStyle ?: properties.subtitleStyle,
                        subtitleForeground ?: properties.subtitleForeground ?: colors.muted,
                    ) { subtitle() }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(actionSpacing ?: properties.actionSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MillenniumStyledContent(
                    resolvedTitleStyle,
                    actionForeground ?: properties.actionForeground ?: colors.text,
                ) { actions() }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(separatorColor ?: properties.separatorColor ?: colors.border),
        )
    }
}

/** Design-time preview of [MillenniumTopAppBar] with navigation, subtitle and two actions. */
@Preview
@Composable
private fun MillenniumTopAppBarPreview() = MillenniumPreview {
    MillenniumTopAppBar(
        title = { MillenniumText("Millennium") },
        subtitle = { MillenniumText("A familiar feeling.") },
        navigationIcon = { MillenniumIconButton(MillenniumIcons.Sparkle, "Home", {}, style = MillenniumIconButtonStyle.Quiet) },
        actions = {
            MillenniumIconButton(MillenniumIcons.Sparkle, "Favorite", {}, style = MillenniumIconButtonStyle.Quiet)
            MillenniumIconButton(MillenniumIcons.Chevron, "More", {}, style = MillenniumIconButtonStyle.Quiet)
        },
    )
}
