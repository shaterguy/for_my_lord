package com.shaterguy.hankan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final DateTimeFormatter HEADER_DATE =
        DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN);
    private static final DateTimeFormatter RECORD_DATE =
        DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN);

    private final String[] familyIdeas = {
        "각자 오늘 가장 웃겼던 일을 하나씩 말해보기",
        "간식 하나를 나눠 먹으며 10분 산책하기",
        "가족 사진첩에서 사진 한 장을 골라 이야기하기",
        "서로에게 고마웠던 일 하나씩 말해보기",
        "집 안에서 보물찾기 문제를 하나씩 내기",
        "좋아하는 노래 한 곡씩 이어서 듣기",
        "종이 한 장에 함께 괴상한 동물 그리기",
        "오늘의 디저트를 가족 투표로 정하기",
        "불을 조금 낮추고 10분 동안 보드게임 하기",
        "내일 하고 싶은 작은 일을 한 가지씩 정하기",
        "휴대폰 없이 15분 동안 동네 한 바퀴 걷기",
        "서로에게 엉뚱한 퀴즈 세 문제씩 내기"
    };

    private HankanStore store;
    private CountDownTimer countDownTimer;
    private TextView timerText;
    private boolean recordsTab;
    private boolean editTask;

    private int background;
    private int card;
    private int surfaceAlt;
    private int primary;
    private int primaryContainer;
    private int ink;
    private int muted;
    private int outline;
    private int success;
    private int successContainer;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        loadColors();
        applySystemBars();
        store = new HankanStore(this);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (store != null && store.resolveExpiredFocus(System.currentTimeMillis())) {
            HankanWidgetProvider.updateAll(this);
            render();
        } else {
            startLocalCountdownIfNeeded();
        }
    }

    @Override
    protected void onDestroy() {
        stopLocalCountdown();
        super.onDestroy();
    }

    private void loadColors() {
        background = getColor(R.color.hankan_background);
        card = getColor(R.color.hankan_card);
        surfaceAlt = getColor(R.color.hankan_surface_alt);
        primary = getColor(R.color.hankan_primary);
        primaryContainer = getColor(R.color.hankan_primary_container);
        ink = getColor(R.color.hankan_ink);
        muted = getColor(R.color.hankan_muted);
        outline = getColor(R.color.hankan_outline);
        success = getColor(R.color.hankan_success);
        successContainer = getColor(R.color.hankan_success_container);
    }

    private void applySystemBars() {
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
            == Configuration.UI_MODE_NIGHT_YES;
        int flags = 0;
        if (!dark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (!dark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private void render() {
        stopLocalCountdown();
        FrameLayout outer = new FrameLayout(this);
        outer.setBackgroundColor(background);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        outer.addView(scroll, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout root = column();
        root.setPadding(dp(18), dp(14), dp(18), dp(32));
        scroll.addView(root, matchWrap());
        applyInsets(root);

        buildHeader(root);
        if (recordsTab) {
            buildRecords(root);
        } else {
            buildToday(root);
        }

        setContentView(outer);
        startLocalCountdownIfNeeded();
    }

    private void applyInsets(View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH) return;
        view.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.ime()
                );
                top = bars.top;
                bottom = bars.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(dp(18), dp(14) + top, dp(18), dp(32) + bottom);
            return insets;
        });
        view.requestApplyInsets();
    }

    private void buildHeader(LinearLayout root) {
        TextView eyebrow = text("오늘을 가볍게", 14, primary, true);
        root.addView(eyebrow);

        LinearLayout titleRow = row();
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(titleRow, matchWrap());

        TextView title = text("한칸", 34, ink, true);
        titleRow.addView(title, weighted());

        TextView streak = text("🔥 " + store.getStreak() + "일 · " + store.getTotalCompleted() + "칸", 14, muted, true);
        streak.setGravity(Gravity.END);
        titleRow.addView(streak);

        TextView date = text(LocalDate.now().format(HEADER_DATE), 15, muted, false);
        date.setPadding(0, dp(2), 0, dp(14));
        root.addView(date);

        LinearLayout tabs = row();
        tabs.setPadding(dp(3), dp(3), dp(3), dp(3));
        tabs.setBackground(roundRect(surfaceAlt, dp(16), outline, 0));
        root.addView(tabs, bottom(16));

        Button today = tabButton("오늘", !recordsTab);
        Button records = tabButton("기록", recordsTab);
        tabs.addView(today, weighted());
        LinearLayout.LayoutParams recordsLp = weighted();
        recordsLp.leftMargin = dp(4);
        tabs.addView(records, recordsLp);

        today.setOnClickListener(v -> {
            if (!recordsTab) return;
            recordsTab = false;
            editTask = false;
            render();
        });
        records.setOnClickListener(v -> {
            if (recordsTab) return;
            hideKeyboard();
            recordsTab = true;
            editTask = false;
            render();
        });
    }

    private void buildToday(LinearLayout root) {
        boolean wide = getResources().getConfiguration().screenWidthDp >= 600;
        LinearLayout main = column();
        LinearLayout secondary = column();

        main.addView(buildTaskCard(), cardLp());
        main.addView(buildFocusCard(), cardLp());
        secondary.addView(buildNoteCard(), cardLp());
        secondary.addView(buildFamilyCard(), cardLp());
        secondary.addView(buildShareCard(), cardLp());

        if (!wide) {
            root.addView(main, matchWrap());
            root.addView(secondary, matchWrap());
            return;
        }

        LinearLayout columns = row();
        columns.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams left = weighted();
        LinearLayout.LayoutParams right = weighted();
        right.leftMargin = dp(14);
        columns.addView(main, left);
        columns.addView(secondary, right);
        root.addView(columns, matchWrap());
    }

    private View buildTaskCard() {
        String date = store.today();
        String task = store.getTask(date);
        boolean completed = store.isCompleted(date);

        LinearLayout box = card(completed ? successContainer : card);
        TextView label = sectionTitle(completed ? "✓ 오늘의 한 칸 완료" : "오늘의 한 칸");
        label.setTextColor(completed ? success : ink);
        box.addView(label);

        if (task.isEmpty() || editTask) {
            EditText input = input("오늘 이것 하나만 끝내면 됩니다", false);
            input.setText(task);
            input.setSelection(input.length());
            box.addView(input, top(10));

            Button save = button(task.isEmpty() ? "오늘로 정하기" : "수정 저장", true);
            box.addView(save, top(10));
            save.setOnClickListener(v -> {
                String value = input.getText().toString().trim();
                if (value.isEmpty()) {
                    toast("오늘의 한 칸을 적어주세요.");
                    return;
                }
                store.setTask(date, value);
                editTask = false;
                HankanWidgetProvider.updateAll(this);
                hideKeyboard();
                render();
                toast("오늘의 한 칸을 정했습니다.");
            });
            if (!task.isEmpty()) {
                Button cancel = button("취소", false);
                box.addView(cancel, top(8));
                cancel.setOnClickListener(v -> {
                    editTask = false;
                    hideKeyboard();
                    render();
                });
            }
            return box;
        }

        TextView taskView = text(task, 21, ink, true);
        taskView.setLineSpacing(0, 1.14f);
        taskView.setPadding(0, dp(10), 0, dp(10));
        box.addView(taskView);

        if (!completed) {
            Button done = button("완료하기 ✓", true);
            box.addView(done);
            done.setOnClickListener(v -> {
                if (store.completeToday()) {
                    HankanWidgetProvider.updateAll(this);
                    render();
                    toast("오늘의 한 칸을 채웠습니다.");
                }
            });

            Button edit = button("수정", false);
            box.addView(edit, top(8));
            edit.setOnClickListener(v -> {
                editTask = true;
                render();
            });
        } else {
            TextView hint = text("오늘은 여기까지 해도 충분합니다.", 14, muted, false);
            hint.setPadding(0, 0, 0, dp(8));
            box.addView(hint);

            Button undo = button("완료 취소", false);
            box.addView(undo);
            undo.setOnClickListener(v -> {
                store.undoTodayCompletion();
                HankanWidgetProvider.updateAll(this);
                render();
                toast("완료 기록을 취소했습니다.");
            });
        }
        return box;
    }

    private View buildFocusCard() {
        LinearLayout box = card(card);
        box.addView(sectionTitle("집중"));

        long now = System.currentTimeMillis();
        store.resolveExpiredFocus(now);
        long remaining = store.getFocusRemainingMs(now);
        long duration = store.getFocusDurationMs();
        boolean running = store.isFocusRunning();
        boolean finished = store.isFocusFinished();
        boolean pausedActive = !running && !finished && remaining > 0 && remaining < duration;

        LinearLayout presets = row();
        presets.setPadding(0, dp(10), 0, 0);
        box.addView(presets, matchWrap());
        int selected = store.getSelectedFocusMinutes();
        int[] choices = {10, 25, 50};
        for (int i = 0; i < choices.length; i++) {
            int minutes = choices[i];
            Button preset = chip(minutes + "분", selected == minutes);
            boolean enabled = !running && !pausedActive;
            preset.setEnabled(enabled);
            preset.setAlpha(enabled ? 1f : 0.45f);
            LinearLayout.LayoutParams lp = weighted();
            if (i > 0) lp.leftMargin = dp(6);
            presets.addView(preset, lp);
            preset.setOnClickListener(v -> {
                store.setFocusPreset(minutes);
                render();
            });
        }

        timerText = text(formatTime(remaining), 42, ink, true);
        timerText.setGravity(Gravity.CENTER);
        timerText.setPadding(0, dp(12), 0, dp(2));
        box.addView(timerText);

        String status;
        if (running) status = "집중 중";
        else if (finished) status = "완료";
        else if (pausedActive) status = "잠시 멈춤";
        else status = "준비";
        TextView statusView = text(status, 14, running ? primary : muted, true);
        statusView.setGravity(Gravity.CENTER);
        box.addView(statusView);

        Button action = button(running ? "잠시 멈춤" : (pausedActive ? "계속하기" : (finished ? "한 번 더" : "집중 시작")), true);
        box.addView(action, top(10));
        action.setOnClickListener(v -> {
            long current = System.currentTimeMillis();
            if (store.isFocusRunning()) {
                store.pauseFocus(current);
            } else {
                store.startFocus(current);
            }
            render();
        });

        Button reset = button("처음으로", false);
        box.addView(reset, top(8));
        reset.setOnClickListener(v -> {
            store.resetFocus();
            render();
        });

        int todayCount = store.getFocusCount(store.today());
        int todayMinutes = store.getFocusMinutes(store.today());
        TextView today = text("오늘 " + todayCount + "회 · " + todayMinutes + "분 집중", 13, muted, false);
        today.setGravity(Gravity.CENTER);
        today.setPadding(0, dp(9), 0, 0);
        box.addView(today);
        return box;
    }

    private View buildNoteCard() {
        String date = store.today();
        LinearLayout box = card(card);
        box.addView(sectionTitle("생각 보관함"));

        EditText note = input("잊기 전에 적어두세요.", true);
        note.setText(store.getNote(date));
        note.setSelection(note.length());
        box.addView(note, top(10));

        Button save = button("메모 저장", false);
        box.addView(save, top(10));
        save.setOnClickListener(v -> {
            store.setNote(date, note.getText().toString());
            hideKeyboard();
            toast("기기에 저장했습니다.");
            render();
        });

        TextView privacy = text("이 메모는 이 기기 밖으로 전송되지 않습니다.", 12, muted, false);
        privacy.setPadding(0, dp(8), 0, 0);
        box.addView(privacy);
        return box;
    }

    private View buildFamilyCard() {
        String date = store.today();
        LinearLayout box = card(card);
        box.addView(sectionTitle("오늘의 작은 재미"));

        String legacy = store.getLegacyIdeaForToday();
        int index = store.getIdeaIndex(date, familyIdeas.length);
        String value = legacy.isEmpty() ? familyIdeas[index] : legacy;

        TextView idea = text(value, 16, ink, false);
        idea.setLineSpacing(0, 1.18f);
        idea.setPadding(0, dp(10), 0, dp(10));
        box.addView(idea);

        Button next = button("다른 아이디어", false);
        box.addView(next);
        next.setOnClickListener(v -> {
            store.nextIdeaIndex(date, familyIdeas.length);
            render();
        });
        return box;
    }

    private View buildShareCard() {
        LinearLayout box = card(surfaceAlt);
        TextView title = text("오늘을 한 줄로 남기기", 17, ink, true);
        box.addView(title);
        TextView hint = text("오늘의 한 칸과 집중 기록을 Android 공유 메뉴로 보냅니다.", 13, muted, false);
        hint.setPadding(0, dp(5), 0, dp(10));
        box.addView(hint);

        Button share = button("오늘 요약 공유", false);
        box.addView(share);
        share.setOnClickListener(v -> shareToday());
        return box;
    }

    private void buildRecords(LinearLayout root) {
        LinearLayout stats = row();
        stats.addView(stat("연속", store.getStreak() + "일"), weighted());
        LinearLayout.LayoutParams middle = weighted();
        middle.leftMargin = dp(7);
        stats.addView(stat("완료", store.getTotalCompleted() + "칸"), middle);
        LinearLayout.LayoutParams right = weighted();
        right.leftMargin = dp(7);
        stats.addView(stat("집중", store.getTotalFocusMinutes() + "분"), right);
        root.addView(stats, cardLp());

        LinearLayout weekCard = card(card);
        weekCard.addView(sectionTitle("최근 7일"));
        LinearLayout week = row();
        week.setPadding(0, dp(12), 0, 0);
        weekCard.addView(week, matchWrap());

        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            String date = day.toString();
            boolean done = store.isCompleted(date);
            LinearLayout cell = column();
            cell.setGravity(Gravity.CENTER);

            TextView dow = text(day.format(DateTimeFormatter.ofPattern("E", Locale.KOREAN)), 12, muted, false);
            dow.setGravity(Gravity.CENTER);
            cell.addView(dow);

            TextView mark = text(done ? "✓" : "○", 19, done ? success : muted, true);
            mark.setGravity(Gravity.CENTER);
            mark.setMinWidth(dp(36));
            mark.setMinHeight(dp(36));
            mark.setGravity(Gravity.CENTER);
            if (day.equals(today)) {
                mark.setBackground(roundRect(primaryContainer, dp(18), primary, dp(1)));
            }
            mark.setContentDescription(day.format(RECORD_DATE) + (done ? " 완료" : " 미완료"));
            cell.addView(mark, top(4));
            week.addView(cell, weighted());
        }
        root.addView(weekCard, cardLp());

        TextView historyTitle = sectionTitle("기록");
        historyTitle.setPadding(0, 0, 0, dp(9));
        root.addView(historyTitle);

        List<String> dates = store.getKnownDatesSorted();
        int shown = 0;
        for (String date : dates) {
            if (shown >= 30) break;
            String task = store.getTask(date);
            String note = store.getNote(date);
            int count = store.getFocusCount(date);
            int minutes = store.getFocusMinutes(date);
            boolean completed = store.isCompleted(date);
            if (task.isEmpty() && note.isEmpty() && count == 0 && !completed) continue;

            root.addView(recordCard(date, task, note, completed, count, minutes), bottom(10));
            shown++;
        }

        if (shown == 0) {
            LinearLayout empty = card(card);
            TextView message = text("첫 한 칸을 채우면 여기에 기록이 쌓입니다.", 16, muted, false);
            empty.addView(message);
            Button todayButton = button("오늘로 이동", true);
            empty.addView(todayButton, top(10));
            todayButton.setOnClickListener(v -> {
                recordsTab = false;
                render();
            });
            root.addView(empty, cardLp());
        }
    }

    private View recordCard(String date, String task, String note, boolean completed, int count, int minutes) {
        LinearLayout box = card(card);
        LocalDate parsed = LocalDate.parse(date);

        LinearLayout head = row();
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView dateView = text(parsed.format(RECORD_DATE), 14, muted, true);
        head.addView(dateView, weighted());
        if (completed) {
            TextView done = text("✓ 완료", 13, success, true);
            done.setGravity(Gravity.END);
            head.addView(done);
        }
        box.addView(head);

        if (!task.isEmpty()) {
            TextView taskView = text(task, 17, ink, true);
            taskView.setPadding(0, dp(6), 0, 0);
            box.addView(taskView);
        }

        if (count > 0 || minutes > 0) {
            TextView focus = text("집중 " + count + "회 · " + minutes + "분", 13, muted, false);
            focus.setPadding(0, dp(5), 0, 0);
            box.addView(focus);
        }

        if (!note.trim().isEmpty()) {
            String preview = note.trim().replace('\n', ' ');
            if (preview.length() > 70) preview = preview.substring(0, 70) + "…";
            TextView noteView = text(preview, 13, muted, false);
            noteView.setPadding(0, dp(5), 0, 0);
            box.addView(noteView);
        }
        return box;
    }

    private View stat(String label, String value) {
        LinearLayout box = card(surfaceAlt);
        box.setGravity(Gravity.CENTER);
        TextView valueView = text(value, 20, ink, true);
        valueView.setGravity(Gravity.CENTER);
        box.addView(valueView);
        TextView labelView = text(label, 12, muted, false);
        labelView.setGravity(Gravity.CENTER);
        labelView.setPadding(0, dp(3), 0, 0);
        box.addView(labelView);
        return box;
    }

    private void shareToday() {
        String date = store.today();
        String task = store.getTask(date);
        boolean completed = store.isCompleted(date);
        int count = store.getFocusCount(date);
        int minutes = store.getFocusMinutes(date);
        String note = store.getNote(date).trim();

        StringBuilder summary = new StringBuilder();
        summary.append("한칸 · ").append(LocalDate.now().format(HEADER_DATE)).append('\n');
        summary.append(completed ? "✓ " : "• ");
        summary.append(task.isEmpty() ? "오늘의 한 칸은 아직 비어 있습니다." : task).append('\n');
        summary.append("집중 ").append(count).append("회 · ").append(minutes).append("분");
        if (!note.isEmpty()) summary.append("\n메모: ").append(note);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, summary.toString());
        startActivity(Intent.createChooser(intent, "오늘 요약 공유"));
    }

    private void startLocalCountdownIfNeeded() {
        stopLocalCountdown();
        if (store == null || !store.isFocusRunning() || timerText == null || recordsTab) return;

        long remaining = store.getFocusRemainingMs(System.currentTimeMillis());
        if (remaining <= 0) {
            if (store.finishFocus(System.currentTimeMillis())) {
                HankanWidgetProvider.updateAll(this);
                toast("집중을 마쳤습니다.");
                render();
            }
            return;
        }

        countDownTimer = new CountDownTimer(remaining, 250L) {
            @Override public void onTick(long millisUntilFinished) {
                if (timerText != null) timerText.setText(formatTime(millisUntilFinished));
            }

            @Override public void onFinish() {
                if (store.finishFocus(System.currentTimeMillis())) {
                    HankanWidgetProvider.updateAll(MainActivity.this);
                    toast("집중을 마쳤습니다.");
                }
                render();
            }
        }.start();
    }

    private void stopLocalCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private Button tabButton(String label, boolean selected) {
        Button b = button(label, false);
        b.setTextColor(selected ? getColor(R.color.hankan_on_primary) : primary);
        b.setBackground(roundRect(selected ? primary : Color.TRANSPARENT, dp(13), Color.TRANSPARENT, 0));
        return b;
    }

    private Button chip(String label, boolean selected) {
        Button b = button(label, false);
        b.setTextColor(selected ? primary : muted);
        b.setBackground(roundRect(selected ? primaryContainer : surfaceAlt, dp(13), selected ? primary : outline, dp(1)));
        return b;
    }

    private LinearLayout card() {
        return card(card);
    }

    private LinearLayout card(int color) {
        LinearLayout l = column();
        l.setPadding(dp(17), dp(16), dp(17), dp(16));
        l.setBackground(roundRect(color, dp(20), outline, dp(1)));
        l.setElevation(dp(1));
        return l;
    }

    private TextView sectionTitle(String value) {
        return text(value, 19, ink, true);
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setLineSpacing(0, 1.08f);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private EditText input(String hint, boolean multi) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(16);
        e.setTextColor(ink);
        e.setHintTextColor(muted);
        e.setPadding(dp(14), dp(12), dp(14), dp(12));
        e.setMinHeight(dp(48));
        if (multi) {
            e.setMinLines(3);
            e.setGravity(Gravity.TOP | Gravity.START);
        } else {
            e.setSingleLine(true);
        }
        e.setBackground(roundRect(surfaceAlt, dp(14), outline, dp(1)));
        return e;
    }

    private Button button(String label, boolean primaryAction) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setMinHeight(dp(48));
        b.setMinimumHeight(dp(48));
        b.setPadding(dp(14), 0, dp(14), 0);
        b.setTextColor(primaryAction ? getColor(R.color.hankan_on_primary) : primary);
        b.setBackground(roundRect(primaryAction ? primary : primaryContainer, dp(14), Color.TRANSPARENT, 0));
        return b;
    }

    private GradientDrawable roundRect(int fill, int radius, int stroke, int strokeWidth) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(radius);
        if (strokeWidth > 0) bg.setStroke(strokeWidth, stroke);
        return bg;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        );
    }

    private LinearLayout.LayoutParams top(int margin) {
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(margin);
        return lp;
    }

    private LinearLayout.LayoutParams bottom(int margin) {
        LinearLayout.LayoutParams lp = matchWrap();
        lp.bottomMargin = dp(margin);
        return lp;
    }

    private LinearLayout.LayoutParams cardLp() {
        return bottom(14);
    }

    private String formatTime(long millis) {
        long seconds = Math.max(0L, (millis + 999L) / 1000L);
        return String.format(Locale.US, "%02d:%02d", seconds / 60L, seconds % 60L);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus == null) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        focus.clearFocus();
    }
}
