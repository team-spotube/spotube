package dev.krtirtho.spotube.modules.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krtirtho.spotube.core.ui.base.PrimaryButton
import dev.krtirtho.spotube.core.ui.component.ApplicationMainBar
import dev.krtirtho.spotube.modules.settings.components.SwitchSettingCard
import dev.krtirtho.spotube.modules.settings.components.TextInputSettingCard
import dev.krtirtho.spotube.modules.settings.sections.SettingsItemIcon
import dev.krtirtho.spotube.modules.settings.sections.settingsSectionCard
import dev.krtirtho.spotube.modules.settings.sections.settingsSectionHeader
import dev.krtirtho.spotube.resources.iconsax.CustomServer
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxEye
import dev.krtirtho.spotube.resources.iconsax.IconsaxInformation
import dev.krtirtho.spotube.resources.iconsax.IconsaxTag
import dev.krtirtho.spotube.resources.iconsax.IconsaxWifiSquare
import dev.krtirtho.spotube.resources.iconsax.User
import dev.krtirtho.spotube.modules.settings.components.SettingCardItem
import dev.krtirtho.spotube.modules.shell.LocalAppShellBottomInset
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_error_port_range
import spotube.composeapp.generated.resources.settings_error_whole_number
import spotube.composeapp.generated.resources.settings_jam_broker_client_id
import spotube.composeapp.generated.resources.settings_jam_broker_client_id_subtitle
import spotube.composeapp.generated.resources.settings_jam_broker_diagnostics_subtitle
import spotube.composeapp.generated.resources.settings_jam_broker_host
import spotube.composeapp.generated.resources.settings_jam_broker_password
import spotube.composeapp.generated.resources.settings_jam_broker_placeholder_note
import spotube.composeapp.generated.resources.settings_jam_broker_port
import spotube.composeapp.generated.resources.settings_jam_broker_test
import spotube.composeapp.generated.resources.settings_jam_broker_testing
import spotube.composeapp.generated.resources.settings_jam_broker_tls
import spotube.composeapp.generated.resources.settings_jam_broker_tls_subtitle
import spotube.composeapp.generated.resources.settings_jam_section_connection
import spotube.composeapp.generated.resources.settings_jam_section_credentials
import spotube.composeapp.generated.resources.settings_jam_section_diagnostics
import spotube.composeapp.generated.resources.settings_jam_section_identity
import spotube.composeapp.generated.resources.settings_jam_broker_username
import spotube.composeapp.generated.resources.settings_section_jam

@Composable
fun JamSettingsScreen(
    viewModel: JamSettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val broker = uiState.settings.jamBroker
    val shellBottomInset = LocalAppShellBottomInset.current
    val contentPadding = remember(shellBottomInset) {
        PaddingValues(top = 16.dp, bottom = 16.dp + shellBottomInset)
    }

    Scaffold(
        topBar = {
            ApplicationMainBar(
                title = { Text(stringResource(Res.string.settings_section_jam)) },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 1280.dp)
                    .align(Alignment.TopCenter),
                contentPadding = contentPadding,
            ) {
                settingsSectionHeader(Res.string.settings_jam_section_connection)
                settingsSectionCard(
                    items = listOf(
                        {
                            TextInputSettingCard(
                                title = stringResource(Res.string.settings_jam_broker_host),
                                value = broker.host,
                                placeholder = "broker.example.com",
                                icon = { SettingsItemIcon(Iconsax.CustomServer, "Broker") },
                                onValueSaved = { host ->
                                    viewModel.update {
                                        copy(
                                            jamBroker = jamBroker.copy(
                                                host = host
                                            )
                                        )
                                    }
                                },
                            )
                        },
                        {
                            val errorWholeNumber =
                                stringResource(Res.string.settings_error_whole_number)
                            val errorPortRange =
                                stringResource(Res.string.settings_error_port_range)

                            TextInputSettingCard(
                                title = stringResource(Res.string.settings_jam_broker_port),
                                value = broker.port.toString(),
                                placeholder = "1883",
                                normalize = { it.filter(Char::isDigit).take(5) },
                                validate = { value ->
                                    val port = value.toIntOrNull()
                                    when {
                                        port == null -> errorWholeNumber
                                        port !in 1..65535 -> errorPortRange
                                        else -> null
                                    }
                                },
                                onValueSaved = { port ->
                                    viewModel.update { copy(jamBroker = jamBroker.copy(port = port.toInt())) }
                                },
                                icon = {
                                    SettingsItemIcon(
                                        Iconsax.CustomServer,
                                        stringResource(Res.string.settings_jam_broker_port)
                                    )
                                },
                            )
                        },
                        {
                            SwitchSettingCard(
                                title = stringResource(Res.string.settings_jam_broker_tls),
                                subtitle = stringResource(Res.string.settings_jam_broker_tls_subtitle),
                                checked = broker.useTls,
                                onCheckedChange = { tls ->
                                    viewModel.update { copy(jamBroker = jamBroker.copy(useTls = tls)) }
                                },
                                icon = {
                                    SettingsItemIcon(
                                        Iconsax.IconsaxWifiSquare,
                                        stringResource(Res.string.settings_jam_broker_tls)
                                    )
                                },
                            )
                        },
                    ),
                )
                settingsSectionHeader(Res.string.settings_jam_section_credentials)
                settingsSectionCard(
                    items = listOf(
                        {
                            TextInputSettingCard(
                                title = stringResource(Res.string.settings_jam_broker_username),
                                value = broker.username.orEmpty(),
                                placeholder = "anonymous",
                                onValueSaved = { username ->
                                    viewModel.update {
                                        copy(jamBroker = jamBroker.copy(username = username.ifBlank { null }))
                                    }
                                },
                                icon = {
                                    SettingsItemIcon(
                                        Iconsax.User,
                                        stringResource(Res.string.settings_jam_broker_username)
                                    )
                                },
                            )
                        },
                        {
                            TextInputSettingCard(
                                title = stringResource(Res.string.settings_jam_broker_password),
                                value = broker.password.orEmpty(),
                                placeholder = "••••••••",
                                onValueSaved = { password ->
                                    viewModel.update {
                                        copy(jamBroker = jamBroker.copy(password = password.ifBlank { null }))
                                    }
                                },
                                icon = {
                                    SettingsItemIcon(
                                        Iconsax.IconsaxEye,
                                        stringResource(Res.string.settings_jam_broker_password)
                                    )
                                },
                            )
                        },
                    ),
                )
                settingsSectionHeader(Res.string.settings_jam_section_identity)
                settingsSectionCard(
                    items = listOf(
                        {
                            TextInputSettingCard(
                                title = stringResource(Res.string.settings_jam_broker_client_id),
                                subtitle = stringResource(Res.string.settings_jam_broker_client_id_subtitle),
                                value = broker.clientIdPrefix,
                                placeholder = "spotube",
                                onValueSaved = { prefix ->
                                    viewModel.update {
                                        copy(jamBroker = jamBroker.copy(clientIdPrefix = prefix.ifBlank { "spotube" }))
                                    }
                                },
                                icon = {
                                    SettingsItemIcon(
                                        Iconsax.IconsaxTag,
                                        stringResource(Res.string.settings_jam_broker_client_id)
                                    )
                                },
                            )
                        },
                    ),
                )
                settingsSectionHeader(Res.string.settings_jam_section_diagnostics)
                settingsSectionCard(
                    items = listOf(
                        {
                            val canTest = broker.host.isNotBlank() && !uiState.isTesting
                            val resultMessage = uiState.testResult?.fold(
                                onSuccess = { it },
                                onFailure = { "Failed: ${it.message ?: "Connection failed"}" },
                            )

                            SettingCardItem(
                                enabled = canTest,
                                title = stringResource(Res.string.settings_jam_broker_test),
                                subtitle = when {
                                    uiState.isTesting -> stringResource(Res.string.settings_jam_broker_testing)
                                    resultMessage != null -> resultMessage
                                    else -> stringResource(Res.string.settings_jam_broker_diagnostics_subtitle)
                                },
                                icon = {
                                    SettingsItemIcon(
                                        Iconsax.IconsaxInformation,
                                        stringResource(Res.string.settings_jam_broker_test),
                                    )
                                },
                                trailingContent = {
                                    PrimaryButton(
                                        onClick = viewModel::testConnection,
                                        enabled = canTest,
                                    ) {
                                        Text(
                                            if (uiState.isTesting) {
                                                stringResource(Res.string.settings_jam_broker_testing)
                                            } else {
                                                stringResource(Res.string.settings_jam_broker_test)
                                            },
                                        )
                                    }
                                },
                                onClick = viewModel::testConnection,
                            )
                        },
                    ),
                )
                item {
                    Text(
                        text = stringResource(Res.string.settings_jam_broker_placeholder_note),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}
