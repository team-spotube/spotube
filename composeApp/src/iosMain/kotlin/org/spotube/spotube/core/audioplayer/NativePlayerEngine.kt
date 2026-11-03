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

package org.spotube.spotube.core.audioplayer

/**
 * Single-media playback engine implemented in Swift on top of MobileVLCKit
 * (`iosApp/iosApp/VlcPlayerEngine.swift`).
 *
 * MobileVLCKit comes from CocoaPods into the Xcode project, so Gradle can't bind it with cinterop.
 * Swift implements this contract instead and registers it in [NativePlayerRegistry] before the
 * Compose view controller is created. The playlist, loop and shuffle logic stays in Kotlin
 * ([AudioPlayer]); the engine only ever plays one URL at a time.
 *
 * Implementations may be called from any thread and must hop to the thread VLC needs
 * themselves. [NativePlayerListener] callbacks may arrive on any thread.
 */
interface NativePlayerEngine {
    fun setListener(listener: NativePlayerListener?)

    /** Replaces the current media. Starts playing right away when [autoPlay] is set. */
    fun load(url: String, autoPlay: Boolean, startPositionMs: Long)
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)

    /** [volume] is 0..1. */
    fun setVolume(volume: Float)
    fun setRate(rate: Float)

    /**
     * Releases the native player. The engine must not be used afterwards. Not named `release`:
     * that would export to Objective-C as `-release` and clash with NSObject.
     */
    fun destroy()
}

interface NativePlayerListener {
    fun onState(state: NativePlayerState)
    fun onTime(positionMs: Long)
    fun onDuration(durationMs: Long)
    fun onError(message: String)
}

enum class NativePlayerState {
    /** Nothing loaded, or playback was stopped. */
    IDLE,
    OPENING,
    BUFFERING,
    PLAYING,
    PAUSED,

    /** The media played to its end. Sent once per loaded media. */
    ENDED
}

interface NativePlayerEngineFactory {
    fun create(): NativePlayerEngine
}

/** Set from Swift (`iOSApp.init`) before the first [AudioPlayer] is created. */
object NativePlayerRegistry {
    var factory: NativePlayerEngineFactory? = null
}
