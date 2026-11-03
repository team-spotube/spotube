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

package org.spotube.spotube.modules.jam

import org.spotube.millennium.composables.MillenniumDropdownMenuItem
import org.spotube.spotube.resources.iconsax.IconsaxUserRemove
import org.spotube.spotube.resources.iconsax.IconsaxForbidden
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.Text
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.settings.sections.JamNameSettingCard
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxSetting2
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumSegmentedButton
import org.spotube.millennium.composables.MillenniumSegmentedButtonDefaults
import org.spotube.millennium.composables.MillenniumSegmentedButtonRow
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.millennium.composables.MillenniumTextField
import org.spotube.millennium.composables.MillenniumTooltip
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumBevelBrush
import org.spotube.millennium.theme.millenniumPanelBrush
import org.spotube.millennium.theme.millenniumSurface
import spotube.composeapp.generated.resources.*

@Composable
fun JamScreen(navigationCommands: NavigationCommands) {
    val viewModel = koinViewModel<JamViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val shellBottomInset = LocalAppShellBottomInset.current

    Column(Modifier.fillMaxSize().background(MillenniumTheme.colors.background)) {
        ApplicationMainBar(title = { Text(stringResource(Res.string.jam_screen_title)) })
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = shellBottomInset + 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.error?.let { error ->
                    JamPanel {
                        Text(
                            error,
                            color = AppTheme.colors.error,
                            style = AppTheme.typography.bodySmall
                        )
                        MillenniumButton(
                            onClick = viewModel::clearError,
                            style = MillenniumButtonStyle.Secondary
                        ) {
                            Text(stringResource(Res.string.jam_error_dismiss))
                        }
                    }
                }
                if (state.isActive) {
                    SessionPanel(
                        state = state,
                        onCopy = {
                            clipboard.setText(AnnotatedString(state.roomCode.orEmpty()))
                            viewModel.onCodeCopied()
                        },
                        onShare = viewModel::shareRoomCode,
                        onLeave = viewModel::leave,
                        onKick = viewModel::kickParticipant,
                        onBan = viewModel::banParticipant,
                        onMenuChange = viewModel::showParticipantMenu
                    )
                } else {
                    SetupPanel(
                        state,
                        viewModel::selectSetupMode,
                        viewModel::createSession,
                        viewModel::joinSession
                    )
                    // Pick a name before starting or joining, without a detour through Settings.
                    JamPanel {
                        JamNameSettingCard(
                            customName = state.customName,
                            defaultName = state.defaultName,
                            onNameSaved = viewModel::setParticipantName,
                            enabled = !state.isBusy
                        )
                    }
                }
                ServerStatus(state) { navigationCommands.navigateTo(Routes.JamSettings) }
            }
        }
    }
}

@Composable
private fun JamPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().millenniumSurface(
            background = millenniumPanelBrush(MillenniumTheme.colors),
            shape = RoundedCornerShape(10.dp),
            borderColor = MillenniumTheme.colors.border,
            borderWidth = 1.dp,
            innerGlowColor = MillenniumTheme.colors.highlight.copy(alpha = 0.25f)
        ).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

@Composable
internal fun SetupPanel(
    state: JamUiState,
    onMode: (JamSetupMode) -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit
) {
    JamPanel {
        MillenniumSegmentedButtonRow(Modifier.fillMaxWidth()) {
            MillenniumSegmentedButton(
                selected = state.setupMode == JamSetupMode.Create,
                onClick = { onMode(JamSetupMode.Create) },
                shape = MillenniumSegmentedButtonDefaults.itemShape(0, 2)
            ) { Text(stringResource(Res.string.jam_setup_create_tab)) }
            MillenniumSegmentedButton(
                selected = state.setupMode == JamSetupMode.Join,
                onClick = { onMode(JamSetupMode.Join) },
                shape = MillenniumSegmentedButtonDefaults.itemShape(1, 2)
            ) { Text(stringResource(Res.string.jam_setup_join_tab)) }
        }
        if (state.setupMode == JamSetupMode.Create) {
            SharedRoomIllustration(Modifier.fillMaxWidth().height(100.dp))
            Text(
                stringResource(Res.string.jam_setup_create_hint),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant
            )
        } else {
            MillenniumTextField(
                state = state.codeInput,
                label = stringResource(Res.string.jam_setup_code_label),
                placeholder = stringResource(Res.string.jam_setup_code_placeholder),
                enabled = !state.isBusy,
                textStyle = AppTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 4.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        MillenniumButton(
            onClick = if (state.setupMode == JamSetupMode.Create) onCreate else onJoin,
            enabled = if (state.setupMode == JamSetupMode.Create) state.canCreate else state.canJoin,
            loading = state.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(
                    when {
                        state.setupMode == JamSetupMode.Create -> Res.string.jam_setup_create_button
                        else -> Res.string.jam_setup_join_button
                    }
                )
            )
        }
    }
}

/** A small media-player diagram: one source feeding two listeners. No stock artwork. */
@Composable
private fun SharedRoomIllustration(modifier: Modifier) {
    val colors = MillenniumTheme.colors
    Canvas(modifier) {
        val scale = minOf(size.width / 260f, size.height / 100f)
        val origin = Offset((size.width - 260f * scale) / 2f, (size.height - 100f * scale) / 2f)
        fun point(x: Float, y: Float) = origin + Offset(x * scale, y * scale)
        val stroke = 1.2f * scale
        val metal = Brush.verticalGradient(
            listOf(colors.raised, colors.surface),
            startY = origin.y,
            endY = origin.y + 100f * scale
        )
        drawLine(colors.border, point(45f, 50f), point(215f, 50f), 2f * scale)
        drawLine(colors.accent.copy(alpha = 0.7f), point(45f, 49f), point(215f, 49f), stroke)
        // Two miniature speakers, with an accent power LED and concentric drivers.
        for (x in listOf(15f, 207f)) {
            drawRoundRect(
                metal,
                point(x, 18f),
                Size(38f * scale, 64f * scale),
                CornerRadius(5f * scale)
            )
            drawRoundRect(
                colors.border,
                point(x, 18f),
                Size(38f * scale, 64f * scale),
                CornerRadius(5f * scale),
                style = Stroke(stroke)
            )
            drawCircle(colors.background, 10f * scale, point(x + 19f, 57f))
            drawCircle(colors.border, 10f * scale, point(x + 19f, 57f), style = Stroke(stroke))
            drawCircle(colors.raised, 5f * scale, point(x + 19f, 57f))
            drawCircle(colors.background, 4f * scale, point(x + 19f, 34f))
            drawCircle(colors.accent, 1.5f * scale, point(x + 19f, 74f))
        }
        // Brushed-metal source unit with a backlit equalizer display.
        drawRoundRect(
            metal,
            point(82f, 10f),
            Size(96f * scale, 80f * scale),
            CornerRadius(8f * scale)
        )
        drawRoundRect(
            colors.border,
            point(82f, 10f),
            Size(96f * scale, 80f * scale),
            CornerRadius(8f * scale),
            style = Stroke(stroke)
        )
        drawRoundRect(
            colors.highlight.copy(alpha = 0.25f),
            point(86f, 13f),
            Size(88f * scale, 1f * scale),
            CornerRadius(scale)
        )
        drawRoundRect(
            colors.background,
            point(92f, 22f),
            Size(76f * scale, 30f * scale),
            CornerRadius(3f * scale)
        )
        val bars = listOf(8f, 16f, 12f, 22f, 18f, 10f, 15f, 7f)
        bars.forEachIndexed { index, height ->
            drawRoundRect(
                colors.accent.copy(alpha = 0.85f),
                point(99f + index * 8f, 47f - height),
                Size(4f * scale, height * scale),
                CornerRadius(scale)
            )
        }
        drawCircle(colors.background, 11f * scale, point(130f, 70f))
        drawCircle(colors.border, 11f * scale, point(130f, 70f), style = Stroke(stroke))
        drawPath(Path().apply {
            moveTo(point(127f, 65f).x, point(127f, 65f).y)
            lineTo(point(135f, 70f).x, point(135f, 70f).y)
            lineTo(point(127f, 75f).x, point(127f, 75f).y)
            close()
        }, colors.text)
        for (x in listOf(101f, 159f)) drawRoundRect(
            colors.border,
            point(x - 4f, 68f),
            Size(8f * scale, 4f * scale),
            CornerRadius(scale)
        )
    }
}

@Composable
internal fun SessionPanel(
    state: JamUiState,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onLeave: () -> Unit,
    onKick: (String) -> Unit,
    onBan: (String) -> Unit,
    onMenuChange: (String?) -> Unit
) {
    JamPanel {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier.size(7.dp).background(
                    if (state.isConnected) AppTheme.colors.primary else AppTheme.colors.onSurfaceVariant,
                    CircleShape
                )
            )
            Text(
                stringResource(if (state.isConnected) Res.string.jam_session_connected else Res.string.jam_session_connecting),
                style = AppTheme.typography.labelMedium
            )
        }
        Column(
            Modifier.fillMaxWidth().millenniumSurface(
                background = SolidColor(MillenniumTheme.colors.background),
                shape = RoundedCornerShape(5.dp),
                borderColor = MillenniumTheme.colors.border,
                borderWidth = 1.dp
            ).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                stringResource(Res.string.jam_setup_code_label),
                style = AppTheme.typography.labelSmall,
                color = AppTheme.colors.onSurfaceVariant
            )
            SelectionContainer {
                Text(
                    state.roomCode.orEmpty(),
                    style = AppTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 5.sp,
                    color = AppTheme.colors.primary
                )
            }
        }
        if (state.isHost) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MillenniumButton(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f),
                    style = MillenniumButtonStyle.Secondary
                ) {
                    Text(stringResource(if (state.codeCopied) Res.string.jam_invite_copied else Res.string.jam_invite_copy))
                }
                if (state.supportsNativeShare) MillenniumButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    style = MillenniumButtonStyle.Secondary
                ) {
                    Text(stringResource(Res.string.jam_invite_share))
                }
            }
        }
        MillenniumSeparator()
        Text(
            stringResource(Res.string.jam_participants_heading, state.participants.size),
            style = AppTheme.typography.titleSmall
        )
        Column(
            Modifier.fillMaxWidth().millenniumSurface(
                background = SolidColor(MillenniumTheme.colors.background),
                shape = RoundedCornerShape(5.dp),
                borderColor = MillenniumTheme.colors.border,
                borderWidth = 1.dp
            ).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.participants.isEmpty()) Text(
                stringResource(Res.string.jam_participants_waiting),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant
            )
            state.participants.forEach { participant ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier.size(32.dp).millenniumSurface(
                            background = millenniumBevelBrush(MillenniumTheme.colors),
                            shape = CircleShape,
                            borderColor = MillenniumTheme.colors.border,
                            borderWidth = 1.dp
                        ), contentAlignment = Alignment.Center
                    ) {
                        Text(participant.initial, style = AppTheme.typography.labelMedium)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (participant.isYou) {
                                stringResource(Res.string.jam_participants_you, participant.displayName)
                            } else {
                                participant.displayName
                            },
                            style = AppTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            stringResource(if (participant.isHost) Res.string.jam_participants_host else Res.string.jam_participants_guest),
                            style = AppTheme.typography.labelSmall,
                            color = AppTheme.colors.onSurfaceVariant
                        )
                    }
                    if (participant.canModerate) {
                        // Keep destructive moderation out of the participant's primary row.
                        org.spotube.millennium.composables.MillenniumDropdownMenu(
                            expanded = participant.menuExpanded,
                            onExpandedChange = { expanded -> onMenuChange(if (expanded) participant.id else null) },
                            anchor = {
                                MillenniumButton(
                                    onClick = { onMenuChange(participant.id) },
                                    style = MillenniumButtonStyle.Secondary
                                ) { Text(stringResource(Res.string.jam_participants_manage)) }
                            }
                        ) {
                            MillenniumDropdownMenuItem(
                                label = stringResource(Res.string.jam_participants_remove),
                                icon = Iconsax.IconsaxUserRemove,
                                onClick = { onKick(participant.id) })
                            MillenniumDropdownMenuItem(
                                label = stringResource(Res.string.jam_participants_ban),
                                icon = Iconsax.IconsaxForbidden,
                                destructive = true,
                                onClick = { onBan(participant.id) })
                        }
                    }
                }
            }
        }
        MillenniumButton(
            onClick = onLeave,
            loading = state.isLeaving,
            style = MillenniumButtonStyle.Secondary,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(stringResource(Res.string.jam_session_leave))
        }
    }
}

@Composable
internal fun ServerStatus(state: JamUiState, onConfigure: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            if (state.serverConfigured) stringResource(
                Res.string.jam_connection_host,
                state.serverHost
            ) else stringResource(Res.string.jam_connection_missing),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        MillenniumTooltip(stringResource(Res.string.jam_connection_settings)) {
            MillenniumIconButton(onClick = onConfigure, style = MillenniumIconButtonStyle.Quiet) {
                MillenniumIcon(
                    Iconsax.IconsaxSetting2,
                    contentDescription = stringResource(Res.string.jam_connection_settings)
                )
            }
        }
    }
}
