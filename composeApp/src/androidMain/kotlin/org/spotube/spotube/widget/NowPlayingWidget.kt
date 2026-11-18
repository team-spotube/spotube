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

import android.app.PendingIntent
import android.app.WallpaperColors
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.spotube.spotube.MainActivity
import org.spotube.spotube.R
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.i18n.getString
import org.spotube.spotube.core.remote.ConnectionState
import org.spotube.spotube.core.remote.RemoteControlClient
import org.spotube.spotube.core.remote.RemoteControlCommand
import org.spotube.spotube.media.PlaybackService
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.widget_next
import spotube.composeapp.generated.resources.widget_nothing_playing
import spotube.composeapp.generated.resources.widget_open_to_play
import spotube.composeapp.generated.resources.widget_pause
import spotube.composeapp.generated.resources.widget_play
import spotube.composeapp.generated.resources.widget_previous
import spotube.composeapp.generated.resources.widget_remote_caption

/**
 * Drives the home screen "Now Playing" widget: renders it from the player's state and runs its
 * button actions. The player is only observed while a widget is placed, so users without one pay
 * nothing, and it re-renders on track or play/pause changes only (never the playback position),
 * which keeps the widget from waking the device.
 *
 * While this device is connected to another Spotube device (the remote control feature) the widget
 * shows what that device is playing and its buttons control that device instead.
 */
object NowPlayingWidget : KoinComponent {
    private const val TAG = "NowPlayingWidget"
    private const val ARTWORK_SIZE_PX = 256
    private const val ARTWORK_CORNER_RATIO = 0.18f // 18dp on the 100dp cover
    private const val DEFAULT_ACCENT = 0xFF1976B9.toInt()
    private const val DEFAULT_ORB_TOP = 0xFF1976B9.toInt() // dark Blue palette accentTop
    private const val DEFAULT_ORB_BOTTOM = 0xFF002D74.toInt() // dark Blue palette accentBottom
    private const val ORB_STRENGTH = 0.48f // MillenniumIconButton's default orbStrength
    private const val ORB_BORDER_FRACTION = 1f / 46f // its 1dp border on the 46dp button
    private const val MIN_ACCENT_SATURATION = 0.18f
    private const val BUTTON_BITMAP_PX = 192

    const val ACTION_PAUSE = "org.spotube.spotube.widget.PAUSE"
    const val ACTION_NEXT = "org.spotube.spotube.widget.NEXT"
    const val ACTION_PREVIOUS = "org.spotube.spotube.widget.PREVIOUS"
    const val ACTION_REMOTE_TOGGLE = "org.spotube.spotube.widget.REMOTE_TOGGLE"
    const val ACTION_REMOTE_NEXT = "org.spotube.spotube.widget.REMOTE_NEXT"
    const val ACTION_REMOTE_PREVIOUS = "org.spotube.spotube.widget.REMOTE_PREVIOUS"

    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    // Skips re-fetching the cover when only play/pause changed.
    private var cachedArtwork: Pair<Any, Artwork>? = null

    /** Starts observing the player when the user has at least one widget placed. */
    fun startIfPlaced(context: Context) {
        if (widgetIds(context).isNotEmpty()) start(context)
    }

    fun start(context: Context) {
        if (job?.isActive == true) return
        val app = context.applicationContext
        val player = get<AudioPlayerInterface>()
        get<AudioPlayerQueue>() // creating it restores the last queue, so Play works from a cold start
        val remote = get<RemoteControlClient>()
        job = scope.launch {
            combine(
                player.currentMediaItemFlow,
                player.playerStateFlow,
                remote.connectionState,
                remote.latestPlayerState,
            ) { item, state, connection, remoteState ->
                if (connection is ConnectionState.Connected) {
                    WidgetState(
                        title = remoteState?.currentTrackTitle,
                        artist = remoteState?.currentTrackArtists,
                        artwork = remoteState?.currentTrackCoverUrl?.takeIf { it.isNotBlank() },
                        playing = remoteState?.isPlaying == true,
                        remoteDevice = connection.deviceName.ifBlank { connection.host })
                } else {
                    WidgetState(
                        title = item?.title,
                        artist = item?.artist,
                        artwork = item?.coverBytes?.takeIf { it.isNotEmpty() }
                            ?: item?.coverURL?.takeIf { it.isNotBlank() },
                        playing = state == PlayerState.PLAYING,
                        remoteDevice = null)
                }
            }.distinctUntilChanged().collectLatest { widgetState ->
                try {
                    render(app, widgetState)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Render failed", e)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        cachedArtwork = null
    }

    /** Runs Pause / Next / Previous on this device's player directly. See [mediaButton] for Play. */
    suspend fun handleLocal(action: String) {
        val player = get<AudioPlayerInterface>()
        when (action) {
            ACTION_PAUSE -> player.pause()
            ACTION_NEXT -> player.skipToNext()
            ACTION_PREVIOUS -> player.skipToPrevious()
        }
    }

    /** Sends a button press to the connected device; a no-op if the connection is gone. */
    suspend fun sendRemote(command: RemoteControlCommand) {
        val client = get<RemoteControlClient>()
        if (client.connectionState.value is ConnectionState.Connected) client.sendCommand(command)
    }

    private fun widgetIds(context: Context): IntArray =
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, NowPlayingWidgetProvider::class.java))

    private suspend fun render(context: Context, state: WidgetState) {
        val ids = widgetIds(context)
        if (ids.isEmpty()) {
            stop()
            return
        }
        val manager = AppWidgetManager.getInstance(context)
        val cached: Artwork? = cachedArtwork?.takeIf { it.first == state.artwork }?.second

        val texts = WidgetTexts(
            title = state.title ?: getString(Res.string.widget_nothing_playing),
            subtitle = state.artist ?: getString(Res.string.widget_open_to_play),
            caption = state.remoteDevice?.let { getString(Res.string.widget_remote_caption, it) },
            play = getString(Res.string.widget_play),
            pause = getString(Res.string.widget_pause),
            next = getString(Res.string.widget_next),
            previous = getString(Res.string.widget_previous),
        )
        manager.updateAppWidget(ids, buildViews(context, texts, state, cached))

        if (state.artwork != null && cached == null) {
            val loaded = loadArtwork(context, state.artwork) ?: return
            cachedArtwork = state.artwork to loaded
            manager.updateAppWidget(ids, buildViews(context, texts, state, loaded))
        }
    }

    private suspend fun loadArtwork(context: Context, model: Any): Artwork? = withContext(Dispatchers.IO) {
        runCatching {
            val request = ImageRequest.Builder(context)
                .data(model)
                .size(Size(ARTWORK_SIZE_PX, ARTWORK_SIZE_PX))
                .allowHardware(false) // RemoteViews can't carry hardware bitmaps
                .build()
            SingletonImageLoader.get(context).execute(request).image?.toBitmap()?.let { raw ->
                Artwork(
                    cover = raw.toRoundedSquare(),
                    accent = WallpaperColors.fromBitmap(raw).primaryColor.toArgb())
            }
        }.onFailure { Log.w(TAG, "Failed to load artwork", it) }.getOrNull()
    }

    /**
     * RemoteViews can't clip an image to rounded corners, so the cover is cropped to a square and
     * rounded here, with the same corner ratio as the placeholder and the frame drawable.
     */
    private fun Bitmap.toRoundedSquare(): Bitmap {
        val side = minOf(width, height)
        val dx = (width - side) / 2f
        val dy = (height - side) / 2f
        val shader = BitmapShader(this, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(Matrix().apply { postTranslate(-dx, -dy) })
        }
        val out = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val radius = side * ARTWORK_CORNER_RATIO
        Canvas(out).drawRoundRect(
            RectF(0f, 0f, side.toFloat(), side.toFloat()),
            radius,
            radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader })
        return out
    }

    private fun buildViews(
        context: Context,
        texts: WidgetTexts,
        state: WidgetState,
        artwork: Artwork?,
    ): RemoteViews = RemoteViews(context.packageName, R.layout.widget_now_playing).apply {
        val playing = state.playing
        val remote = state.remoteDevice != null
        setViewVisibility(R.id.widget_caption, if (remote) View.VISIBLE else View.GONE)
        if (texts.caption != null) setTextViewText(R.id.widget_caption, texts.caption)
        setImageViewBitmap(R.id.widget_play_bg, accentButton(artwork?.accent ?: DEFAULT_ACCENT))
        setTextViewText(R.id.widget_title, texts.title)
        setTextViewText(R.id.widget_artist, texts.subtitle)
        if (artwork != null) setImageViewBitmap(R.id.widget_art, artwork.cover)
        else setImageViewResource(R.id.widget_art, R.drawable.widget_art_placeholder)

        setImageViewResource(
            R.id.widget_play_pause,
            if (playing) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)
        setContentDescription(R.id.widget_play_pause, if (playing) texts.pause else texts.play)
        setContentDescription(R.id.widget_next, texts.next)
        setContentDescription(R.id.widget_previous, texts.previous)

        setOnClickPendingIntent(
            R.id.widget_play_pause,
            when {
                remote -> broadcast(context, ACTION_REMOTE_TOGGLE, 1)
                playing -> broadcast(context, ACTION_PAUSE, 4)
                state.title != null -> mediaButton(context, KeyEvent.KEYCODE_MEDIA_PLAY)
                else -> openApp(context) // nothing loaded: playing would not start the service
            })
        setOnClickPendingIntent(
            R.id.widget_next, broadcast(context, if (remote) ACTION_REMOTE_NEXT else ACTION_NEXT, 2))
        setOnClickPendingIntent(
            R.id.widget_previous,
            broadcast(context, if (remote) ACTION_REMOTE_PREVIOUS else ACTION_PREVIOUS, 3))
        setOnClickPendingIntent(R.id.widget_root, openApp(context))
    }

    private fun openApp(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    /**
     * Play is a media-button event for [PlaybackService]: the system refuses a plain background
     * service start, so with the app not running playback would start without its foreground
     * service. Media3 handles the event and promotes the service itself once playback begins.
     *
     * Only Play may go this way. A foreground-service start obliges the service to call
     * startForeground within seconds, which Media3 does only when playing; pausing or skipping
     * through it crashes the app about ten seconds later. So those never start the service.
     */
    private fun mediaButton(context: Context, keyCode: Int): PendingIntent =
        PendingIntent.getForegroundService(
            context,
            keyCode,
            Intent(Intent.ACTION_MEDIA_BUTTON)
                .setComponent(ComponentName(context, PlaybackService::class.java))
                .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    /** Handled by the non-exported [NowPlayingWidgetActionReceiver], which runs in this process. */
    private fun broadcast(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, NowPlayingWidgetActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    /** The cover as drawn in the widget, and the colour that dominates it. */
    private class Artwork(val cover: Bitmap, val accent: Int)

    /**
     * The Play/Pause orb: a port of the app's own Primary icon button (Millennium's
     * `MillenniumIconButton`, `millenniumIconOrb` and `millenniumIconOrbStops`), so the widget's
     * button is the player's button. RemoteViews can't draw Compose or tint a gradient per
     * instance, so it is drawn here, once per cover, at the library's default strength.
     *
     * The colours come from the cover instead of the palette: [orbPair] gives the bright and deep
     * accent for the cover's hue, which for the default blue is exactly the dark theme's accent.
     */
    private fun accentButton(accent: Int): Bitmap {
        val (top, bottom) = orbPair(accent)
        val size = BUTTON_BITMAP_PX
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
     * blue accent (#1976B9 over #002D74), so a blue cover gives exactly the player's orb. A cover
     * with no colour of its own (grey, near white) keeps the app's blue.
     */
    private fun orbPair(accent: Int): Pair<Int, Int> {
        val hsv = FloatArray(3)
        Color.colorToHSV(accent, hsv)
        if (hsv[1] < MIN_ACCENT_SATURATION) return DEFAULT_ORB_TOP to DEFAULT_ORB_BOTTOM
        val hue = hsv[0]
        return Color.HSVToColor(floatArrayOf(hue, 0.86f, 0.73f)) to Color.HSVToColor(floatArrayOf(hue, 1f, 0.45f))
    }

    private data class WidgetState(
        val title: String?,
        val artist: String?,
        val artwork: Any?,
        val playing: Boolean,
        val remoteDevice: String?,
    )

    private data class WidgetTexts(
        val title: String,
        val subtitle: String,
        val caption: String?,
        val play: String,
        val pause: String,
        val next: String,
        val previous: String,
    )
}
