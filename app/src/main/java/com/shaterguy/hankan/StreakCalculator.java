package com.shaterguy.hankan;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class StreakCalculator {
    private StreakCalculator() {}

    public static int nextStreak(String lastCompletedDate, LocalDate today, int current) {
        if (lastCompletedDate == null || lastCompletedDate.isBlank()) return 1;
        LocalDate previous = LocalDate.parse(lastCompletedDate);
        long gap = ChronoUnit.DAYS.between(previous, today);
        if (gap == 0) return Math.max(1, current);
        if (gap == 1) return Math.max(0, current) + 1;
        return 1;
    }
}
