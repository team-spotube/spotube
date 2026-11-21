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

package org.spotube.spotube.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.core.graphics.ColorUtils
import kotlin.math.roundToInt

/**
 * The glass Play orb, drawn as a bitmap, for the places that can't run the app's Compose button:
 * the home screen widget and the Android Auto lists. (CarPlay draws its own copy in Swift.)
 */
internal object PlayOrb {
    /** The app's dark Blue palette accent, used when there is no colour to follow. */
    const val DEFAULT_ACCENT = 0xFF1976B9.toInt()
    private const val DEFAULT_ORB_TOP = 0xFF1976B9.toInt() // dark Blue palette accentTop
    private const val DEFAULT_ORB_BOTTOM = 0xFF002D74.toInt() // dark Blue palette accentBottom
    private const val ORB_STRENGTH = 0.48f // MillenniumIconButton's default orbStrength
    private const val ORB_BORDER_FRACTION = 1f / 46f // its 1dp border on the 46dp button
    private const val MIN_ACCENT_SATURATION = 0.18f
    const val DEFAULT_SIZE_PX = 192

    /** The orb with a white play glyph on it, as the player draws its Play button. */
    fun withPlayGlyph(accent: Int = DEFAULT_ACCENT, size: Int = DEFAULT_SIZE_PX): Bitmap {
        val out = draw(accent, size)
        // ic_widget_play's glyph, at the player's 30dp-in-46dp proportion.
        val scale = size * (30f / 46f) / 24f
        val offset = (size - 24f * scale) / 2f
        val glyph = Path().apply {
            moveTo(8f, 5f)
            rLineTo(0f, 14f)
            rLineTo(11f, -7f)
            close()
        }
        val canvas = Canvas(out)
        canvas.translate(offset, offset)
        canvas.scale(scale, scale)
        canvas.drawPath(glyph, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        return out
    }

    /**
     * The Play/Pause orb: a port of the app's own Primary icon button (Millennium's
     * `MillenniumIconButton`, `millenniumIconOrb` and `millenniumIconOrbStops`), so the widget's
     * button is the player's button. RemoteViews and the car screens can't draw Compose or tint a
     * gradient per instance, so it is drawn here, once per accent, at the library's default strength.
     *
     * The colours come from an accent (a cover's) instead of the palette: [pair] gives the bright and deep
     * accent for the cover's hue, which for the default blue is exactly the dark theme's accent.
     */
    fun draw(accent: Int, size: Int = DEFAULT_SIZE_PX): Bitmap {
        val (top, bottom) = pair(accent)
        val s = size.toFloat()
        val half = s / 2f
        val strength = ORB_STRENGTH
        fun alpha(color: Int, factor: Float) = ColorUtils.setAlphaComponent(color, (255 * factor).roundToInt())

        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Body: dark jewel glass, brightest only at the lower lip (millenniumIconOrbStops).
        val body = ColorUtils.blendARGB(bottom, Color.BLACK, 0.52f)
        paint.shader = LinearGradient(
            0f, 0f, 0f, s,
            intArrayOf(ColorUtils.blendARGB(bottom, body, 0.65f), body, body, ColorUtils.blendARGB(body, top, 0.40f), top),
            floatArrayOf(0f, 0.28f, 0.60f, 0.82f, 1f),
            Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, s, s, paint)

        // A light pool under the sphere.
        val glow = if (Color.blue(top) > Color.red(top) && Color.blue(top) > Color.green(top)) {
            ColorUtils.blendARGB(top, 0xFF65E8FF.toInt(), 0.65f)
        } else {
            ColorUtils.blendARGB(top, Color.WHITE, 0.25f)
        }
        paint.shader = RadialGradient(
            half, s * 1.12f, s * 0.66f,
            intArrayOf(alpha(glow, strength * 0.95f), alpha(glow, strength * 0.65f), alpha(glow, strength * 0.10f), Color.TRANSPARENT),
            floatArrayOf(0f, 0.40f, 0.78f, 1f),
            Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, s, s, paint)

        // The curved glass dome on the upper half.
        val dome = Path().apply {
            moveTo(s * 0.08f, s * 0.37f)
            cubicTo(s * 0.12f, -s * 0.04f, s * 0.88f, -s * 0.04f, s * 0.92f, s * 0.37f)
            cubicTo(s * 0.94f, s * 0.49f, s * 0.81f, s * 0.39f, s * 0.70f, s * 0.38f)
            cubicTo(s * 0.48f, s * 0.32f, s * 0.28f, s * 0.43f, s * 0.12f, s * 0.44f)
            cubicTo(s * 0.08f, s * 0.45f, s * 0.07f, s * 0.41f, s * 0.08f, s * 0.37f)
            close()
        }
        paint.shader = RadialGradient(
            s * 0.46f, s * 0.04f, s * 0.63f,
            intArrayOf(alpha(Color.WHITE, strength), alpha(Color.WHITE, strength * 0.42f), alpha(Color.WHITE, strength * 0.03f)),
            floatArrayOf(0f, 0.50f, 1f),
            Shader.TileMode.CLAMP)
        canvas.drawPath(dome, paint)

        // Metallic rim, then the thin inner rim lit from above and below.
        val rim = maxOf(s * ORB_BORDER_FRACTION, s * 0.025f)
        paint.style = Paint.Style.STROKE
        paint.shader = LinearGradient(
            0f, 0f, s * 0.65f, s,
            intArrayOf(0xFF26364F.toInt(), 0xFF66748A.toInt(), 0xFFC6D4DA.toInt(), 0xFF89949D.toInt()),
            floatArrayOf(0f, 0.28f, 0.62f, 1f),
            Shader.TileMode.CLAMP)
        paint.strokeWidth = rim * 2f
        canvas.drawCircle(half, half, half, paint)

        val inset = rim * 1.7f
        paint.shader = LinearGradient(
            0f, inset, 0f, s - inset,
            intArrayOf(alpha(Color.WHITE, strength * 0.70f), Color.TRANSPARENT, Color.TRANSPARENT, alpha(glow, strength * 0.75f)),
            floatArrayOf(0f, 0.35f, 0.75f, 1f),
            Shader.TileMode.CLAMP)
        paint.strokeWidth = s * 0.018f
        canvas.drawCircle(half, half, half - inset, paint)

        // Everything above is square; clear what lies outside the circle, with an anti-aliased edge.
        // (A DST_IN circle would not do: it only touches the area the circle covers.)
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        canvas.drawPath(
            Path().apply {
                addCircle(half, half, half, Path.Direction.CW)
                fillType = Path.FillType.INVERSE_WINDING
            },
            paint)
        return out
    }

    /**
     * The bright and deep accent for a cover's hue. The pair keeps the proportions of the app's dark
     * blue accent (#1976B9 over #002D74). No cover, or one with no colour of its own (grey, near
     * white), gets that accent exactly; a blue cover gets a close relative of it.
     */
    fun pair(accent: Int): Pair<Int, Int> {
        val hsv = FloatArray(3)
        Color.colorToHSV(accent, hsv)
        // No colour to follow, or the default itself: the app's own pair, not one derived from a hue.
        if (accent == DEFAULT_ACCENT || hsv[1] < MIN_ACCENT_SATURATION) return DEFAULT_ORB_TOP to DEFAULT_ORB_BOTTOM
        val hue = hsv[0]
        return Color.HSVToColor(floatArrayOf(hue, 0.86f, 0.73f)) to Color.HSVToColor(floatArrayOf(hue, 1f, 0.45f))
    }
}
