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

package org.spotube.spotube.core.ui.component

import org.spotube.spotube.core.ui.theming.engine.AppComponents
import org.spotube.spotube.core.ui.theming.engine.AppState
import org.spotube.spotube.core.ui.theming.engine.ContentSlots
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import compose.icons.FeatherIcons
import compose.icons.feathericons.Lock
import compose.icons.feathericons.LogIn
import org.spotube.spotube.core.ui.theming.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.spotube.core.ui.theming.AppIcon
import spotube.composeapp.generated.resources.*

/** Friendly "sign in first" state for screens whose metadata plugin needs an account. */
@Composable
fun LoginRequiredDisplay(
    pluginName: String?,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
    isLoggingIn: Boolean = false) {
    val look = AppComponents.slot(ContentSlots.LoginRequired)[AppState.Default]
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(look.padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(look.spacing)) {
        Box(
            modifier = Modifier
                .size(look.badgeSize)
                .clip(look.badgeShape)
                .background(look.badgeBackground),
            contentAlignment = Alignment.Center) {
            AppIcon(
                imageVector = FeatherIcons.Lock,
                contentDescription = null,
                modifier = Modifier.size(look.iconSize),
                tint = look.iconTint)
        }

        Text(
            text = stringResource(Res.string.login_required_title),
            style = look.titleStyle,
            textAlign = TextAlign.Center)

        Text(
            text = if (pluginName.isNullOrBlank()) {
                stringResource(Res.string.login_required_description)
            } else {
                stringResource(Res.string.login_required_description_plugin, pluginName)
            },
            style = look.descriptionStyle,
            textAlign = TextAlign.Center)

        MillenniumButton(
            onClick = onLogin,
            loading = isLoggingIn,
            modifier = Modifier.height(look.buttonHeight)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(
                    imageVector = FeatherIcons.LogIn,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp).size(look.buttonIconSize))
                Text(stringResource(Res.string.login_required_action))
            }
        }
    }
}
