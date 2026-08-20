package com.shaterguy.hankan;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class HankanWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_COMPLETE =
        "com.shaterguy.hankan.action.COMPLETE_TODAY";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateOne(context, manager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_COMPLETE.equals(intent.getAction())) {
            HankanStore store = new HankanStore(context);
            store.completeToday();
            updateAll(context);
        }
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, HankanWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(provider);
        for (int id : ids) {
            updateOne(context, manager, id);
        }
    }

    private static void updateOne(Context context, AppWidgetManager manager, int appWidgetId) {
        HankanStore store = new HankanStore(context);
        String date = store.today();
        String task = store.getTask(date);
        boolean completed = store.isCompleted(date);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_hankan);
        views.setTextViewText(
            R.id.widget_date,
            LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일 E", Locale.KOREAN))
        );
        views.setTextViewText(
            R.id.widget_task,
            task.isEmpty() ? "오늘의 한 칸을 정해보세요" : task
        );
        views.setTextViewText(
            R.id.widget_streak,
            "연속 " + store.getStreak() + "일 · 누적 " + store.getTotalCompleted() + "칸"
        );
        views.setTextViewText(
            R.id.widget_action,
            completed ? "✓ 오늘 완료" : (task.isEmpty() ? "한칸 열기" : "완료 ✓")
        );

        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openIntent = PendingIntent.getActivity(
            context,
            2001,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_root, openIntent);

        if (task.isEmpty() || completed) {
            views.setOnClickPendingIntent(R.id.widget_action, openIntent);
        } else {
            Intent complete = new Intent(context, HankanWidgetProvider.class);
            complete.setAction(ACTION_COMPLETE);
            PendingIntent completeIntent = PendingIntent.getBroadcast(
                context,
                2002,
                complete,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.widget_action, completeIntent);
        }

        manager.updateAppWidget(appWidgetId, views);
    }
}
