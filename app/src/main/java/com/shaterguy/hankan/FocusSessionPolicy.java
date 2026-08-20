package com.shaterguy.hankan;

public final class FocusSessionPolicy {
    private static final int[] PRESETS = {10, 25, 50};

    private FocusSessionPolicy() {}

    public static int normalizePreset(int minutes) {
        for (int preset : PRESETS) {
            if (preset == minutes) return preset;
        }
        return 10;
    }

    public static long remainingMillis(boolean running, long endEpoch, long savedRemaining, long now) {
        if (!running) return Math.max(0L, savedRemaining);
        return Math.max(0L, endEpoch - now);
    }

    public static boolean shouldCount(long sessionId, long lastCountedSessionId) {
        return sessionId > 0L && sessionId != lastCountedSessionId;
    }
}
