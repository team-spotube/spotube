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
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.millennium.composables.MillenniumSegmentedButton
import org.spotube.millennium.composables.MillenniumSegmentedButtonDefaults
import org.spotube.millennium.composables.MillenniumSegmentedButtonRow
import org.spotube.millennium.composables.MillenniumTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.selection.SelectionContainer
import com.mikepenz.markdown.m3.Markdown
import spotube.composeapp.generated.resources.plugin_support_unavailable
import spotube.composeapp.generated.resources.plugin_action_support
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import org.spotube.spotube.modules.plugin.components.DiscoverPluginCard
import org.spotube.spotube.modules.plugin.components.GelIconTile
import org.spotube.spotube.modules.plugin.components.PluginListPanel
import org.spotube.spotube.modules.plugin.components.PluginRowSeparator
import org.spotube.spotube.modules.plugin.components.PluginSectionHeader
import org.spotube.spotube.resources.iconsax.IconsaxHeart
import spotube.composeapp.generated.resources.plugin_defaults_title
import spotube.composeapp.generated.resources.plugin_installed_title
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.spotube.spotube.PlatformType
import org.spotube.spotube.core.ui.component.AdaptiveDialogBottomSheet
import org.spotube.spotube.core.ui.component.AdaptiveDropdownBottomSheet
import org.spotube.spotube.core.ui.component.AdaptiveMenuItem
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.core.ui.component.HeaderDisplayMode
import org.spotube.spotube.getPlatform
import org.spotube.spotube.modules.plugin.components.DefaultAbilityPluginList
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
import org.spotube.spotube.modules.plugin.components.PluginPermissionDialog
import org.spotube.spotube.modules.plugin.components.abilityIcon
import org.spotube.spotube.modules.plugin.components.accentColor
import org.spotube.spotube.modules.plugin.components.displayLabel
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.openUrlInBrowser
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAdd
import org.spotube.spotube.resources.iconsax.IconsaxArrowDown4
import org.spotube.spotube.resources.iconsax.IconsaxBox
import org.spotube.spotube.resources.iconsax.IconsaxEdit
import org.spotube.spotube.resources.iconsax.IconsaxExportArrowBulk
import org.spotube.spotube.resources.iconsax.IconsaxImportArrow2Bulk
import org.spotube.spotube.resources.iconsax.IconsaxLink
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import okio.Path
import kotlinx.coroutines.flow.collect
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.plugin_info_abilities
import spotube.composeapp.generated.resources.plugin_info_api_version
import spotube.composeapp.generated.resources.plugin_info_capabilities
import spotube.composeapp.generated.resources.plugin_info_contact
import spotube.composeapp.generated.resources.plugin_info_label_format
import spotube.composeapp.generated.resources.plugin_info_license
import spotube.composeapp.generated.resources.plugin_info_report_bugs
import spotube.composeapp.generated.resources.plugin_info_repository
import spotube.composeapp.generated.resources.common_close
import spotube.composeapp.generated.resources.plugin_action_remove
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
import spotube.composeapp.generated.resources.plugin_empty_subtitle
import spotube.composeapp.generated.resources.plugin_empty_title
import spotube.composeapp.generated.resources.plugin_error_download_failed
import spotube.composeapp.generated.resources.plugin_error_enter_url
import spotube.composeapp.generated.resources.plugin_error_url_scheme
import spotube.composeapp.generated.resources.plugin_install_section_title
import spotube.composeapp.generated.resources.plugin_installed_plugins_count
import spotube.composeapp.generated.resources.plugin_screen_title
import spotube.composeapp.generated.resources.plugin_section_install
import spotube.composeapp.generated.resources.plugin_section_url_title
import spotube.composeapp.generated.resources.plugin_url_placeholder
import spotube.composeapp.generated.resources.settings_plugins_action_change
import spotube.composeapp.generated.resources.settings_plugins_action_select
import spotube.composeapp.generated.resources.settings_plugins_default_ability_title
import spotube.composeapp.generated.resources.settings_plugins_no_plugins
import spotube.composeapp.generated.resources.settings_plugins_no_selection
import spotube.composeapp.generated.resources.settings_plugins_plugin_content_description

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
                supportText = state.supportText,
                isLoadingSupport = state.isLoadingSupport,
                onRemove = if (state.pluginInfoRemovable) viewModel::removeShownPlugin else null,
                onDismiss = { viewModel.dismissPluginInfo() }
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
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val horizontalPadding = if (maxWidth < 600.dp) 12.dp else 20.dp
                    // Community plugins sit two to a row once each card gets ~340dp.
                    val discoverColumns = if (maxWidth.coerceAtMost(1280.dp) - horizontalPadding * 2 >= 700.dp) 2 else 1
                    val discoverListState = rememberLazyListState()
                    LazyColumn(
                        state = discoverListState,
                        modifier = Modifier.widthIn(max = 1280.dp).fillMaxWidth().align(Alignment.TopCenter),
                        contentPadding = PaddingValues(
                            start = horizontalPadding,
                            end = horizontalPadding,
                            top = 8.dp,
                            bottom = 24.dp + shellBottomInset
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (onboarding) {
                            item {
                                Column(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
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
                                MillenniumSegmentedButtonRow {
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
                                PluginSectionHeader(title = stringResource(Res.string.plugin_defaults_title))
                            }
                            item {
                                DefaultAbilityPluginList(
                                    modifier = Modifier.fillMaxWidth(),
                                    selections = data.abilitySelections,
                                    onSelected = { ability, plugin ->
                                        viewModel.selectPlugin(ability, plugin)
                                    },
                                    onLogin = viewModel::login,
                                    onLogout = viewModel::logout)
                            }

                            item {
                                PluginSectionHeader(
                                    title = stringResource(Res.string.plugin_installed_title),
                                    action = {
                                        MillenniumButton(onClick = { viewModel.showInstallSheet() }) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                MillenniumIcon(
                                                    Iconsax.IconsaxAdd,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp))
                                                Text(stringResource(Res.string.plugin_section_install))
                                            }
                                        }
                                    })
                            }

                            item {
                                PluginListPanel {
                                    if (data.plugins.isEmpty()) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            GelIconTile(Iconsax.IconsaxBox, size = 56.dp)
                                            Text(
                                                stringResource(Res.string.plugin_empty_title),
                                                style = AppTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold)
                                            Text(
                                                stringResource(Res.string.plugin_empty_subtitle),
                                                style = AppTheme.typography.bodySmall,
                                                color = AppTheme.colors.onSurfaceVariant)
                                        }
                                    }
                                    data.plugins.forEachIndexed { index, item ->
                                        if (index > 0) PluginRowSeparator()
                                        PluginCard(
                                            item = item,
                                            onInfo = { viewModel.showPluginInfo(item.plugin) })
                                    }
                                }
                            }
                        }
                        if ((!onboarding || data.onboardingDiscover) && (onboarding || data.discover.isLoading || data.discover.repos.isNotEmpty() || data.discover.error != null)) {
                            item {
                                PluginSectionHeader(
                                    title = stringResource(Res.string.welcome_plugins_discover),
                                    action = if (onboarding) {
                                        {
                                            MillenniumButton(
                                                onClick = viewModel::showInstallSheet,
                                                style = MillenniumButtonStyle.Secondary) {
                                                Text(stringResource(Res.string.plugin_section_install))
                                            }
                                        }
                                    } else null)
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
                                data.discover.repos.chunked(discoverColumns),
                                key = { row -> row.first().id }
                            ) { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    row.forEach { repo ->
                                        DiscoverPluginCard(
                                            repo = repo,
                                            isInstalling = data.discover.installingRepoId == repo.id,
                                            onInstall = { viewModel.showInstallDialog(repo) },
                                            modifier = Modifier.weight(1f).fillMaxHeight())
                                    }
                                    repeat(discoverColumns - row.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
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
    supportText: String?,
    isLoadingSupport: Boolean,
    onRemove: (() -> Unit)?,
    onDismiss: () -> Unit) {
    AdaptiveDialogBottomSheet(
        onDismiss = onDismiss,
        title = { PluginDialogTitle(stringResource(Res.string.plugin_info_dialog_title)) },
        dialogTitle = stringResource(Res.string.plugin_info_dialog_title),
        actions = {
            if (onRemove != null) {
                PluginDialogActions(
                    secondaryLabel = stringResource(Res.string.plugin_action_remove),
                    onSecondary = onRemove,
                    primaryLabel = stringResource(Res.string.common_close),
                    onPrimary = onDismiss)
            } else {
                PluginDialogActions(
                    secondaryLabel = stringResource(Res.string.common_close),
                    onSecondary = onDismiss)
            }
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

            PluginSupportSection(supportText = supportText, isLoading = isLoadingSupport)

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

/** The plugin's own support/donation text (markdown), shown inside the details sheet. */
@Composable
private fun PluginSupportSection(supportText: String?, isLoading: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PluginIconTile(Iconsax.IconsaxHeart, size = 28.dp, tint = AppTheme.colors.error)
            Text(
                stringResource(Res.string.plugin_action_support),
                style = AppTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.onSurface)
        }
        PluginPanel {
            Box(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                contentAlignment = Alignment.CenterStart) {
                when {
                    isLoading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        MillenniumDotsLoader()
                    }
                    supportText.isNullOrBlank() -> Text(
                        stringResource(Res.string.plugin_support_unavailable),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant)
                    else -> SelectionContainer {
                        Markdown(content = supportText)
                    }
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

