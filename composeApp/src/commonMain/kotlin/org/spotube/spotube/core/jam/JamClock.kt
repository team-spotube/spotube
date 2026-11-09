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

package org.spotube.spotube.core.jam

/**
 * Estimates the offset between this device's clock and the room server's (NTP-style).
 *
 * Each ping is sent at local time `c` and answered with server time `s`; it arrives back
 * at local time `r`. Assuming symmetric latency, the server read its clock halfway
 * through, so `offset = s - (c + r) / 2`. Samples with the lowest round trip have the
 * least room for asymmetry, so the estimate uses the fastest of the recent samples.
 * Samples older than [maxAgeMs] are dropped, so the estimate follows slow clock drift.
 */
class JamClock(private val window: Int = 16, private val maxAgeMs: Long = 15 * 60_000L) {
    private data class Sample(val rttMs: Long, val offsetMs: Long, val atMs: Long)

    private val samples = ArrayDeque<Sample>()

    /** Local-to-server offset in ms, or null before the first sample. */
    var offsetMs: Long? = null
        private set

    /** Round trip of the sample the offset came from. */
    var rttMs: Long? = null
        private set

    fun addSample(clientSentMs: Long, serverMs: Long, clientReceivedMs: Long) {
        val rtt = clientReceivedMs - clientSentMs
        if (rtt < 0) return
        samples.addLast(Sample(rtt, serverMs - (clientSentMs + clientReceivedMs) / 2, clientReceivedMs))
        while (samples.size > window || samples.first().atMs < clientReceivedMs - maxAgeMs) samples.removeFirst()
        val best = samples.minBy { it.rttMs }
        offsetMs = best.offsetMs
        rttMs = best.rttMs
    }

    fun serverTime(localMs: Long): Long = localMs + (offsetMs ?: 0L)

    fun reset() {
        samples.clear()
        offsetMs = null
        rttMs = null
    }
}

/** What a guest does about the gap between where the room is and where it is. */
sealed class JamCorrection {
    /** In sync: play at the room's rate. */
    data object None : JamCorrection()

    /** Slightly off: speed up or slow down until caught up (inaudible, no gap). */
    data class Nudge(val rate: Float) : JamCorrection()

    /** Too far off to nudge: jump to [positionMs]. */
    data class Seek(val positionMs: Long) : JamCorrection()
}

/**
 * Turns drift into a [JamCorrection]. Players report their position coarsely (VLC in
 * ~250 ms steps), so decisions use the median of the last few drift readings, and
 * small drift inside [deadBandMs] is left alone.
 */
class JamDriftCorrector(
    private val deadBandMs: Long = 120,
    private val seekThresholdMs: Long = 1_500,
    /** Drift is closed over roughly this long while nudging. */
    private val catchUpMs: Long = 3_000,
    private val maxNudge: Float = 0.05f,
    private val window: Int = 5,
    /** Readings needed before a seek, so one noisy report right after a change can't cause one. */
    private val minReadingsToSeek: Int = 3) {
    private val drifts = ArrayDeque<Long>()

    /**
     * [expectedMs] is the room's position, [actualMs] ours, [roomRate] the host's rate.
     * [seekLeadMs] is how long a seek takes to resume here; a seek aims that far ahead.
     */
    fun correct(expectedMs: Long, actualMs: Long, roomRate: Float, seekLeadMs: Long): JamCorrection {
        val drift = expectedMs - actualMs
        // A huge jump is acted on at once; waiting for the median would replay it for seconds.
        if (kotlin.math.abs(drift) > seekThresholdMs * 2) {
            reset()
            return JamCorrection.Seek(expectedMs + seekLeadMs)
        }
        drifts.addLast(drift)
        while (drifts.size > window) drifts.removeFirst()
        val smoothed = drifts.sorted()[drifts.size / 2]

        return when {
            kotlin.math.abs(smoothed) > seekThresholdMs && drifts.size >= minReadingsToSeek -> {
                reset()
                JamCorrection.Seek(expectedMs + seekLeadMs)
            }
            kotlin.math.abs(smoothed) <= deadBandMs -> JamCorrection.None
            else -> {
                val nudge = (smoothed.toFloat() / catchUpMs).coerceIn(-maxNudge, maxNudge)
                JamCorrection.Nudge(roomRate * (1f + nudge))
            }
        }
    }

    fun reset() = drifts.clear()
}
