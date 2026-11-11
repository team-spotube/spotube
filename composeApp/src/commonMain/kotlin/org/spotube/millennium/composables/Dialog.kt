package org.spotube.millennium.composables

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.DialogPanel
import com.composeunstyled.Scrim
import com.composeunstyled.Text
import com.composeunstyled.UnstyledButton
import com.composeunstyled.UnstyledDialog
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumPreview
import org.spotube.millennium.theme.MillenniumStyledContent
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.collectIsKeyboardFocusedAsState
import org.spotube.millennium.theme.gelBrush
import org.spotube.millennium.theme.millenniumActionPointer
import org.spotube.millennium.theme.millenniumInteractionSurface
import org.spotube.millennium.theme.millenniumPanelBrush
import org.spotube.millennium.theme.millenniumSurface

/**
 * Resolved visual and layout properties for [MillenniumDialog] and its bricks.
 *
 * The dialog and its bricks read the `default` state of [MillenniumDialogTheme.states]. Nullable colours
 * and brushes are derived from the active palette when null.
 *
 * @property panelBackground Panel fill. Null uses `millenniumPanelBrush(colors)`.
 * @property shape Corner shape of the panel.
 * @property foreground Default text colour inside the dialog. Null uses `colors.text`.
 * @property titleStyle Typography of [MillenniumDialogTitle].
 * @property panelBorderColor Panel hairline colour. Null uses `colors.border`.
 * @property panelBorderWidth Panel hairline thickness.
 * @property panelShadowElevation Drop-shadow elevation under the panel.
 * @property panelGlowColor Inner top glow of the panel. Null uses `colors.highlight` at 60% alpha.
 * @property panelMaxWidth Maximum panel width.
 * @property overlayPadding Margin between the panel and the window edges.
 * @property contentPadding Padding of [MillenniumDialogBody].
 * @property contentSpacing Spacing between children of [MillenniumDialogBody].
 * @property scrimColor Colour of the backdrop. Null uses `colors.scrim`.
 * @property enterScale Initial scale of the panel when entering.
 * @property exitScale Target scale of the panel when exiting.
 * @property animationMillis Duration of the scrim and panel fade-in.
 * @property closeAnimationMillis Duration of the exit transition.
 * @property alpha Overall panel opacity.
 * @property titleBarBackground Optional fill behind the header; null leaves it transparent.
 * @property titleBarForeground Header text colour. Null falls back to [foreground], then `colors.text`.
 * @property titleBarHeight Minimum height of [MillenniumDialogHeader].
 * @property titleBarPadding Padding of the header row.
 * @property titleBarBorderColor Header divider colour. Null uses `colors.border`.
 * @property descriptionStyle Typography of [MillenniumDialogDescription].
 * @property footerBackground Optional fill behind the footer; null leaves it transparent.
 * @property footerPadding Padding of the footer row.
 * @property footerSpacing Gap between footer actions.
 * @property footerBorderColor Footer divider colour. Null uses `colors.border`.
 * @property closeTargetSize Touch target of [MillenniumDialogClose] (44dp).
 * @property closeCapSize Visible size of the red close cap.
 *
 * @see MillenniumDialogTheme
 * @see defaultMillenniumDialogTheme
 */
@Immutable
data class MillenniumDialogThemeProperties(
    val panelBackground: Brush? = null,
    val shape: Shape,
    val foreground: Color? = null,
    val titleStyle: TextStyle,
    val panelBorderColor: Color? = null,
    val panelBorderWidth: Dp,
    val panelShadowElevation: Dp,
    val panelGlowColor: Color? = null,
    val panelMaxWidth: Dp,
    val overlayPadding: Dp,
    val contentPadding: Dp,
    val contentSpacing: Dp,
    val scrimColor: Color? = null,
    val enterScale: Float,
    val exitScale: Float = 0.94f,
    val animationMillis: Int,
    val closeAnimationMillis: Int = 150,
    val alpha: Float = 1f,
    val titleBarBackground: Brush? = null,
    val titleBarForeground: Color? = null,
    val titleBarHeight: Dp = 46.dp,
    val titleBarPadding: PaddingValues = PaddingValues(horizontal = 14.dp),
    val titleBarBorderColor: Color? = null,
    val descriptionStyle: TextStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    val footerBackground: Brush? = null,
    val footerPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    val footerSpacing: Dp = 10.dp,
    val footerBorderColor: Color? = null,
    val closeTargetSize: Dp = 44.dp,
    val closeCapSize: Dp = 28.dp,
)

/**
 * Theme of [MillenniumDialog] and its bricks, resolved per control state.
 *
 * Installed as `MillenniumTheme.dialogTheme`; every dialog brick accepts a `theme` parameter that
 * defaults to it, so one dialog can be restyled without touching the global theme.
 *
 * @property states State-dependent [MillenniumDialogThemeProperties]. Dialogs use `default`.
 *
 * @see defaultMillenniumDialogTheme
 */
@Immutable
data class MillenniumDialogTheme(
    val states: MillenniumControlStateProperty<MillenniumDialogThemeProperties>,
)

/**
 * Builds the stock dialog theme: a 10dp rounded pearl/smoke glass panel (480dp max width, 12dp shadow,
 * 24dp padding and overlay margin), 22sp semibold titles, a 46dp title bar, 44dp close target with a
 * 28dp red cap, and spring-based motion (220ms scrim/fade in, 150ms out, enter scale 0.92, exit 0.94).
 *
 * Customise by copying: `defaultMillenniumDialogTheme().let { t -> t.copy(states =
 * t.states.copy(default = t.states.default.copy(panelMaxWidth = 560.dp))) }`.
 *
 * @return A fresh [MillenniumDialogTheme] with palette-derived colours left null.
 * @see MillenniumDialogThemeProperties
 */
fun defaultMillenniumDialogTheme(): MillenniumDialogTheme {
    val base = MillenniumDialogThemeProperties(
        shape = RoundedCornerShape(10.dp),
        titleStyle = TextStyle(fontSize = 22.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold),
        panelBorderWidth = 1.dp,
        panelShadowElevation = 12.dp,
        panelMaxWidth = 480.dp,
        overlayPadding = 24.dp,
        contentPadding = 24.dp,
        contentSpacing = 16.dp,
        enterScale = 0.92f,
        animationMillis = 220,
    )
    return MillenniumDialogTheme(
        states = MillenniumControlStateProperty(
            default = base,
            hovered = base,
            focused = base.copy(panelBorderWidth = 2.dp),
            pressed = base.copy(panelShadowElevation = 8.dp),
            disabled = base.copy(alpha = 0.65f),
            selected = base,
            error = base.copy(panelBorderWidth = 2.dp),
        ),
    )
}

// ---------------------------------------------------------------------------
// Root
// ---------------------------------------------------------------------------

/**
 * Handle that lets bricks inside a [MillenniumDialog] dismiss the enclosing dialog.
 *
 * Created by [MillenniumDialog] (one per `onDismissRequest` instance) and published through
 * [LocalMillenniumDialog]. Callers do not construct it; [MillenniumDialogClose] reads it. Its
 * [dismiss] simply invokes the dialog's `onDismissRequest`; the caller still owns `visible`.
 *
 * @property dismiss Invokes the dialog's dismiss request.
 */
@Stable
class MillenniumDialogScope internal constructor(val dismiss: () -> Unit)

/**
 * The enclosing dialog's [MillenniumDialogScope], or null outside a [MillenniumDialog].
 *
 * Provided by [MillenniumDialog] around its content. Read it from custom bricks to dismiss the dialog:
 *
 * ```kotlin
 * val dialog = LocalMillenniumDialog.current
 * MillenniumButton(onClick = { dialog?.dismiss?.invoke() }) { Text("Done") }
 * ```
 */
val LocalMillenniumDialog = staticCompositionLocalOf<MillenniumDialogScope?> { null }

/**
 * A themed, caller-owned modal dialog. Compose the panel from lego bricks.
 *
 * Built on Compose Unstyled's `UnstyledDialog`, so modal behaviour, focus handling and dismissal
 * (Escape / back, scrim click) come from that primitive and surface as [onDismissRequest]. The scrim
 * fades in and the panel springs in (scale plus a small upward slide); exit is a crisp fade, scale and
 * slide. [paneTitle] is the accessible name of the dialog pane. State is caller-owned: keep [visible]
 * in your own state and clear it in [onDismissRequest].
 *
 * ```kotlin
 * var visible by remember { mutableStateOf(false) }
 * MillenniumDialog(visible, onDismissRequest = { visible = false }) {
 *     MillenniumDialogHeader {              // fixed
 *         MillenniumDialogTitle { Text("Share playlist") }
 *         MillenniumDialogClose()
 *     }
 *     MillenniumDialogBody {                // scrolls when tall
 *         MillenniumDialogDescription { Text("Anyone with the link can listen.") }
 *         MillenniumTextField(state = linkState)
 *     }
 *     MillenniumDialogFooter {              // fixed, end-aligned actions
 *         MillenniumButton(onClick = { visible = false }, style = MillenniumButtonStyle.Secondary) { Text("Cancel") }
 *         MillenniumButton(onClick = { share(); visible = false }) { Text("Share") }
 *     }
 * }
 * ```
 *
 * Every brick is optional, and any composable may sit between them. [MillenniumDialogBody] must be a
 * direct child because it uses the column's weight.
 *
 * @param visible Whether the dialog is shown. Caller-owned.
 * @param onDismissRequest Called when the user asks to dismiss (Escape, scrim click, close button).
 * @param modifier Modifier applied to the dialog panel.
 * @param paneTitle Accessible title of the dialog pane announced by assistive technology.
 * @param panelBackground Panel fill override; null falls back to the theme, then `millenniumPanelBrush`.
 * @param shape Panel shape override; null falls back to the theme.
 * @param foreground Default content colour override; null falls back to the theme, then `colors.text`.
 * @param panelBorderColor Panel border colour override; null falls back to the theme.
 * @param panelBorderWidth Panel border thickness override; null falls back to the theme.
 * @param panelShadowElevation Panel shadow override; null falls back to the theme.
 * @param panelGlowColor Panel inner glow override; null falls back to the theme.
 * @param panelMaxWidth Maximum panel width override; null falls back to the theme.
 * @param overlayPadding Window-edge margin override; null falls back to the theme.
 * @param scrimColor Backdrop colour override; null falls back to the theme, then `colors.scrim`.
 * @param enterScale Initial scale when entering; null falls back to the theme.
 * @param exitScale Target scale when exiting; null falls back to the theme.
 * @param animationMillis Scrim/panel fade-in duration; null falls back to the theme.
 * @param closeAnimationMillis Exit duration; null falls back to the theme.
 * @param alpha Panel opacity; null falls back to the theme.
 * @param theme Component theme; defaults to `MillenniumTheme.dialogTheme`.
 * @param content Panel body in a [ColumnScope], composed from the `MillenniumDialog*` bricks.
 * @see MillenniumAlertDialog
 * @see MillenniumDialogHeader
 * @see MillenniumDialogBody
 * @see MillenniumDialogFooter
 */
@Composable
fun MillenniumDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    paneTitle: String = "Dialog",
    panelBackground: Brush? = null,
    shape: Shape? = null,
    foreground: Color? = null,
    panelBorderColor: Color? = null,
    panelBorderWidth: Dp? = null,
    panelShadowElevation: Dp? = null,
    panelGlowColor: Color? = null,
    panelMaxWidth: Dp? = null,
    overlayPadding: Dp? = null,
    scrimColor: Color? = null,
    enterScale: Float? = null,
    exitScale: Float? = null,
    animationMillis: Int? = null,
    closeAnimationMillis: Int? = null,
    alpha: Float? = null,
    theme: MillenniumDialogTheme = MillenniumTheme.dialogTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MillenniumTheme.colors
    val properties = theme.states.resolve(MillenniumControlState.Default)
    val resolvedForeground = foreground ?: properties.foreground ?: colors.text
    val resolvedPanelBorderColor = panelBorderColor ?: properties.panelBorderColor ?: colors.border
    val resolvedPanelBackground = panelBackground ?: properties.panelBackground ?: millenniumPanelBrush(colors)
    val resolvedPanelGlowColor = panelGlowColor ?: properties.panelGlowColor ?: colors.highlight.copy(alpha = 0.6f)
    val resolvedScrimColor = scrimColor ?: properties.scrimColor ?: colors.scrim
    val resolvedAnimationMillis = animationMillis ?: properties.animationMillis
    val resolvedCloseMillis = closeAnimationMillis ?: properties.closeAnimationMillis
    val scope = remember(onDismissRequest) { MillenniumDialogScope(onDismissRequest) }
    UnstyledDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        overlay = {
            Scrim(
                modifier = Modifier.millenniumActionPointer(),
                scrimColor = resolvedScrimColor,
                enter = fadeIn(animationSpec = tween(resolvedAnimationMillis, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(resolvedCloseMillis, easing = FastOutLinearInEasing)),
            )
        },
    ) {
        Box(
            Modifier.fillMaxSize().padding(overlayPadding ?: properties.overlayPadding),
            contentAlignment = Alignment.Center,
        ) {
            DialogPanel(
                modifier.widthIn(max = panelMaxWidth ?: properties.panelMaxWidth).fillMaxWidth()
                    .alpha(alpha ?: properties.alpha)
                    .millenniumSurface(
                        background = resolvedPanelBackground,
                        shape = shape ?: properties.shape,
                        borderColor = resolvedPanelBorderColor,
                        borderWidth = panelBorderWidth ?: properties.panelBorderWidth,
                        shadowElevation = panelShadowElevation ?: properties.panelShadowElevation,
                        innerGlowColor = resolvedPanelGlowColor,
                    ),
                paneTitle = paneTitle,
                enter = fadeIn(animationSpec = tween(resolvedAnimationMillis, easing = FastOutSlowInEasing)) +
                    scaleIn(
                        initialScale = enterScale ?: properties.enterScale,
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                    ) +
                    slideInVertically(
                        animationSpec = spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessMediumLow),
                        initialOffsetY = { it / 10 },
                    ),
                exit = fadeOut(animationSpec = tween(resolvedCloseMillis, easing = FastOutLinearInEasing)) +
                    scaleOut(
                        targetScale = exitScale ?: properties.exitScale,
                        animationSpec = tween(resolvedCloseMillis, easing = FastOutLinearInEasing),
                    ) +
                    slideOutVertically(
                        animationSpec = tween(resolvedCloseMillis, easing = FastOutLinearInEasing),
                        targetOffsetY = { it / 14 },
                    ),
            ) {
                CompositionLocalProvider(LocalMillenniumDialog provides scope) {
                    MillenniumStyledContent(TextStyle(color = resolvedForeground), resolvedForeground) {
                        Column { content() }
                    }
                }
            }
        }
    }
}

/**
 * Convenience dialog with a title bar, close cap and scrolling body, built from the same bricks as the
 * lego overload.
 *
 * [title] is used both as the visible heading and as the accessible pane title.
 *
 * ```kotlin
 * MillenniumDialog(visible, { visible = false }, title = "About") {
 *     MillenniumDialogDescription("Millennium: Aero for 2026.")
 * }
 * ```
 *
 * @param visible Whether the dialog is shown. Caller-owned.
 * @param onDismissRequest Called when the user asks to dismiss.
 * @param title Heading text and accessible pane title.
 * @param modifier Modifier applied to the dialog panel.
 * @param panelBackground Panel fill override; null falls back to the theme.
 * @param shape Panel shape override; null falls back to the theme.
 * @param foreground Default content colour override; null falls back to the theme.
 * @param titleStyle Title typography override; null falls back to the theme.
 * @param panelBorderColor Panel border colour override; null falls back to the theme.
 * @param panelBorderWidth Panel border thickness override; null falls back to the theme.
 * @param panelShadowElevation Panel shadow override; null falls back to the theme.
 * @param panelGlowColor Panel inner glow override; null falls back to the theme.
 * @param panelMaxWidth Maximum panel width override; null falls back to the theme.
 * @param overlayPadding Window-edge margin override; null falls back to the theme.
 * @param contentPadding Body padding (all sides) override; null falls back to the theme.
 * @param contentSpacing Spacing between body children; null falls back to the theme.
 * @param scrimColor Backdrop colour override; null falls back to the theme.
 * @param enterScale Initial scale when entering; null falls back to the theme.
 * @param exitScale Target scale when exiting; null falls back to the theme.
 * @param animationMillis Fade-in duration; null falls back to the theme.
 * @param closeAnimationMillis Exit duration; null falls back to the theme.
 * @param alpha Panel opacity; null falls back to the theme.
 * @param titleBarBackground Header fill override; null falls back to the theme.
 * @param titleBarForeground Header text colour override; null falls back to the theme.
 * @param titleBarHeight Minimum header height override; null falls back to the theme.
 * @param titleBarPadding Header padding override; null falls back to the theme.
 * @param titleBarBorderColor Header divider colour override; null falls back to the theme.
 * @param theme Component theme; defaults to `MillenniumTheme.dialogTheme`.
 * @param content Body content inside a scrolling [MillenniumDialogBody].
 * @see MillenniumDialog
 */
@Composable
fun MillenniumDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    panelBackground: Brush? = null,
    shape: Shape? = null,
    foreground: Color? = null,
    titleStyle: TextStyle? = null,
    panelBorderColor: Color? = null,
    panelBorderWidth: Dp? = null,
    panelShadowElevation: Dp? = null,
    panelGlowColor: Color? = null,
    panelMaxWidth: Dp? = null,
    overlayPadding: Dp? = null,
    contentPadding: Dp? = null,
    contentSpacing: Dp? = null,
    scrimColor: Color? = null,
    enterScale: Float? = null,
    exitScale: Float? = null,
    animationMillis: Int? = null,
    closeAnimationMillis: Int? = null,
    alpha: Float? = null,
    titleBarBackground: Brush? = null,
    titleBarForeground: Color? = null,
    titleBarHeight: Dp? = null,
    titleBarPadding: PaddingValues? = null,
    titleBarBorderColor: Color? = null,
    theme: MillenniumDialogTheme = MillenniumTheme.dialogTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
    MillenniumDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        paneTitle = title,
        panelBackground = panelBackground,
        shape = shape,
        foreground = foreground,
        panelBorderColor = panelBorderColor,
        panelBorderWidth = panelBorderWidth,
        panelShadowElevation = panelShadowElevation,
        panelGlowColor = panelGlowColor,
        panelMaxWidth = panelMaxWidth,
        overlayPadding = overlayPadding,
        scrimColor = scrimColor,
        enterScale = enterScale,
        exitScale = exitScale,
        animationMillis = animationMillis,
        closeAnimationMillis = closeAnimationMillis,
        alpha = alpha,
        theme = theme,
    ) {
        MillenniumDialogHeader(
            background = titleBarBackground,
            foreground = titleBarForeground,
            minimumHeight = titleBarHeight,
            contentPadding = titleBarPadding,
            dividerColor = titleBarBorderColor,
            theme = theme,
        ) {
            MillenniumDialogTitle(textStyle = titleStyle, theme = theme) { Text(title) }
            MillenniumDialogClose(theme = theme)
        }
        MillenniumDialogBody(
            contentPadding = contentPadding?.let { PaddingValues(it) },
            contentSpacing = contentSpacing,
            theme = theme,
            content = content,
        )
    }
}

/**
 * A ready-made confirmation dialog: optional [icon], [title], [description], an optional custom
 * [content] body and caller-supplied [actions] in the footer. Every part is a slot.
 *
 * The body is only emitted when a [description] or [content] is present. The header close cap can be
 * hidden with [showClose], but Escape and scrim dismissal still go through [onDismissRequest].
 *
 * ```kotlin
 * MillenniumAlertDialog(
 *     visible = confirmDelete,
 *     onDismissRequest = { confirmDelete = false },
 *     title = { Text("Delete playlist?") },
 *     description = { Text("This cannot be undone.") },
 *     icon = { MillenniumIcon(MillenniumIcons.Sparkle, contentDescription = null) },
 * ) {
 *     MillenniumButton(onClick = { confirmDelete = false }, style = MillenniumButtonStyle.Secondary) { Text("Cancel") }
 *     MillenniumButton(onClick = { delete(); confirmDelete = false }, style = MillenniumButtonStyle.Danger) { Text("Delete") }
 * }
 * ```
 *
 * @param visible Whether the dialog is shown. Caller-owned.
 * @param onDismissRequest Called when the user asks to dismiss.
 * @param title Title slot, placed in the header.
 * @param modifier Modifier applied to the dialog panel.
 * @param icon Optional leading header content.
 * @param description Optional supporting text slot, shown in the body.
 * @param paneTitle Accessible pane title; defaults to "Alert".
 * @param showClose Whether to show the [MillenniumDialogClose] cap in the header.
 * @param theme Component theme; defaults to `MillenniumTheme.dialogTheme`.
 * @param content Optional extra body content in a [ColumnScope], after the description.
 * @param actions Footer actions in a [RowScope], end-aligned.
 * @see MillenniumDialog
 */
@Composable
fun MillenniumAlertDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    description: (@Composable () -> Unit)? = null,
    paneTitle: String = "Alert",
    showClose: Boolean = true,
    theme: MillenniumDialogTheme = MillenniumTheme.dialogTheme,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit,
) {
    MillenniumDialog(visible, onDismissRequest, modifier, paneTitle = paneTitle, theme = theme) {
        MillenniumDialogHeader(theme = theme) {
            if (icon != null) icon()
            MillenniumDialogTitle(theme = theme, content = title)
            if (showClose) MillenniumDialogClose(theme = theme)
        }
        if (description != null || content != null) {
            MillenniumDialogBody(theme = theme) {
                if (description != null) MillenniumDialogDescription(theme = theme, content = description)
                content?.invoke(this)
            }
        }
        MillenniumDialogFooter(theme = theme, content = actions)
    }
}

// ---------------------------------------------------------------------------
// Header, title, close
// ---------------------------------------------------------------------------

/**
 * Fixed title bar. Place [MillenniumDialogTitle], icons and a [MillenniumDialogClose] inside.
 *
 * Children are laid out in a centred row with [contentSpacing]; a hairline divider closes the bar.
 * Optionally filled with the theme's title-bar brush (transparent by default).
 *
 * ```kotlin
 * MillenniumDialogHeader {
 *     MillenniumDialogTitle("Settings")
 *     MillenniumDialogClose()
 * }
 * ```
 *
 * @param modifier Modifier for the header column.
 * @param background Fill override; null falls back to the theme (transparent when none).
 * @param foreground Text colour override; null falls back to the theme, then `colors.text`.
 * @param minimumHeight Minimum bar height; null uses the theme (46dp).
 * @param contentPadding Row padding; null uses the theme.
 * @param contentSpacing Gap between children.
 * @param showDivider Draw the 1dp divider below the bar.
 * @param dividerColor Divider colour override; null falls back to the theme, then `colors.border`.
 * @param theme Component theme; defaults to `MillenniumTheme.dialogTheme`.
 * @param content Header content in a [RowScope].
 * @see MillenniumDialogFooter
 */
@Composable
fun MillenniumDialogHeader(
    modifier: Modifier = Modifier,
    background: Brush? = null,
    foreground: Color? = null,
    minimumHeight: Dp? = null,
    contentPadding: PaddingValues? = null,
    contentSpacing: Dp = 12.dp,
    showDivider: Boolean = true,
    dividerColor: Color? = null,
    theme: MillenniumDialogTheme = MillenniumTheme.dialogTheme,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MillenniumTheme.colors
    val properties = theme.states.default
    val resolvedBackground = background ?: properties.titleBarBackground
    val resolvedForeground = foreground ?: properties.titleBarForeground ?: properties.foreground ?: colors.text
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = minimumHeight ?: properties.titleBarHeight)
                .then(if (resolvedBackground != null) Modifier.background(resolvedBackground) else Modifier)
                .padding(contentPadding ?: properties.titleBarPadding),
            horizontalArrangement = Arrangement.spacedBy(contentSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MillenniumStyledContent(TextStyle.Default, resolvedForeground) { content() }
        }
        if (showDivider) {
            Box(
                Modifier.fillMaxWidth().height(1.dp)
                    .background(dividerColor ?: properties.titleBarBorderColor ?: colors.border),
            )
        }
    }
}

/**
 * The title; takes the remaining header width so trailing items sit at the end.
 *
 * Must be used inside a [MillenniumDialogHeader] (it relies on the row's weight).
 *
 * ```kotlin
 * MillenniumDialogHeader { MillenniumDialogTitle { Text("Share playlist") } }
 * ```
 *
 * @param modifier Modifier for the title box.
 * @param textStyle Typography override; null uses [MillenniumDialogThemeProperties.titleStyle].
 * @param foreground Colour override; null falls back to the theme's title bar/foreground colours.
 * @param theme Component theme; defaults to `MillenniumTheme.dialogTheme`.
 * @param content Title content.
 */
@Composable
fun RowScope.MillenniumDialogTitle(
    modifier: Modifier = Modifier,
    textStyle: TextStyle? = null,
    foreground: Color? = null,
    theme: MillenniumDialogTheme = MillenniumTheme.dialogTheme,
    content: @Composable () -> Unit,
) {
    val properties = theme.states.default
    Box(modifier.weight(1f)) {
        MillenniumStyledContent(
            textStyle ?: properties.titleStyle,
            foreground ?: properties.titleBarForeground ?: properties.foreground ?: MillenniumTheme.colors.text,
            content,
        )
    }
}

/**
 * String convenience overload of [MillenniumDialogTitle].
 *
 * @param text Title text.
 * @param modifier Modifier for the title box.
 * @param textStyle Typography override; null falls back to the theme.
 * @param foreground Colour override; null falls back to the theme.
 * @param theme Component theme; defaults to `MillenniumTheme.dialogTheme`.
 */
@Composable
fun RowScope.MillenniumDialogTitle(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle? = null,
    foreground: Color? = null,
    theme: MillenniumDialogTheme = MillenniumTheme.dialogTheme,
) {
    MillenniumDialogTitle(modifier, textStyle, foreground, theme) { Text(text) }
}

/**
 * A 44dp close target. By default it dismisses the enclosing dialog and draws the red glass cap; pass
 * [content] to draw anything else.
 *
 * Accessible as a button with [contentDescription]. Hover and keyboard focus grow it slightly and press
 * shrinks it with a spring; the focus look is keyboard-only. [content] receives the resolved
 * [MillenniumControlState]. With no [onClick] it calls [LocalMillenniumDialog]'s dismiss; outside a
 * dialog and without [onClick] it does nothing.
 *
 * ```kotlin
 * MillenniumDialogHeader {
 *     MillenniumDialogTitle("Share")
 *     MillenniumDialogClose(contentDescription = "Close share dialog")
 * }
 * ```
 *
 * @param modifier Modifier applied to the target.
 * @param onClick Custom click handler; null dismisses the enclosing dialog.
 * @param contentDescription Accessibility label announced for the button.
 * @param targetSize Touch target size; null uses [MillenniumDialogThemeProperties.closeTargetSize] (44dp).
 * @param theme Component theme; defaults to `MillenniumTheme.dialogTheme`.
 * @param content Visual drawn inside the target, given the current state; defaults to [MillenniumDialogCloseCap].
 * @see MillenniumDialogCloseCap
 */
@Composable
fun MillenniumDialogClose(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentDescription: String = "Close dialog",
    targetSize: Dp? = null,
    theme: MillenniumDialogTheme = MillenniumTheme.dialogTheme,
    content: @Composable (MillenniumControlState) -> Unit = { MillenniumDialogCloseCap(it, theme = theme) },
) {
    val dismiss = LocalMillenniumDialog.current?.dismiss
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsKeyboardFocusedAsState()
    val state = when {
        pressed -> MillenniumControlState.Pressed
        focused -> MillenniumControlState.Focused
        hovered -> MillenniumControlState.Hovered
        else -> MillenniumControlState.Default
    }
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.90f
            hovered || focused -> 1.08f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium),
    )
    UnstyledButton(
        onClick = { (onClick ?: dismiss)?.invoke() },
        role = Role.Button,
        indication = null,
        interactionSource = interaction,
        contentPadding = PaddingValues(0.dp),
        contentAlignment = Alignment.Center,
        modifier = modifier.millenniumActionPointer()
            .size(targetSize ?: theme.states.default.closeTargetSize)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) { content(state) }
    }
}

/**
 * The default red glass close cap; reusable inside a custom [MillenniumDialogClose].
 *
 * Purely visual (no click handling or semantics). Elevation and border react to [state]: raised on
 * hover/focus, flat on press, accent border when focused.
 *
 * ```kotlin
 * MillenniumDialogClose { state -> MillenniumDialogCloseCap(state, size = 32.dp) }
 * ```
 *
 * @param state Current control state, usually supplied by [MillenniumDialogClose].
 * @param modifier Modifier for the cap.
 * @param size Cap size; null uses [MillenniumDialogThemeProperties.closeCapSize].
 * @param theme Component theme; defaults to `MillenniumTheme.dialogTheme`.
 * @param content Glyph drawn in the cap; defaults to a multiplication sign in the surface colour.
 */
@Composable
fun MillenniumDialogCloseCap(
    state: MillenniumControlState = MillenniumControlState.Default,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    theme: MillenniumDialogTheme = MillenniumTheme.dialogTheme,
    content: @Composable () -> Unit = {
        Text("×", color = MillenniumTheme.colors.surface, fontSize = 21.sp, fontWeight = FontWeight.Medium)
    },
) {
    val colors = MillenniumTheme.colors
    val hovered = state == MillenniumControlState.Hovered
    val focused = state == MillenniumControlState.Focused
    val pressed = state == MillenniumControlState.Pressed
    val elevation by animateDpAsState(
        targetValue = when {
            pressed -> 0.dp
            hovered || focused -> 3.dp
            else -> 1.dp
        },
        animationSpec = tween(120),
    )
    val capShape = RoundedCornerShape(5.dp)
    Box(
        modifier.size(size ?: theme.states.default.closeCapSize)
            .millenniumSurface(
                background = gelBrush(colors.danger, colors.danger.copy(alpha = 0.78f)),
                shape = capShape,
                borderColor = if (focused) colors.accent
                else colors.onAccent.copy(alpha = if (hovered) 0.85f else 0.62f),
                borderWidth = if (focused) 2.dp else 1.dp,
                shadowElevation = elevation,
                innerGlowColor = colors.highlight.copy(alpha = if (hovered) 0.65f else 0.45f),
            )
            .millenniumInteractionSurface(state, capShape, tint = colors.danger),
        contentAlignment = Alignment.Center,
    ) { content() }
}

// ---------------------------------------------------------------------------
// Body, description, footer
// ---------------------------------------------------------------------------

/**
 * The main region. Scrolls when the dialog is taller than the screen, so the header and footer stay put.
 * Must be placed directly inside [MillenniumDialog].
 *
 * It takes the remaining height via the column's `weight` (non-filling), which is why it is a
 * [ColumnScope] extension.
 *
 * ```kotlin
 * MillenniumDialogBody(scrollable = true) {
 *     MillenniumDialogDescription("Pick a quality.")
 *     MillenniumRadioGroup(...)
 * }
 * ```
 *
 * @param modifier Modifier for the body column.
 * @param scrollable Whether the body scrolls vertically when it overflows.
 * @param contentPadding Padding override; null uses the theme's uniform content padding.
 * @param contentSpacing Spacing between children; null uses the theme.
 * @param theme Component theme; defaults to `MillenniumTheme.dialogTheme`.
 * @param content Body content in a [ColumnScope].
 * @see MillenniumDialogHeader
 */
@Composable
fun ColumnScope.MillenniumDialogBody(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    contentPadding: PaddingValues? = null,
    contentSpacing: Dp? = null,
    theme: MillenniumDialogTheme = MillenniumTheme.dialogTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
    val properties = theme.states.default
    Column(
        modifier.weight(1f, fill = false).fillMaxWidth()
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(contentPadding ?: PaddingValues(properties.contentPadding)),
        verticalArrangement = Arrangement.spacedBy(contentSpacing ?: properties.contentSpacing),
    ) { content() }
}

/**
 * Muted supporting text for a dialog body; any composable may go inside.
 *
 * ```kotlin
 * MillenniumDialogDescription { Text("Anyone with the link can listen.") }
 * ```
 *
 * @param modifier Modifier for the description box.
 * @param textStyle Typography override; null uses [MillenniumDialogThemeProperties.descriptionStyle].
 * @param foreground Colour override; null uses `colors.muted`.
 * @param theme Component theme; defaults to `MillenniumTheme.dialogTheme`.
 * @param content Description content.
 */
@Composable
fun MillenniumDialogDescription(
    modifier: Modifier = Modifier,
    textStyle: TextStyle? = null,
    foreground: Color? = null,
    theme: MillenniumDialogTheme = MillenniumTheme.dialogTheme,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        MillenniumStyledContent(
            textStyle ?: theme.states.default.descriptionStyle,
            foreground ?: MillenniumTheme.colors.muted,
            content,
        )
    }
}

/**
 * String convenience overload of [MillenniumDialogDescription].
 *
 * @param text Description text.
 * @param modifier Modifier for the description box.
 * @param textStyle Typography override; null falls back to the theme.
 * @param foreground Colour override; null uses `colors.muted`.
 * @param theme Component theme; defaults to `MillenniumTheme.dialogTheme`.
 */
@Composable
fun MillenniumDialogDescription(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle? = null,
    foreground: Color? = null,
    theme: MillenniumDialogTheme = MillenniumTheme.dialogTheme,
) {
    MillenniumDialogDescription(modifier, textStyle, foreground, theme) { Text(text) }
}

/**
 * Fixed action row, end-aligned by default.
 *
 * A divider is drawn above the row. Order actions with the safest first and the primary last.
 *
 * ```kotlin
 * MillenniumDialogFooter {
 *     MillenniumButton(onClick = onCancel, style = MillenniumButtonStyle.Secondary) { Text("Cancel") }
 *     MillenniumButton(onClick = onConfirm) { Text("Confirm") }
 * }
 * ```
 *
 * @param modifier Modifier for the footer column.
 * @param horizontalArrangement Custom arrangement; null spaces actions by the theme's footer spacing, end-aligned.
 * @param background Fill override; null falls back to the theme (transparent when none).
 * @param contentPadding Row padding override; null falls back to the theme.
 * @param showDivider Draw the 1dp divider above the row.
 * @param dividerColor Divider colour override; null falls back to the theme, then `colors.border`.
 * @param theme Component theme; defaults to `MillenniumTheme.dialogTheme`.
 * @param content Actions in a [RowScope].
 * @see MillenniumDialogHeader
 */
@Composable
fun MillenniumDialogFooter(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal? = null,
    background: Brush? = null,
    contentPadding: PaddingValues? = null,
    showDivider: Boolean = true,
    dividerColor: Color? = null,
    theme: MillenniumDialogTheme = MillenniumTheme.dialogTheme,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MillenniumTheme.colors
    val properties = theme.states.default
    val resolvedBackground = background ?: properties.footerBackground
    Column(modifier.fillMaxWidth()) {
        if (showDivider) {
            Box(
                Modifier.fillMaxWidth().height(1.dp)
                    .background(dividerColor ?: properties.footerBorderColor ?: colors.border),
            )
        }
        Row(
            Modifier.fillMaxWidth()
                .then(if (resolvedBackground != null) Modifier.background(resolvedBackground) else Modifier)
                .padding(contentPadding ?: properties.footerPadding),
            horizontalArrangement = horizontalArrangement
                ?: Arrangement.spacedBy(properties.footerSpacing, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) { content() }
    }
}

/**
 * Preview of a dialog composed from header, body and footer bricks.
 */
@Preview
@Composable
private fun MillenniumDialogPreview() = MillenniumPreview {
    MillenniumDialog(visible = true, onDismissRequest = {}) {
        MillenniumDialogHeader {
            MillenniumDialogTitle { Text("A little update") }
            MillenniumDialogClose()
        }
        MillenniumDialogBody {
            MillenniumDialogDescription("A familiar message panel, dressed for today.")
        }
        MillenniumDialogFooter {
            MillenniumButton(onClick = {}, style = MillenniumButtonStyle.Secondary) { Text("Later") }
            MillenniumButton(onClick = {}) { Text("Update") }
        }
    }
}
