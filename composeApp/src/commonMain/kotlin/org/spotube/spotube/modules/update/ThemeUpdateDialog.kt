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

package org.spotube.spotube.modules.update

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.composeunstyled.Text
import com.mikepenz.markdown.m3.Markdown
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumAlertDialog
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.theme_update_dialog_current_version
import spotube.composeapp.generated.resources.theme_update_dialog_no_notes
import spotube.composeapp.generated.resources.theme_update_dialog_title
import spotube.composeapp.generated.resources.update_action_ignore
import spotube.composeapp.generated.resources.update_action_update

/** Offers a newer release of the selected theme. [onUpdate] downloads and installs it; [onIgnore] silences this version. */
@Composable
fun ThemeUpdateDialog(
    update: AvailableThemeUpdate,
    onDismiss: () -> Unit,
    onIgnore: () -> Unit,
    onUpdate: () -> Unit,
) {
    MillenniumAlertDialog(
        visible = true,
        onDismissRequest = onDismiss,
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        title = { Text(stringResource(Res.string.theme_update_dialog_title, update.themeName, update.update.version)) },
        description = {
            update.currentVersion?.let { Text(stringResource(Res.string.theme_update_dialog_current_version, it)) }
        },
        content = {
            if (update.update.notes.isNotBlank()) {
                SelectionContainer { Markdown(content = update.update.notes) }
            } else {
                Text(stringResource(Res.string.theme_update_dialog_no_notes))
            }
        },
    ) {
        MillenniumButton(onClick = onIgnore, style = MillenniumButtonStyle.Secondary) {
            Text(stringResource(Res.string.update_action_ignore))
        }
        MillenniumButton(onClick = onUpdate) {
            Text(stringResource(Res.string.update_action_update))
        }
    }
}
