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
import org.spotube.millennium.theme.millenniumPanelBrush
import org.spotube.millennium.theme.millenniumSurface
import org.spotube.spotube.core.ui.theming.AppTheme
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
import spotube.composeapp.generated.resources.settings_theme_releases_title
import spotube.composeapp.generated.resources.settings_theme_stars
import spotube.composeapp.generated.resources.settings_themes_discover_empty
import spotube.composeapp.generated.resources.settings_themes_discover_failed
import spotube.composeapp.generated.resources.settings_themes_discover_load_more
import spotube.composeapp.generated.resources.settings_themes_discover_loading
import spotube.composeapp.generated.resources.settings_themes_discover_retry
import spotube.composeapp.generated.resources.settings_themes_discover_subtitle
import spotube.composeapp.generated.resources.settings_themes_discover_title

/** Theme repositories on GitHub (topics `spotube` + `spotube-theme`, no forks, most starred first). */
@Composable
internal fun ThemeDiscoverSection(
    state: ThemeDiscoverUi,
    onOpen: (ThemeRepoUi) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(Res.string.settings_themes_discover_title), style = AppTheme.typography.titleMedium, color = AppTheme.colors.onSurface)
            Text(stringResource(Res.string.settings_themes_discover_subtitle), style = AppTheme.typography.bodySmall, color = AppTheme.colors.onSurfaceVariant)
        }
        when {
            state.repos.isEmpty() && state.loading ->
                Text(stringResource(Res.string.settings_themes_discover_loading), style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceVariant)
            state.repos.isEmpty() && state.failed -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.settings_themes_discover_failed), style = AppTheme.typography.bodyMedium, color = AppTheme.colors.error)
                MillenniumButton(onClick = onRetry, style = MillenniumButtonStyle.Secondary) { Text(stringResource(Res.string.settings_themes_discover_retry)) }
            }
            state.repos.isEmpty() ->
                Text(stringResource(Res.string.settings_themes_discover_empty), style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceVariant)
            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .millenniumSurface(
                        background = millenniumPanelBrush(MillenniumTheme.colors),
                        shape = AppTheme.shapes.medium,
                        borderColor = MillenniumTheme.colors.border,
                        borderWidth = 1.dp,
                        shadowElevation = 6.dp,
                    )
                    .padding(vertical = 4.dp),
            ) {
                state.repos.forEachIndexed { index, repo ->
                    if (index > 0) MillenniumSeparator()
                    ThemeRepoRow(repo, onClick = { onOpen(repo) })
                }
            }
        }
        if (state.canLoadMore && state.repos.isNotEmpty()) {
            MillenniumButton(onClick = onLoadMore, style = MillenniumButtonStyle.Secondary, enabled = !state.loading) {
                Text(stringResource(if (state.loading) Res.string.settings_themes_discover_loading else Res.string.settings_themes_discover_load_more))
            }
        }
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
                listOfNotNull(repo.owner, repo.description?.takeIf { it.isNotBlank() }).joinToString(" · "),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailing = {
            Text(stringResource(Res.string.settings_theme_stars, repo.stars), style = AppTheme.typography.labelMedium, color = AppTheme.colors.onSurfaceVariant)
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
            MillenniumDialogTitle(stringResource(Res.string.settings_theme_releases_title, repo.name))
            MillenniumDialogClose()
        }
        MillenniumDialogBody {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
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
                            release.assets.forEach { asset ->
                                MillenniumButton(
                                    onClick = { onInstall(asset.url) },
                                    style = MillenniumButtonStyle.Secondary,
                                    enabled = state.downloading == null,
                                ) {
                                    Text(if (state.downloading == asset.url) stringResource(Res.string.settings_theme_downloading) else asset.name)
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
