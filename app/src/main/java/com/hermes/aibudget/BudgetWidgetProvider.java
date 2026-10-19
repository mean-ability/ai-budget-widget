package com.hermes.aibudget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import org.json.JSONObject;
import java.util.Locale;

public class BudgetWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_REFRESH = "com.hermes.aibudget.ACTION_REFRESH";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateAllWidgets(context, false);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            updateAllWidgets(context, true);
        }
    }

    public static void updateAllWidgets(final Context context, final boolean triggerSync) {
        new Thread(() -> {
            JSONObject data = BudgetFetcher.fetchLatest(triggerSync);
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(context, BudgetWidgetProvider.class));
            for (int id : ids) {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_budget);

                Intent refreshIntent = new Intent(context, BudgetWidgetProvider.class);
                refreshIntent.setAction(ACTION_REFRESH);
                PendingIntent refreshPi = PendingIntent.getBroadcast(
                        context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                views.setOnClickPendingIntent(R.id.btn_refresh, refreshPi);

                Intent openIntent = new Intent(context, MainActivity.class);
                PendingIntent openPi = PendingIntent.getActivity(
                        context, 1, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                views.setOnClickPendingIntent(R.id.widget_root, openPi);

                if (data != null) {
                    try {
                        JSONObject s = data.getJSONObject("summary");
                        double today = s.optDouble("today_hermes_usd", 0.0);
                        double twd = s.optDouble("ai_studio_remaining_twd", 2690.0);
                        double claudeApi = s.optDouble("claude_api_remaining_usd", 111.43);
                        int sessPct = s.optInt("claude_sub_session_pct", 8);
                        int weekPct = s.optInt("claude_sub_weekly_pct", 2);
                        double creditPkg = s.optDouble("claude_desktop_credit_usd", 1000.0);
                        String updated = data.optString("updated_at", "");

                        views.setTextViewText(R.id.tv_title, String.format(Locale.US, "💰 AI 預算管家 (今日 $%.2f)", today));
                        views.setTextViewText(R.id.tv_line1, String.format(Locale.US, "AI Studio 餘額: NT$%,.0f | Claude API: US$%.2f", twd, claudeApi));
                        views.setTextViewText(R.id.tv_line2, String.format(Locale.US, "Claude訂閱額度: 工作階段 %d%% / 每週 %d%% (額度包 $%.0f)", sessPct, weekPct, creditPkg));
                        views.setTextViewText(R.id.tv_updated, "最後同步: " + updated + " (支援區網+4G雲端)");
                    } catch (Exception ignored) {}
                }
                mgr.updateAppWidget(id, views);
            }
        }).start();
    }
}
