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

package org.spotube.spotube.modules.settings.components


import org.spotube.millennium.composables.MillenniumDialogHeader
import org.spotube.millennium.composables.MillenniumDialogTitle
import org.spotube.millennium.composables.MillenniumDialogClose
import org.spotube.millennium.composables.MillenniumDialogBody
import org.spotube.millennium.composables.MillenniumDialogFooter
import org.spotube.millennium.composables.MillenniumDialogDescription
import androidx.compose.foundation.layout.Box
import com.composeunstyled.currentWindowWidthBreakpoint
import org.spotube.millennium.adaptive.WindowBreakpoints
import androidx.compose.ui.Alignment
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumDialog
import org.spotube.millennium.composables.MillenniumRadioGroup
import org.spotube.millennium.composables.MillenniumSwitch
import org.spotube.millennium.composables.MillenniumTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.rememberTextFieldState

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.spotube.spotube.core.ui.component.AdaptiveDropdownBottomSheet
import org.spotube.spotube.core.ui.component.AdaptiveMenuItem
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxArrowDown4
import kotlinx.coroutines.flow.drop
import spotube.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SwitchSettingCard(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: (@Composable () -> Unit)? = null
) {
    SettingCardItem(
        title = title,
        subtitle = subtitle,
        icon = icon,
        trailingContent = {
            MillenniumSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                contentSpacing = 0.dp
            )
        },
        onClick = {
            onCheckedChange(!checked)
        }
    )
}

@Composable
internal fun <T> SelectionSettingCard(
    title: String,
    subtitle: String? = null,
    selectedOption: T,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onOptionSelected: (T) -> Unit,
    dialogTitle: String = title,
    icon: (@Composable () -> Unit)? = null,
    inlineSelectorMinWidth: Dp = 700.dp,
    filter: ((AdaptiveMenuItem, String) -> Boolean)? = null
) {
    var isDialogOpen by remember { mutableStateOf(false) }

    Box {
        val isWideLayout = currentWindowWidthBreakpoint() isAtLeast WindowBreakpoints.Medium

        SettingCardItem(
            title = title,
            subtitle = subtitle,
            icon = icon,
            trailingContent = {
                AdaptiveDropdownBottomSheet(
                    items = options.map { option ->
                        AdaptiveMenuItem(
                            label = optionLabel(option),
                            onClick = { onOptionSelected(option) },
                            selected = option == selectedOption
                        )
                    },
                    trigger = { onClick ->
                        MillenniumButton(
                            onClick = onClick,
                            style = MillenniumButtonStyle.Secondary
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(optionLabel(selectedOption))
                                MillenniumIcon(
                                    imageVector = Iconsax.IconsaxArrowDown4,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    },
                    filter = filter
                )
            },
            onClick = {
                if (isWideLayout) {
                    isDialogOpen = true
                }
            }
        )

        if (!isWideLayout && isDialogOpen) {
            val labels = options.map { optionLabel(it) }
            MillenniumDialog(
                visible = true,
                onDismissRequest = { isDialogOpen = false },
                title = dialogTitle
            ) {
                MillenniumRadioGroup(
                    value = selectedOption,
                    onValueChange = { option ->
                        onOptionSelected(option)
                        isDialogOpen = false
                    },
                    options = options,
                    label = dialogTitle,
                    optionLabel = { option -> labels[options.indexOf(option)] })
                MillenniumButton(
                    onClick = { isDialogOpen = false },
                    style = MillenniumButtonStyle.Quiet
                ) {
                    Text(stringResource(Res.string.common_close))
                }
            }
        }
    }
}

@Composable
internal fun TextInputSettingCard(
    title: String,
    subtitle: String? = null,
    value: String,
    onValueSaved: (String) -> Unit,
    dialogTitle: String = title,
    dialogDescription: String? = null,
    placeholder: String = "",
    normalize: (String) -> String = { it.trim() },
    validate: (String) -> String? = { null },
    icon: (@Composable () -> Unit)? = null,
    inlineTextFieldMinWidth: Dp = 700.dp,
    inlineTextFieldWidth: Dp = 220.dp,
    enabled: Boolean = true
) {
    var isDialogOpen by remember { mutableStateOf(false) }

    Box {
        val isWideLayout = currentWindowWidthBreakpoint() isAtLeast WindowBreakpoints.Medium
        val inlineState = rememberTextFieldState(value)

        LaunchedEffect(inlineState, isWideLayout) {
            snapshotFlow { inlineState.text.toString() }
                .drop(1)
                .collect { draft ->
                    val normalizedValue = normalize(draft)
                    if (validate(normalizedValue) == null) {
                        onValueSaved(normalizedValue)
                    }
                }
        }

        SettingCardItem(
            enabled = enabled,
            title = title,
            subtitle = subtitle,
            icon = icon,
            trailingContent = if (isWideLayout) {
                {
                    MillenniumTextField(
                        state = inlineState,
                        modifier = Modifier.width(inlineTextFieldWidth),
                        placeholder = placeholder,
                        enabled = enabled
                    )
                }
            } else {
                null
            },
            onClick = {
                if (!isWideLayout) {
                    isDialogOpen = true
                }
            }
        )

        if (!isWideLayout && isDialogOpen) {
            val dialogState = rememberTextFieldState(value)
            var draft by remember(value, isDialogOpen) { mutableStateOf(value) }

            LaunchedEffect(dialogState) {
                snapshotFlow { dialogState.text.toString() }
                    .drop(1)
                    .collect { draft = it }
            }

            val normalizedValue = normalize(draft)
            val errorMessage = validate(normalizedValue)

            MillenniumDialog(visible = true, onDismissRequest = { isDialogOpen = false }) {
                MillenniumDialogHeader {
                    MillenniumDialogTitle(dialogTitle)
                    MillenniumDialogClose()
                }
                MillenniumDialogBody {
                    if (dialogDescription != null) {
                        MillenniumDialogDescription(dialogDescription)
                    }
                    MillenniumTextField(
                        state = dialogState,
                        label = title,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = placeholder,
                        error = errorMessage
                    )
                }
                MillenniumDialogFooter {
                    MillenniumButton(
                        onClick = { isDialogOpen = false },
                        style = MillenniumButtonStyle.Secondary
                    ) {
                        Text(stringResource(Res.string.common_cancel))
                    }
                    MillenniumButton(
                        onClick = {
                            if (errorMessage == null) {
                                onValueSaved(normalizedValue)
                                isDialogOpen = false
                            }
                        },
                        enabled = errorMessage == null
                    ) {
                        Text(stringResource(Res.string.common_save))
                    }
                }
            }
        }
    }
}
