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

package org.spotube.spotube.modules.support

import org.spotube.millennium.composables.MillenniumDialogHeader
import org.spotube.millennium.composables.MillenniumDialogTitle
import org.spotube.millennium.composables.MillenniumDialogClose
import org.spotube.millennium.composables.MillenniumDialogBody
import org.spotube.millennium.composables.MillenniumDialogDescription
import org.spotube.millennium.composables.MillenniumDialogFooter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.openUrlInBrowser
import org.spotube.spotube.resources.iconsax.BrandBuyMeACoffee
import org.spotube.spotube.resources.iconsax.BrandOpencollective
import org.spotube.spotube.resources.iconsax.BrandPatreon
import org.spotube.spotube.resources.iconsax.Iconsax
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumDialog
import org.spotube.millennium.composables.MillenniumIcon
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.support_dialog_dismiss
import spotube.composeapp.generated.resources.support_dialog_message
import spotube.composeapp.generated.resources.support_dialog_reminder_hint
import spotube.composeapp.generated.resources.support_dialog_title
import spotube.composeapp.generated.resources.welcome_donate_bmac_subtitle
import spotube.composeapp.generated.resources.welcome_donate_bmac_title
import spotube.composeapp.generated.resources.welcome_donate_opencollective_subtitle
import spotube.composeapp.generated.resources.welcome_donate_opencollective_title
import spotube.composeapp.generated.resources.welcome_donate_patreon_subtitle
import spotube.composeapp.generated.resources.welcome_donate_patreon_title

/** A single donation destination offered across the app. */
data class DonationOption(
    val id: String,
    val title: StringResource,
    val subtitle: StringResource,
    val url: String,
    val icon: ImageVector)

/** The donation destinations shown in onboarding, settings and the support prompt. */
val donationOptions: List<DonationOption> = listOf(
    DonationOption(
        id = "patreon",
        title = Res.string.welcome_donate_patreon_title,
        subtitle = Res.string.welcome_donate_patreon_subtitle,
        url = "https://patreon.com/krtirtho",
        icon = Iconsax.BrandPatreon),
    DonationOption(
        id = "buymeacoffee",
        title = Res.string.welcome_donate_bmac_title,
        subtitle = Res.string.welcome_donate_bmac_subtitle,
        url = "https://www.buymeacoffee.com/krtirtho",
        icon = Iconsax.BrandBuyMeACoffee),
    DonationOption(
        id = "opencollective",
        title = Res.string.welcome_donate_opencollective_title,
        subtitle = Res.string.welcome_donate_opencollective_subtitle,
        url = "https://opencollective.com/spotube",
        icon = Iconsax.BrandOpencollective))

/** Minimum time between two automatic support reminders. */
const val SupportReminderIntervalMillis: Long = 7L * 24 * 60 * 60 * 1000

/**
 * Opens the support prompt from anywhere inside the app shell. Hosted at the app root so the
 * sidebar, home top bar and the periodic reminder all drive the same dialog.
 */
val LocalSupportPrompt = staticCompositionLocalOf { {} }

/** Opens a donation destination in the user's browser. */
fun openDonation(option: DonationOption) {
    runCatching { openUrlInBrowser(option.url) }
}

/** A dialog that lets the user pick a way to support the project. */
@Composable
fun SupportDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    isReminder: Boolean = false) {
    if (!visible) return
    MillenniumDialog(visible = true, onDismissRequest = onDismissRequest) {
        MillenniumDialogHeader {
            MillenniumDialogTitle(stringResource(Res.string.support_dialog_title))
            MillenniumDialogClose()
        }
        MillenniumDialogBody {
            MillenniumDialogDescription(stringResource(Res.string.support_dialog_message))
            if (isReminder) {
                Text(
                    stringResource(Res.string.support_dialog_reminder_hint),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant)
            }
            donationOptions.forEach { option ->
                MillenniumButton(
                    onClick = {
                        openDonation(option)
                        onDismissRequest()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = MillenniumButtonStyle.Secondary) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MillenniumIcon(
                            imageVector = option.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (option.id == "patreon") {
                                AppTheme.colors.onSurface
                            } else {
                                Color.Unspecified
                            })
                        Text(stringResource(option.title))
                    }
                }
            }
        }
        MillenniumDialogFooter {
            MillenniumButton(
                onClick = onDismissRequest,
                style = MillenniumButtonStyle.Quiet) {
                Text(stringResource(Res.string.support_dialog_dismiss))
            }
        }
    }
}
