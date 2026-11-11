package org.spotube.millennium.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

/**
 * Shared light-theme frame used by the component previews: installs [MillenniumTheme] (light), paints
 * [MillenniumColors.background], pads by 24dp and caps the content column at 480dp.
 *
 * ```kotlin
 * @Preview
 * @Composable
 * private fun ButtonPreview() = MillenniumPreview {
 *     MillenniumButton(onClick = { }) { Text("Save") }
 * }
 * ```
 *
 * @param content Preview content, laid out in a [Column].
 */
@Composable
fun MillenniumPreview(content: @Composable () -> Unit) {
    MillenniumTheme(dark = false) {
        Box(
            Modifier.fillMaxWidth()
                .background(MillenniumTheme.colors.background)
                .padding(24.dp),
        ) {
            Column(Modifier.widthIn(max = 480.dp)) { content() }
        }
    }
}

/** Sample preview of [MillenniumPreview] with heading and muted caption text. */
@Preview
@Composable
private fun MillenniumDefaultPreview() {
    MillenniumPreview {
        org.spotube.millennium.composables.MillenniumText(
            "Millennium preview",
            size = 22.sp,
            weight = FontWeight.SemiBold,
        )
        org.spotube.millennium.composables.MillenniumText(
            "Each control owns its palette and styling.",
            color = MillenniumTheme.colors.muted,
        )
    }
}
