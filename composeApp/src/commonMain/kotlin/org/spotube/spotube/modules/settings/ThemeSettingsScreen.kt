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

package org.spotube.spotube.modules.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.spotube.spotube.modules.settings.sections.ThemeModeCard
import org.spotube.spotube.modules.settings.sections.settingsSectionCard
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import org.jetbrains.compose.resources.stringResource
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.modules.settings.sections.ThemesPanel
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_section_appearance
import spotube.composeapp.generated.resources.settings_theme_title
import org.spotube.spotube.modules.settings.sections.SettingsSectionLabel

/** Appearance as its own page on compact screens: the Light/Dark/System choice, then the themes. Large screens show the same inline in Settings. */
@Composable
fun ThemeSettingsScreen() {
    val shellBottomInset = LocalAppShellBottomInset.current
    val contentPadding = remember(shellBottomInset) {
        PaddingValues(top = 16.dp, bottom = 16.dp + shellBottomInset)
    }
    val settingsViewModel = koinViewModel<SettingsViewModel>()
    val settings by settingsViewModel.settingsState.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        ApplicationMainBar(title = { Text(stringResource(Res.string.settings_section_appearance)) })
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 1280.dp).align(Alignment.TopCenter),
                contentPadding = contentPadding,
            ) {
                settings?.let { current ->
                    item { SettingsSectionLabel(Res.string.settings_theme_title) }
                    settingsSectionCard(items = listOf { ThemeModeCard(current, settingsViewModel) })
                }
                item { ThemesPanel() }
            }
        }
    }
}
