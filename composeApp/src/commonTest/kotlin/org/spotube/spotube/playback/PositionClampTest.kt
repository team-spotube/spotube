package org.spotube.spotube.playback

import kotlin.test.Test
import kotlin.test.assertEquals

class PositionClampTest {

    @Test
    fun `clamps position to duration when over`() {
        assertEquals(10000L, PositionClamp.clampPosition(15000, 10000))
        assertEquals(5000L, PositionClamp.clampPosition(5000, 10000))
        assertEquals(10000L, PositionClamp.clampPosition(10000, 10000))
    }

    @Test
    fun `clamps negative position to zero`() {
        assertEquals(0L, PositionClamp.clampPosition(-100, 10000))
        assertEquals(0L, PositionClamp.clampPosition(-1, 10000))
    }

    @Test
    fun `returns position unchanged when duration invalid`() {
        assertEquals(-100L, PositionClamp.clampPosition(-100, 0))
        assertEquals(5000L, PositionClamp.clampPosition(5000, 0))
        assertEquals(15000L, PositionClamp.clampPosition(15000, -1))
    }

    @Test
    fun `clamps percentage correctly`() {
        assertEquals(0.5f, PositionClamp.clampPositionPercentage(5000, 10000))
        assertEquals(1.0f, PositionClamp.clampPositionPercentage(15000, 10000)) // Clamped to 1.0
        assertEquals(0.0f, PositionClamp.clampPositionPercentage(-100, 10000)) // Clamped to 0.0
        assertEquals(0.0f, PositionClamp.clampPositionPercentage(5000, 0)) // Invalid duration
    }

    @Test
    fun `safeProgress always returns 0.0 to 1.0`() {
        assertEquals(0.0f, PositionClamp.safeProgress(-100, 10000))
        assertEquals(0.5f, PositionClamp.safeProgress(5000, 10000))
        assertEquals(1.0f, PositionClamp.safeProgress(15000, 10000))
        assertEquals(0.0f, PositionClamp.safeProgress(5000, 0))
    }
}