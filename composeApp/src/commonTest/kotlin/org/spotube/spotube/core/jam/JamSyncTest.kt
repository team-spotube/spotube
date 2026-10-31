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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JamSyncTest {

    // --- JamClock ---

    @Test
    fun clockIsUnknownBeforeTheFirstSample() {
        val clock = JamClock()
        assertNull(clock.offsetMs)
        assertEquals(1_000L, clock.serverTime(1_000L))
    }

    @Test
    fun clockAssumesSymmetricLatency() {
        val clock = JamClock()
        // Server is 5 s ahead; 40 ms each way.
        clock.addSample(clientSentMs = 1_000, serverMs = 6_040, clientReceivedMs = 1_080)
        assertEquals(5_000L, clock.offsetMs)
        assertEquals(80L, clock.rttMs)
    }

    @Test
    fun clockTrustsTheFastestRoundTrip() {
        val clock = JamClock()
        clock.addSample(clientSentMs = 0, serverMs = 5_030, clientReceivedMs = 60) // offset 5000
        // A slow sample with a lopsided path skews the estimate; it must not win.
        clock.addSample(clientSentMs = 1_000, serverMs = 6_400, clientReceivedMs = 1_500) // offset 5150
        assertEquals(5_000L, clock.offsetMs)
    }

    @Test
    fun clockForgetsSamplesOutsideTheWindow() {
        val clock = JamClock(window = 2)
        clock.addSample(0, 5_010, 20) // fast, offset 5000
        clock.addSample(100, 5_250, 300) // offset 5050
        clock.addSample(400, 5_660, 600) // offset 5160 — the fast one is now gone
        assertEquals(5_050L, clock.offsetMs)
    }

    @Test
    fun clockIgnoresNegativeRoundTrips() {
        val clock = JamClock()
        clock.addSample(clientSentMs = 100, serverMs = 0, clientReceivedMs = 50)
        assertNull(clock.offsetMs)
    }

    // --- JamAnchor ---

    @Test
    fun anchorExtrapolatesWhileAdvancing() {
        val anchor = JamAnchor("t", 0, positionMs = 10_000, serverTimeMs = 50_000, playing = true, rate = 1.5f)
        assertEquals(13_000L, anchor.positionAt(52_000))
    }

    @Test
    fun anchorHoldsStillWhenNotAdvancing() {
        val paused = JamAnchor("t", 0, positionMs = 10_000, serverTimeMs = 50_000, playing = false)
        assertEquals(10_000L, paused.positionAt(60_000))
        val buffering = JamAnchor("t", 0, 10_000, 50_000, playing = true, advancing = false)
        assertEquals(10_000L, buffering.positionAt(60_000))
    }

    // --- JamDriftCorrector ---

    @Test
    fun smallDriftIsLeftAlone() {
        val corrector = JamDriftCorrector()
        assertEquals(JamCorrection.None, corrector.correct(10_080, 10_000, 1f, seekLeadMs = 300))
    }

    @Test
    fun behindSpeedsUpAndAheadSlowsDown() {
        val behind = JamDriftCorrector().correct(10_600, 10_000, 1f, seekLeadMs = 300)
        assertIs<JamCorrection.Nudge>(behind)
        assertTrue(behind.rate > 1f && behind.rate <= 1.05f)

        val ahead = JamDriftCorrector().correct(10_000, 10_600, 1f, seekLeadMs = 300)
        assertIs<JamCorrection.Nudge>(ahead)
        assertTrue(ahead.rate < 1f && ahead.rate >= 0.95f)
    }

    @Test
    fun nudgeFollowsTheRoomRate() {
        val nudge = JamDriftCorrector().correct(10_300, 10_000, 1.25f, seekLeadMs = 0)
        assertIs<JamCorrection.Nudge>(nudge)
        assertEquals(1.25f * 1.05f, nudge.rate, 0.001f)
    }

    @Test
    fun largeDriftSeeksAheadByTheLead() {
        val seek = JamDriftCorrector().correct(20_000, 10_000, 1f, seekLeadMs = 400)
        assertEquals(JamCorrection.Seek(20_400), seek)
    }

    @Test
    fun aSingleNoisyReadingDoesNotSeek() {
        val corrector = JamDriftCorrector(seekThresholdMs = 1_500)
        repeat(4) { corrector.correct(10_000, 10_000, 1f, 0) }
        // One coarse report 2 s off (under the immediate-seek bar) is outvoted by the median.
        assertEquals(JamCorrection.None, corrector.correct(12_000, 10_000, 1f, 0))
    }

    @Test
    fun persistentDriftSeeksOnceTheMedianAgrees() {
        val corrector = JamDriftCorrector(seekThresholdMs = 1_500)
        val results = List(3) { corrector.correct(12_000, 10_000, 1f, 0) }
        assertIs<JamCorrection.Nudge>(results[0])
        assertIs<JamCorrection.Nudge>(results[1])
        assertEquals(JamCorrection.Seek(12_000), results[2])
    }
}
