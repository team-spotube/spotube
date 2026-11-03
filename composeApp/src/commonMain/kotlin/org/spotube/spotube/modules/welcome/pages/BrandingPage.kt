package org.spotube.spotube.modules.welcome.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.spotube.spotube.modules.welcome.WelcomeWordmark
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.*

@Composable
fun BrandingPage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Image(painterResource(Res.drawable.spotube_logo), null, Modifier.size(88.dp))
        Spacer(Modifier.height(24.dp))
        WelcomeWordmark(fontSize = 48.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(Res.string.welcome_brand_freedom),
            style = AppTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(Res.string.welcome_brand_quiet_description),
            modifier = Modifier.widthIn(max = 440.dp),
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center)
    }
}
