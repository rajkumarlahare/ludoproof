package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Test

class RealtimeReconnectBackoffTest {
    @Test
    fun centeredJitterUsesExponentialDelaysAndCapsAtSixteenSeconds() {
        val delays =
            (0..7).map { attempt ->
                RealtimeReconnectBackoff.delayMillis(
                    attempt = attempt,
                    jitterUnit = 0.5,
                )
            }

        assertEquals(
            listOf(
                1_000L,
                2_000L,
                4_000L,
                8_000L,
                16_000L,
                16_000L,
                16_000L,
                16_000L,
            ),
            delays,
        )
    }

    @Test
    fun jitterStaysWithinTwentyPercentOfBackoffBase() {
        assertEquals(
            6_400L,
            RealtimeReconnectBackoff.delayMillis(
                attempt = 3,
                jitterUnit = 0.0,
            ),
        )
        assertEquals(
            9_600L,
            RealtimeReconnectBackoff.delayMillis(
                attempt = 3,
                jitterUnit = 1.0,
            ),
        )
    }

    @Test
    fun negativeAttemptsAndOutOfRangeJitterAreClampedSafely() {
        assertEquals(
            800L,
            RealtimeReconnectBackoff.delayMillis(
                attempt = -4,
                jitterUnit = -10.0,
            ),
        )
        assertEquals(
            1_200L,
            RealtimeReconnectBackoff.delayMillis(
                attempt = 0,
                jitterUnit = 10.0,
            ),
        )
    }
}
