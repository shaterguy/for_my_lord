package com.shaterguy.hankan;

import static org.junit.Assert.assertEquals;
import java.time.LocalDate;
import org.junit.Test;

public class StreakCalculatorTest {
    private final LocalDate today = LocalDate.of(2026, 8, 20);

    @Test public void startsAtOne() {
        assertEquals(1, StreakCalculator.nextStreak("", today, 0));
    }

    @Test public void incrementsOnNextDay() {
        assertEquals(4, StreakCalculator.nextStreak("2026-08-19", today, 3));
    }

    @Test public void doesNotDoubleCountToday() {
        assertEquals(3, StreakCalculator.nextStreak("2026-08-20", today, 3));
    }

    @Test public void resetsAfterGap() {
        assertEquals(1, StreakCalculator.nextStreak("2026-08-17", today, 9));
    }
}
