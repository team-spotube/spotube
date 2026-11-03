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

package org.spotube.spotube.modules.plugin.guard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.Text
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.core.ui.component.LoginRequiredDisplay
import org.spotube.spotube.modules.plugin.MetadataAuthViewModel
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAdd
import org.spotube.spotube.resources.iconsax.IconsaxBoxAdd
import org.spotube.spotube.resources.iconsax.IconsaxCheckCircle
import org.spotube.spotube.resources.iconsax.IconsaxMusicLibrary
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumCard
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.theme.MillenniumTheme
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.no_metadata_plugin_action_install
import spotube.composeapp.generated.resources.no_metadata_plugin_action_local
import spotube.composeapp.generated.resources.no_metadata_plugin_local_hint
import spotube.composeapp.generated.resources.no_metadata_plugin_action_select
import spotube.composeapp.generated.resources.no_metadata_plugin_description_install
import spotube.composeapp.generated.resources.no_metadata_plugin_description_select
import spotube.composeapp.generated.resources.no_metadata_plugin_step_discover
import spotube.composeapp.generated.resources.no_metadata_plugin_step_install
import spotube.composeapp.generated.resources.no_metadata_plugin_step_select
import spotube.composeapp.generated.resources.no_metadata_plugin_title

/**
 * Renders [content] only while a metadata plugin is selected. Otherwise shows a guided
 * "no plugin" state with a call to action leading to the plugin manager, instead of letting
 * the screen sit on an endless loading skeleton.
 *
 * @param backButton whether the placeholder's top bar shows a back button (for pushed screens).
 * @param requireLogin also swap [content] for a friendly sign-in prompt while the plugin needs an
 * account and the user isn't signed in (for screens that only make sense when logged in).
 * @param showTopBar false when the host screen already draws its own app bar (e.g. Library tabs).
 */
@Composable
fun MetadataPluginGuard(
    backButton: Boolean = false,
    showTopBar: Boolean = true,
    requireLogin: Boolean = false,
    viewModel: MetadataPluginGuardViewModel = koinViewModel(),
    authViewModel: MetadataAuthViewModel = koinViewModel(),
    content: @Composable () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    when (val s = state) {
        is MetadataPluginGuardState.Available ->
            if (requireLogin && authState.loginRequired) {
                Column(Modifier.fillMaxSize().background(MillenniumTheme.colors.background)) {
                    if (showTopBar) ApplicationMainBar(backButton = backButton)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = LocalAppShellBottomInset.current),
                        contentAlignment = Alignment.Center) {
                        LoginRequiredDisplay(
                            pluginName = authState.pluginName,
                            onLogin = authViewModel::login,
                            isLoggingIn = authState.isLoggingIn,
                            modifier = Modifier.widthIn(max = 420.dp))
                    }
                }
            } else {
                content()
            }
        is MetadataPluginGuardState.Loading -> Box(
            Modifier.fillMaxSize().background(MillenniumTheme.colors.background))
        is MetadataPluginGuardState.Missing -> NoMetadataPluginScreen(
            hasInstalledPlugins = s.hasInstalledPlugins,
            backButton = backButton,
            showTopBar = showTopBar,
            onOpenPlugins = viewModel::openPlugins,
            onOpenLocalTracks = viewModel::openLocalTracks)
    }
}

@Composable
private fun NoMetadataPluginScreen(
    hasInstalledPlugins: Boolean,
    backButton: Boolean,
    showTopBar: Boolean,
    onOpenPlugins: () -> Unit,
    onOpenLocalTracks: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MillenniumTheme.colors.background)) {
        if (showTopBar) ApplicationMainBar(backButton = backButton)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = LocalAppShellBottomInset.current),
            contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HeroBadge()

                Text(
                    text = stringResource(Res.string.no_metadata_plugin_title),
                    style = AppTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.onSurface,
                    textAlign = TextAlign.Center)

                Text(
                    text = stringResource(
                        if (hasInstalledPlugins) Res.string.no_metadata_plugin_description_select
                        else Res.string.no_metadata_plugin_description_install),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center)

                if (!hasInstalledPlugins) {
                    MillenniumCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Step(1, Res.string.no_metadata_plugin_step_discover)
                            Step(2, Res.string.no_metadata_plugin_step_install)
                            Step(3, Res.string.no_metadata_plugin_step_select)
                        }
                    }
                }

                MillenniumButton(
                    onClick = onOpenPlugins,
                    style = MillenniumButtonStyle.Primary) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MillenniumIcon(
                            if (hasInstalledPlugins) Iconsax.IconsaxCheckCircle else Iconsax.IconsaxAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Text(
                            stringResource(
                                if (hasInstalledPlugins) Res.string.no_metadata_plugin_action_select
                                else Res.string.no_metadata_plugin_action_install))
                    }
                }

                MillenniumButton(
                    onClick = onOpenLocalTracks,
                    style = MillenniumButtonStyle.Secondary) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MillenniumIcon(
                            Iconsax.IconsaxMusicLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Text(stringResource(Res.string.no_metadata_plugin_action_local))
                    }
                }

                Text(
                    text = stringResource(Res.string.no_metadata_plugin_local_hint),
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun HeroBadge() {
    val primary = AppTheme.colors.primary
    Box(
        modifier = Modifier
            .size(104.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(primary.copy(alpha = 0.22f), primary.copy(alpha = 0f)))),
        contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center) {
            MillenniumIcon(
                Iconsax.IconsaxBoxAdd,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = primary)
        }
    }
}

@Composable
private fun Step(index: Int, label: StringResource) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center) {
            Text(
                text = index.toString(),
                style = AppTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.primary)
        }
        Text(
            text = stringResource(label),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurface)
    }
}
