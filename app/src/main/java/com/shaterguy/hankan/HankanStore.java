package com.shaterguy.hankan;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class HankanStore {
    private static final String PREFS = "hankan_prefs";
    private static final String KNOWN_DATES = "known_dates";
    private static final String MIGRATED_110 = "migrated_110";

    private final SharedPreferences prefs;

    public HankanStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        migrateLegacy();
        resolveExpiredFocus(System.currentTimeMillis());
    }

    public String today() {
        return LocalDate.now().toString();
    }

    public String getTask(String date) {
        return prefs.getString(key("task", date), "");
    }

    public void setTask(String date, String task) {
        String value = task == null ? "" : task.trim();
        prefs.edit().putString(key("task", date), value).apply();
        touchDate(date);
    }

    public boolean isCompleted(String date) {
        return prefs.getBoolean(key("completed", date), false);
    }

    public boolean completeToday() {
        String date = today();
        String task = getTask(date).trim();
        if (task.isEmpty() || isCompleted(date)) return false;

        int currentStreak = prefs.getInt("streak", 0);
        int currentTotal = prefs.getInt("total", 0);
        String previousLastDate = prefs.getString("last_date", "");
        int nextStreak = StreakCalculator.nextStreak(previousLastDate, LocalDate.parse(date), currentStreak);

        prefs.edit()
            .putBoolean(key("completed", date), true)
            .putInt(key("pre_streak", date), currentStreak)
            .putString(key("pre_last_date", date), previousLastDate)
            .putInt("streak", nextStreak)
            .putInt("total", currentTotal + 1)
            .putString("last_date", date)
            .putString("last_task", task)
            .apply();
        touchDate(date);
        return true;
    }

    public boolean undoTodayCompletion() {
        String date = today();
        if (!isCompleted(date)) return false;

        int restoredStreak = prefs.getInt(key("pre_streak", date), Math.max(0, prefs.getInt("streak", 0) - 1));
        String restoredLastDate = prefs.getString(key("pre_last_date", date), "");
        int restoredTotal = Math.max(0, prefs.getInt("total", 0) - 1);

        prefs.edit()
            .putBoolean(key("completed", date), false)
            .putInt("streak", restoredStreak)
            .putInt("total", restoredTotal)
            .putString("last_date", restoredLastDate)
            .apply();
        return true;
    }

    public int getStreak() {
        return prefs.getInt("streak", 0);
    }

    public int getTotalCompleted() {
        return prefs.getInt("total", 0);
    }

    public String getNote(String date) {
        return prefs.getString(key("note", date), "");
    }

    public void setNote(String date, String note) {
        prefs.edit().putString(key("note", date), note == null ? "" : note).apply();
        touchDate(date);
    }

    public int getFocusCount(String date) {
        return prefs.getInt(key("focus_count", date), 0);
    }

    public int getFocusMinutes(String date) {
        return prefs.getInt(key("focus_minutes", date), 0);
    }

    public int getTotalFocusMinutes() {
        int total = 0;
        for (String date : getKnownDatesSorted()) {
            total += getFocusMinutes(date);
        }
        return total;
    }

    public int getSelectedFocusMinutes() {
        return prefs.getInt("focus_selected_minutes", 10);
    }

    public long getFocusDurationMs() {
        return getSelectedFocusMinutes() * 60_000L;
    }

    public long getFocusRemainingMs(long now) {
        boolean running = isFocusRunning();
        long end = prefs.getLong("focus_end_epoch", 0L);
        long saved = prefs.getLong("focus_remaining_ms", getFocusDurationMs());
        return FocusSessionPolicy.remainingMillis(running, end, saved, now);
    }

    public boolean isFocusRunning() {
        return prefs.getBoolean("focus_running", false);
    }

    public boolean isFocusFinished() {
        return prefs.getBoolean("focus_finished", false);
    }

    public void setFocusPreset(int minutes) {
        if (isFocusRunning()) return;
        long currentRemaining = prefs.getLong("focus_remaining_ms", getFocusDurationMs());
        long currentDuration = getFocusDurationMs();
        boolean activePausedSession = !isFocusFinished() && currentRemaining > 0 && currentRemaining < currentDuration;
        if (activePausedSession) return;

        int safe = FocusSessionPolicy.normalizePreset(minutes);
        prefs.edit()
            .putInt("focus_selected_minutes", safe)
            .putLong("focus_remaining_ms", safe * 60_000L)
            .putBoolean("focus_finished", false)
            .putLong("focus_end_epoch", 0L)
            .apply();
    }

    public void startFocus(long now) {
        if (isFocusRunning()) return;

        long duration = getFocusDurationMs();
        long remaining = prefs.getLong("focus_remaining_ms", duration);
        boolean finished = isFocusFinished();
        long sessionId = prefs.getLong("focus_session_id", 0L);

        long lastCountedId = prefs.getLong("focus_last_counted_session_id", 0L);
        boolean newSession = finished || remaining <= 0 || remaining > duration
            || (remaining >= duration && sessionId == lastCountedId);
        String sessionDate = prefs.getString("focus_session_date", "");
        if (newSession) {
            remaining = duration;
            sessionId += 1L;
            finished = false;
            sessionDate = today();
        } else if (sessionId <= 0L) {
            sessionId = 1L;
        }
        if (sessionDate.isEmpty()) sessionDate = today();

        prefs.edit()
            .putLong("focus_session_id", sessionId)
            .putString("focus_session_date", sessionDate)
            .putLong("focus_remaining_ms", remaining)
            .putBoolean("focus_finished", finished)
            .putBoolean("focus_running", true)
            .putLong("focus_end_epoch", now + remaining)
            .apply();
    }

    public void pauseFocus(long now) {
        if (!isFocusRunning()) return;
        long remaining = Math.max(0L, prefs.getLong("focus_end_epoch", now) - now);
        prefs.edit()
            .putBoolean("focus_running", false)
            .putLong("focus_end_epoch", 0L)
            .putLong("focus_remaining_ms", remaining)
            .apply();
        if (remaining <= 0L) {
            finishFocus(now);
        }
    }

    public void resetFocus() {
        int selected = getSelectedFocusMinutes();
        prefs.edit()
            .putBoolean("focus_running", false)
            .putBoolean("focus_finished", false)
            .putLong("focus_end_epoch", 0L)
            .putLong("focus_remaining_ms", selected * 60_000L)
            .putString("focus_session_date", "")
            .apply();
    }

    public boolean resolveExpiredFocus(long now) {
        if (!isFocusRunning()) return false;
        long end = prefs.getLong("focus_end_epoch", 0L);
        if (end > now) return false;
        return finishFocus(now);
    }

    public boolean finishFocus(long now) {
        long sessionId = prefs.getLong("focus_session_id", 0L);
        long lastCountedId = prefs.getLong("focus_last_counted_session_id", 0L);
        String sessionDate = prefs.getString("focus_session_date", "");
        if (sessionDate.isEmpty()) sessionDate = today();

        boolean shouldCount = FocusSessionPolicy.shouldCount(sessionId, lastCountedId);
        SharedPreferences.Editor editor = prefs.edit()
            .putBoolean("focus_running", false)
            .putBoolean("focus_finished", true)
            .putLong("focus_end_epoch", 0L)
            .putLong("focus_remaining_ms", 0L);

        if (shouldCount) {
            int count = getFocusCount(sessionDate) + 1;
            int minutes = getFocusMinutes(sessionDate) + getSelectedFocusMinutes();
            editor
                .putInt(key("focus_count", sessionDate), count)
                .putInt(key("focus_minutes", sessionDate), minutes)
                .putLong("focus_last_counted_session_id", sessionId);
        }
        editor.apply();
        touchDate(sessionDate);
        return shouldCount;
    }

    public List<String> getKnownDatesSorted() {
        Set<String> raw = prefs.getStringSet(KNOWN_DATES, Collections.emptySet());
        List<String> dates = new ArrayList<>(raw == null ? Collections.emptySet() : raw);
        Collections.sort(dates, Collections.reverseOrder());
        return dates;
    }

    public String getLegacyIdeaForToday() {
        return prefs.getString("legacy_idea_today", "");
    }

    public int getIdeaIndex(String date, int ideaCount) {
        if (ideaCount <= 0) return 0;
        String k = key("idea_index", date);
        if (prefs.contains(k)) {
            return Math.floorMod(prefs.getInt(k, 0), ideaCount);
        }
        int index = Math.floorMod(date.hashCode(), ideaCount);
        prefs.edit().putInt(k, index).apply();
        touchDate(date);
        return index;
    }

    public int nextIdeaIndex(String date, int ideaCount) {
        if (ideaCount <= 0) return 0;
        int index = Math.floorMod(getIdeaIndex(date, ideaCount) + 1, ideaCount);
        prefs.edit().putInt(key("idea_index", date), index).putString("legacy_idea_today", "").apply();
        touchDate(date);
        return index;
    }

    private void migrateLegacy() {
        if (prefs.getBoolean(MIGRATED_110, false)) return;

        String today = today();
        SharedPreferences.Editor editor = prefs.edit();

        String legacyTask = prefs.getString("task", "");
        if (!legacyTask.isEmpty() && !prefs.contains(key("task", today))) {
            editor.putString(key("task", today), legacyTask);
        }

        String legacyNote = prefs.getString("note", "");
        if (!legacyNote.isEmpty() && !prefs.contains(key("note", today))) {
            editor.putString(key("note", today), legacyNote);
        }

        String legacyIdea = prefs.getString("idea", "");
        if (!legacyIdea.isEmpty()) {
            editor.putString("legacy_idea_today", legacyIdea);
        }

        String lastDate = prefs.getString("last_date", "");
        String lastTask = prefs.getString("last_task", "");
        Set<String> dates = new HashSet<>(prefs.getStringSet(KNOWN_DATES, Collections.emptySet()));
        if (!legacyTask.isEmpty() || !legacyNote.isEmpty()) dates.add(today);
        if (!lastDate.isEmpty()) {
            dates.add(lastDate);
            if (!lastTask.isEmpty() && !prefs.contains(key("task", lastDate))) {
                editor.putString(key("task", lastDate), lastTask);
            }
            editor.putBoolean(key("completed", lastDate), true);
        }

        editor.putStringSet(KNOWN_DATES, dates)
            .putBoolean(MIGRATED_110, true)
            .apply();

        if (!prefs.contains("focus_selected_minutes")) {
            prefs.edit()
                .putInt("focus_selected_minutes", 10)
                .putLong("focus_remaining_ms", 10 * 60_000L)
                .putBoolean("focus_finished", false)
                .apply();
        }
    }

    private void touchDate(String date) {
        Set<String> existing = prefs.getStringSet(KNOWN_DATES, Collections.emptySet());
        Set<String> updated = new HashSet<>(existing == null ? Collections.emptySet() : existing);
        updated.add(date);
        prefs.edit().putStringSet(KNOWN_DATES, updated).apply();
    }

    private static String key(String prefix, String date) {
        return prefix + "_" + date;
    }
}
