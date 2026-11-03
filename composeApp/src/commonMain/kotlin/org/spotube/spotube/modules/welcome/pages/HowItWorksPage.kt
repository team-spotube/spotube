package org.spotube.spotube.modules.welcome.pages

import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumListTile
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxDocumentDownload
import org.spotube.spotube.resources.iconsax.IconsaxMusic
import org.spotube.spotube.resources.iconsax.IconsaxSearch
import org.spotube.spotube.resources.iconsax.User
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.welcome_guide_lyrics_desc
import spotube.composeapp.generated.resources.welcome_guide_lyrics_title
import spotube.composeapp.generated.resources.welcome_guide_offline_desc
import spotube.composeapp.generated.resources.welcome_guide_offline_title
import spotube.composeapp.generated.resources.welcome_guide_search_desc
import spotube.composeapp.generated.resources.welcome_guide_search_title
import spotube.composeapp.generated.resources.welcome_guide_signin_desc
import spotube.composeapp.generated.resources.welcome_guide_signin_title
import spotube.composeapp.generated.resources.welcome_guide_subtitle
import spotube.composeapp.generated.resources.welcome_guide_title

/**
 * The onboarding guide: a short, concrete answer to "how do I actually use this app?".
 * No marketing prose — just the four things a new user needs to do.
 */
@Composable
fun HowItWorksPage(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(Res.string.welcome_guide_title),
                style = AppTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif))
            Text(
                stringResource(Res.string.welcome_guide_subtitle),
                style = AppTheme.typography.bodyLarge,
                color = AppTheme.colors.onSurfaceVariant)
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WelcomeGuideStep(
                icon = Iconsax.User,
                title = Res.string.welcome_guide_signin_title,
                description = Res.string.welcome_guide_signin_desc)
            WelcomeGuideStep(
                icon = Iconsax.IconsaxSearch,
                title = Res.string.welcome_guide_search_title,
                description = Res.string.welcome_guide_search_desc)
            WelcomeGuideStep(
                icon = Iconsax.IconsaxDocumentDownload,
                title = Res.string.welcome_guide_offline_title,
                description = Res.string.welcome_guide_offline_desc)
            WelcomeGuideStep(
                icon = Iconsax.IconsaxMusic,
                title = Res.string.welcome_guide_lyrics_title,
                description = Res.string.welcome_guide_lyrics_desc)
        }
    }
}

@Composable
private fun WelcomeGuideStep(
    icon: ImageVector,
    title: StringResource,
    description: StringResource,
) {
    MillenniumListTile(
        leading = {
            MillenniumIcon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = AppTheme.colors.primary)
        },
        title = { Text(stringResource(title)) },
        subtitle = { Text(stringResource(description)) })
}
