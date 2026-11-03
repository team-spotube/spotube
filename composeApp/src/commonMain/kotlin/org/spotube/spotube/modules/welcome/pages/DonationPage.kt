package org.spotube.spotube.modules.welcome.pages


import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumIcon
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.spotube.spotube.modules.support.DonationOption
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DonationPage(
    donations: List<DonationOption>,
    onDonate: (String) -> Unit,
    modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(
            stringResource(Res.string.welcome_donate_quiet_title),
            style = AppTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(Res.string.welcome_donate_letter),
            modifier = Modifier.widthIn(max = 540.dp),
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            donations.forEach { option ->
                MillenniumButton(
                    onClick = { onDonate(option.url) },
                    style = MillenniumButtonStyle.Secondary) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MillenniumIcon(
                            option.icon, null, Modifier.size(24.dp),
                            tint = if (option.id == "patreon") AppTheme.colors.onSurface else Color.Unspecified)
                        Text(stringResource(option.title))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(Res.string.welcome_donate_optional),
            modifier = Modifier.widthIn(max = 500.dp),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center)
    }
}
