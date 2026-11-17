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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.spotube.millennium.theme.MillenniumPalette
import org.spotube.spotube.core.ui.theming.engine.LoadedTheme
import org.spotube.spotube.core.ui.theming.engine.PreparedTheme
import org.spotube.spotube.core.ui.theming.engine.ThemeLimits
import org.spotube.spotube.core.ui.theming.engine.ThemePackageError
import org.spotube.spotube.core.ui.theming.engine.ThemePackageException
import org.spotube.spotube.core.ui.theming.engine.ThemePackageRepository

/** Why an import was refused; the UI maps each to a message. */
enum class ThemeImportError { Unreadable, TooLarge, Corrupt, Unsafe, Disallowed, InvalidTheme, InvalidFont, InvalidImage, MissingMetadata }

/** One theme card: a built-in Millennium look or an installed theme package. */
@Immutable
data class ThemeCardUi(
    val id: String,
    /** Display name; blank for built-in themes, whose name comes from a string resource. */
    val name: String,
    val version: String?,
    val author: String?,
    val description: String?,
    val logo: ImageBitmap?,
    /** Set for the built-in themes (one per palette). */
    val builtinPalette: MillenniumPalette?,
    val selected: Boolean,
    val repository: String?,
    val bugs: String?,
    val homepage: String?,
    val contact: String?,
    val license: String?,
    val licenseText: String?,
    val fontFiles: Int,
    val icons: Int,
    val problems: List<String>,
) {
    val isBuiltin: Boolean get() = builtinPalette != null
}

/** A freshly validated theme awaiting the user's confirmation. */
@Immutable
data class PendingThemeUi(val card: ThemeCardUi, val replacesVersion: String?, val replaces: Boolean, val sizeKb: Int)

@Immutable
data class ThemeStoreUiState(
    val themes: List<ThemeCardUi> = emptyList(),
    val pending: PendingThemeUi? = null,
    val importError: ThemeImportError? = null,
    /** The missing manifest field for [ThemeImportError.MissingMetadata]. */
    val importErrorDetail: String? = null,
)

private const val BUILTIN_PREFIX = "builtin:"
private const val MILLENNIUM_REPO = "https://github.com/team-spotube/millennium"

private fun builtinId(palette: MillenniumPalette) = BUILTIN_PREFIX + palette.name.lowercase()

/** Lists, selects, installs and removes themes (built-in Millennium looks plus installed packages). */
class ThemeStoreViewModel(
    private val settings: SettingsRepository,
    private val themes: ThemePackageRepository,
) : ViewModel() {
    private val prepared = MutableStateFlow<PreparedTheme?>(null)
    private val importError = MutableStateFlow<Pair<ThemeImportError, String?>?>(null)

    val uiState: StateFlow<ThemeStoreUiState> = combine(
        settings.userSettings, themes.installed, prepared, importError,
    ) { userSettings, installed, candidate, error ->
        val selectedId = userSettings.selectedThemeId
            ?.takeIf { id -> id.startsWith(BUILTIN_PREFIX) || installed.any { it.id == id } }
            ?: builtinId(userSettings.palette)
        val cards = MillenniumPalette.entries.map { it.toBuiltinCard(selectedId) } +
            installed.map { it.toCard(selectedId) }
        ThemeStoreUiState(
            themes = cards,
            pending = candidate?.let {
                PendingThemeUi(
                    card = it.theme.toCard(selectedId),
                    replacesVersion = it.replaces?.meta?.version,
                    replaces = it.replaces != null,
                    sizeKb = it.theme.packageBytes / 1024,
                )
            },
            importError = error?.first,
            importErrorDetail = error?.second,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeStoreUiState())

    fun select(id: String) {
        viewModelScope.launch {
            val current = settings.getCurrentSettings()
            val palette = MillenniumPalette.entries.firstOrNull { builtinId(it) == id }
            when {
                palette != null -> settings.updateSettings(current.copy(selectedThemeId = id, palette = palette))
                themes.installed.value.any { it.id == id } -> settings.updateSettings(current.copy(selectedThemeId = id))
            }
        }
    }

    fun remove(id: String) {
        viewModelScope.launch {
            val current = settings.getCurrentSettings()
            themes.remove(id)
            if (current.selectedThemeId == id) {
                settings.updateSettings(current.copy(selectedThemeId = builtinId(current.palette)))
            }
        }
    }

    /** Reads the picked file (size-checked first) and validates it. Nothing is installed yet. */
    fun onFileSelected(file: PlatformFile?) {
        if (file == null) return
        viewModelScope.launch {
            try {
                if (file.size() > ThemeLimits.MAX_ARCHIVE_BYTES) {
                    importError.value = ThemeImportError.TooLarge to null
                    return@launch
                }
                prepare(file.readBytes())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                importError.value = ThemeImportError.Unreadable to null
            }
        }
    }

    /** Validates [bytes] (a theme zip). On success the user is asked to confirm. */
    fun prepare(bytes: ByteArray) {
        viewModelScope.launch {
            importError.value = null
            prepared.value = null
            try {
                prepared.value = themes.prepare(bytes)
            } catch (e: ThemePackageException) {
                importError.value = e.error.toUi() to e.detail
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                importError.value = ThemeImportError.InvalidTheme to null
            }
        }
    }

    fun confirmInstall() {
        val candidate = prepared.value ?: return
        viewModelScope.launch {
            try {
                themes.install(candidate)
                settings.updateSettings(settings.getCurrentSettings().copy(selectedThemeId = candidate.theme.id))
                prepared.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                importError.value = ThemeImportError.Unreadable to null
            }
        }
    }

    fun cancelInstall() {
        prepared.value = null
    }

    fun dismissError() {
        importError.value = null
    }

    private fun MillenniumPalette.toBuiltinCard(selectedId: String) = ThemeCardUi(
        id = builtinId(this), name = "", version = null, author = "Spotube Team", description = null, logo = null,
        builtinPalette = this, selected = builtinId(this) == selectedId,
        repository = MILLENNIUM_REPO, bugs = "$MILLENNIUM_REPO/issues", homepage = null, contact = null,
        license = "AGPL-3.0", licenseText = null, fontFiles = 0, icons = 0, problems = emptyList(),
    )

    private fun LoadedTheme.toCard(selectedId: String) = ThemeCardUi(
        id = id, name = name, version = meta.version, author = meta.author, description = meta.description, logo = logo,
        builtinPalette = null, selected = id == selectedId,
        repository = meta.repository, bugs = meta.bugs, homepage = meta.homepage, contact = meta.contact,
        license = meta.license, licenseText = licenseText, fontFiles = fontFileCount, icons = iconCount,
        problems = problems.toList(),
    )

    private fun ThemePackageError.toUi() = when (this) {
        ThemePackageError.TooLarge, ThemePackageError.EntryTooLarge, ThemePackageError.TooManyEntries -> ThemeImportError.TooLarge
        ThemePackageError.Corrupt, ThemePackageError.Encrypted -> ThemeImportError.Corrupt
        ThemePackageError.UnsafePath, ThemePackageError.DuplicateEntry -> ThemeImportError.Unsafe
        ThemePackageError.DisallowedFile -> ThemeImportError.Disallowed
        ThemePackageError.MissingThemeJson, ThemePackageError.InvalidThemeJson -> ThemeImportError.InvalidTheme
        ThemePackageError.InvalidFont -> ThemeImportError.InvalidFont
        ThemePackageError.InvalidImage -> ThemeImportError.InvalidImage
        ThemePackageError.MissingMetadata -> ThemeImportError.MissingMetadata
    }
}
