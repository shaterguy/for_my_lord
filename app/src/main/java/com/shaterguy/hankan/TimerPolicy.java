package com.shaterguy.hankan;

public final class TimerPolicy {
    private TimerPolicy() {}

    public static long durationForStart(long remainingMs, long fullDurationMs) {
        return remainingMs <= 0 ? fullDurationMs : remainingMs;
    }
}
