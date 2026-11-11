package org.spotube.millennium.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import org.spotube.millennium.theme.collectIsKeyboardFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.composeunstyled.Text
import com.composeunstyled.TextInput
import com.composeunstyled.UnstyledButton
import com.composeunstyled.UnstyledTextField
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumStyledContent
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumActionPointer
import org.spotube.millennium.theme.gelBrush
import org.spotube.millennium.theme.millenniumBevelBrush
import org.spotube.millennium.theme.millenniumSurface
import org.spotube.millennium.theme.millenniumInteractionSurface
import androidx.compose.foundation.hoverable

/**
 * Component-owned popup layout and suggestion-row styling for [MillenniumAutocomplete].
 *
 * The editor itself is styled by [MillenniumTextFieldTheme]; these properties cover only the popup
 * menu. The popup resolves the `selected` state while open and `default` otherwise; rows resolve
 * `pressed`, `selected` (highlighted), `hovered` or `default`. Null colours and brushes derive from the
 * active palette.
 *
 * @property panelBackground Popup fill. Null uses `millenniumBevelBrush(colors)`.
 * @property panelShape Popup corner shape.
 * @property panelBorderColor Popup hairline colour. Null uses `colors.border`.
 * @property panelBorderWidth Popup hairline thickness.
 * @property panelShadowElevation Drop-shadow elevation under the popup.
 * @property panelGlowColor Popup inner glow. Null uses `colors.highlight` at 50% alpha.
 * @property minimumMenuWidth Minimum popup width; the popup is at least as wide as the editor.
 * @property menuMaxHeight Maximum popup height before the results list scrolls.
 * @property menuPadding Padding between the popup edge and its content.
 * @property menuSectionPadding Padding of the fixed header and footer sections.
 * @property dividerColor Colour of the double hairline around header/footer. Null uses `colors.border`.
 * @property itemBackground Fill of a non-highlighted row. Null is transparent.
 * @property highlightedItemBackground Fill of the highlighted row. Null uses an accent gel.
 * @property itemForeground Row text colour override for every state. Null derives it.
 * @property highlightedItemForeground Text colour of the highlighted row. Null uses `colors.onAccent`.
 * @property itemShape Row corner shape.
 * @property itemPadding Padding inside a row.
 * @property itemMinimumHeight Minimum row height; 44dp by default.
 * @property itemSpacing Vertical gap between rows.
 * @property itemTextStyle Row typography.
 * @property animationMillis General animation duration kept for the popup theme.
 * @property openAnimationMillis Duration of the popup fade-in.
 * @property closeAnimationMillis Duration of the popup exit transition.
 * @property itemStateAnimationMillis Duration of row colour, border and elevation transitions.
 * @property alpha Overall popup opacity.
 *
 * @see MillenniumAutocompleteTheme
 * @see defaultMillenniumAutocompleteTheme
 */
@Immutable
data class MillenniumAutocompleteThemeProperties(
    val panelBackground: Brush? = null,
    val panelShape: Shape,
    val panelBorderColor: Color? = null,
    val panelBorderWidth: Dp,
    val panelShadowElevation: Dp,
    val panelGlowColor: Color? = null,
    val minimumMenuWidth: Dp,
    val menuMaxHeight: Dp,
    val menuPadding: PaddingValues,
    val menuSectionPadding: PaddingValues,
    val dividerColor: Color? = null,
    val itemBackground: Brush? = null,
    val highlightedItemBackground: Brush? = null,
    val itemForeground: Color? = null,
    val highlightedItemForeground: Color? = null,
    val itemShape: Shape,
    val itemPadding: PaddingValues,
    val itemMinimumHeight: Dp,
    val itemSpacing: Dp,
    val itemTextStyle: TextStyle,
    val animationMillis: Int,
    val openAnimationMillis: Int = 170,
    val closeAnimationMillis: Int = 130,
    val itemStateAnimationMillis: Int = 120,
    val alpha: Float = 1f,
)

/**
 * Theme of the [MillenniumAutocomplete] popup, resolved per control state.
 *
 * Installed as `MillenniumTheme.autocompleteTheme`. The input field is themed separately via
 * `MillenniumTheme.textFieldTheme`.
 *
 * @property states State-dependent [MillenniumAutocompleteThemeProperties].
 *
 * @see defaultMillenniumAutocompleteTheme
 */
@Immutable
data class MillenniumAutocompleteTheme(
    val states: MillenniumControlStateProperty<MillenniumAutocompleteThemeProperties>,
)

/**
 * Builds the stock autocomplete theme: a 10dp rounded bevelled popup (240dp minimum width, 320dp max
 * height, 8dp shadow, 4dp padding), 7dp rounded rows with a 44dp minimum height and 14sp text, and
 * spring-based open (170ms) and close (130ms) motion. Focus and error thicken the border, disabled
 * drops to 45% alpha.
 *
 * Customise by copying: `defaultMillenniumAutocompleteTheme().let { t -> t.copy(states =
 * t.states.copy(default = t.states.default.copy(menuMaxHeight = 240.dp))) }`.
 *
 * @return A fresh [MillenniumAutocompleteTheme] with palette-derived colours left null.
 * @see MillenniumAutocompleteThemeProperties
 */
fun defaultMillenniumAutocompleteTheme(): MillenniumAutocompleteTheme {
    val base = MillenniumAutocompleteThemeProperties(
        panelShape = RoundedCornerShape(10.dp),
        panelBorderWidth = 1.dp,
        panelShadowElevation = 8.dp,
        minimumMenuWidth = 240.dp,
        menuMaxHeight = 320.dp,
        menuPadding = PaddingValues(4.dp),
        menuSectionPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        itemShape = RoundedCornerShape(7.dp),
        itemPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        itemMinimumHeight = 44.dp,
        itemSpacing = 2.dp,
        itemTextStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
        animationMillis = 120,
    )
    return MillenniumAutocompleteTheme(
        states = MillenniumControlStateProperty(
            default = base,
            hovered = base,
            focused = base.copy(panelBorderWidth = 2.dp),
            pressed = base,
            disabled = base.copy(alpha = 0.45f),
            selected = base,
            error = base.copy(panelBorderWidth = 2.dp),
        ),
    )
}

/**
 * Appearance and interaction facts passed to a custom suggestion row.
 *
 * Created for each row by [MillenniumAutocomplete] and handed to its `suggestionContent` lambda.
 * Read-only; the foreground is already animated.
 *
 * @property highlighted True for the keyboard/pointer highlighted row (the one Enter would commit).
 * @property hovered True while the pointer is over the row.
 * @property pressed True while the row is being pressed.
 * @property foreground Current text colour (on-accent for the highlighted gel row).
 */
@Immutable
data class MillenniumAutocompleteItemVisuals(
    val highlighted: Boolean,
    val hovered: Boolean,
    val pressed: Boolean,
    val foreground: Color,
)

/**
 * Editable field with a pointer-selectable, keyboard-navigable suggestion menu.
 *
 * Focus remains on the editor while the non-focusable popup is open, so Tab and default focus traversal
 * are untouched. Suggestions are caller-filtered: compute [suggestions] from [state]'s current text and
 * pass them in. The popup is shown while the editor is focused, enabled, and there is something to show
 * (suggestions, [emptyContent], [menuHeader] or [menuFooter]), unless dismissed for the current query.
 *
 * The header and footer stay fixed while result rows scroll. Leading/trailing slots decorate default
 * rows; [suggestionContent] can replace a row completely.
 *
 * Keyboard: Down/Up move the highlight (clamped at the ends, no wrap; reopening a dismissed menu),
 * Enter commits the highlighted row (the first row if none is highlighted), and Escape dismisses
 * without clearing text. Modified keys (Alt/Ctrl/Meta) are ignored. Pointer: hovering a row highlights
 * it, clicking commits, clicking outside dismisses; the menu returns when the query changes. Committing
 * replaces the field text with `suggestionLabel(item)`, then calls [onSuggestionSelected]. Each row is
 * a button with a selected state and `contentDescription` equal to its label, with a minimum height of
 * 44dp by default. The focus ring of the editor appears only for keyboard focus. The popup is placed
 * below the editor (above if it does not fit) and enters/leaves with a spring/fade animation.
 *
 * ```kotlin
 * val query = rememberTextFieldState()
 * val results = remember(query.text) { artists.filter { it.name.contains(query.text, ignoreCase = true) } }
 *
 * MillenniumAutocomplete(
 *     state = query,
 *     label = "Artist",
 *     placeholder = "Search artists",
 *     suggestions = results,
 *     suggestionLabel = { it.name },
 *     onSuggestionSelected = { artist -> select(artist) },
 *     leadingContent = { MillenniumAvatar(it.initials, it.name, size = 28.dp) },
 *     trailingContent = { MillenniumSidebarBadge(it.followers) },
 *     menuFooter = { MillenniumText("Up/Down to navigate, Enter to select") },
 *     emptyContent = { MillenniumText("No artists found") },
 * )
 * ```
 *
 * @param T Type of a suggestion.
 * @param state Caller-owned text state of the editor.
 * @param label Optional label above the field; also the accessibility label of the editor.
 * @param suggestions Current, already filtered suggestions (empty hides the list).
 * @param onSuggestionSelected Called with the committed suggestion after the text was replaced.
 * @param suggestionLabel Maps a suggestion to its label text (row text and committed value).
 * @param modifier Modifier applied to the editor.
 * @param placeholder Hint shown while the field is empty.
 * @param enabled Disabled fields do not take focus and never show the popup.
 * @param error Optional error message shown beneath the field; also switches the field to its error state.
 * @param leadingContent Optional content before each default row label.
 * @param trailingContent Optional content after each default row label.
 * @param menuHeader Optional fixed content above the results (separated by a hairline).
 * @param menuFooter Optional fixed content below the results (separated by a hairline).
 * @param emptyContent Content shown instead of the list when [suggestions] is empty; also keeps the popup open.
 * @param suggestionContent Replaces the whole default row content; receives the item and its
 * [MillenniumAutocompleteItemVisuals].
 * @param panelBackground Popup fill override; null falls back to the theme.
 * @param panelShape Popup shape override; null falls back to the theme.
 * @param panelBorderColor Popup border colour override; null falls back to the theme.
 * @param panelBorderWidth Popup border thickness override; null falls back to the theme.
 * @param panelShadowElevation Popup shadow override; null falls back to the theme.
 * @param panelGlowColor Popup inner glow override; null falls back to the theme.
 * @param minimumMenuWidth Minimum popup width override; null falls back to the theme.
 * @param menuMaxHeight Maximum popup height override; null falls back to the theme.
 * @param menuPadding Popup padding override; null falls back to the theme.
 * @param menuSectionPadding Header/footer padding override; null falls back to the theme.
 * @param dividerColor Header/footer divider colour override; null falls back to the theme.
 * @param itemBackground Row fill override; null falls back to the theme.
 * @param highlightedItemBackground Highlighted row fill override; null falls back to the theme.
 * @param itemForeground Row text colour override; null falls back to the theme.
 * @param highlightedItemForeground Highlighted row text colour override; null falls back to the theme.
 * @param itemShape Row shape override; null falls back to the theme.
 * @param itemPadding Row padding override; null falls back to the theme.
 * @param itemMinimumHeight Minimum row height override; null falls back to the theme (44dp).
 * @param itemSpacing Gap between rows override; null falls back to the theme.
 * @param itemTextStyle Row typography override; null falls back to the theme.
 * @param openAnimationMillis Popup fade-in duration override; null falls back to the theme.
 * @param closeAnimationMillis Popup exit duration override; null falls back to the theme.
 * @param itemStateAnimationMillis Row state transition duration override; null falls back to the theme.
 * @param alpha Popup opacity override; null falls back to the theme.
 * @param theme Popup theme; defaults to `MillenniumTheme.autocompleteTheme`.
 * @param textFieldTheme Theme of the editor; defaults to `MillenniumTheme.textFieldTheme`.
 * @see MillenniumAutocompleteItemVisuals
 * @see MillenniumAutocompleteTheme
 * @see MillenniumTextField
 */
@Composable
fun <T> MillenniumAutocomplete(
    state: TextFieldState,
    label: String? = null,
    suggestions: List<T>,
    onSuggestionSelected: (T) -> Unit,
    suggestionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    error: String? = null,
    leadingContent: (@Composable (T) -> Unit)? = null,
    trailingContent: (@Composable (T) -> Unit)? = null,
    menuHeader: (@Composable ColumnScope.() -> Unit)? = null,
    menuFooter: (@Composable ColumnScope.() -> Unit)? = null,
    emptyContent: (@Composable ColumnScope.() -> Unit)? = null,
    suggestionContent: (@Composable RowScope.(T, MillenniumAutocompleteItemVisuals) -> Unit)? = null,
    panelBackground: Brush? = null,
    panelShape: Shape? = null,
    panelBorderColor: Color? = null,
    panelBorderWidth: Dp? = null,
    panelShadowElevation: Dp? = null,
    panelGlowColor: Color? = null,
    minimumMenuWidth: Dp? = null,
    menuMaxHeight: Dp? = null,
    menuPadding: PaddingValues? = null,
    menuSectionPadding: PaddingValues? = null,
    dividerColor: Color? = null,
    itemBackground: Brush? = null,
    highlightedItemBackground: Brush? = null,
    itemForeground: Color? = null,
    highlightedItemForeground: Color? = null,
    itemShape: Shape? = null,
    itemPadding: PaddingValues? = null,
    itemMinimumHeight: Dp? = null,
    itemSpacing: Dp? = null,
    itemTextStyle: TextStyle? = null,
    openAnimationMillis: Int? = null,
    closeAnimationMillis: Int? = null,
    itemStateAnimationMillis: Int? = null,
    alpha: Float? = null,
    theme: MillenniumAutocompleteTheme = MillenniumTheme.autocompleteTheme,
    textFieldTheme: MillenniumTextFieldTheme = MillenniumTheme.textFieldTheme,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focusRing by interaction.collectIsKeyboardFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val query = state.text.toString()
    var dismissedQuery by remember { mutableStateOf<String?>(null) }
    var highlightedIndex by remember { mutableIntStateOf(-1) }
    val hasMenuContent =
        suggestions.isNotEmpty() || emptyContent != null || menuHeader != null || menuFooter != null
    val menuOpen =
        autocompleteMenuShouldShow(enabled, focused, query, hasMenuContent, dismissedQuery)
    val controlState = when {
        !enabled -> MillenniumControlState.Disabled
        error != null -> MillenniumControlState.Error
        pressed -> MillenniumControlState.Pressed
        focusRing -> MillenniumControlState.Focused
        hovered -> MillenniumControlState.Hovered
        else -> MillenniumControlState.Default
    }
    val field = textFieldTheme.states.resolve(controlState)
    val colors = MillenniumTheme.colors
    val foreground = field.foreground
        ?: field.textStyle.color.takeUnless { it == Color.Unspecified }
        ?: if (enabled) colors.text else colors.muted
    val placeholderColor = field.placeholderColor
        ?: field.placeholderStyle.color.takeUnless { it == Color.Unspecified }
        ?: colors.muted
    val borderColor = field.borderColor ?: when (controlState) {
        MillenniumControlState.Focused, MillenniumControlState.Hovered, MillenniumControlState.Pressed -> colors.accent
        MillenniumControlState.Error -> colors.danger
        else -> colors.border
    }
    val animatedBorder by animateColorAsState(borderColor, tween(field.stateAnimationMillis))
    val menu = theme.states.resolve(
        if (menuOpen) MillenniumControlState.Selected else MillenniumControlState.Default,
    )
    val resolvedMenu = menu.copy(
        panelBackground = panelBackground ?: menu.panelBackground,
        panelShape = panelShape ?: menu.panelShape,
        panelBorderColor = panelBorderColor ?: menu.panelBorderColor,
        panelBorderWidth = panelBorderWidth ?: menu.panelBorderWidth,
        panelShadowElevation = panelShadowElevation ?: menu.panelShadowElevation,
        panelGlowColor = panelGlowColor ?: menu.panelGlowColor,
        minimumMenuWidth = minimumMenuWidth ?: menu.minimumMenuWidth,
        menuMaxHeight = menuMaxHeight ?: menu.menuMaxHeight,
        menuPadding = menuPadding ?: menu.menuPadding,
        menuSectionPadding = menuSectionPadding ?: menu.menuSectionPadding,
        dividerColor = dividerColor ?: menu.dividerColor,
        itemSpacing = itemSpacing ?: menu.itemSpacing,
        openAnimationMillis = openAnimationMillis ?: menu.openAnimationMillis,
        closeAnimationMillis = closeAnimationMillis ?: menu.closeAnimationMillis,
        alpha = alpha ?: menu.alpha,
    )
    val menuVisibility = remember { MutableTransitionState(false) }
    LaunchedEffect(menuOpen) { menuVisibility.targetState = menuOpen }
    val density = LocalDensity.current
    var anchorWidth by remember { mutableStateOf(0.dp) }
    val popupPositionProvider = remember(density) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val gap = with(density) { 4.dp.roundToPx() }
                val left = if (layoutDirection == LayoutDirection.Ltr) anchorBounds.left
                else anchorBounds.right - popupContentSize.width
                val x =
                    left.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                val below = anchorBounds.bottom + gap
                val y = if (below + popupContentSize.height <= windowSize.height) below
                else (anchorBounds.top - popupContentSize.height - gap).coerceAtLeast(0)
                return IntOffset(x, y)
            }
        }
    }

    LaunchedEffect(query, suggestions, focused) {
        highlightedIndex = if (focused && suggestions.isNotEmpty()) 0 else -1
    }
    fun commit(index: Int) {
        val item = suggestions.getOrNull(index) ?: return
        val value = suggestionLabel(item)
        state.edit { replace(0, length, value) }
        dismissedQuery = value
        highlightedIndex = -1
        onSuggestionSelected(item)
    }

    UnstyledTextField(
        state = state,
        modifier = modifier.alpha(field.alpha)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.isAltPressed || event.isCtrlPressed || event.isMetaPressed) {
                    return@onPreviewKeyEvent false
                }
                when (event.key) {
                    Key.Escape -> if (menuOpen) {
                        dismissedQuery = query
                        highlightedIndex = -1
                        true
                    } else false

                    Key.DirectionDown -> if (suggestions.isNotEmpty()) {
                        dismissedQuery = null
                        highlightedIndex = nextAutocompleteIndex(
                            highlightedIndex,
                            suggestions.size,
                            forward = true
                        )
                        true
                    } else false

                    Key.DirectionUp -> if (suggestions.isNotEmpty()) {
                        dismissedQuery = null
                        highlightedIndex = nextAutocompleteIndex(
                            highlightedIndex,
                            suggestions.size,
                            forward = false
                        )
                        true
                    } else false

                    Key.Enter -> if (menuOpen && suggestions.isNotEmpty()) {
                        commit(highlightedIndex.takeIf { it in suggestions.indices } ?: 0)
                        true
                    } else false

                    else -> false
                }
            },
        enabled = enabled,
        accessibilityLabel = label,
        interactionSource = interaction,
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = field.textStyle.copy(color = foreground),
        cursorBrush = SolidColor(field.cursorColor ?: colors.accent),
        selectionColors = TextSelectionColors(
            field.cursorColor ?: colors.accent,
            field.selectionColor ?: colors.accent.copy(alpha = 0.25f),
        ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(field.contentSpacing)) {
            if (!label.isNullOrBlank()) {
                Text(label, style = field.labelStyle.copy(color = foreground))
            }
            Box(
                Modifier.fillMaxWidth()
                    .heightIn(min = field.minimumHeight)
                    .hoverable(interaction, enabled)
                    .onGloballyPositioned { anchorWidth = with(density) { it.size.width.toDp() } }
                    .millenniumSurface(
                        background = field.background ?: millenniumBevelBrush(
                            colors,
                            sunken = true
                        ),
                        shape = field.shape,
                        borderColor = animatedBorder,
                        borderWidth = field.borderWidth,
                        insetShadeColor = field.insetShadeColor ?: colors.text.copy(alpha = 0.09f),
                        innerGlowColor = field.innerGlowColor ?: colors.border.copy(alpha = 0.4f),
                        bottomRimColor = field.bottomRimColor
                            ?: colors.highlight.copy(alpha = 0.8f),
                    )
                    .millenniumInteractionSurface(controlState, field.shape, animationMillis = field.stateAnimationMillis),
                contentAlignment = Alignment.CenterStart,
            ) {
                TextInput(
                    Modifier.fillMaxWidth().padding(field.contentPadding),
                    placeholder = {
                        Text(
                            placeholder,
                            style = field.placeholderStyle.copy(color = placeholderColor),
                            color = placeholderColor
                        )
                    },
                )
                if ((menuOpen || menuVisibility.currentState || menuVisibility.targetState) && anchorWidth > 0.dp) {
                    Popup(
                        popupPositionProvider = popupPositionProvider,
                        onDismissRequest = { dismissedQuery = query; highlightedIndex = -1 },
                        properties = PopupProperties(
                            focusable = false,
                            dismissOnBackPress = false,
                            dismissOnClickOutside = true,
                        ),
                    ) {
                        AutocompleteAnimatedMenu(
                            visibleState = menuVisibility,
                            suggestions = suggestions,
                            suggestionLabel = suggestionLabel,
                            highlightedIndex = highlightedIndex,
                            onHighlight = { highlightedIndex = it },
                            onSelect = ::commit,
                            leadingContent = leadingContent,
                            trailingContent = trailingContent,
                            suggestionContent = suggestionContent,
                            header = menuHeader,
                            footer = menuFooter,
                            emptyContent = emptyContent,
                            width = maxOf(anchorWidth, resolvedMenu.minimumMenuWidth),
                            properties = resolvedMenu,
                            theme = theme,
                            itemShape = itemShape,
                            itemPadding = itemPadding,
                            itemMinimumHeight = itemMinimumHeight,
                            itemTextStyle = itemTextStyle,
                            itemForeground = itemForeground,
                            highlightedItemForeground = highlightedItemForeground,
                            itemBackground = itemBackground,
                            highlightedItemBackground = highlightedItemBackground,
                            itemStateAnimationMillis = itemStateAnimationMillis,
                        )
                    }
                }
            }
            if (error != null) Text(
                error,
                style = field.errorStyle.copy(color = field.errorColor ?: colors.danger)
            )
        }
    }
}

/**
 * Computes the next highlighted row for Up/Down navigation.
 *
 * Clamps at both ends (no wrapping). With no current highlight (negative), forward selects the first
 * row and backward selects the last. Returns -1 when there are no items.
 *
 * @param current Current highlighted index, or a negative value for none.
 * @param itemCount Number of rows.
 * @param forward True for Down, false for Up.
 * @return The new highlighted index, or -1 if [itemCount] is not positive.
 */
internal fun nextAutocompleteIndex(current: Int, itemCount: Int, forward: Boolean): Int {
    if (itemCount <= 0) return -1
    val lastIndex = itemCount - 1
    return when {
        current < 0 && forward -> 0
        current < 0 -> lastIndex
        forward -> (current + 1).coerceAtMost(lastIndex)
        else -> (current - 1).coerceAtLeast(0)
    }
}

/**
 * Decides whether the suggestion popup should be visible.
 *
 * The popup shows only for an enabled, focused editor that has something to display, and not when the
 * user dismissed it for exactly the current query (it reappears once the query changes).
 *
 * @param enabled Whether the editor is enabled.
 * @param focused Whether the editor has focus.
 * @param query Current text.
 * @param hasContent Whether there are suggestions, empty content, header or footer to show.
 * @param dismissedQuery Query for which the user dismissed the popup, or null.
 * @return True if the popup should be open.
 */
internal fun autocompleteMenuShouldShow(
    enabled: Boolean,
    focused: Boolean,
    query: String,
    hasContent: Boolean,
    dismissedQuery: String?,
): Boolean = enabled && focused && hasContent && dismissedQuery != query

/**
 * Wraps [AutocompleteMenu] in the spring expand/scale/fade enter and fade/shrink exit transition.
 */
@Composable
private fun <T> AutocompleteAnimatedMenu(
    visibleState: MutableTransitionState<Boolean>,
    suggestions: List<T>,
    suggestionLabel: (T) -> String,
    highlightedIndex: Int,
    onHighlight: (Int) -> Unit,
    onSelect: (Int) -> Unit,
    leadingContent: (@Composable (T) -> Unit)?,
    trailingContent: (@Composable (T) -> Unit)?,
    suggestionContent: (@Composable RowScope.(T, MillenniumAutocompleteItemVisuals) -> Unit)?,
    header: (@Composable ColumnScope.() -> Unit)?,
    footer: (@Composable ColumnScope.() -> Unit)?,
    emptyContent: (@Composable ColumnScope.() -> Unit)?,
    width: Dp,
    properties: MillenniumAutocompleteThemeProperties,
    theme: MillenniumAutocompleteTheme,
    itemShape: Shape?,
    itemPadding: PaddingValues?,
    itemMinimumHeight: Dp?,
    itemTextStyle: TextStyle?,
    itemForeground: Color?,
    highlightedItemForeground: Color?,
    itemBackground: Brush?,
    highlightedItemBackground: Brush?,
    itemStateAnimationMillis: Int?,
) {
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(properties.openAnimationMillis, easing = FastOutSlowInEasing)) +
                expandVertically(
                    animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow),
                    expandFrom = Alignment.Top,
                ) + slideInVertically(
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
                    initialOffsetY = { -it / 6 },
                ) + scaleIn(
            initialScale = 0.94f,
            animationSpec = spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessMediumLow),
            transformOrigin = TransformOrigin(0.5f, 0f),
        ),
        exit = fadeOut(tween(properties.closeAnimationMillis, easing = FastOutLinearInEasing)) +
                shrinkVertically(
                    animationSpec = tween(properties.closeAnimationMillis, easing = FastOutLinearInEasing),
                    shrinkTowards = Alignment.Top,
                ) + slideOutVertically(
                    animationSpec = tween(properties.closeAnimationMillis, easing = FastOutLinearInEasing),
                    targetOffsetY = { -it / 8 },
                ) + scaleOut(
            targetScale = 0.96f,
            animationSpec = tween(properties.closeAnimationMillis, easing = FastOutLinearInEasing),
            transformOrigin = TransformOrigin(0.5f, 0f),
        ),
    ) {
        AutocompleteMenu(
            suggestions = suggestions,
            suggestionLabel = suggestionLabel,
            highlightedIndex = highlightedIndex,
            onHighlight = onHighlight,
            onSelect = onSelect,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            suggestionContent = suggestionContent,
            header = header,
            footer = footer,
            emptyContent = emptyContent,
            width = width,
            properties = properties,
            theme = theme,
            itemShape = itemShape,
            itemPadding = itemPadding,
            itemMinimumHeight = itemMinimumHeight,
            itemTextStyle = itemTextStyle,
            itemForeground = itemForeground,
            highlightedItemForeground = highlightedItemForeground,
            itemBackground = itemBackground,
            highlightedItemBackground = highlightedItemBackground,
            itemStateAnimationMillis = itemStateAnimationMillis,
        )
    }
}

/**
 * Popup surface: fixed header, scrolling rows with hover/press/highlight styling, fixed footer.
 */
@Composable
private fun <T> AutocompleteMenu(
    suggestions: List<T>,
    suggestionLabel: (T) -> String,
    highlightedIndex: Int,
    onHighlight: (Int) -> Unit,
    onSelect: (Int) -> Unit,
    leadingContent: (@Composable (T) -> Unit)?,
    trailingContent: (@Composable (T) -> Unit)?,
    suggestionContent: (@Composable RowScope.(T, MillenniumAutocompleteItemVisuals) -> Unit)?,
    header: (@Composable ColumnScope.() -> Unit)?,
    footer: (@Composable ColumnScope.() -> Unit)?,
    emptyContent: (@Composable ColumnScope.() -> Unit)?,
    width: Dp,
    properties: MillenniumAutocompleteThemeProperties,
    theme: MillenniumAutocompleteTheme,
    itemShape: Shape?,
    itemPadding: PaddingValues?,
    itemMinimumHeight: Dp?,
    itemTextStyle: TextStyle?,
    itemForeground: Color?,
    highlightedItemForeground: Color?,
    itemBackground: Brush?,
    highlightedItemBackground: Brush?,
    itemStateAnimationMillis: Int?,
) {
    val colors = MillenniumTheme.colors
    val scroll = rememberScrollState()
    Column(
        Modifier.width(width)
            .heightIn(max = properties.menuMaxHeight)
            .alpha(properties.alpha)
            .millenniumSurface(
                background = properties.panelBackground ?: millenniumBevelBrush(colors),
                shape = properties.panelShape,
                borderColor = properties.panelBorderColor ?: colors.border,
                borderWidth = properties.panelBorderWidth,
                shadowElevation = properties.panelShadowElevation,
                innerGlowColor = properties.panelGlowColor ?: colors.highlight.copy(alpha = 0.5f),
            )
            .padding(properties.menuPadding),
    ) {
        if (header != null) {
            Column(Modifier.fillMaxWidth().padding(properties.menuSectionPadding), content = header)
            AutocompleteDivider(properties.dividerColor ?: colors.border)
        }
        if (suggestions.isEmpty() && emptyContent != null) {
            Column(Modifier.weight(1f, fill = false).fillMaxWidth(), content = emptyContent)
        } else {
            Column(
                Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(properties.itemSpacing),
            ) {
                suggestions.forEachIndexed { index, item ->
                    val highlighted = index == highlightedIndex
                    val interaction = remember(item) { MutableInteractionSource() }
                    val hovered by interaction.collectIsHoveredAsState()
                    val pressed by interaction.collectIsPressedAsState()
                    LaunchedEffect(hovered) { if (hovered) onHighlight(index) }
                    val itemTheme = theme.states.resolve(
                        when {
                            pressed -> MillenniumControlState.Pressed
                            highlighted -> MillenniumControlState.Selected
                            hovered -> MillenniumControlState.Hovered
                            else -> MillenniumControlState.Default
                        },
                    )
                    val resolvedItemAnimationMillis =
                        itemStateAnimationMillis ?: itemTheme.itemStateAnimationMillis
                    val targetForeground = itemForeground ?: itemTheme.itemForeground
                    ?: if (highlighted) {
                        highlightedItemForeground ?: itemTheme.highlightedItemForeground
                        ?: colors.onAccent
                    } else colors.text
                    val foreground by animateColorAsState(
                        targetForeground,
                        tween(resolvedItemAnimationMillis),
                    )
                    val background = if (highlighted) {
                        highlightedItemBackground ?: itemTheme.highlightedItemBackground
                        ?: gelBrush(colors.accentTop, colors.accentBottom)
                    } else itemBackground ?: itemTheme.itemBackground
                    ?: Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                    val targetBorder = when {
                        highlighted -> colors.accentBottom
                        hovered -> colors.border
                        else -> Color.Transparent
                    }
                    val border by animateColorAsState(
                        targetBorder,
                        tween(resolvedItemAnimationMillis),
                    )
                    val elevation by animateDpAsState(
                        when {
                            pressed -> 0.dp
                            highlighted -> 2.dp
                            hovered -> 1.dp
                            else -> 0.dp
                        },
                        tween(resolvedItemAnimationMillis),
                    )
                    val rowScale by animateFloatAsState(
                        when {
                            pressed -> 0.98f
                            hovered && !highlighted -> 1.008f
                            else -> 1f
                        },
                        spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
                    )
                    UnstyledButton(
                        onClick = { onSelect(index) },
                        interactionSource = interaction,
                        indication = null,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.millenniumActionPointer().fillMaxWidth()
                            .heightIn(min = itemMinimumHeight ?: itemTheme.itemMinimumHeight)
                            .semantics {
                                selected = highlighted
                                contentDescription = suggestionLabel(item)
                            }
                            .graphicsLayer { scaleX = rowScale; scaleY = rowScale }
                            .millenniumSurface(
                                background = background,
                                shape = itemShape ?: itemTheme.itemShape,
                                borderColor = border,
                                borderWidth = if (highlighted || hovered) 1.dp else 0.dp,
                                shadowElevation = elevation,
                                // Only the highlighted row's gel is opaque; a hover fill is translucent.
                                opaqueFill = highlighted,
                                innerGlowColor = if (highlighted || hovered) colors.highlight.copy(
                                    alpha = 0.25f
                                ) else null,
                            )
                            .millenniumInteractionSurface(
                                when {
                                    pressed -> MillenniumControlState.Pressed
                                    hovered -> MillenniumControlState.Hovered
                                    else -> MillenniumControlState.Default
                                },
                                itemShape ?: itemTheme.itemShape,
                                animationMillis = resolvedItemAnimationMillis,
                            ),
                    ) {
                        MillenniumStyledContent(
                            itemTextStyle ?: itemTheme.itemTextStyle,
                            foreground
                        ) {
                            Row(
                                Modifier.fillMaxWidth()
                                    .padding(itemPadding ?: itemTheme.itemPadding),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                if (suggestionContent != null) {
                                    suggestionContent(
                                        item,
                                        MillenniumAutocompleteItemVisuals(
                                            highlighted,
                                            hovered,
                                            pressed,
                                            foreground
                                        )
                                    )
                                } else {
                                    leadingContent?.invoke(item)
                                    Box(Modifier.weight(1f)) { Text(suggestionLabel(item)) }
                                    trailingContent?.invoke(item)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (footer != null) {
            AutocompleteDivider(properties.dividerColor ?: colors.border)
            Column(Modifier.fillMaxWidth().padding(properties.menuSectionPadding), content = footer)
        }
    }
}

/**
 * Double hairline (full and 35% alpha) used around the header and footer.
 */
@Composable
private fun AutocompleteDivider(color: Color) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(color))
        Box(Modifier.fillMaxWidth().height(1.dp).background(color.copy(alpha = 0.35f)))
    }
}
