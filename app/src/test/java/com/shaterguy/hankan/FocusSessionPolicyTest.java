package com.shaterguy.hankan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FocusSessionPolicyTest {
    @Test public void normalizesKnownPresets() {
        assertEquals(10, FocusSessionPolicy.normalizePreset(10));
        assertEquals(25, FocusSessionPolicy.normalizePreset(25));
        assertEquals(50, FocusSessionPolicy.normalizePreset(50));
        assertEquals(10, FocusSessionPolicy.normalizePreset(30));
    }

    @Test public void runningTimerUsesEndEpoch() {
        assertEquals(5_000L, FocusSessionPolicy.remainingMillis(true, 10_000L, 8_000L, 5_000L));
        assertEquals(0L, FocusSessionPolicy.remainingMillis(true, 4_000L, 8_000L, 5_000L));
    }

    @Test public void pausedTimerUsesSavedRemaining() {
        assertEquals(8_000L, FocusSessionPolicy.remainingMillis(false, 0L, 8_000L, 5_000L));
    }

    @Test public void completionIsIdempotentPerSession() {
        assertTrue(FocusSessionPolicy.shouldCount(3L, 2L));
        assertFalse(FocusSessionPolicy.shouldCount(3L, 3L));
        assertFalse(FocusSessionPolicy.shouldCount(0L, 0L));
    }
}
