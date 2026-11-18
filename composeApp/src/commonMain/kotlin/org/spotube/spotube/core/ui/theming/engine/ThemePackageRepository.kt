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

package org.spotube.spotube.core.ui.theming.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.swiftzer.semver.SemVer
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM

/**
 * How an incoming theme package relates to the installed theme with the same id, like the plugin manager's
 * install/update/replace prompts. Only [Update] is a plain upgrade; every other relation to an installed copy needs
 * the user to confirm an explicit replacement.
 */
enum class ThemeInstallKind {
    /** Nothing with this id is installed. */
    Install,
    /** The package is a newer semantic version than the installed one. */
    Update,
    /** The same version is installed already. */
    SameVersion,
    /** The package is older than the installed version. */
    Downgrade,
    /** The incoming version isn't valid semver, so it can't be compared. */
    InvalidIncomingVersion,
    /** The installed version isn't valid semver, so it can't be compared. */
    InvalidInstalledVersion,
}

/** Classifies [incoming] against the [installed] version (null = not installed). Pure, so it is unit tested. */
fun classifyThemeInstall(installed: String?, incoming: String?, isInstalled: Boolean = installed != null): ThemeInstallKind {
    if (!isInstalled) return ThemeInstallKind.Install
    val newVersion = incoming?.let(::parseSemVerOrNull) ?: return ThemeInstallKind.InvalidIncomingVersion
    val oldVersion = installed?.let(::parseSemVerOrNull) ?: return ThemeInstallKind.InvalidInstalledVersion
    return when {
        newVersion > oldVersion -> ThemeInstallKind.Update
        newVersion < oldVersion -> ThemeInstallKind.Downgrade
        else -> ThemeInstallKind.SameVersion
    }
}

private fun parseSemVerOrNull(version: String): SemVer? = try {
    SemVer.parse(version.trim())
} catch (_: Exception) {
    null
}

/** A validated, not-yet-installed theme shown to the user for confirmation. */
class PreparedTheme internal constructor(
    internal val raw: ByteArray,
    val theme: LoadedTheme,
    /** The installed theme this would replace (same id), if any. */
    val replaces: LoadedTheme?,
) {
    val kind: ThemeInstallKind = classifyThemeInstall(replaces?.meta?.version, theme.meta.version, isInstalled = replaces != null)
}

/**
 * Owns every installed theme, one folder each: `<app data>/themes/<id>/theme.zip`.
 *
 * Like plugins, a theme is a package with metadata (name, version, author, links, logo, license).
 * The stored bytes are re-validated on every launch with the same strict reader used at import,
 * so a tampered or corrupted file is rejected instead of trusted. Assets only ever live in memory
 * (Android additionally needs font files, which go to the cache under content-hash names).
 *
 * Which theme is *selected* is a user setting; this class only stores and loads them.
 */
class ThemePackageRepository(private val dataDir: String, private val cacheDir: String) {
    private val fs = FileSystem.SYSTEM
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _installed = MutableStateFlow<List<LoadedTheme>>(emptyList())
    val installed: StateFlow<List<LoadedTheme>> = _installed.asStateFlow()

    /** The bundled Modern theme (`builtin:modern`); no package, nothing to install or remove. */
    val modern: LoadedTheme = BuiltInThemes.loadModern(cacheDir)

    /** The theme to apply for the selected id: a built-in one, an installed one, or null for the plain Millennium look. */
    fun themeFor(selectedId: String?, installed: List<LoadedTheme>): LoadedTheme? = when {
        BuiltInThemes.isBuiltIn(selectedId) -> modern
        else -> installed.firstOrNull { it.id == selectedId }
    }

    /** False until the first scan of the themes folder finished, so the UI doesn't flash defaults. */
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val root: Path get() = "$dataDir/themes".toPath()

    init {
        scope.launch { loadInstalled() }
    }

    private suspend fun loadInstalled() = withContext(Dispatchers.IO) {
        val loaded = ArrayList<LoadedTheme>()
        if (fs.exists(root)) {
            for (dir in fs.list(root)) {
                val zip = dir / ZIP_NAME
                if (!fs.exists(zip)) continue
                try {
                    val theme = loadTheme(readThemeArchive(fs.read(zip) { readByteArray() }), cacheDir, LinkedHashSet())
                    // The folder name must be the theme id, otherwise a swapped file could shadow another theme.
                    if (theme != null && theme.id == dir.name) loaded += theme
                } catch (e: Exception) {
                    // Invalid on disk: skip it; the folder stays so the user can remove it by reinstalling.
                }
            }
        }
        _installed.value = loaded.sortedBy { it.name.lowercase() }
        _ready.value = true
    }

    /**
     * Validates [bytes] (a theme `.zip`) without installing anything. Throws [ThemePackageException]
     * with a reason when it is unsafe, invalid or lacks the required metadata.
     */
    suspend fun prepare(bytes: ByteArray): PreparedTheme = withContext(Dispatchers.Default) {
        val pkg = readThemeArchive(bytes)
        val theme = loadTheme(pkg, cacheDir, LinkedHashSet())
            ?: throw ThemePackageException(ThemePackageError.InvalidThemeJson)
        theme.meta.missingRequired(theme.doc.name.takeIf { theme.doc.hasExplicitName })?.let {
            throw ThemePackageException(ThemePackageError.MissingMetadata, it)
        }
        PreparedTheme(bytes, theme, _installed.value.firstOrNull { it.id == theme.id })
    }

    suspend fun install(prepared: PreparedTheme) = withContext(Dispatchers.IO) {
        val id = prepared.theme.id
        require(THEME_DIR.matches(id)) { "unsafe theme id" }
        val dir = root / id
        fs.createDirectories(dir)
        val tmp = dir / "incoming.tmp"
        fs.write(tmp) { write(prepared.raw) }
        fs.atomicMove(tmp, dir / ZIP_NAME)
        _installed.update { list -> (list.filter { it.id != id } + prepared.theme).sortedBy { it.name.lowercase() } }
    }

    suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        if (THEME_DIR.matches(id)) fs.deleteRecursively(root / id, mustExist = false)
        _installed.update { list -> list.filter { it.id != id } }
    }

    private companion object {
        const val ZIP_NAME = "theme.zip"
        val THEME_DIR = Regex("[A-Za-z0-9._-]{1,48}")
    }
}
