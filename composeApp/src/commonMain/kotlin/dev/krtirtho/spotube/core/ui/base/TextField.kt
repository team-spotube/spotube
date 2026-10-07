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
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxSearchBroken
import dev.krtirtho.spotube.resources.iconsax.IconsaxTrash
import dev.krtirtho.spotube.resources.iconsax.User

private val TextFieldMinHeight = 44.dp

private data class ResolvedTextFieldState(
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
): ResolvedTextFieldState {
    return when {
        isFocused -> ResolvedTextFieldState(theme.background.focused, theme.highlight.focused, theme.shape.focused, theme.border.focused, theme.shadow.focused, theme.foreground.focused)
        isHovered -> ResolvedTextFieldState(theme.background.hovered, theme.highlight.hovered, theme.shape.hovered, theme.border.hovered, theme.shadow.hovered, theme.foreground.hovered)
        else -> ResolvedTextFieldState(theme.background.normal, theme.highlight.normal, theme.shape.normal, theme.border.normal, theme.shadow.normal, theme.foreground.normal)
    }
}

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    textStyle: TextStyle = TextStyle.Default,
    cursorBrush: Color? = null,
    theme: BaseUITheme.TextFieldTheme? = null,
) {
    val baseTheme = LocalBaseUITheme.current.textField
    val textFieldTheme = theme ?: baseTheme
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val state = resolveTextFieldState(textFieldTheme, isFocused, isHovered)

    val resolvedBorder = if (isError) {
        BaseUITheme.Border(MaterialTheme.colorScheme.error, state.border.width)
    } else {
        state.border
    }

    val contentColor = when {
        !enabled -> state.foreground.copy(alpha = 0.38f)
        else -> state.foreground
    }

    val cursor = cursorBrush?.let { SolidColor(it) } ?: textFieldTheme.cursor

    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = label != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            label?.invoke()
        }

        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = TextFieldMinHeight)
                .then(
                    if (state.shadow.elevation > 0.dp) {
                        Modifier.shadow(
                            elevation = state.shadow.elevation,
                            shape = state.shape,
                            ambientColor = state.shadow.ambientColor,
                            spotColor = state.shadow.spotColor,
                        )
                    } else Modifier
                )
                .clip(state.shape)
                .background(state.background, state.shape)
                .border(BorderStroke(resolvedBorder.width, resolvedBorder.color), state.shape)
                .highlight(state.highlight)
                .padding(textFieldTheme.padding),
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
                            .hoverable(interactionSource = interactionSource, enabled = enabled),
                        enabled = enabled,
                        readOnly = readOnly,
                        textStyle = textStyle.copy(color = contentColor),
                        cursorBrush = cursor,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
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
}

@BaseUIPhonePreview
@Composable
private fun TextFieldPreview() {
    BaseUIPreview {
        var email by remember { mutableStateOf("") }
        var query by remember { mutableStateOf("Twenty One Pilots") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("you@example.com") },
                label = { TextFieldPreviewLabel("Email address") },
                leadingIcon = {
                    Icon(
                        imageVector = Iconsax.User,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                singleLine = true,
            )

            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search songs, artists, albums...") },
                leadingIcon = {
                    Icon(
                        imageVector = Iconsax.IconsaxSearchBroken,
                        contentDescription = "Search",
                        modifier = Modifier.size(18.dp),
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Iconsax.IconsaxTrash,
                        contentDescription = "Clear",
                        modifier = Modifier.size(18.dp),
                    )
                },
                singleLine = true,
            )

            TextField(
                value = "not-an-email",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { TextFieldPreviewLabel("Error state") },
                isError = true,
                singleLine = true,
            )

            TextField(
                value = "Read only value",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { TextFieldPreviewLabel("Disabled") },
                enabled = false,
                singleLine = true,
            )
        }
    }
}

@Composable
private fun TextFieldPreviewLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(bottom = 6.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    )
}
