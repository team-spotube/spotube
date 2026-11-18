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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.spotube.spotube.core.network.HttpClientFactory
import org.spotube.spotube.modules.plugin.GitHubRepo
import org.spotube.spotube.modules.update.ThemeUpdateService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.toArgb
import org.spotube.millennium.theme.MillenniumColorSchemes
import org.spotube.millennium.theme.MillenniumPalette
import org.spotube.millennium.theme.resolve
import org.spotube.spotube.core.i18n.getString
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_palette_blue
import spotube.composeapp.generated.resources.settings_palette_lime
import spotube.composeapp.generated.resources.settings_palette_orange
import spotube.composeapp.generated.resources.settings_theme_accent_label
import org.spotube.spotube.core.ui.theming.engine.BuiltInThemes
import org.spotube.spotube.core.ui.theming.engine.LoadedTheme
import org.spotube.spotube.core.ui.theming.engine.PreparedTheme
import org.spotube.spotube.core.ui.theming.engine.ThemeInstallKind
import org.spotube.spotube.core.ui.theming.engine.ThemeLimits
import org.spotube.spotube.core.ui.theming.engine.ThemePackageError
import org.spotube.spotube.core.ui.theming.engine.ThemePackageException
import org.spotube.spotube.core.ui.theming.engine.ThemeDocument
import org.spotube.spotube.core.ui.theming.engine.ThemePackageRepository
import org.spotube.spotube.core.ui.theming.engine.ThemeVarDecl
import org.spotube.spotube.core.ui.theming.engine.ThemeVarType
import org.spotube.spotube.core.ui.theming.engine.parseHexColor
import kotlinx.serialization.json.JsonPrimitive

/** Why an import was refused; the UI maps each to a message. */
enum class ThemeImportError { Unreadable, TooLarge, Corrupt, Unsafe, Disallowed, InvalidTheme, InvalidFont, InvalidImage, MissingMetadata }

/** One choice of an enum parameter; [color] is a swatch for colour-themed choices (accent presets). */
@Immutable
data class ThemeParamOptionUi(val value: String, val label: String, val color: String?)

/** A user-adjustable setting a theme offers, ready to render as a control. */
@Immutable
data class ThemeParamUi(
    val name: String,
    val label: String,
    val description: String?,
    val type: ThemeVarType,
    /** Current value as text: `#RRGGBB`, a number, `true`/`false` or an option value. */
    val value: String,
    val default: String,
    val min: Float,
    val max: Float,
    val step: Float,
    val options: List<ThemeParamOptionUi>,
    val presets: List<String>,
)

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
    /** True for the plain Millennium look, whose only setting is its accent (Blue/Orange/Lime). */
    val isMillennium: Boolean = false,
    /** `#RRGGBB` of the current accent, drawn as the logo of the built-in Millennium card. */
    val swatchColor: String? = null,
    /** True for the bundled Modern theme, whose name and description come from string resources. */
    val isModern: Boolean = false,
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
    /** The settings this theme offers to the user (empty for most themes). */
    val parameters: List<ThemeParamUi> = emptyList(),
    /** True when the user has changed any parameter from its default. */
    val hasCustomParameters: Boolean = false,
    /** True when the theme declares where to look for new versions (`updates` in theme.json). */
    val checksForUpdates: Boolean = false,
    /** A newer version found by the last update check. */
    val updateVersion: String? = null,
) {
    val isBuiltin: Boolean get() = isMillennium || isModern
}

/** A freshly validated theme awaiting the user's confirmation. */
@Immutable
data class PendingThemeUi(
    val card: ThemeCardUi,
    val kind: ThemeInstallKind,
    /** Version of the installed copy this replaces, when there is one. */
    val installedVersion: String?,
    val incomingVersion: String?,
    val sizeKb: Int,
)

/** A GitHub repository tagged `spotube` and `spotube-theme`. */
@Immutable
data class ThemeRepoUi(
    val fullName: String,
    val name: String,
    val owner: String,
    val avatarUrl: String,
    val description: String?,
    val stars: Int,
    val url: String,
)

@Immutable
data class ThemeAssetUi(val name: String, val url: String)

/** A release of a theme repository with the `.zip` assets that can be installed. */
@Immutable
data class ThemeReleaseUi(val tag: String, val title: String, val prerelease: Boolean, val assets: List<ThemeAssetUi>)

/** The "discover themes on GitHub" list and the release picker opened from it. */
@Immutable
data class ThemeDiscoverUi(
    val repos: List<ThemeRepoUi> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false,
    val canLoadMore: Boolean = false,
    /** The repository whose releases are being browsed, if any. */
    val repo: ThemeRepoUi? = null,
    val releases: List<ThemeReleaseUi> = emptyList(),
    val loadingReleases: Boolean = false,
    val releasesFailed: Boolean = false,
    /** URL of the asset being downloaded (also set while an update downloads). */
    val downloading: String? = null,
    val downloadFailed: Boolean = false,
)

@Immutable
data class ThemeStoreUiState(
    val themes: List<ThemeCardUi> = emptyList(),
    val pending: PendingThemeUi? = null,
    val importError: ThemeImportError? = null,
    /** The missing manifest field for [ThemeImportError.MissingMetadata]. */
    val importErrorDetail: String? = null,
    val discover: ThemeDiscoverUi = ThemeDiscoverUi(),
    val checkingUpdates: Boolean = false,
    /** Set after a manual check when nothing is newer (so the UI can say so). */
    val upToDate: Boolean = false,
    val updateCheckFailed: Boolean = false,
)

private const val MILLENNIUM_ID = BuiltInThemes.MILLENNIUM_ID
private const val MILLENNIUM_REPO = "https://github.com/team-spotube/millennium"
private const val SPOTUBE_REPO = "https://github.com/team-spotube/spotube"


/** The theme id to treat as selected: unknown, removed and older `builtin:blue/orange/lime` ids all mean Millennium. */
internal fun normalizeSelectedThemeId(selected: String?, installedIds: List<String>): String =
    selected?.takeIf { it == MILLENNIUM_ID || it == BuiltInThemes.MODERN_ID || it in installedIds } ?: MILLENNIUM_ID

/** The accent swatch of [palette] (its light-mode accent), as `#RRGGBB`. */
internal fun MillenniumPalette.accentHex(): String {
    val c = MillenniumColorSchemes().resolve(this, dark = false).accent
    return "#${(c.toArgb() and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')}"
}

/** Lists, selects, installs and removes themes (built-in Millennium looks plus installed packages). */
class ThemeStoreViewModel(
    private val settings: SettingsRepository,
    private val themes: ThemePackageRepository,
    httpClientFactory: HttpClientFactory,
    private val updateService: ThemeUpdateService,
) : ViewModel() {
    private val github = ThemeGitHubRepository(httpClientFactory.json)
    private val discover = MutableStateFlow(ThemeDiscoverUi(loading = true))
    private var discoverPage = 1
    private val prepared = MutableStateFlow<PreparedTheme?>(null)

    /** Parameter edits not yet written to settings (sliders fire on every tick); null when persisted. */
    private val pendingParameters = MutableStateFlow<Map<String, String>?>(null)
    private val importError = MutableStateFlow<Pair<ThemeImportError, String?>?>(null)

    private val baseState: Flow<ThemeStoreUiState> = combine(
        settings.userSettings, themes.installed, prepared, importError, pendingParameters,
    ) { userSettings, installed, candidate, error, pendingParams ->
        val overrides = pendingParams ?: userSettings.themeParameters
        // Unknown ids and the older builtin:blue/orange/lime values all mean the Millennium look.
        val selectedId = normalizeSelectedThemeId(userSettings.selectedThemeId, installed.map { it.id })
        val cards = listOf(millenniumCard(selectedId, userSettings.palette)) +
            modernCard(selectedId, overrides) +
            installed.map { it.toCard(selectedId, overrides) }
        ThemeStoreUiState(
            themes = cards,
            pending = candidate?.let {
                PendingThemeUi(
                    card = it.theme.toCard(selectedId, overrides),
                    kind = it.kind,
                    installedVersion = it.replaces?.meta?.version,
                    incomingVersion = it.theme.meta.version,
                    sizeKb = it.theme.packageBytes / 1024,
                )
            },
            importError = error?.first,
            importErrorDetail = error?.second,
        )
    }

    val uiState: StateFlow<ThemeStoreUiState> = combine(baseState, discover, updateService.updates, updateService.status) { base, disc, found, status ->
        base.copy(
            themes = base.themes.map { it.copy(updateVersion = found[it.id]?.version) },
            discover = disc,
            checkingUpdates = status.checking,
            upToDate = status.upToDate,
            updateCheckFailed = status.failed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeStoreUiState())

    init {
        loadDiscover()
        // Persist once edits settle: the theme recompiles on the saved value, so one drag costs one compile.
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            pendingParameters.filterNotNull().debounce(300).collect { values ->
                settings.updateSettings(settings.getCurrentSettings().copy(themeParameters = values))
                pendingParameters.compareAndSet(values, null)
            }
        }
    }

    /** Sets one parameter of [themeId], after checking [value] against the declared type, range and options. */
    fun setParameter(themeId: String, name: String, value: String) {
        if (themeId == MILLENNIUM_ID) {
            // The Millennium accent is the app-wide palette; it has no JSON document behind it.
            val palette = MillenniumPalette.entries.firstOrNull { it.name.lowercase() == value && name == "accent" } ?: return
            viewModelScope.launch { settings.updateSettings(settings.getCurrentSettings().copy(palette = palette)) }
            return
        }
        val decl = themeDocFor(themeId)?.parameters?.get(name) ?: return
        val clean = normalizeParameterValue(decl, value) ?: return
        viewModelScope.launch {
            val base = pendingParameters.value ?: settings.getCurrentSettings().themeParameters
            pendingParameters.value = base + ("$themeId.$name" to clean)
        }
    }

    /** Puts every parameter of [themeId] back to the theme's defaults. */
    fun resetParameters(themeId: String) {
        if (themeId == MILLENNIUM_ID) {
            viewModelScope.launch { settings.updateSettings(settings.getCurrentSettings().copy(palette = MillenniumPalette.Blue)) }
            return
        }
        viewModelScope.launch {
            val current = settings.getCurrentSettings()
            val remaining = (pendingParameters.value ?: current.themeParameters).filterKeys { !it.startsWith("$themeId.") }
            pendingParameters.value = null
            settings.updateSettings(current.copy(themeParameters = remaining))
        }
    }

    private fun themeDocFor(id: String) =
        (if (BuiltInThemes.isBuiltIn(id)) themes.modern else themes.installed.value.firstOrNull { it.id == id })?.doc

    fun select(id: String) {
        viewModelScope.launch {
            val current = settings.getCurrentSettings()
            if (id == MILLENNIUM_ID || BuiltInThemes.isBuiltIn(id) || themes.installed.value.any { it.id == id }) {
                settings.updateSettings(current.copy(selectedThemeId = id))
            }
        }
    }

    fun remove(id: String) {
        viewModelScope.launch {
            val current = settings.getCurrentSettings()
            themes.remove(id)
            if (current.selectedThemeId == id) {
                settings.updateSettings(current.copy(selectedThemeId = MILLENNIUM_ID))
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

    // ---- discover on GitHub ---------------------------------------------------------------------------------------

    /** Loads the first page of theme repositories (once; [retryDiscover] reloads). */
    fun loadDiscover() {
        if (discover.value.repos.isNotEmpty() && !discover.value.failed) return
        discoverPage = 1
        discover.update { it.copy(loading = true, failed = false) }
        viewModelScope.launch {
            runCatching { github.searchThemes(page = 1) }
                .onSuccess { r ->
                    discover.update { it.copy(repos = r.items.map(::toRepoUi), loading = false, canLoadMore = r.items.size < r.totalCount) }
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    discover.update { it.copy(loading = false, failed = true) }
                }
        }
    }

    fun retryDiscover() {
        discover.update { it.copy(repos = emptyList(), failed = false) }
        loadDiscover()
    }

    fun loadMoreDiscover() {
        val state = discover.value
        if (state.loading || !state.canLoadMore) return
        discover.update { it.copy(loading = true) }
        viewModelScope.launch {
            runCatching { github.searchThemes(page = discoverPage + 1) }
                .onSuccess { r ->
                    discoverPage++
                    discover.update {
                        val merged = (it.repos + r.items.map(::toRepoUi)).distinctBy { repo -> repo.fullName }
                        it.copy(repos = merged, loading = false, canLoadMore = r.items.isNotEmpty() && merged.size < r.totalCount)
                    }
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    discover.update { it.copy(loading = false, canLoadMore = false) }
                }
        }
    }

    private fun toRepoUi(r: GitHubRepo) = ThemeRepoUi(
        fullName = r.fullName,
        name = r.fullName.substringAfterLast('/'),
        owner = r.owner.login,
        avatarUrl = r.owner.avatarUrl,
        description = r.description,
        stars = r.stargazersCount,
        url = r.htmlUrl,
    )

    /** Opens the release picker of [repo]: every published release with the `.zip` assets it carries. */
    fun openRepo(repo: ThemeRepoUi) {
        discover.update { it.copy(repo = repo, releases = emptyList(), loadingReleases = true, releasesFailed = false, downloadFailed = false) }
        val (owner, name) = repo.fullName.split('/', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        viewModelScope.launch {
            runCatching { github.releases(owner, name) }
                .onSuccess { list ->
                    val releases = list.filter { !it.draft }.mapNotNull { r ->
                        val zips = r.assets.filter { it.name.endsWith(".zip", ignoreCase = true) && isGitHubAssetUrl(it.browserDownloadUrl) }
                        if (zips.isEmpty()) null
                        else ThemeReleaseUi(r.tagName, r.name?.takeIf { n -> n.isNotBlank() } ?: r.tagName, r.prerelease, zips.map { ThemeAssetUi(it.name, it.browserDownloadUrl) })
                    }
                    discover.update { it.copy(releases = releases, loadingReleases = false) }
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    discover.update { it.copy(loadingReleases = false, releasesFailed = true) }
                }
        }
    }

    fun closeRepo() {
        discover.update { it.copy(repo = null, releases = emptyList(), releasesFailed = false, downloadFailed = false) }
    }

    /** Downloads a release asset and validates it; on success the usual install dialog is shown. */
    fun installAsset(url: String) {
        if (discover.value.downloading != null) return
        discover.update { it.copy(downloading = url, downloadFailed = false) }
        viewModelScope.launch {
            try {
                val bytes = github.download(url)
                discover.update { it.copy(downloading = null, repo = null, releases = emptyList()) }
                prepare(bytes)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                discover.update { it.copy(downloading = null, downloadFailed = true) }
            }
        }
    }

    // ---- updates ------------------------------------------------------------------------------------------------

    /** The manual check of every installed theme with an update source (the selected one is also checked on app start). */
    fun checkUpdates() = updateService.checkAll()

    /** Downloads the newer release found for [themeId]; the install dialog then asks to confirm the update. */
    fun updateTheme(themeId: String) {
        val update = updateService.updates.value[themeId] ?: return
        installAsset(update.assetUrl)
    }

    fun confirmInstall() {
        val candidate = prepared.value ?: return
        viewModelScope.launch {
            try {
                themes.install(candidate)
                // A fresh install is selected right away; updating or replacing an installed theme keeps the user's choice.
                if (candidate.kind == ThemeInstallKind.Install) {
                    settings.updateSettings(settings.getCurrentSettings().copy(selectedThemeId = candidate.theme.id))
                }
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

    private suspend fun millenniumCard(selectedId: String, palette: MillenniumPalette) = ThemeCardUi(
        id = MILLENNIUM_ID, name = "", version = null, author = "Spotube Team", description = null, logo = null,
        isMillennium = true, swatchColor = palette.accentHex(), selected = MILLENNIUM_ID == selectedId,
        repository = MILLENNIUM_REPO, bugs = "$MILLENNIUM_REPO/issues", homepage = null, contact = null,
        license = "AGPL-3.0", licenseText = null, fontFiles = 0, icons = 0, problems = emptyList(),
        parameters = listOf(
            ThemeParamUi(
                name = "accent", label = getString(Res.string.settings_theme_accent_label), description = null,
                type = ThemeVarType.Enum, value = palette.name.lowercase(), default = MillenniumPalette.Blue.name.lowercase(),
                min = 0f, max = 100f, step = 1f,
                options = MillenniumPalette.entries.map {
                    ThemeParamOptionUi(
                        value = it.name.lowercase(),
                        label = getString(
                            when (it) {
                                MillenniumPalette.Blue -> Res.string.settings_palette_blue
                                MillenniumPalette.Orange -> Res.string.settings_palette_orange
                                MillenniumPalette.Lime -> Res.string.settings_palette_lime
                            }
                        ),
                        color = it.accentHex(),
                    )
                },
                presets = emptyList(),
            ),
        ),
        hasCustomParameters = palette != MillenniumPalette.Blue,
    )

    private fun modernCard(selectedId: String, overrides: Map<String, String>) = ThemeCardUi(
        id = BuiltInThemes.MODERN_ID, name = "", version = null, author = "Spotube Team", description = null, logo = null,
        isModern = true, selected = BuiltInThemes.MODERN_ID == selectedId,
        repository = SPOTUBE_REPO, bugs = "$SPOTUBE_REPO/issues", homepage = null, contact = null,
        license = "AGPL-3.0", licenseText = null, fontFiles = 0, icons = 0, problems = emptyList(),
        parameters = parameterUis(BuiltInThemes.MODERN_ID, themes.modern.doc, overrides),
        hasCustomParameters = overrides.keys.any { it.startsWith("${BuiltInThemes.MODERN_ID}.") },
    )

    private fun LoadedTheme.toCard(selectedId: String, overrides: Map<String, String>) = ThemeCardUi(
        id = id, name = name, version = meta.version, author = meta.author, description = meta.description, logo = logo,
        selected = id == selectedId,
        repository = meta.repository, bugs = meta.bugs, homepage = meta.homepage, contact = meta.contact,
        license = meta.license, licenseText = licenseText, fontFiles = fontFileCount, icons = iconCount,
        problems = problems.toList(),
        parameters = parameterUis(id, doc, overrides),
        hasCustomParameters = overrides.keys.any { it.startsWith("$id.") },
        checksForUpdates = meta.updates != null,
    )

    private fun parameterUis(themeId: String, doc: ThemeDocument, overrides: Map<String, String>): List<ThemeParamUi> =
        doc.parameters.values.map { d ->
            val default = (d.default as? JsonPrimitive)?.content.orEmpty()
            ThemeParamUi(
                name = d.name,
                label = d.label ?: d.name,
                description = d.description,
                type = d.type,
                value = overrides["$themeId.${d.name}"] ?: default,
                default = default,
                min = (d.min ?: 0.0).toFloat(),
                max = (d.max ?: 100.0).toFloat(),
                step = (d.step ?: 1.0).toFloat(),
                options = d.options.map { ThemeParamOptionUi(it, d.optionLabels[it] ?: it, d.optionColors[it]) },
                presets = d.presets,
            )
        }

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

/** Validates [raw] for [decl] and returns the cleaned text to store, or null if it is not acceptable. */
internal fun normalizeParameterValue(decl: ThemeVarDecl, raw: String): String? = when (decl.type) {
    ThemeVarType.Color -> raw.trim().takeIf { parseHexColor(it) != null }
    ThemeVarType.Number -> raw.trim().toDoubleOrNull()?.coerceIn(decl.min ?: Double.NEGATIVE_INFINITY, decl.max ?: Double.POSITIVE_INFINITY)
        ?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }
    ThemeVarType.Bool -> raw.trim().toBooleanStrictOrNull()?.toString()
    ThemeVarType.Enum -> raw.takeIf { it in decl.options }
    ThemeVarType.String -> raw.take(80)
}
