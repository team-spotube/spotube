package org.spotube.spotube.modules.welcome.pages

import androidx.compose.runtime.Composable
import org.spotube.spotube.modules.plugin.PluginScreen

/** The same installation, discovery, permissions and sign-in flow as Settings, in-place. */
@Composable
fun PluginsPage() {
    PluginScreen(onboarding = true)
}
