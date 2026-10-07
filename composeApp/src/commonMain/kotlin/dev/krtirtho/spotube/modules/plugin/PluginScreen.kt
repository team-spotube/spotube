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

package dev.krtirtho.spotube.modules.plugin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import dev.krtirtho.spotube.PlatformType
import dev.krtirtho.spotube.core.extras.kebabToTitleCase
import dev.krtirtho.spotube.core.ui.base.BaseScaffold
import dev.krtirtho.spotube.core.ui.base.Card
import dev.krtirtho.spotube.core.ui.base.OutlineButton
import dev.krtirtho.spotube.core.ui.base.PrimaryButton
import dev.krtirtho.spotube.core.ui.base.SecondaryButton
import dev.krtirtho.spotube.core.ui.base.SecondaryIconButton
import dev.krtirtho.spotube.core.ui.base.TextField
import dev.krtirtho.spotube.core.ui.base.ThemedDialog
import dev.krtirtho.spotube.core.ui.component.AdaptiveDialogBottomSheet
import dev.krtirtho.spotube.core.ui.component.AdaptiveDropdownBottomSheet
import dev.krtirtho.spotube.core.ui.component.AdaptiveMenuItem
import dev.krtirtho.spotube.core.ui.component.ApplicationMainBar
import dev.krtirtho.spotube.core.ui.component.HeaderDisplayMode
import dev.krtirtho.spotube.getPlatform
import dev.krtirtho.spotube.modules.plugin.components.DefaultAbilityPluginGrid
import dev.krtirtho.spotube.modules.plugin.components.PluginCard
import dev.krtirtho.spotube.modules.plugin.components.PluginInstallDialog
import dev.krtirtho.spotube.modules.plugin.components.PluginLogo
import dev.krtirtho.spotube.modules.plugin.components.PluginPermissionDialog
import dev.krtirtho.spotube.modules.plugin.components.abilityIcon
import dev.krtirtho.spotube.modules.plugin.components.accentColor
import dev.krtirtho.spotube.modules.plugin.components.displayLabel
import dev.krtirtho.spotube.modules.shell.LocalAppShellBottomInset
import dev.krtirtho.spotube.openUrlInBrowser
import dev.krtirtho.spotube.resources.iconsax.CarbonGithubLogo
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxAdd
import dev.krtirtho.spotube.resources.iconsax.IconsaxArrowDown4
import dev.krtirtho.spotube.resources.iconsax.IconsaxBox
import dev.krtirtho.spotube.resources.iconsax.IconsaxCheckCircle
import dev.krtirtho.spotube.resources.iconsax.IconsaxEdit
import dev.krtirtho.spotube.resources.iconsax.IconsaxExportArrowBulk
import dev.krtirtho.spotube.resources.iconsax.IconsaxGlobe
import dev.krtirtho.spotube.resources.iconsax.IconsaxHeart
import dev.krtirtho.spotube.resources.iconsax.IconsaxImportArrow2Bulk
import dev.krtirtho.spotube.resources.iconsax.IconsaxLink
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import okio.Path
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import spotube.composeapp.generated.resources.Res
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
import spotube.composeapp.generated.resources.plugin_installed_count
import spotube.composeapp.generated.resources.plugin_installed_plural
import spotube.composeapp.generated.resources.plugin_installed_singular
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginScreen(
    viewModel: PluginViewModel = koinViewModel(),
    onboarding: Boolean = false,
) {
    BaseScaffold(
        topBar = {
            if (!onboarding) {
                ApplicationMainBar(title = { Text(stringResource(Res.string.plugin_screen_title)) })
            }
        },
    ) { innerPadding ->
        PluginScreenContent(
            viewModel = viewModel,
            onboarding = onboarding,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * The plugin management UI without its own [BaseScaffold] / app bar, so it can be embedded
 * as a tab in the large-screen settings layout as well as hosted by [PluginScreen].
 */
@Composable
internal fun PluginScreenContent(
    viewModel: PluginViewModel,
    onboarding: Boolean = false,
    modifier: Modifier = Modifier,
) {
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
                ) { CircularProgressIndicator() }
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
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        stringResource(Res.string.welcome_plugins_title),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        stringResource(Res.string.welcome_plugins_setup_guide),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            item {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    OutlineButton(onClick = { viewModel.showOnboardingDiscovery(true) }) {
                                        Text(
                                            stringResource(Res.string.welcome_plugins_discover),
                                            fontWeight = if (data.onboardingDiscover) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                    OutlineButton(onClick = {
                                        viewModel.showOnboardingDiscovery(
                                            false
                                        )
                                    }) {
                                        Text(
                                            stringResource(Res.string.welcome_plugins_sources),
                                            fontWeight = if (!data.onboardingDiscover) FontWeight.Bold else FontWeight.Normal
                                        )
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
                                    },
                                )
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
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    PrimaryButton(onClick = { viewModel.showInstallSheet() }) {
                                        Icon(
                                            Iconsax.IconsaxAdd,
                                            contentDescription = stringResource(Res.string.plugin_install_section_title),
                                        )
                                        Text(stringResource(Res.string.plugin_install_section_title))
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
                                            Surface(
                                                modifier = Modifier.size(72.dp)
                                                    .clip(RoundedCornerShape(18.dp)),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Iconsax.IconsaxBox,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(32.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                            Text(
                                                stringResource(Res.string.plugin_empty_title),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                stringResource(Res.string.plugin_empty_subtitle),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                item {
                                    val noun = if (data.plugins.size == 1) {
                                        stringResource(Res.string.plugin_installed_singular)
                                    } else {
                                        stringResource(Res.string.plugin_installed_plural)
                                    }
                                    Text(
                                        stringResource(
                                            Res.string.plugin_installed_count,
                                            data.plugins.size,
                                            noun
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(
                                            horizontal = 4.dp,
                                            vertical = 4.dp
                                        )
                                    )
                                }
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            data.plugins.forEachIndexed { index, item ->
                                                if (index > 0) {
                                                    HorizontalDivider(
                                                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                            alpha = 0.5f
                                                        ),
                                                    )
                                                }
                                                PluginCard(
                                                    plugin = item.plugin,
                                                    isSelected = item.isSelected,
                                                    onRemove = { viewModel.removePlugin(item.plugin) },
                                                    isLoggedIn = item.authState.isLoggedIn,
                                                    logoPath = item.logoPath,
                                                    onInfo = { viewModel.showPluginInfo(item.plugin) },
                                                    onSupport = if (item.isSelected) {
                                                        { viewModel.loadSupport(item.plugin) }
                                                    } else null,
                                                    onLogin = if (item.isSelected && item.authState.requiresAuth) {
                                                        { viewModel.login(item.plugin) }
                                                    } else null,
                                                    onLogout = if (item.isSelected && item.authState.requiresAuth) {
                                                        { viewModel.logout(item.plugin) }
                                                    } else null,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                        }
                        if ((!onboarding || data.onboardingDiscover) && (onboarding || data.discover.isLoading || data.discover.repos.isNotEmpty() || data.discover.error != null)) {
                            if (onboarding) {
                                item {
                                    OutlineButton(onClick = viewModel::showInstallSheet) {
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
                                    Icon(
                                        Iconsax.IconsaxGlobe,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        stringResource(Res.string.welcome_plugins_discover),
                                        style = MaterialTheme.typography.titleLarge,
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
                                        CircularProgressIndicator(Modifier.size(24.dp))
                                    }
                                }
                            }
                            if (onboarding && data.discover.isInitialLoaded && !data.discover.isLoading && data.discover.repos.isEmpty() && data.discover.error == null) {
                                item {
                                    Text(
                                        stringResource(Res.string.welcome_plugins_discovery_empty),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }

                            data.discover.error?.let { error ->
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            stringResource(Res.string.welcome_plugins_discovery_error),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(
                                                horizontal = 4.dp,
                                                vertical = 8.dp
                                            )
                                        )
                                        OutlineButton(onClick = viewModel::retryDiscovery) {
                                            Text(stringResource(Res.string.welcome_plugins_retry))
                                        }
                                    }
                                }
                            }
                            if (onboarding && data.discover.hasMore && !data.discover.isLoading && !data.discover.isLoadingMore && data.discover.error == null) {
                                item {
                                    OutlineButton(onClick = viewModel::loadNextDiscoverPage) {
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
    onUrlChange: (String) -> Unit,
    onSubmitUrl: () -> Unit,
    onPickFile: () -> Unit,
    onDismiss: () -> Unit,
) {
    AdaptiveDialogBottomSheet(
        onDismiss = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Iconsax.IconsaxImportArrow2Bulk,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(Res.string.plugin_install_section_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(Res.string.plugin_section_url_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = urlInput,
                    onValueChange = onUrlChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            stringResource(Res.string.plugin_url_placeholder),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Iconsax.IconsaxLink,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    isError = urlError != null,
                    singleLine = true,
                )
                SecondaryIconButton(
                    onClick = onSubmitUrl,
                    enabled = !isLoadingUrl,
                ) {
                    if (isLoadingUrl) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            Iconsax.IconsaxImportArrow2Bulk,
                            contentDescription = stringResource(Res.string.plugin_action_download),
                        )
                    }
                }
            }

            urlError?.let { error ->
                Text(
                    error.text(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Text(
                stringResource(Res.string.plugin_section_file_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlineButton(
                onClick = onPickFile,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Iconsax.IconsaxExportArrowBulk,
                    contentDescription = stringResource(Res.string.plugin_action_install_from_file)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.plugin_action_install_from_file))
            }
        }
    }
}

@Composable
private fun PluginInfoDialog(
    plugin: PluginEntry,
    logoPath: Path?,
    onDismiss: () -> Unit,
) {
    ThemedDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    PluginLogo(
                        plugin = plugin,
                        logoPath = logoPath,
                        contentDescription = plugin.name,
                        modifier = Modifier.fillMaxSize(),
                        fallback = {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Iconsax.IconsaxBox,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                    )
                }
                Text(
                    plugin.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        actions = {
            PrimaryButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (plugin.description.isNotBlank()) {
                Text(
                    plugin.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            DetailRow("Version", plugin.version)
            DetailRow("API Version", plugin.apiVersion)
            DetailRow("Author", plugin.author)
            if (plugin.license.isNotBlank()) {
                DetailRow("License", plugin.license)
            }

            if (plugin.capabilities.isNotEmpty()) {
                DetailChipsRow("Capabilities", plugin.capabilities.map { it.name })
            }

            if (plugin.abilities.isNotEmpty()) {
                DetailChipsRow("Abilities", plugin.abilities.map { ability ->
                    when (ability) {
                        PluginAbility.METADATA -> "Metadata"
                        PluginAbility.AUDIO -> "Audio"
                        PluginAbility.LYRICS -> "Lyrics"
                        PluginAbility.SCROBBLE -> "Scrobble"
                    }
                })
            }

            if (plugin.repository.isNotBlank()) {
                ClickableDetailRow(label = "Repository", value = plugin.repository) {
                    openUrlInBrowser(plugin.repository)
                }
            }

            if (plugin.contact.isNotBlank()) {
                DetailRow("Contact", plugin.contact)
            }

            if (plugin.bugs.isNotBlank()) {
                ClickableDetailRow(label = "Report Bugs", value = plugin.bugs) {
                    openUrlInBrowser(plugin.bugs)
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
    onDismiss: () -> Unit,
) {
    ThemedDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Support ${plugin.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        actions = {
            PrimaryButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Text(
                supportText ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DiscoverRepoCard(
    repo: GitHubRepo,
    isInstalling: Boolean,
    onInstall: () -> Unit,
) {
    val isOfficial = repo.owner.login in OFFICIAL_PLUGIN_OWNERS
    val isVerified = repo.owner.login in VERIFIED_PLUGIN_OWNERS

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
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
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isOfficial) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "Official",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    } else if (isVerified) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    Iconsax.IconsaxCheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = Color(0xFF4CAF50)
                                )
                                Text(
                                    "Verified",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }
                if (!repo.description.isNullOrBlank()) {
                    Text(
                        repo.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        repo.owner.login,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Iconsax.IconsaxHeart,
                            contentDescription = "Github Stars",
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            repo.stargazersCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable {
                            openUrlInBrowser(repo.htmlUrl)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Iconsax.CarbonGithubLogo,
                                contentDescription = "Github Repository URL",
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "github.com",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            SecondaryButton(
                onClick = onInstall,
                enabled = !isInstalling
            ) {
                if (isInstalling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Iconsax.IconsaxAdd,
                        contentDescription = null,
                    )
                }
                Text(stringResource(Res.string.plugin_section_install))
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
            "$label:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 90.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
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
            "$label:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 90.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
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
            "$label:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 90.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            chips.forEach { chip ->
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        chip,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
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
    onSelected: (PluginEntry?) -> Unit = { },
) {
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
                        selected = isSelected,
                    )
                )
            }
        } else {
            add(
                AdaptiveMenuItem(
                    label = noPluginsText,
                    onClick = { },
                    enabled = false,
                )
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
            Surface(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                color = selection.ability.accentColor().copy(alpha = 0.1f)
            ) {
                Icon(
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
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (selectedPlugin != null) {
                    Text(
                        selectedPlugin.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        stringResource(Res.string.settings_plugins_no_selection),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                        color = selection.ability.accentColor().copy(alpha = 0.1f)
                    ) {
                        Icon(
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
                            style = MaterialTheme.typography.titleMedium,
                        )
                        selectedPlugin?.let {
                            Text(
                                it.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            },
            trigger = { onClick ->
                OutlineButton(onClick = onClick) {
                    Text(
                        if (selectedPlugin != null) {
                            stringResource(Res.string.settings_plugins_action_change)
                        } else {
                            stringResource(Res.string.settings_plugins_action_select) + "  "
                        },
                    )
                    Icon(
                        imageVector = if (selectedPlugin != null) Iconsax.IconsaxEdit else Iconsax.IconsaxArrowDown4,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                }
            },
        )
    }
}

