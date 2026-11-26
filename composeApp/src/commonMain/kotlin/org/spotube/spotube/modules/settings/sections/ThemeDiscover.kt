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

package org.spotube.spotube.modules.settings.sections

import org.spotube.spotube.core.ui.theming.appPanelBrush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronRight
import org.spotube.spotube.core.ui.theming.AppIcon
import com.composeunstyled.Text
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumDialog
import org.spotube.millennium.composables.MillenniumDialogBody
import org.spotube.millennium.composables.MillenniumDialogClose
import org.spotube.millennium.composables.MillenniumDialogFooter
import org.spotube.millennium.composables.MillenniumDialogHeader
import org.spotube.millennium.composables.MillenniumDialogTitle
import org.spotube.millennium.composables.MillenniumListTile
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumSurface
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxImportArrow2
import org.spotube.spotube.modules.settings.ThemeDiscoverUi
import org.spotube.spotube.modules.settings.ThemeRepoUi
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.common_cancel
import spotube.composeapp.generated.resources.settings_theme_download_failed
import spotube.composeapp.generated.resources.settings_theme_downloading
import spotube.composeapp.generated.resources.settings_theme_release_prerelease
import spotube.composeapp.generated.resources.settings_theme_releases_failed
import spotube.composeapp.generated.resources.settings_theme_releases_loading
import spotube.composeapp.generated.resources.settings_theme_releases_none
import spotube.composeapp.generated.resources.settings_theme_stars
import spotube.composeapp.generated.resources.settings_themes_discover_empty
import spotube.composeapp.generated.resources.settings_themes_discover_failed
import spotube.composeapp.generated.resources.settings_themes_discover_rate_limited
import spotube.composeapp.generated.resources.settings_themes_discover_load_more
import spotube.composeapp.generated.resources.settings_themes_discover_loading
import spotube.composeapp.generated.resources.settings_themes_discover_retry
import spotube.composeapp.generated.resources.settings_themes_discover_title

/** Theme repositories on GitHub (topics `spotube` + `spotube-theme`, no forks, most starred first). */
@Composable
internal fun ThemeDiscoverSection(
    state: ThemeDiscoverUi,
    onOpen: (ThemeRepoUi) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    SettingsSectionLabel(Res.string.settings_themes_discover_title)
    when {
        state.repos.isEmpty() && state.loading -> DiscoverNote(Res.string.settings_themes_discover_loading)
        state.repos.isEmpty() && state.failed -> DiscoverNote(
            if (state.rateLimited) Res.string.settings_themes_discover_rate_limited else Res.string.settings_themes_discover_failed,
            error = true,
        ) {
            MillenniumButton(onClick = onRetry, style = MillenniumButtonStyle.Secondary) {
                Text(stringResource(Res.string.settings_themes_discover_retry))
            }
        }
        state.repos.isEmpty() -> DiscoverNote(Res.string.settings_themes_discover_empty)
        else -> ThemeCardList {
            state.repos.forEachIndexed { index, repo ->
                if (index > 0) MillenniumSeparator()
                ThemeRepoRow(repo, onClick = { onOpen(repo) })
            }
        }
    }
    if (state.canLoadMore && state.repos.isNotEmpty()) {
        MillenniumButton(
            onClick = onLoadMore,
            style = MillenniumButtonStyle.Quiet,
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        ) {
            Text(stringResource(if (state.loading) Res.string.settings_themes_discover_loading else Res.string.settings_themes_discover_load_more))
        }
    }
}

/** A one-line status (loading, empty, failed) aligned with the list cards, with an optional action under it. */
@Composable
private fun DiscoverNote(text: org.jetbrains.compose.resources.StringResource, error: Boolean = false, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(text),
            style = AppTheme.typography.bodyMedium,
            color = if (error) AppTheme.colors.error else AppTheme.colors.onSurfaceVariant,
        )
        action?.invoke()
    }
}

@Composable
private fun ThemeRepoRow(repo: ThemeRepoUi, onClick: () -> Unit) {
    MillenniumListTile(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        leading = {
            AsyncImage(
                model = repo.avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape),
            )
        },
        title = {
            Text(repo.name, style = AppTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        subtitle = {
            Text(
                "${repo.owner} · ${stringResource(Res.string.settings_theme_stars, repo.stars)}",
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailing = {
            AppIcon(FeatherIcons.ChevronRight, contentDescription = null, tint = AppTheme.colors.onSurfaceVariant)
        },
    )
}

/** The releases of one repository with their `.zip` assets; picking one downloads it and opens the install dialog. */
@Composable
internal fun ThemeReleasesDialog(
    state: ThemeDiscoverUi,
    onInstall: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val repo = state.repo ?: return
    MillenniumDialog(visible = true, onDismissRequest = onDismiss) {
        MillenniumDialogHeader {
            MillenniumDialogTitle(repo.fullName)
            MillenniumDialogClose()
        }
        MillenniumDialogBody {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                repo.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceVariant)
                }
                when {
                    state.loadingReleases ->
                        Text(stringResource(Res.string.settings_theme_releases_loading), style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceVariant)
                    state.releasesFailed ->
                        Text(stringResource(Res.string.settings_theme_releases_failed), style = AppTheme.typography.bodyMedium, color = AppTheme.colors.error)
                    state.releases.isEmpty() ->
                        Text(stringResource(Res.string.settings_theme_releases_none), style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceVariant)
                    else -> state.releases.forEach { release ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(release.title, style = AppTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppTheme.colors.onSurface)
                                if (release.prerelease) {
                                    Text(stringResource(Res.string.settings_theme_release_prerelease), style = AppTheme.typography.labelSmall, color = AppTheme.colors.onSurfaceVariant)
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .millenniumSurface(
                                        background = appPanelBrush(),
                                        shape = AppTheme.shapes.medium,
                                        borderColor = MillenniumTheme.colors.border,
                                        borderWidth = 1.dp,
                                        shadowElevation = 0.dp,
                                    )
                                    .padding(vertical = 2.dp),
                            ) {
                                release.assets.forEachIndexed { index, asset ->
                                    if (index > 0) MillenniumSeparator()
                                    MillenniumListTile(
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = state.downloading == null,
                                        onClick = { onInstall(asset.url) },
                                        title = {
                                            Text(asset.name.removeSuffix(".zip"), style = AppTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        },
                                        trailing = {
                                            if (state.downloading == asset.url) {
                                                Text(stringResource(Res.string.settings_theme_downloading), style = AppTheme.typography.labelMedium, color = AppTheme.colors.onSurfaceVariant)
                                            } else {
                                                AppIcon(Iconsax.IconsaxImportArrow2, contentDescription = null, tint = AppTheme.colors.onSurfaceVariant)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                if (state.downloadFailed) {
                    Text(stringResource(Res.string.settings_theme_download_failed), style = AppTheme.typography.bodyMedium, color = AppTheme.colors.error)
                }
            }
        }
        MillenniumDialogFooter {
            MillenniumButton(onClick = onDismiss, style = MillenniumButtonStyle.Secondary) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    }
}
