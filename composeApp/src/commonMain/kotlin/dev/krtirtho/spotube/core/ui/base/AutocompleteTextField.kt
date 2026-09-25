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

package dev.krtirtho.spotube.core.ui.base

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxSearchBroken

private val AutocompleteTextFieldMinHeight = 44.dp
private val AutocompleteMenuDefaultMaxHeight = 300.dp
private val AutocompleteMenuDefaultOffset = 4.dp

private data class ResolvedAutoCompleteTextFieldState(
    val background: Brush,
    val highlight: Color,
    val shape: Shape,
    val border: BaseUITheme.Border,
    val shadow: BaseUITheme.Shadow,
    val foreground: Color,
)

@Composable
private fun resolveTextFieldState(
    theme: BaseUITheme.TextFieldTheme,
    isFocused: Boolean,
    isHovered: Boolean,
): ResolvedAutoCompleteTextFieldState {
    return when {
        isFocused -> ResolvedAutoCompleteTextFieldState(
            theme.background.focused,
            theme.highlight.focused,
            theme.shape.focused,
            theme.border.focused,
            theme.shadow.focused,
            theme.foreground.focused
        )

        isHovered -> ResolvedAutoCompleteTextFieldState(
            theme.background.hovered,
            theme.highlight.hovered,
            theme.shape.hovered,
            theme.border.hovered,
            theme.shadow.hovered,
            theme.foreground.hovered
        )

        else -> ResolvedAutoCompleteTextFieldState(
            theme.background.normal,
            theme.highlight.normal,
            theme.shape.normal,
            theme.border.normal,
            theme.shadow.normal,
            theme.foreground.normal
        )
    }
}

@Composable
fun <T> AutocompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    items: List<T>,
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onItemSelected: (T) -> Unit = {},
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    textStyle: TextStyle = TextStyle.Default,
    cursorBrush: Color? = null,
    onKeyEvent: ((KeyEvent) -> Boolean)? = null,
    menuMaxHeight: Dp = AutocompleteMenuDefaultMaxHeight,
    menuOffset: Dp = AutocompleteMenuDefaultOffset,
    textFieldTheme: BaseUITheme.TextFieldTheme? = null,
    menuTheme: BaseUITheme.AutoCompleteMenuTheme? = null,
) {
    val density = LocalDensity.current
    val baseTextFieldTheme = LocalBaseUITheme.current.textField
    val baseMenuTheme = LocalBaseUITheme.current.autoCompleteMenuTheme
    val resolvedTextFieldTheme = textFieldTheme ?: baseTextFieldTheme
    val resolvedMenuTheme = menuTheme ?: baseMenuTheme
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    var isMenuOpen by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var textFieldSize by remember { mutableStateOf(IntSize.Zero) }
    val menuListState = rememberLazyListState()
    val textFieldFocusRequester = remember { FocusRequester() }

    val menuExpanded = isMenuOpen && items.isNotEmpty()

    LaunchedEffect(isFocused) {
        if (isFocused) {
            isMenuOpen = true
        }
    }

    LaunchedEffect(value) {
        if (selectedIndex != -1) selectedIndex = -1
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0 && menuExpanded) {
            menuListState.scrollToItem(selectedIndex)
        }
    }

    val wrappedKeyboardActions = remember(keyboardActions, menuExpanded, selectedIndex, items) {
        KeyboardActions(
            onSearch = {
                if (menuExpanded && selectedIndex in items.indices) {
                    onItemSelected(items[selectedIndex])
                    selectedIndex = -1
                    isMenuOpen = false
                } else {
                    keyboardActions.onSearch?.invoke(this)
                }
            },
            onDone = keyboardActions.onDone,
            onGo = keyboardActions.onGo,
            onNext = keyboardActions.onNext,
            onPrevious = keyboardActions.onPrevious,
            onSend = keyboardActions.onSend,
        )
    }

    val fieldState = resolveTextFieldState(resolvedTextFieldTheme, isFocused, isHovered)

    val resolvedBorder = if (isError) {
        BaseUITheme.Border(MaterialTheme.colorScheme.error, fieldState.border.width)
    } else {
        fieldState.border
    }

    val contentColor = when {
        !enabled -> fieldState.foreground.copy(alpha = 0.38f)
        else -> fieldState.foreground
    }

    val cursor = cursorBrush?.let { SolidColor(it) } ?: resolvedTextFieldTheme.cursor

    fun navigate(delta: Int) {
        if (items.isEmpty()) return
        selectedIndex = ((selectedIndex + delta) % items.size + items.size) % items.size
    }

    Box(modifier = modifier) {
        Column {
            AnimatedVisibility(
                visible = label != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                label?.invoke()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { textFieldSize = it }
                    .defaultMinSize(minHeight = AutocompleteTextFieldMinHeight)
                    .then(
                        if (fieldState.shadow.elevation > 0.dp) {
                            Modifier.shadow(
                                elevation = fieldState.shadow.elevation,
                                shape = fieldState.shape,
                                ambientColor = fieldState.shadow.ambientColor,
                                spotColor = fieldState.shadow.spotColor,
                            )
                        } else Modifier
                    )
                    .clip(fieldState.shape)
                    .background(fieldState.background, fieldState.shape)
                    .border(
                        BorderStroke(resolvedBorder.width, resolvedBorder.color),
                        fieldState.shape
                    )
                    .highlight(fieldState.highlight)
                    .padding(resolvedTextFieldTheme.padding),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (leadingIcon != null) {
                        leadingIcon()
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .hoverable(
                                    interactionSource = interactionSource,
                                    enabled = enabled,
                                )
                                .focusRequester(textFieldFocusRequester)
                                .onFocusChanged { state ->
                                    if (!state.isFocused) {
                                        isMenuOpen = false
                                        selectedIndex = -1
                                    }
                                }
                                .onPreviewKeyEvent { event ->
                                    if (onKeyEvent?.invoke(event) == true) {
                                        return@onPreviewKeyEvent true
                                    }
                                    if (!menuExpanded) return@onPreviewKeyEvent false
                                    when (event.key) {
                                        Key.DirectionDown -> {
                                            navigate(1)
                                            true
                                        }

                                        Key.DirectionUp -> {
                                            if (selectedIndex >= 0) {
                                                navigate(-1)
                                                true
                                            } else false
                                        }

                                        Key.Enter -> {
                                            if (selectedIndex in items.indices) {
                                                onItemSelected(items[selectedIndex])
                                                selectedIndex = -1
                                                isMenuOpen = false
                                                return@onPreviewKeyEvent true
                                            }
                                            false
                                        }

                                        Key.Escape -> {
                                            selectedIndex = -1
                                            isMenuOpen = false
                                            true
                                        }

                                        else -> false
                                    }
                                },
                            enabled = enabled,
                            readOnly = readOnly,
                            textStyle = textStyle.copy(color = contentColor),
                            cursorBrush = cursor,
                            keyboardOptions = keyboardOptions,
                            keyboardActions = wrappedKeyboardActions,
                            singleLine = singleLine,
                            maxLines = maxLines,
                            visualTransformation = visualTransformation,
                            interactionSource = interactionSource,
                            decorationBox = { innerTextField ->
                                if (value.isEmpty() && placeholder != null && !isFocused) {
                                    CompositionLocalProvider(LocalTextStyle provides textStyle) {
                                        Box(contentAlignment = Alignment.CenterStart) {
                                            placeholder()
                                        }
                                    }
                                } else {
                                    innerTextField()
                                }
                            },
                        )
                    }

                    if (trailingIcon != null) {
                        trailingIcon()
                    }
                }
            }
        }

        if (menuExpanded && textFieldSize.width > 0) {
            val textFieldHeightPx = textFieldSize.height
            val menuOffsetPx = with(density) { menuOffset.roundToPx() }
            val textFieldWidthDp = with(density) { textFieldSize.width.toDp() }

            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, textFieldHeightPx + menuOffsetPx),
                properties = PopupProperties(focusable = false),
                onDismissRequest = {
                    selectedIndex = -1
                    isMenuOpen = false
                },
            ) {
                Box(
                    modifier = Modifier
                        .width(textFieldWidthDp)
                        .heightIn(max = menuMaxHeight)
                        .shadow(resolvedMenuTheme.shadowElevation, resolvedMenuTheme.shape)
                        .background(resolvedMenuTheme.background, resolvedMenuTheme.shape)
                        .clip(resolvedMenuTheme.shape),
                ) {
                    LazyColumn(
                        state = menuListState,
                        modifier = Modifier
                            .heightIn(max = menuMaxHeight),
                    ) {
                        items(items.size) { index ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onItemSelected(items[index])
                                        selectedIndex = -1
                                        isMenuOpen = false
                                        textFieldFocusRequester.requestFocus()
                                    },
                            ) {
                                itemContent(items[index], selectedIndex == index)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AutocompleteTextFieldPreview() {
    MaterialTheme {
        val theme = rememberBaseUITheme()
        CompositionLocalProvider(LocalBaseUITheme provides theme) {
            androidx.compose.material3.Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.padding(24.dp),
            ) {
                val artists = remember {
                    listOf(
                        "Twenty One Pilots",
                        "The Beatles",
                        "Adele",
                        "Drake",
                        "Taylor Swift",
                        "Arctic Monkeys",
                        "Billie Eilish",
                    )
                }
                var value by remember { mutableStateOf("") }

                AutocompleteTextField(
                    value = value,
                    onValueChange = {
                        value = it
                    },
                    items = artists.filter {
                        it.contains(value, ignoreCase = true) && value.isNotEmpty()
                    },
                    onItemSelected = { selected ->
                        value = selected
                    },
                    placeholder = { Text("Search artists...") },
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            imageVector = Iconsax.IconsaxSearchBroken,
                            contentDescription = "Search",
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    itemContent = { item, isSelected ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    } else {
                                        Color.Transparent
                                    },
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Iconsax.IconsaxSearchBroken,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    },
                )
            }
        }
    }
}
