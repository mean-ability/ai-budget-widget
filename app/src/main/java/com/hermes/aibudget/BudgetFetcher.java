package com.hermes.aibudget;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class BudgetFetcher {
    public static final String LAN_API = "http://192.168.0.61:8765/api/budget";
    public static final String CLOUD_API = "https://ntfy.sh/hermes_budget_faab028015b6fe6812d0/json?poll=1";

    public static JSONObject fetchLatest(boolean triggerSync) {
        // 1. Try LAN first (fast 1.5s timeout)
        try {
            String urlStr = LAN_API + (triggerSync ? "?sync=1" : "");
            String raw = httpGet(urlStr, 1800);
            if (raw != null && raw.contains("summary")) {
                return new JSONObject(raw);
            }
        } catch (Exception ignored) {}

        // 2. Fallback to Cloud ntfy cache (4G/5G outside home)
        try {
            String rawLines = httpGet(CLOUD_API, 4000);
            if (rawLines != null) {
                String[] lines = rawLines.trim().split("\\n");
                for (int i = lines.length - 1; i >= 0; i--) {
                    JSONObject msg = new JSONObject(lines[i]);
                    if (msg.has("message")) {
                        String inner = msg.getString("message");
                        if (inner.contains("summary")) {
                            return new JSONObject(inner);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String httpGet(String urlStr, int timeoutMs) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() != 200) return null;
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();
        return sb.toString();
    }
}
