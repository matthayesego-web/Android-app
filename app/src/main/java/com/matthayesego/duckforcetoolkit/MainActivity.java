package com.matthayesego.duckforcetoolkit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(13, 17, 23);
    private static final int PANEL = Color.rgb(22, 27, 34);
    private static final int BORDER = Color.rgb(48, 54, 61);
    private static final int ACCENT = Color.rgb(240, 180, 41);
    private static final int TEXT = Color.rgb(240, 246, 252);
    private static final int MUTED = Color.rgb(139, 148, 158);

    private LinearLayout root;
    private WebView webView;
    private Tool currentTool;

    private enum Tool {
        AUDITOR(
                "💊 Faction Xanax Auditor",
                "Audit faction members and Xanax activity with saved contributor snapshots.",
                "auditor.duckforce.app",
                "xanax_auditor.html"
        ),
        ARMORY(
                "📦 Xanax Armory Log",
                "Find Xanax use since the most recent large faction-armory deposit.",
                "armory.duckforce.app",
                "armory_log.html"
        ),
        TRAIN(
                "🚂 Train Payment Calculator",
                "Calculate train payments using cash and Xanax combinations.",
                "train.duckforce.app",
                "train_calculator.html"
        );

        final String title;
        final String subtitle;
        final String domain;
        final String asset;

        Tool(String title, String subtitle, String domain, String asset) {
            this.title = title;
            this.subtitle = subtitle;
            this.domain = domain;
            this.asset = asset;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        showHome();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable rounded(int color, int strokeColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private TextView text(String value, float sizeSp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private void showHome() {
        currentTool = null;
        destroyWebView();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(28));
        root.setBackgroundColor(BG);
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView title = text("🦆 Duck Force Toolkit", 28, TEXT, true);
        root.addView(title);

        TextView subtitle = text("Three Torn tools. One Android app.", 15, MUTED, false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.topMargin = dp(5);
        subtitleParams.bottomMargin = dp(20);
        root.addView(subtitle, subtitleParams);

        addToolCard(Tool.AUDITOR);
        addToolCard(Tool.ARMORY);
        addToolCard(Tool.TRAIN);

        TextView footer = text("Prototype v0.1.0 • Original tools bundled locally", 12, MUTED, false);
        footer.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        footerParams.topMargin = dp(12);
        root.addView(footer, footerParams);

        setContentView(scroll);
    }

    private void addToolCard(Tool tool) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(rounded(PANEL, BORDER, 16));
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> openTool(tool));

        TextView name = text(tool.title, 20, TEXT, true);
        card.addView(name);

        TextView desc = text(tool.subtitle, 14, MUTED, false);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        descParams.topMargin = dp(6);
        descParams.bottomMargin = dp(14);
        card.addView(desc, descParams);

        Button open = new Button(this);
        open.setText("Open Tool");
        open.setAllCaps(false);
        open.setTextColor(Color.rgb(17, 17, 17));
        open.setTextSize(15);
        open.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        open.setBackground(rounded(ACCENT, ACCENT, 11));
        open.setOnClickListener(v -> openTool(tool));
        card.addView(open, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
        ));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(14);
        root.addView(card, cardParams);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void openTool(Tool tool) {
        currentTool = tool;
        destroyWebView();

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(BG);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(10), dp(8), dp(12), dp(8));
        toolbar.setBackgroundColor(PANEL);

        Button home = new Button(this);
        home.setText("← Home");
        home.setAllCaps(false);
        home.setTextColor(TEXT);
        home.setTextSize(14);
        home.setBackground(rounded(BG, BORDER, 10));
        home.setOnClickListener(v -> showHome());
        toolbar.addView(home, new LinearLayout.LayoutParams(dp(92), dp(44)));

        TextView barTitle = text(tool.title.replaceFirst("^[^ ]+\\s", ""), 16, TEXT, true);
        barTitle.setSingleLine(true);
        barTitle.setPadding(dp(12), 0, 0, 0);
        toolbar.addView(barTitle, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        page.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(false);
        settings.setUserAgentString(settings.getUserAgentString() + " DuckForceToolkit/0.1.0");

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .setDomain(tool.domain)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.setWebViewClient(new ToolWebViewClient(assetLoader, tool));
        webView.setBackgroundColor(BG);

        page.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        setContentView(page);
        webView.loadUrl("https://" + tool.domain + "/assets/tools/" + tool.asset);
    }

    private void destroyWebView() {
        if (webView != null) {
            webView.stopLoading();
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (currentTool != null) {
            if (webView != null && webView.canGoBack()) {
                webView.goBack();
            } else {
                showHome();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        destroyWebView();
        super.onDestroy();
    }

    private class ToolWebViewClient extends WebViewClient {
        private final WebViewAssetLoader assetLoader;
        private final Tool tool;

        ToolWebViewClient(WebViewAssetLoader assetLoader, Tool tool) {
            this.assetLoader = assetLoader;
            this.tool = tool;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if ("api.torn.com".equalsIgnoreCase(uri.getHost()) && "GET".equalsIgnoreCase(request.getMethod())) {
                WebResourceResponse proxied = proxyTornApi(uri);
                if (proxied != null) return proxied;
            }
            return assetLoader.shouldInterceptRequest(uri);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String host = uri.getHost();
            if (host != null && (host.equalsIgnoreCase(tool.domain) || host.equalsIgnoreCase("api.torn.com"))) {
                return false;
            }
            String scheme = uri.getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Unable to open link.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        }
    }

    private WebResourceResponse proxyTornApi(Uri uri) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(uri.toString()).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json, text/plain, */*");
            connection.setRequestProperty("User-Agent", "DuckForceToolkit/0.1.0 Android");

            int status = connection.getResponseCode();
            InputStream raw = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (raw == null) return null;

            final HttpURLConnection finalConnection = connection;
            InputStream stream = new FilterInputStream(raw) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        finalConnection.disconnect();
                    }
                }
            };

            String mime = "application/json";
            String charset = "UTF-8";
            String contentType = connection.getContentType();
            if (contentType != null) {
                String[] parts = contentType.split(";");
                if (parts.length > 0 && !parts[0].trim().isEmpty()) mime = parts[0].trim();
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (trimmed.toLowerCase().startsWith("charset=")) {
                        charset = trimmed.substring(8).trim();
                    }
                }
            }

            Map<String, String> headers = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                    headers.put(entry.getKey(), entry.getValue().get(0));
                }
            }
            headers.put("Access-Control-Allow-Origin", "*");
            headers.put("Access-Control-Allow-Methods", "GET, OPTIONS");
            headers.put("Cache-Control", "no-store");

            String reason = connection.getResponseMessage();
            if (reason == null || reason.isEmpty()) reason = status >= 400 ? "Error" : "OK";

            return new WebResourceResponse(mime, charset, status, reason, headers, stream);
        } catch (Exception e) {
            if (connection != null) connection.disconnect();
            return null;
        }
    }
}
