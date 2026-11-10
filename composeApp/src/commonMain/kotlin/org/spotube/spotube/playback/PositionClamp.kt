package org.spotube.spotube.playback

/**
 * Utility for clamping playback position to valid range.
 *
 * Prevents position drift in UI and MediaSession:
 * - Position > duration (overrun)
 * - Position < 0 (negative)
 * - Duration <= 0 (invalid)
 *
 * Based on DuranMusic's PositionClamp in `position_clamp.dart`.
 */
object PositionClamp {

    /**
     * Clamps position to the range [0, duration].
     * @param position Current playback position in milliseconds.
     * @param duration Track duration in milliseconds.
     * @return Clamped position in milliseconds.
     */
    fun clampPosition(position: Long, duration: Long): Long {
        return when {
            duration <= 0 -> position
            position < 0 -> 0
            position > duration -> duration
            else -> position
        }
    }

    /**
     * Clamps position as a percentage (0.0 - 1.0) relative to duration.
     * @param position Current playback position in milliseconds.
     * @param duration Track duration in milliseconds.
     * @return Clamped percentage as Float (0.0f to 1.0f).
     */
    fun clampPositionPercentage(position: Long, duration: Long): Float {
        if (duration <= 0) return 0.0f
        val clamped = clampPosition(position, duration)
        return clamped.toFloat() / duration
    }

    /**
     * Creates a safe progress value for UI progress bars.
     * Ensures progress is always between 0.0 and 1.0.
     */
    fun safeProgress(position: Long, duration: Long): Float {
        return clampPositionPercentage(position, duration).coerceIn(0.0f, 1.0f)
    }
}