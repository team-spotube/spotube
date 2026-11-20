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
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import android.os.SystemClock
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
    private const val HOLE_FRACTION = 0.14f // of the disc radius, a CD's 15mm hole in its 120mm
    private const val HUB_FRACTION = 0.32f // the clear plastic ring round the hole
    private const val DISC_DEGREES_PER_SECOND = 36f // one turn in 10 seconds, as res/anim/widget_disc_spin
    private const val GLASS_ALPHA = 184 // 72%, matches android:alpha on widget_glass
    private const val GLASS_TEXT = 0xFFF2F6FC.toInt() // the artist line, the dimmest text on the card
    private const val MIN_TEXT_CONTRAST = 4.5

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

    // The launcher turns the disc, so the app only knows where it is by the clock: [discAngle] is where it
    // was when the current spin began ([spinStartedAt], 0 while it isn't turning).
    private var discAngle = 0f
    private var spinStartedAt = 0L

    // The parts of the disc that don't turn: rim, grooves, hub, glints and the see-through hole.
    private val discOverlay: Bitmap by lazy { drawDiscOverlay() }
    private var appLogo: Bitmap? = null

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
        spinStartedAt = 0L
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

    /**
     * Where the disc is now, and starts counting again from there. Widgets can't animate, so the
     * launcher turns the disc with a looping animation (res/anim/widget_disc_spin) that restarts from
     * zero every time the flipper is told to show it. Giving the view this angle as its own rotation
     * first makes the restart seamless, and it is also where a paused disc stands still.
     */
    private fun rebaseDisc(playing: Boolean): Float {
        val now = SystemClock.elapsedRealtime()
        if (spinStartedAt != 0L) {
            discAngle = (discAngle + (now - spinStartedAt) * DISC_DEGREES_PER_SECOND / 1000f) % 360f
        }
        spinStartedAt = if (playing) now else 0L
        return discAngle
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
                    disc = discFrame(raw.toSquare(ARTWORK_SIZE_PX)),
                    accent = WallpaperColors.fromBitmap(raw).primaryColor.toArgb())
            }
        }.onFailure { Log.w(TAG, "Failed to load artwork", it) }.getOrNull()
    }

    /** Centre-crops to a square and scales to [sidePx], so the disc's shader maps one to one. */
    private fun Bitmap.toSquare(sidePx: Int): Bitmap {
        val side = minOf(width, height)
        val cropped = Bitmap.createBitmap(this, (width - side) / 2, (height - side) / 2, side, side)
        return Bitmap.createScaledBitmap(cropped, sidePx, sidePx, true)
    }

    /** The cover cut to a disc with the centre hole punched through. It is drawn once; turning is the view's rotation. */
    private fun discFrame(source: Bitmap): Bitmap {
        val s = source.width
        val c = s / 2f
        val out = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(c, c, c, paint)
        punchHole(canvas, c)
        return out
    }

    /** Clears the spindle hole; the card behind the widget shows through it. */
    private fun punchHole(canvas: Canvas, c: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
        canvas.drawCircle(c, c, c * HOLE_FRACTION, paint)
    }

    /**
     * The app's logo as a round badge, shown when there is no cover. It is the launcher icon's own
     * foreground (so the nightly flavor shows its own), whose 108 unit adaptive layer has a 72 unit
     * window in the middle; the layer is drawn large enough for that window to fill the badge.
     */
    private fun appLogo(context: Context): Bitmap = appLogo ?: drawAppLogo(context).also { appLogo = it }

    private fun drawAppLogo(context: Context): Bitmap {
        val s = ARTWORK_SIZE_PX
        val c = s / 2f
        val out = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(context.getColor(R.color.ic_launcher_background))
        val layer = (s * 108f / 72f).roundToInt()
        val offset = (s - layer) / 2
        context.getDrawable(R.mipmap.ic_launcher_foreground)?.apply {
            setBounds(offset, offset, offset + layer, offset + layer)
            draw(canvas)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        canvas.drawPath(
            Path().apply {
                addCircle(c, c, c, Path.Direction.CW)
                fillType = Path.FillType.INVERSE_WINDING
            },
            paint)
        paint.xfermode = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = 0x73FFFFFF
        canvas.drawCircle(c, c, c - 2f, paint)
        return out
    }

    /**
     * What sits over the turning cover and doesn't turn with it, as light doesn't on a real disc:
     * two glints, faint grooves, the clear hub ring, the silver rim and the hole's own ring.
     */
    private fun drawDiscOverlay(): Bitmap {
        val s = ARTWORK_SIZE_PX
        val c = s / 2f
        val out = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = SweepGradient(
            c, c,
            intArrayOf(0x00FFFFFF, 0x4DD2E9FF, 0x00FFFFFF, 0x00FFFFFF, 0x45FFD9F2, 0x00FFFFFF, 0x00FFFFFF),
            floatArrayOf(0f, 0.06f, 0.13f, 0.50f, 0.56f, 0.63f, 1f))
        canvas.drawCircle(c, c, c, paint)

        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = 0x1AFFFFFF
        for (fraction in floatArrayOf(0.52f, 0.62f, 0.72f, 0.82f, 0.92f)) canvas.drawCircle(c, c, c * fraction, paint)

        paint.style = Paint.Style.FILL
        paint.color = 0x14FFFFFF
        canvas.drawCircle(c, c, c * HUB_FRACTION, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = 0x4DFFFFFF
        canvas.drawCircle(c, c, c * HUB_FRACTION, paint)

        val silver = LinearGradient(
            0f, 0f, s.toFloat(), s.toFloat(), 0xE6FFFFFF.toInt(), 0x80A9B6C6.toInt(), Shader.TileMode.CLAMP)
        paint.shader = silver
        paint.strokeWidth = 5f
        canvas.drawCircle(c, c, c - 2.5f, paint)

        punchHole(canvas, c)
        paint.shader = silver
        paint.strokeWidth = 3f
        canvas.drawCircle(c, c, c * HOLE_FRACTION, paint)
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
        setImageViewBitmap(R.id.widget_play_bg, PlayOrb.draw(artwork?.accent ?: PlayOrb.DEFAULT_ACCENT))
        setInt(R.id.widget_glass, "setColorFilter", glassTint(artwork?.accent ?: PlayOrb.DEFAULT_ACCENT))
        setTextViewText(R.id.widget_title, texts.title)
        setTextViewText(R.id.widget_artist, texts.subtitle)
        val disc = artwork?.disc ?: appLogo(context) // no cover: the logo, which turns like a cover
        var angle = rebaseDisc(playing)
        if (!playing && artwork == null) {
            // A paused disc stays where it stopped; the logo goes back upright.
            discAngle = 0f
            angle = 0f
        }
        if (playing) {
            setImageViewBitmap(R.id.widget_art_spin, disc)
            setFloat(R.id.widget_art_spin, "setRotation", angle)
            setViewVisibility(R.id.widget_art, View.GONE)
            setDisplayedChild(R.id.widget_disc, 1)
        } else {
            setImageViewBitmap(R.id.widget_art, disc)
            setFloat(R.id.widget_art, "setRotation", angle)
            setViewVisibility(R.id.widget_art, View.VISIBLE)
            setDisplayedChild(R.id.widget_disc, 0)
        }
        if (artwork != null) {
            setImageViewBitmap(R.id.widget_art_overlay, discOverlay)
            setViewVisibility(R.id.widget_art_overlay, View.VISIBLE)
        } else {
            // The logo isn't a disc: no hole or grooves over it.
            setViewVisibility(R.id.widget_art_overlay, View.GONE)
        }

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

    /** The cover cut to a disc, and the colour that dominates it. */
    private class Artwork(val disc: Bitmap, val accent: Int)

    /**
     * The colour of the glass card for a cover: the cover's deep jewel tone, the same hue as the
     * Play/Pause orb. Darkened until the text keeps 4.5:1 on the lightest case, a white wallpaper
     * behind the card at its 72% opacity with the finish's light top edge, so a pale or yellow cover
     * can't wash the text out.
     */
    private fun glassTint(accent: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(PlayOrb.pair(accent).first, hsv)
        fun tint(value: Float) = Color.HSVToColor(floatArrayOf(hsv[0], hsv[1], value))
        fun textContrast(tint: Int): Double {
            val onWhite = ColorUtils.compositeColors(ColorUtils.setAlphaComponent(tint, GLASS_ALPHA), Color.WHITE)
            val lightestEdge = ColorUtils.blendARGB(onWhite, Color.WHITE, 0.12f)
            return ColorUtils.calculateContrast(GLASS_TEXT, lightestEdge)
        }
        var value = hsv[2] * 0.45f
        while (value > 0.08f && textContrast(tint(value)) < MIN_TEXT_CONTRAST) value *= 0.9f
        return tint(value)
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
