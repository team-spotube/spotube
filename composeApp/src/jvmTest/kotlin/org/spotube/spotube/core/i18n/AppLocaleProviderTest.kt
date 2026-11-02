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

package org.spotube.spotube.core.i18n

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.use
import org.spotube.spotube.modules.settings.SupportedLanguages
import kotlinx.coroutines.runBlocking
import org.spotube.spotube.core.i18n.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.common_cancel
import spotube.composeapp.generated.resources.plugin_installed_plugins_count
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalComposeUiApi::class)
class AppLocaleProviderTest {
    @Test
    fun switchingLanguageUpdatesStringsWithoutRestart() {
        val systemLocale = Locale.getDefault()
        var language by mutableStateOf(SupportedLanguages.DE)
        var cancel = ""
        var plugins = ""
        var direction = LayoutDirection.Ltr

        try {
            ImageComposeScene(width = 100, height = 100) {
                AppLocaleProvider(language) {
                    val cancelText = stringResource(Res.string.common_cancel)
                    val pluginsText = pluralStringResource(Res.plurals.plugin_installed_plugins_count, 3, 3)
                    val layoutDirection = LocalLayoutDirection.current
                    SideEffect {
                        cancel = cancelText
                        plugins = pluginsText
                        direction = layoutDirection
                    }
                }
            }.use { scene ->
                scene.render()
                assertEquals("Abbrechen", cancel)
                assertEquals("3 Plugins installiert", plugins)
                // Non-composable lookups (ViewModels, services) follow the override too.
                assertEquals("Abbrechen", runBlocking { getString(Res.string.common_cancel) })

                language = SupportedLanguages.ZH_TW
                scene.render()
                assertEquals("取消", cancel)
                assertEquals("已安裝 3 個外掛程式", plugins)

                language = SupportedLanguages.RU
                scene.render()
                assertEquals("3 плагина установлено", plugins)

                language = SupportedLanguages.AR
                scene.render()
                assertEquals(LayoutDirection.Rtl, direction)

                language = SupportedLanguages.SYSTEM
                scene.render()
                assertEquals(systemLocale, Locale.getDefault())
            }
        } finally {
            Locale.setDefault(systemLocale)
        }
    }
}
