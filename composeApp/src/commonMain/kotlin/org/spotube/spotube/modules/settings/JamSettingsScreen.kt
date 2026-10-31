package org.spotube.spotube.modules.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import com.composeunstyled.Text
import org.spotube.millennium.composables.MillenniumButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.modules.settings.components.TextInputSettingCard
import org.spotube.spotube.modules.settings.sections.JamNameSettingCard
import org.spotube.spotube.modules.settings.sections.SettingsItemIcon
import org.spotube.spotube.modules.settings.sections.settingsSectionCard
import org.spotube.spotube.modules.settings.sections.settingsSectionHeader
import org.spotube.spotube.resources.iconsax.CustomServer
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxInformation
import org.spotube.spotube.modules.settings.components.SettingCardItem
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.jam_error_connection_failed
import spotube.composeapp.generated.resources.settings_jam_section_connection
import spotube.composeapp.generated.resources.settings_jam_section_diagnostics
import spotube.composeapp.generated.resources.settings_jam_section_you
import spotube.composeapp.generated.resources.settings_jam_server_diagnostics_subtitle
import spotube.composeapp.generated.resources.settings_jam_server_note
import spotube.composeapp.generated.resources.settings_jam_server_test
import spotube.composeapp.generated.resources.settings_jam_server_test_fail
import spotube.composeapp.generated.resources.settings_jam_server_testing
import spotube.composeapp.generated.resources.settings_jam_server_title
import spotube.composeapp.generated.resources.settings_jam_server_url
import spotube.composeapp.generated.resources.settings_jam_server_url_invalid
import spotube.composeapp.generated.resources.settings_section_jam

@Composable
fun JamSettingsScreen(
    viewModel: JamSettingsViewModel = koinViewModel()) {
    Column(modifier = Modifier.fillMaxSize()) {
        ApplicationMainBar(
            title = { Text(stringResource(Res.string.settings_section_jam)) })
        JamSettingsContent(
            viewModel = viewModel,
            modifier = Modifier.weight(1f))
    }
}

/**
 * The Group Jam settings UI without its own app bar, so it can be embedded
 * as a tab in the large-screen settings layout as well as hosted by [JamSettingsScreen].
 */
@Composable
internal fun JamSettingsContent(
    viewModel: JamSettingsViewModel = koinViewModel(),
    modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val shellBottomInset = LocalAppShellBottomInset.current
    val contentPadding = remember(shellBottomInset) {
        PaddingValues(top = 16.dp, bottom = 16.dp + shellBottomInset)
    }

    Box(
        modifier = modifier
            .fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 1280.dp)
                .align(Alignment.TopCenter),
            contentPadding = contentPadding) {
            settingsSectionHeader(Res.string.settings_jam_section_you)
            settingsSectionCard(
                items = listOf(
                    {
                        JamNameSettingCard(
                            customName = uiState.settings.jamParticipantName,
                            defaultName = uiState.defaultName,
                            onNameSaved = viewModel::setParticipantName)
                    }))
            settingsSectionHeader(Res.string.settings_jam_section_connection)
            settingsSectionCard(
                items = listOf(
                    {
                        val invalidUrl = stringResource(Res.string.settings_jam_server_url_invalid)
                        TextInputSettingCard(
                            title = stringResource(Res.string.settings_jam_server_url),
                            value = uiState.settings.jamServerUrl,
                            placeholder = DEFAULT_JAM_SERVER_URL,
                            validate = { value ->
                                if (value.isBlank() || isJamServerUrl(value)) null else invalidUrl
                            },
                            icon = {
                                SettingsItemIcon(
                                    Iconsax.CustomServer,
                                    stringResource(Res.string.settings_jam_server_title))
                            },
                            onValueSaved = viewModel::setServerUrl)
                    }))
            settingsSectionHeader(Res.string.settings_jam_section_diagnostics)
            settingsSectionCard(
                items = listOf(
                    {
                        val resultMessage = uiState.testResult?.fold(
                            onSuccess = { it },
                            onFailure = {
                                stringResource(
                                    Res.string.settings_jam_server_test_fail,
                                    it.message ?: stringResource(Res.string.jam_error_connection_failed))
                            })

                        SettingCardItem(
                            enabled = !uiState.isTesting,
                            title = stringResource(Res.string.settings_jam_server_test),
                            subtitle = when {
                                uiState.isTesting -> stringResource(Res.string.settings_jam_server_testing)
                                resultMessage != null -> resultMessage
                                else -> stringResource(Res.string.settings_jam_server_diagnostics_subtitle)
                            },
                            icon = {
                                SettingsItemIcon(
                                    Iconsax.IconsaxInformation,
                                    stringResource(Res.string.settings_jam_server_test))
                            },
                            trailingContent = {
                                MillenniumButton(
                                    onClick = viewModel::testConnection,
                                    loading = uiState.isTesting) {
                                    Text(stringResource(Res.string.settings_jam_server_test))
                                }
                            },
                            onClick = viewModel::testConnection)
                    }))
            item {
                Text(
                    text = stringResource(Res.string.settings_jam_server_note),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }
    }
}
