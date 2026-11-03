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

package org.spotube.spotube.modules.plugin


import org.spotube.millennium.composables.MillenniumCircularProgress
import org.spotube.millennium.composables.MillenniumDotsLoader
import org.spotube.millennium.composables.MillenniumSpokeSpinner
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumDialog
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.millennium.composables.MillenniumSegmentedButton
import org.spotube.millennium.composables.MillenniumSegmentedButtonDefaults
import org.spotube.millennium.composables.MillenniumSegmentedButtonRow
import org.spotube.millennium.composables.MillenniumTextField
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumPanelBrush
import org.spotube.millennium.theme.millenniumSurface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState

import org.spotube.spotube.core.ui.theming.AppTheme

import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.spotube.spotube.PlatformType
import org.spotube.spotube.core.extras.kebabToTitleCase
import org.spotube.spotube.core.ui.component.AdaptiveDialogBottomSheet
import org.spotube.spotube.core.ui.component.AdaptiveDropdownBottomSheet
import org.spotube.spotube.core.ui.component.AdaptiveMenuItem
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.core.ui.component.HeaderDisplayMode
import org.spotube.spotube.getPlatform
import org.spotube.spotube.modules.plugin.components.DefaultAbilityPluginGrid
import org.spotube.spotube.modules.plugin.components.PluginCard
import org.spotube.spotube.modules.plugin.components.PluginDialogActions
import org.spotube.spotube.modules.plugin.components.PluginDialogBody
import org.spotube.spotube.modules.plugin.components.PluginDialogTitle
import org.spotube.spotube.modules.plugin.components.PluginIconTile
import org.spotube.spotube.modules.plugin.components.PluginLogoTile
import org.spotube.spotube.modules.plugin.components.PluginPanel
import org.spotube.spotube.modules.plugin.components.PluginPill
import org.spotube.spotube.resources.iconsax.IconsaxBoxAdd
import org.spotube.spotube.resources.iconsax.User
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronRight
import spotube.composeapp.generated.resources.plugin_install_sheet_subtitle
import spotube.composeapp.generated.resources.plugin_install_file_hint
import spotube.composeapp.generated.resources.plugin_install_or
import spotube.composeapp.generated.resources.plugin_info_dialog_title
import spotube.composeapp.generated.resources.plugin_version_label
import org.spotube.spotube.modules.plugin.components.PluginInstallDialog
import org.spotube.spotube.modules.plugin.components.PluginLogo
import org.spotube.spotube.modules.plugin.components.PluginPermissionDialog
import org.spotube.spotube.modules.plugin.components.abilityIcon
import org.spotube.spotube.modules.plugin.components.accentColor
import org.spotube.spotube.modules.plugin.components.displayLabel
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.openUrlInBrowser
import org.spotube.spotube.resources.iconsax.CarbonGithubLogo
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAdd
import org.spotube.spotube.resources.iconsax.IconsaxArrowDown4
import org.spotube.spotube.resources.iconsax.IconsaxBox
import org.spotube.spotube.resources.iconsax.IconsaxCheckCircle
import org.spotube.spotube.resources.iconsax.IconsaxEdit
import org.spotube.spotube.resources.iconsax.IconsaxExportArrowBulk
import org.spotube.spotube.resources.iconsax.IconsaxGlobe
import org.spotube.spotube.resources.iconsax.IconsaxHeart
import org.spotube.spotube.resources.iconsax.IconsaxImportArrow2Bulk
import org.spotube.spotube.resources.iconsax.IconsaxLink
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import okio.Path
import kotlinx.coroutines.flow.collect
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.plugin_discover_badge_official
import spotube.composeapp.generated.resources.plugin_discover_badge_verified
import spotube.composeapp.generated.resources.plugin_discover_repository_content_description
import spotube.composeapp.generated.resources.plugin_discover_repository_host
import spotube.composeapp.generated.resources.plugin_discover_stars_content_description
import spotube.composeapp.generated.resources.plugin_info_abilities
import spotube.composeapp.generated.resources.plugin_info_api_version
import spotube.composeapp.generated.resources.plugin_info_author
import spotube.composeapp.generated.resources.plugin_info_capabilities
import spotube.composeapp.generated.resources.plugin_info_contact
import spotube.composeapp.generated.resources.plugin_info_label_format
import spotube.composeapp.generated.resources.plugin_info_license
import spotube.composeapp.generated.resources.plugin_info_report_bugs
import spotube.composeapp.generated.resources.plugin_info_repository
import spotube.composeapp.generated.resources.plugin_info_version
import spotube.composeapp.generated.resources.plugin_support_dialog_title
import spotube.composeapp.generated.resources.common_close
import spotube.composeapp.generated.resources.welcome_plugins_title
import spotube.composeapp.generated.resources.welcome_plugins_setup_guide
import spotube.composeapp.generated.resources.welcome_plugins_discover
import spotube.composeapp.generated.resources.welcome_plugins_retry
import spotube.composeapp.generated.resources.welcome_plugins_more
import spotube.composeapp.generated.resources.welcome_plugins_discovery_empty
import spotube.composeapp.generated.resources.welcome_plugins_discovery_error
import spotube.composeapp.generated.resources.welcome_plugins_sources
import spotube.composeapp.generated.resources.plugin_action_download
import spotube.composeapp.generated.resources.plugin_action_install_from_file
import spotube.composeapp.generated.resources.plugin_configure_title
import spotube.composeapp.generated.resources.plugin_empty_subtitle
import spotube.composeapp.generated.resources.plugin_empty_title
import spotube.composeapp.generated.resources.plugin_error_download_failed
import spotube.composeapp.generated.resources.plugin_error_enter_url
import spotube.composeapp.generated.resources.plugin_error_url_scheme
import spotube.composeapp.generated.resources.plugin_install_section_title
import spotube.composeapp.generated.resources.plugin_installed_plugins_count
import spotube.composeapp.generated.resources.plugin_screen_title
import spotube.composeapp.generated.resources.plugin_section_file_title
import spotube.composeapp.generated.resources.plugin_section_install
import spotube.composeapp.generated.resources.plugin_section_url_title
import spotube.composeapp.generated.resources.plugin_url_placeholder
import spotube.composeapp.generated.resources.settings_plugins_action_change
import spotube.composeapp.generated.resources.settings_plugins_action_select
import spotube.composeapp.generated.resources.settings_plugins_default_ability_title
import spotube.composeapp.generated.resources.settings_plugins_no_plugins
import spotube.composeapp.generated.resources.settings_plugins_no_selection
import spotube.composeapp.generated.resources.settings_plugins_plugin_content_description

private val OFFICIAL_PLUGIN_OWNERS = setOf("KRTirtho", "team-spotube")

private val VERIFIED_PLUGIN_OWNERS = setOf<String>()

@Composable
fun PluginScreen(
    viewModel: PluginViewModel = koinViewModel(),
    onboarding: Boolean = false) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (!onboarding) {
            ApplicationMainBar(title = { Text(stringResource(Res.string.plugin_screen_title)) })
        }
        PluginScreenContent(
            viewModel = viewModel,
            onboarding = onboarding,
            modifier = Modifier.weight(1f))
    }
}

/**
 * The plugin management UI without its own scaffold / app bar, so it can be embedded
 * as a tab in the large-screen settings layout as well as hosted by [PluginScreen].
 */
@Composable
internal fun PluginScreenContent(
    viewModel: PluginViewModel,
    onboarding: Boolean = false,
    modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val shellBottomInset = LocalAppShellBottomInset.current

    val platform = remember { getPlatform() }
    val launcher = rememberFilePickerLauncher(
        type = FileKitType.File(
            extensions = if (platform.type == PlatformType.Android) listOf() else listOf("smplug")
        )
    ) { file ->
        viewModel.onFileSelected(file)
    }

    val state = uiState
    if (state is PluginUiState.Data) {
        state.pendingPlugin?.let { pending ->
            PluginPermissionDialog(
                pluginInfo = pending.entry,
                title = pending.title,
                message = pending.message,
                confirmLabel = pending.confirmLabel,
                existingPlugin = pending.existingEntry,
                logoPath = state.pendingPluginLogoPath,
                onConfirm = if (pending.kind != PluginManager.InstallPromptKind.INFO && pending.confirmLabel != null) {
                    { viewModel.confirmInstall() }
                } else {
                    null
                },
                isConfirming = state.isConfirmingInstall,
                onDismiss = { viewModel.dismissInstall() }
            )
        }

        state.showPluginInfo?.let { plugin ->
            PluginInfoDialog(
                plugin = plugin,
                logoPath = state.pluginInfoLogoPath,
                onDismiss = { viewModel.dismissPluginInfo() }
            )
        }

        state.showPluginSupport?.let { plugin ->
            SupportDialog(
                plugin = plugin,
                supportText = state.supportText,
                isLoading = state.isLoadingSupport,
                onDismiss = { viewModel.dismissSupport() }
            )
        }

        state.installDialogRepo?.let { repo ->
            PluginInstallDialog(
                repo = repo,
                releases = state.releases,
                isLoadingReleases = state.isLoadingReleases,
                onDismiss = { viewModel.dismissInstallDialog() },
                onInstall = { release ->
                    viewModel.dismissInstallDialog()
                    viewModel.installPlugin(release, repo.id)
                }
            )
        }

        if (state.showInstallSheet) {
            InstallSheet(
                urlInput = state.urlInput,
                urlError = state.urlError,
                isLoadingUrl = state.isLoadingUrl,
                isPreparingFile = state.isPreparingFile,
                onUrlChange = { viewModel.onUrlInputChange(it) },
                onSubmitUrl = { viewModel.submitUrl() },
                onPickFile = { launcher.launch() },
                onDismiss = { viewModel.dismissInstallSheet() }
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val data = uiState) {
            is PluginUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { MillenniumCircularProgress(size = 40.dp) }
            }

            is PluginUiState.Data -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    val discoverListState = rememberLazyListState()
                    LazyColumn(
                        state = discoverListState,
                        modifier = Modifier.widthIn(max = 1280.dp).align(Alignment.TopCenter),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 8.dp,
                            bottom = 24.dp + shellBottomInset
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onboarding) {
                            item {
                                Column(
                                    modifier = Modifier.padding(
                                        horizontal = 4.dp,
                                        vertical = 16.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        stringResource(Res.string.welcome_plugins_title),
                                        style = AppTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.SemiBold)
                                    Text(
                                        stringResource(Res.string.welcome_plugins_setup_guide),
                                        style = AppTheme.typography.bodyMedium,
                                        color = AppTheme.colors.onSurfaceVariant)
                                }
                            }
                            item {
                                MillenniumSegmentedButtonRow(
                                    modifier = Modifier.padding(bottom = 12.dp)) {
                                    MillenniumSegmentedButton(
                                        selected = data.onboardingDiscover,
                                        onClick = { viewModel.showOnboardingDiscovery(true) },
                                        shape = MillenniumSegmentedButtonDefaults.itemShape(0, 2)) {
                                        Text(stringResource(Res.string.welcome_plugins_discover))
                                    }
                                    MillenniumSegmentedButton(
                                        selected = !data.onboardingDiscover,
                                        onClick = { viewModel.showOnboardingDiscovery(false) },
                                        shape = MillenniumSegmentedButtonDefaults.itemShape(1, 2)) {
                                        Text(stringResource(Res.string.welcome_plugins_sources))
                                    }
                                }
                            }
                        }
                        if (!onboarding || !data.onboardingDiscover) {

                            item {
                                DefaultAbilityPluginGrid(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    selections = data.abilitySelections,
                                    onSelected = { ability, plugin ->
                                        viewModel.selectPlugin(ability, plugin)
                                    })
                            }

                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(Res.string.plugin_configure_title),
                                        style = AppTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    MillenniumButton(onClick = { viewModel.showInstallSheet() }) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            MillenniumIcon(
                                                Iconsax.IconsaxAdd,
                                                contentDescription = stringResource(Res.string.plugin_install_section_title))
                                            Text(stringResource(Res.string.plugin_install_section_title))
                                        }
                                    }
                                }
                            }

                            if (data.plugins.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.size(72.dp)
                                                    .clip(RoundedCornerShape(18.dp))
                                                    .background(AppTheme.colors.primary.copy(alpha = 0.1f))
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    MillenniumIcon(
                                                        Iconsax.IconsaxBox,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(32.dp),
                                                        tint = AppTheme.colors.primary
                                                    )
                                                }
                                            }
                                            Text(
                                                stringResource(Res.string.plugin_empty_title),
                                                style = AppTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                stringResource(Res.string.plugin_empty_subtitle),
                                                style = AppTheme.typography.bodySmall,
                                                color = AppTheme.colors.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                item {
                                    Text(
                                        pluralStringResource(
                                            Res.plurals.plugin_installed_plugins_count,
                                            data.plugins.size,
                                            data.plugins.size
                                        ),
                                        style = AppTheme.typography.labelMedium,
                                        color = AppTheme.colors.onSurfaceVariant,
                                        modifier = Modifier.padding(
                                            horizontal = 4.dp,
                                            vertical = 4.dp
                                        )
                                    )
                                }
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .millenniumSurface(
                                                background = millenniumPanelBrush(MillenniumTheme.colors),
                                                shape = RoundedCornerShape(12.dp),
                                                borderColor = AppTheme.colors.outlineVariant,
                                                borderWidth = 1.dp,
                                                shadowElevation = 6.dp)) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            data.plugins.forEachIndexed { index, item ->
                                                if (index > 0) {
                                                    MillenniumSeparator()
                                                }
                                                PluginCard(
                                                    plugin = item.plugin,
                                                    isSelected = item.isSelected,
                                                    onRemove = { viewModel.removePlugin(item.plugin) },
                                                    isLoggedIn = item.authState.isLoggedIn,
                                                    logoPath = item.logoPath,
                                                    isAuthInProgress = item.isAuthInProgress,
                                                    isRemoving = item.isRemoving,
                                                    onInfo = { viewModel.showPluginInfo(item.plugin) },
                                                    onSupport = if (item.isSelected) {
                                                        { viewModel.loadSupport(item.plugin) }
                                                    } else null,
                                                    onLogin = if (item.isSelected && item.authState.requiresAuth) {
                                                        { viewModel.login(item.plugin) }
                                                    } else null,
                                                    onLogout = if (item.isSelected && item.authState.requiresAuth) {
                                                        { viewModel.logout(item.plugin) }
                                                    } else null)
                                            }
                                        }
                                    }
                                }
                            }

                        }
                        if ((!onboarding || data.onboardingDiscover) && (onboarding || data.discover.isLoading || data.discover.repos.isNotEmpty() || data.discover.error != null)) {
                            if (onboarding) {
                                item {
                                    MillenniumButton(
                                        onClick = viewModel::showInstallSheet,
                                        style = MillenniumButtonStyle.Secondary) {
                                        Text(stringResource(Res.string.plugin_install_section_title))
                                    }
                                }
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MillenniumIcon(
                                        Iconsax.IconsaxGlobe,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = AppTheme.colors.primary
                                    )
                                    Text(
                                        stringResource(Res.string.welcome_plugins_discover),
                                        style = AppTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            if (data.discover.isLoading) {
                                item {
                                    Box(
                                        Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        MillenniumDotsLoader()
                                    }
                                }
                            }
                            if (onboarding && data.discover.isInitialLoaded && !data.discover.isLoading && data.discover.repos.isEmpty() && data.discover.error == null) {
                                item {
                                    Text(
                                        stringResource(Res.string.welcome_plugins_discovery_empty),
                                        color = AppTheme.colors.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            items(
                                data.discover.repos,
                                key = { it.id }
                            ) { repo ->
                                DiscoverRepoCard(
                                    repo = repo,
                                    isInstalling = data.discover.installingRepoId == repo.id,
                                    onInstall = { viewModel.showInstallDialog(repo) }
                                )
                            }

                            if (data.discover.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        MillenniumDotsLoader()
                                    }
                                }
                            }

                            data.discover.error?.let { error ->
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            stringResource(Res.string.welcome_plugins_discovery_error),
                                            style = AppTheme.typography.bodySmall,
                                            color = AppTheme.colors.error,
                                            modifier = Modifier.padding(
                                                horizontal = 4.dp,
                                                vertical = 8.dp
                                            )
                                        )
                                        MillenniumButton(
                                            onClick = viewModel::retryDiscovery,
                                            style = MillenniumButtonStyle.Secondary) {
                                            Text(stringResource(Res.string.welcome_plugins_retry))
                                        }
                                    }
                                }
                            }
                            if (onboarding && data.discover.hasMore && !data.discover.isLoading && !data.discover.isLoadingMore && data.discover.error == null) {
                                item {
                                    MillenniumButton(
                                        onClick = viewModel::loadNextDiscoverPage,
                                        style = MillenniumButtonStyle.Secondary) {
                                        Text(stringResource(Res.string.welcome_plugins_more))
                                    }
                                }
                            }
                        }
                    }

                    val density = LocalDensity.current
                    val shouldLoadMore = remember(density) {
                        derivedStateOf {
                            val totalItems = discoverListState.layoutInfo.totalItemsCount
                            val lastVisibleIndex =
                                discoverListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                                    ?: 0
                            totalItems > 0 && lastVisibleIndex >= totalItems - 3
                        }
                    }

                    // Pagination trigger stays in the composable because it is driven by
                    // LazyList layout information, which only exists in UI scope.
                    // The actual loading call is forwarded to the ViewModel.
                    LaunchedEffect(shouldLoadMore.value, onboarding) {
                        if (!onboarding && shouldLoadMore.value) {
                            viewModel.loadNextDiscoverPage()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UrlError.text(): String = when (this) {
    is UrlError.Empty -> stringResource(Res.string.plugin_error_enter_url)
    is UrlError.InvalidScheme -> stringResource(Res.string.plugin_error_url_scheme)
    is UrlError.DownloadFailed -> stringResource(Res.string.plugin_error_download_failed)
    is UrlError.Message -> message
}

@Composable
private fun InstallSheet(
    urlInput: String,
    urlError: UrlError?,
    isLoadingUrl: Boolean,
    isPreparingFile: Boolean,
    onUrlChange: (String) -> Unit,
    onSubmitUrl: () -> Unit,
    onPickFile: () -> Unit,
    onDismiss: () -> Unit) {
    AdaptiveDialogBottomSheet(
        onDismiss = onDismiss,
        title = {
            PluginDialogTitle(
                title = stringResource(Res.string.plugin_install_section_title),
                icon = Iconsax.IconsaxBoxAdd)
        },
        dialogTitle = stringResource(Res.string.plugin_install_section_title)) {
        PluginDialogBody {
            Text(
                stringResource(Res.string.plugin_install_sheet_subtitle),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant)

            val urlState = rememberTextFieldState(urlInput)
            LaunchedEffect(urlInput) {
                if (urlState.text.toString() != urlInput) {
                    urlState.edit { replace(0, length, urlInput) }
                }
            }
            LaunchedEffect(urlState) {
                snapshotFlow { urlState.text.toString() }.collect { value ->
                    if (value != urlInput) onUrlChange(value)
                }
            }

            PluginPanel {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PluginIconTile(Iconsax.IconsaxLink)
                        Text(
                            stringResource(Res.string.plugin_section_url_title),
                            style = AppTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                    }
                    MillenniumTextField(
                        state = urlState,
                        label = "",
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = stringResource(Res.string.plugin_url_placeholder),
                        error = urlError?.text())
                    MillenniumButton(
                        onClick = onSubmitUrl,
                        loading = isLoadingUrl,
                        modifier = Modifier.fillMaxWidth(),
                        style = MillenniumButtonStyle.Primary) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MillenniumIcon(
                                Iconsax.IconsaxImportArrow2Bulk,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp))
                            Text(stringResource(Res.string.plugin_action_download))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MillenniumSeparator(modifier = Modifier.weight(1f))
                Text(
                    stringResource(Res.string.plugin_install_or),
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.onSurfaceVariant)
                MillenniumSeparator(modifier = Modifier.weight(1f))
            }

            PluginPanel(modifier = Modifier.clickable(enabled = !isPreparingFile, onClick = onPickFile)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PluginIconTile(Iconsax.IconsaxExportArrowBulk)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            stringResource(Res.string.plugin_action_install_from_file),
                            style = AppTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(Res.string.plugin_install_file_hint),
                            style = AppTheme.typography.labelSmall,
                            color = AppTheme.colors.onSurfaceVariant)
                    }
                    if (isPreparingFile) {
                        MillenniumSpokeSpinner(size = 18.dp)
                    } else {
                        MillenniumIcon(
                            FeatherIcons.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = AppTheme.colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginInfoDialog(
    plugin: PluginEntry,
    logoPath: Path?,
    onDismiss: () -> Unit) {
    AdaptiveDialogBottomSheet(
        onDismiss = onDismiss,
        title = { PluginDialogTitle(stringResource(Res.string.plugin_info_dialog_title)) },
        dialogTitle = stringResource(Res.string.plugin_info_dialog_title),
        actions = {
            PluginDialogActions(
                secondaryLabel = stringResource(Res.string.common_close),
                onSecondary = onDismiss)
        }) {
        PluginDialogBody {
            PluginPanel {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    PluginLogoTile(plugin = plugin, logoPath = logoPath)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            plugin.name,
                            style = AppTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PluginPill(plugin.author, icon = Iconsax.User)
                            PluginPill(
                                stringResource(Res.string.plugin_version_label, plugin.version),
                                tint = AppTheme.colors.primary)
                        }
                    }
                }
                if (plugin.description.isNotBlank()) {
                    Text(
                        plugin.description,
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant)
                }
            }

            PluginPanel {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailRow(stringResource(Res.string.plugin_info_api_version), plugin.apiVersion)
                    if (plugin.license.isNotBlank()) {
                        DetailRow(stringResource(Res.string.plugin_info_license), plugin.license)
                    }
                    if (plugin.capabilities.isNotEmpty()) {
                        DetailChipsRow(
                            stringResource(Res.string.plugin_info_capabilities),
                            plugin.capabilities.map { it.name })
                    }
                    if (plugin.abilities.isNotEmpty()) {
                        DetailChipsRow(
                            stringResource(Res.string.plugin_info_abilities),
                            plugin.abilities.map { ability -> ability.displayLabel() })
                    }
                    if (plugin.repository.isNotBlank()) {
                        ClickableDetailRow(label = stringResource(Res.string.plugin_info_repository), value = plugin.repository) {
                            openUrlInBrowser(plugin.repository)
                        }
                    }
                    if (plugin.contact.isNotBlank()) {
                        DetailRow(stringResource(Res.string.plugin_info_contact), plugin.contact)
                    }
                    if (plugin.bugs.isNotBlank()) {
                        ClickableDetailRow(label = stringResource(Res.string.plugin_info_report_bugs), value = plugin.bugs) {
                            openUrlInBrowser(plugin.bugs)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportDialog(
    plugin: PluginEntry,
    supportText: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit) {
    AdaptiveDialogBottomSheet(
        onDismiss = onDismiss,
        title = { PluginDialogTitle(stringResource(Res.string.plugin_support_dialog_title, plugin.name)) },
        dialogTitle = stringResource(Res.string.plugin_support_dialog_title, plugin.name),
        actions = {
            PluginDialogActions(
                secondaryLabel = stringResource(Res.string.common_close),
                onSecondary = onDismiss)
        }) {
        PluginDialogBody {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center) {
                    MillenniumCircularProgress(size = 36.dp)
                }
            } else {
                PluginPanel {
                    Text(
                        supportText ?: "",
                        modifier = Modifier.padding(14.dp),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.onSurface)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoverRepoCard(
    repo: GitHubRepo,
    isInstalling: Boolean,
    onInstall: () -> Unit) {
    val isOfficial = repo.owner.login in OFFICIAL_PLUGIN_OWNERS
    val isVerified = repo.owner.login in VERIFIED_PLUGIN_OWNERS

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .millenniumSurface(
                background = millenniumPanelBrush(MillenniumTheme.colors),
                shape = RoundedCornerShape(12.dp),
                borderColor = AppTheme.colors.outlineVariant,
                borderWidth = 1.dp,
                shadowElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val platformContext = LocalPlatformContext.current
            AsyncImage(
                model = ImageRequest.Builder(platformContext)
                    .data(repo.owner.avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = repo.owner.login,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        repo.fullName.split("/")
                            .last()
                            .replace("spotube-plugin-", "")
                            .kebabToTitleCase(),
                        style = AppTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isOfficial) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AppTheme.colors.primary.copy(alpha = 0.15f))
                        ) {
                            Text(
                                stringResource(Res.string.plugin_discover_badge_official),
                                style = AppTheme.typography.labelSmall,
                                color = AppTheme.colors.primary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    } else if (isVerified) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                MillenniumIcon(
                                    Iconsax.IconsaxCheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = Color(0xFF4CAF50)
                                )
                                Text(
                                    stringResource(Res.string.plugin_discover_badge_verified),
                                    style = AppTheme.typography.labelSmall,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }
                if (!repo.description.isNullOrBlank()) {
                    Text(
                        repo.description,
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        repo.owner.login,
                        style = AppTheme.typography.labelSmall,
                        color = AppTheme.colors.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        MillenniumIcon(
                            Iconsax.IconsaxHeart,
                            contentDescription = stringResource(Res.string.plugin_discover_stars_content_description),
                            modifier = Modifier.size(11.dp),
                            tint = AppTheme.colors.onSurfaceVariant
                        )
                        Text(
                            repo.stargazersCount.toString(),
                            style = AppTheme.typography.labelSmall,
                            color = AppTheme.colors.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AppTheme.colors.surfaceVariant)
                            .clickable {
                                openUrlInBrowser(repo.htmlUrl)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            MillenniumIcon(
                                Iconsax.CarbonGithubLogo,
                                contentDescription = stringResource(Res.string.plugin_discover_repository_content_description),
                                modifier = Modifier.size(10.dp),
                                tint = AppTheme.colors.onSurfaceVariant
                            )
                            Text(
                                stringResource(Res.string.plugin_discover_repository_host),
                                style = AppTheme.typography.labelSmall,
                                color = AppTheme.colors.onSurfaceVariant,
                                singleLine = true
                            )
                        }
                    }
                }
            }
            MillenniumButton(
                onClick = onInstall,
                loading = isInstalling,
                style = MillenniumButtonStyle.Secondary
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MillenniumIcon(
                        Iconsax.IconsaxAdd,
                        contentDescription = null)
                    Text(stringResource(Res.string.plugin_section_install))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(Res.string.plugin_info_label_format, label),
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 90.dp)
        )
        Text(
            value,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurface
        )
    }
}

@Composable
private fun ClickableDetailRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(Res.string.plugin_info_label_format, label),
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 90.dp)
        )
        Text(
            value,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailChipsRow(label: String, chips: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(Res.string.plugin_info_label_format, label),
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 90.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            chips.forEach { chip ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AppTheme.colors.secondaryContainer.copy(alpha = 0.6f))
                ) {
                    Text(
                        chip,
                        style = AppTheme.typography.labelSmall,
                        color = AppTheme.colors.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DefaultAbilityPluginSelector(
    selection: AbilitySelection,
    onSelected: (PluginEntry?) -> Unit = { }) {
    val plugins = selection.plugins
    val selectedPlugin = selection.selectedPlugin
    val noPluginsText = stringResource(Res.string.settings_plugins_no_plugins)

    val menuItems = buildList {
        if (plugins.isNotEmpty()) {
            plugins.forEach { plugin ->
                val isSelected = selectedPlugin?.name == plugin.name
                add(
                    AdaptiveMenuItem(
                        label = plugin.name,
                        onClick = { onSelected(plugin) },
                        selected = isSelected)
                )
            }
        } else {
            add(
                AdaptiveMenuItem(
                    label = noPluginsText,
                    onClick = { },
                    enabled = false)
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(selection.ability.accentColor().copy(alpha = 0.1f))
            ) {
                MillenniumIcon(
                    imageVector = selection.ability.abilityIcon(),
                    contentDescription = stringResource(
                        Res.string.settings_plugins_plugin_content_description,
                        selection.ability.displayLabel()
                    ),
                    modifier = Modifier.padding(8.dp),
                    tint = selection.ability.accentColor()
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(
                        Res.string.settings_plugins_default_ability_title,
                        selection.ability.displayLabel()
                    ),
                    style = AppTheme.typography.labelLarge,
                    color = AppTheme.colors.onSurface
                )
                if (selectedPlugin != null) {
                    Text(
                        selectedPlugin.name,
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        stringResource(Res.string.settings_plugins_no_selection),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        AdaptiveDropdownBottomSheet(
            items = menuItems,
            headerDisplayMode = HeaderDisplayMode.OnlyInBottomSheet,
            header = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(selection.ability.accentColor().copy(alpha = 0.1f))
                    ) {
                        MillenniumIcon(
                            imageVector = selection.ability.abilityIcon(),
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp),
                            tint = selection.ability.accentColor()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(
                                Res.string.settings_plugins_default_ability_title,
                                selection.ability.displayLabel()
                            ),
                            style = AppTheme.typography.titleMedium)
                        selectedPlugin?.let {
                            Text(
                                it.name,
                                style = AppTheme.typography.bodyMedium,
                                color = AppTheme.colors.primary)
                        }
                    }
                }
            },
            trigger = { onClick ->
                MillenniumButton(
                    onClick = onClick,
                    style = MillenniumButtonStyle.Secondary) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            if (selectedPlugin != null) {
                                stringResource(Res.string.settings_plugins_action_change)
                            } else {
                                stringResource(Res.string.settings_plugins_action_select)
                            })
                        MillenniumIcon(
                            imageVector = if (selectedPlugin != null) Iconsax.IconsaxEdit else Iconsax.IconsaxArrowDown4,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp))
                    }
                }
            })
    }
}

