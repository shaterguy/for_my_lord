package com.shaterguy.hankan;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class TimerPolicyTest {
    private static final long TEN_MINUTES = 600_000L;

    @Test public void keepsPausedTime() {
        assertEquals(245_000L, TimerPolicy.durationForStart(245_000L, TEN_MINUTES));
    }

    @Test public void restartsAfterCompletion() {
        assertEquals(TEN_MINUTES, TimerPolicy.durationForStart(0L, TEN_MINUTES));
    }

    @Test public void recoversInvalidNegativeTime() {
        assertEquals(TEN_MINUTES, TimerPolicy.durationForStart(-1L, TEN_MINUTES));
    }
}
