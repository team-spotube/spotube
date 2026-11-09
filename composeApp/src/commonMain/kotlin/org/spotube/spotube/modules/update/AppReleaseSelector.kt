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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.swiftzer.semver.SemVer

@Serializable
internal data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val body: String? = null,
    val draft: Boolean = false)

/**
 * Picks the release to offer from GitHub's release list using full SemVer 2.0.0 precedence
 * (pre-release identifiers compared field by field, build metadata ignored).
 *
 * Stable installs are only offered stable releases. Pre-release installs (e.g. `6.0.0-beta.1`)
 * are also offered newer pre-releases, which `releases/latest` would never return.
 */
internal object AppReleaseSelector {
    /** Parses a release tag such as `v6.0.0`, `V6.0.0-beta.2` or `6.0.0+build.7`; null if not SemVer. */
    fun parseTag(tag: String): SemVer? {
        val trimmed = tag.trim()
        val withoutPrefix = if (trimmed.startsWith('v') || trimmed.startsWith('V')) trimmed.drop(1) else trimmed
        return SemVer.parseOrNull(withoutPrefix)
    }

    fun select(currentVersion: SemVer, releases: List<GitHubRelease>, ignoredTag: String?): GitHubRelease? {
        val ignoredVersion = ignoredTag?.let(::parseTag)
        val includePreReleases = currentVersion.preRelease != null

        return releases.asSequence()
            .filterNot { it.draft }
            .mapNotNull { release -> parseTag(release.tagName)?.let { release to it } }
            .filter { (_, version) -> includePreReleases || version.preRelease == null }
            .maxWithOrNull(compareBy { it.second })
            ?.takeIf { (_, version) -> version > currentVersion }
            ?.takeIf { (_, version) -> ignoredVersion == null || version.compareTo(ignoredVersion) != 0 }
            ?.first
    }
}
