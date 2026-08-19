package com.shaterguy.hankan;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class MainActivity extends Activity {
    private static final String PREFS = "hankan_prefs";
    private static final long FOCUS_MS = 10 * 60 * 1000L;
    private static final int PURPLE = Color.rgb(103, 80, 164);
    private static final int INK = Color.rgb(36, 35, 42);
    private static final int MUTED = Color.rgb(103, 101, 112);
    private static final int SURFACE = Color.rgb(247, 245, 252);
    private static final int CARD = Color.WHITE;

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
        "내일 하고 싶은 작은 일을 한 가지씩 정하기"
    };

    private SharedPreferences prefs;
    private EditText taskInput;
    private EditText noteInput;
    private TextView streakText;
    private TextView timerText;
    private TextView ideaText;
    private Button timerButton;
    private CountDownTimer timer;
    private long remainingMs = FOCUS_MS;
    private boolean timerRunning;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        getWindow().setStatusBarColor(SURFACE);
        getWindow().setNavigationBarColor(SURFACE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(buildScreen());
        restore();
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(SURFACE);

        LinearLayout root = column();
        root.setPadding(dp(20), dp(24), dp(20), dp(40));
        scroll.addView(root, matchWrap());

        TextView eyebrow = text("오늘을 가볍게", 14, PURPLE, true);
        root.addView(eyebrow);
        TextView title = text("한칸", 34, INK, true);
        title.setPadding(0, dp(3), 0, 0);
        root.addView(title);
        TextView subtitle = text(LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일")) + " · 딱 한 칸만 채워도 충분합니다.", 15, MUTED, false);
        subtitle.setPadding(0, dp(3), 0, dp(18));
        root.addView(subtitle);

        LinearLayout status = card();
        LinearLayout.LayoutParams statusLp = matchWrap();
        statusLp.bottomMargin = dp(14);
        root.addView(status, statusLp);
        streakText = text("", 17, INK, true);
        status.addView(streakText);
        TextView statusHint = text("완료 버튼을 누르면 오늘 기록이 남습니다.", 13, MUTED, false);
        statusHint.setPadding(0, dp(5), 0, 0);
        status.addView(statusHint);

        LinearLayout taskCard = card();
        root.addView(taskCard, cardLp());
        taskCard.addView(sectionTitle("오늘의 한 가지"));
        taskInput = input("지금 가장 끝내고 싶은 일", false);
        taskCard.addView(taskInput, matchWrap());
        LinearLayout taskButtons = row();
        Button save = button("저장", false);
        Button done = button("완료 ✓", true);
        taskButtons.addView(save, weighted());
        LinearLayout.LayoutParams gap = weighted();
        gap.leftMargin = dp(8);
        taskButtons.addView(done, gap);
        taskCard.addView(taskButtons, top(10));
        save.setOnClickListener(v -> {
            prefs.edit().putString("task", taskInput.getText().toString().trim()).apply();
            hideKeyboard();
            toast("오늘의 한 칸을 저장했습니다.");
        });
        done.setOnClickListener(v -> completeToday());

        LinearLayout timerCard = card();
        root.addView(timerCard, cardLp());
        timerCard.addView(sectionTitle("10분 집중"));
        timerText = text("10:00", 38, INK, true);
        timerText.setGravity(Gravity.CENTER_HORIZONTAL);
        timerCard.addView(timerText, top(5));
        timerButton = button("집중 시작", true);
        timerCard.addView(timerButton, top(8));
        timerButton.setOnClickListener(v -> toggleTimer());
        Button reset = button("처음으로", false);
        timerCard.addView(reset, top(8));
        reset.setOnClickListener(v -> resetTimer());

        LinearLayout noteCard = card();
        root.addView(noteCard, cardLp());
        noteCard.addView(sectionTitle("생각 보관함"));
        noteInput = input("잊기 전에 적어두세요.", true);
        noteCard.addView(noteInput, matchWrap());
        Button noteSave = button("메모 저장", false);
        noteCard.addView(noteSave, top(10));
        noteSave.setOnClickListener(v -> {
            prefs.edit().putString("note", noteInput.getText().toString()).apply();
            hideKeyboard();
            toast("기기에 안전하게 저장했습니다.");
        });

        LinearLayout familyCard = card();
        root.addView(familyCard, cardLp());
        familyCard.addView(sectionTitle("가족과 작은 재미"));
        ideaText = text("", 16, INK, false);
        ideaText.setLineSpacing(0, 1.18f);
        ideaText.setPadding(0, dp(8), 0, dp(8));
        familyCard.addView(ideaText);
        Button nextIdea = button("다른 활동 뽑기", false);
        familyCard.addView(nextIdea);
        nextIdea.setOnClickListener(v -> pickIdea());

        TextView privacy = text("모든 기록은 이 기기에만 저장됩니다. 로그인과 인터넷 연결이 필요하지 않습니다.", 12, MUTED, false);
        privacy.setGravity(Gravity.CENTER);
        privacy.setPadding(dp(10), dp(8), dp(10), 0);
        root.addView(privacy);
        return scroll;
    }

    private void restore() {
        taskInput.setText(prefs.getString("task", ""));
        noteInput.setText(prefs.getString("note", ""));
        int streak = prefs.getInt("streak", 0);
        int total = prefs.getInt("total", 0);
        streakText.setText("🔥 " + streak + "일 연속  ·  지금까지 " + total + "칸");
        ideaText.setText(prefs.getString("idea", familyIdeas[0]));
    }

    private void completeToday() {
        String task = taskInput.getText().toString().trim();
        if (task.isEmpty()) {
            toast("먼저 오늘의 한 가지를 적어주세요.");
            return;
        }
        LocalDate today = LocalDate.now();
        String todayString = today.toString();
        String last = prefs.getString("last_date", "");
        boolean already = todayString.equals(last);
        int streak = StreakCalculator.nextStreak(last, today, prefs.getInt("streak", 0));
        int total = prefs.getInt("total", 0) + (already ? 0 : 1);
        prefs.edit()
            .putInt("streak", streak)
            .putInt("total", total)
            .putString("last_date", todayString)
            .putString("last_task", task)
            .apply();
        streakText.setText("🔥 " + streak + "일 연속  ·  지금까지 " + total + "칸");
        toast(already ? "오늘은 이미 완료했습니다. 충분히 잘했습니다." : "오늘의 한 칸을 채웠습니다!");
        hideKeyboard();
    }

    private void toggleTimer() {
        if (timerRunning) {
            timer.cancel();
            timerRunning = false;
            timerButton.setText("계속하기");
            return;
        }
        timerRunning = true;
        timerButton.setText("잠시 멈춤");
        timer = new CountDownTimer(remainingMs, 1000) {
            public void onTick(long ms) {
                remainingMs = ms;
                updateTimer();
            }
            public void onFinish() {
                remainingMs = 0;
                timerRunning = false;
                updateTimer();
                timerButton.setText("한 번 더");
                toast("10분 집중을 마쳤습니다!");
            }
        }.start();
    }

    private void resetTimer() {
        if (timer != null) timer.cancel();
        timerRunning = false;
        remainingMs = FOCUS_MS;
        updateTimer();
        timerButton.setText("집중 시작");
    }

    private void updateTimer() {
        long sec = (remainingMs + 999) / 1000;
        timerText.setText(String.format("%02d:%02d", sec / 60, sec % 60));
    }

    private void pickIdea() {
        String current = ideaText.getText().toString();
        Random random = new Random();
        String next;
        do next = familyIdeas[random.nextInt(familyIdeas.length)];
        while (familyIdeas.length > 1 && next.equals(current));
        ideaText.setText(next);
        prefs.edit().putString("idea", next).apply();
    }

    @Override protected void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
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

    private LinearLayout card() {
        LinearLayout l = column();
        l.setPadding(dp(18), dp(16), dp(18), dp(16));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(22));
        bg.setStroke(dp(1), Color.rgb(234, 231, 239));
        l.setBackground(bg);
        l.setElevation(dp(2));
        return l;
    }

    private TextView sectionTitle(String value) {
        return text(value, 19, INK, true);
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private EditText input(String hint, boolean multi) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(16);
        e.setTextColor(INK);
        e.setHintTextColor(Color.rgb(140, 137, 147));
        e.setPadding(dp(14), dp(12), dp(14), dp(12));
        if (multi) {
            e.setMinLines(3);
            e.setGravity(Gravity.TOP);
        } else {
            e.setSingleLine(true);
        }
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.rgb(249, 247, 252));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), Color.rgb(222, 218, 227));
        e.setBackground(bg);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(10);
        e.setLayoutParams(lp);
        return e;
    }

    private Button button(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? Color.WHITE : PURPLE);
        b.setMinHeight(dp(48));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(primary ? PURPLE : Color.rgb(239, 232, 255));
        bg.setCornerRadius(dp(14));
        b.setBackground(bg);
        return b;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    private LinearLayout.LayoutParams top(int margin) {
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(margin);
        return lp;
    }

    private LinearLayout.LayoutParams cardLp() {
        LinearLayout.LayoutParams lp = matchWrap();
        lp.bottomMargin = dp(14);
        return lp;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus != null) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(focus.getWindowToken(), 0);
            focus.clearFocus();
        }
    }
}
