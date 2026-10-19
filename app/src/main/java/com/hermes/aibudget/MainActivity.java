package com.hermes.aibudget;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONObject;
import java.util.Locale;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        webView.setBackgroundColor(Color.parseColor("#0F172A"));
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        setContentView(webView);

        BudgetWidgetProvider.updateAllWidgets(this, false);
        loadDashboard();
    }

    private void loadDashboard() {
        new Thread(() -> {
            JSONObject data = BudgetFetcher.fetchLatest(false);
            runOnUiThread(() -> {
                if (data != null) {
                    renderJsonHtml(data);
                } else {
                    webView.loadUrl("http://192.168.0.61:8765/");
                }
            });
        }).start();
    }

    private void renderJsonHtml(JSONObject d) {
        try {
            JSONObject s = d.getJSONObject("summary");
            String html = "<!DOCTYPE html><html><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>body{background:#0f172a;color:#f8fafc;font-family:sans-serif;padding:16px;margin:0}"
                + ".card{background:#1e293b;border:1px solid #334155;border-radius:14px;padding:14px;margin-bottom:12px}"
                + ".lbl{color:#94a3b8;font-size:12px;margin-bottom:4px}"
                + ".val{color:#4ade80;font-size:24px;font-weight:800}"
                + ".sub{color:#cbd5e1;font-size:13px;margin-top:4px}"
                + "button{background:#0284c7;color:#fff;border:none;border-radius:8px;padding:10px 16px;font-weight:bold;width:100%;margin-bottom:14px}"
                + "</style></head><body>"
                + "<h2 style='margin:0 0 12px 0;color:#38bdf8'>💰 AI 預算即時小工具</h2>"
                + "<div class='card'><div class='lbl'>Google AI Studio 可用餘額</div>"
                + "<div class='val'>NT$" + String.format(Locale.US, "%,.1f", s.optDouble("ai_studio_remaining_twd", 2690)) + "</div>"
                + "<div class='sub'>本月累計淨花費: US$" + String.format(Locale.US, "%.2f", s.optDouble("gemini_calibrated_month_usd", 399.42))
                + " (快取已省 US$" + String.format(Locale.US, "%.2f", s.optDouble("gemini_official_savings_usd", 315.08)) + ")</div></div>"
                + "<div class='card'><div class='lbl'>Claude Platform API 剩餘點數</div>"
                + "<div class='val'>US$" + String.format(Locale.US, "%.2f", s.optDouble("claude_api_remaining_usd", 111.43)) + "</div>"
                + "<div class='sub'>過去 30 日累計花費: US$" + String.format(Locale.US, "%.2f", s.optDouble("claude_api_calibrated_30d_usd", 32.40)) + "</div></div>"
                + "<div class='card'><div class='lbl'>Claude 訂閱額度 (工作階段 & 每週分開顯示)</div>"
                + "<div class='val' style='color:#38bdf8;font-size:20px'>工作階段: 已用 " + s.optInt("claude_sub_session_pct", 8) + "% ｜ 每週: 已用 " + s.optInt("claude_sub_weekly_pct", 2) + "%</div>"
                + "<div class='sub'>桌面版附加額度包餘額: US$" + String.format(Locale.US, "%.0f", s.optDouble("claude_desktop_credit_usd", 1000)) + "</div></div>"
                + "<div class='card'><div class='lbl'>今日 Hermes 本地即時累計</div>"
                + "<div class='val' style='color:#fbbf24'>US$" + String.format(Locale.US, "%.4f", s.optDouble("today_hermes_usd", 0)) + "</div>"
                + "<div class='sub'>最後同步: " + d.optString("updated_at", "") + "</div></div>"
                + "</body></html>";
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        } catch (Exception e) {
            webView.loadUrl("http://192.168.0.61:8765/");
        }
    }
}
