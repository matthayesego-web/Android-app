package com.matthayesego.duckforcetoolkit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String APP_VERSION = "0.2.0";
    private static final String DUCK_FORCE_NAME = "Duck Force";

    // TODO: replace 0 with Duck Force's numeric faction ID after the first verified
    // login reveals it. Until then, the exact faction name is enforced.
    private static final int DUCK_FORCE_FACTION_ID = 0;

    private static final int BG = Color.rgb(13, 17, 23);
    private static final int PANEL = Color.rgb(22, 27, 34);
    private static final int BORDER = Color.rgb(48, 54, 61);
    private static final int ACCENT = Color.rgb(240, 180, 41);
    private static final int TEXT = Color.rgb(240, 246, 252);
    private static final int MUTED = Color.rgb(139, 148, 158);
    private static final int BAD = Color.rgb(248, 81, 73);

    private enum Screen { LOGIN, HOME, ACCESS, TOOL }
    private Screen screen = Screen.LOGIN;

    private LinearLayout root;
    private WebView webView;
    private Tool currentTool;
    private AuthSession session;
    private SecureApiKeyStore keyStore;

    private enum Tool {
        AUDITOR(
                "💊 Faction Xanax Auditor",
                "Audit faction members and Xanax activity with saved contributor snapshots.",
                "auditor.duckforce.app",
                "xanax_auditor.html",
                AccessTier.ORANGE
        ),
        ARMORY(
                "📦 Xanax Armory Log",
                "Find Xanax use since the most recent large faction-armory deposit.",
                "armory.duckforce.app",
                "armory_log.html",
                AccessTier.ORANGE
        ),
        TRAIN(
                "🚂 Train Payment Calculator",
                "Calculate train payments using cash and Xanax combinations.",
                "train.duckforce.app",
                "train_calculator.html",
                AccessTier.GREEN
        );

        final String title;
        final String subtitle;
        final String domain;
        final String asset;
        final AccessTier minimumTier;

        Tool(String title, String subtitle, String domain, String asset, AccessTier minimumTier) {
            this.title = title;
            this.subtitle = subtitle;
            this.domain = domain;
            this.asset = asset;
            this.minimumTier = minimumTier;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        keyStore = new SecureApiKeyStore(this);

        String savedKey = keyStore.load();
        if (savedKey == null || savedKey.trim().isEmpty()) {
            showLogin(null);
        } else {
            authenticate(savedKey, true);
        }
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

    @SuppressWarnings("deprecation")
    private void applySystemBarInsets(View view) {
        final int baseLeft = view.getPaddingLeft();
        final int baseTop = view.getPaddingTop();
        final int baseRight = view.getPaddingRight();
        final int baseBottom = view.getPaddingBottom();

        view.setOnApplyWindowInsetsListener((v, insets) -> {
            int insetLeft = insets.getSystemWindowInsetLeft();
            int insetTop = insets.getSystemWindowInsetTop();
            int insetRight = insets.getSystemWindowInsetRight();
            int insetBottom = insets.getSystemWindowInsetBottom();

            if (android.os.Build.VERSION.SDK_INT >= 28 && insets.getDisplayCutout() != null) {
                insetLeft = Math.max(insetLeft, insets.getDisplayCutout().getSafeInsetLeft());
                insetTop = Math.max(insetTop, insets.getDisplayCutout().getSafeInsetTop());
                insetRight = Math.max(insetRight, insets.getDisplayCutout().getSafeInsetRight());
                insetBottom = Math.max(insetBottom, insets.getDisplayCutout().getSafeInsetBottom());
            }

            v.setPadding(baseLeft + insetLeft, baseTop + insetTop,
                    baseRight + insetRight, baseBottom + insetBottom);
            return insets;
        });
    }

    private ScrollView pageShell() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        applySystemBarInsets(scroll);
        return scroll;
    }

    private LinearLayout contentColumn(ScrollView scroll) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(18), dp(22), dp(18), dp(28));
        column.setBackgroundColor(BG);
        scroll.addView(column, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return column;
    }

    private Button smallButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(TEXT);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(rounded(BG, BORDER, 10));
        return b;
    }

    private void showLogin(String errorMessage) {
        screen = Screen.LOGIN;
        session = null;
        currentTool = null;
        destroyWebView();

        ScrollView scroll = pageShell();
        LinearLayout column = contentColumn(scroll);

        column.addView(text("🦆 Duck Force Toolkit", 28, TEXT, true));

        TextView subtitle = text("Connect your Torn account to continue.", 15, MUTED, false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(5);
        subtitleParams.bottomMargin = dp(22);
        column.addView(subtitle, subtitleParams);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(rounded(PANEL, BORDER, 16));

        card.addView(text("Torn API Key", 20, TEXT, true));

        TextView info = text(
                "Enter your own Torn API key. The app verifies your identity, confirms Duck Force membership, and determines access from your faction position and Torn permissions.",
                14, MUTED, false);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infoParams.topMargin = dp(7);
        infoParams.bottomMargin = dp(14);
        card.addView(info, infoParams);

        EditText input = new EditText(this);
        input.setHint("Paste Torn API key");
        input.setHintTextColor(MUTED);
        input.setTextColor(TEXT);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setPadding(dp(12), 0, dp(12), 0);
        input.setBackground(rounded(BG, BORDER, 10));
        card.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        Button connect = new Button(this);
        connect.setText("Connect Torn Account");
        connect.setAllCaps(false);
        connect.setTextColor(Color.rgb(17, 17, 17));
        connect.setTextSize(15);
        connect.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        connect.setBackground(rounded(ACCENT, ACCENT, 11));
        LinearLayout.LayoutParams connectParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        connectParams.topMargin = dp(12);
        card.addView(connect, connectParams);

        TextView status = text(
                errorMessage == null
                        ? "Your key is encrypted on this device using Android Keystore."
                        : errorMessage,
                13,
                errorMessage == null ? MUTED : BAD,
                false);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = dp(12);
        card.addView(status, statusParams);

        connect.setOnClickListener(v -> {
            String key = input.getText().toString().trim();
            if (key.isEmpty()) {
                status.setText("Enter an API key first.");
                status.setTextColor(BAD);
                return;
            }
            connect.setEnabled(false);
            connect.setText("Checking…");
            status.setText("Verifying Torn account and faction…");
            status.setTextColor(MUTED);
            authenticate(key, false);
        });

        column.addView(card);
        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    private void showLoading(String message) {
        screen = Screen.LOGIN;
        ScrollView scroll = pageShell();
        LinearLayout column = contentColumn(scroll);
        column.addView(text("🦆 Duck Force Toolkit", 28, TEXT, true));

        TextView status = text(message, 16, MUTED, false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(16);
        column.addView(status, p);

        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    private void authenticate(String apiKey, boolean fromSavedKey) {
        if (fromSavedKey) showLoading("Verifying saved Torn account…");

        new Thread(() -> {
            try {
                AuthSession result = TornApiClient.authenticate(apiKey);
                if (!isDuckForce(result)) {
                    throw new IOException("This build is restricted to Duck Force members.");
                }
                keyStore.save(apiKey);
                session = result;
                runOnUiThread(this::showHome);
            } catch (Exception e) {
                if (fromSavedKey) keyStore.clear();
                String message = e.getMessage() == null
                        ? "Unable to verify Torn account."
                        : e.getMessage();
                runOnUiThread(() -> showLogin(message));
            }
        }).start();
    }

    private boolean isDuckForce(AuthSession result) {
        if (result == null || result.factionName == null) return false;
        if (!DUCK_FORCE_NAME.equalsIgnoreCase(result.factionName.trim())) return false;
        return DUCK_FORCE_FACTION_ID == 0 || DUCK_FORCE_FACTION_ID == result.factionId;
    }

    private void showHome() {
        if (session == null) {
            showLogin("Connect a Torn API key to continue.");
            return;
        }

        screen = Screen.HOME;
        currentTool = null;
        destroyWebView();

        ScrollView scroll = pageShell();
        root = contentColumn(scroll);

        root.addView(text("🦆 Duck Force Toolkit", 28, TEXT, true));

        TextView subtitle = text("Authenticated tools for Duck Force.", 15, MUTED, false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(5);
        subtitleParams.bottomMargin = dp(14);
        root.addView(subtitle, subtitleParams);

        addAccountCard();

        TextView toolsHeading = text("Tools", 18, TEXT, true);
        LinearLayout.LayoutParams hParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hParams.topMargin = dp(8);
        hParams.bottomMargin = dp(10);
        root.addView(toolsHeading, hParams);

        for (Tool tool : Tool.values()) {
            if (canAccess(tool)) addToolCard(tool);
        }

        if (session.canManageAccess()) addAccessControlCard();

        TextView footer = text("Prototype v" + APP_VERSION + " • Torn-linked access foundation",
                12, MUTED, false);
        footer.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerParams.topMargin = dp(12);
        root.addView(footer, footerParams);

        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    private void addAccountCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(rounded(PANEL, BORDER, 16));

        card.addView(text(session.playerName + " [" + session.playerId + "]", 20, TEXT, true));

        TextView faction = text(
                session.factionName + " [" + session.factionId + "] • " + session.position,
                14, MUTED, false);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fp.topMargin = dp(4);
        card.addView(faction, fp);

        TextView access = text("Access: " + session.accessLabel(),
                14, session.tier.displayColor(), true);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ap.topMargin = dp(8);
        card.addView(access, ap);

        String apiStatus = session.factionApiAccess
                ? "Faction API Access detected — rank abilities synced."
                : "No Faction API Access detected — member fallback applies unless you are Leader/Co-leader.";
        TextView api = text(apiStatus, 12, MUTED, false);
        LinearLayout.LayoutParams apiParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        apiParams.topMargin = dp(6);
        card.addView(api, apiParams);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        Button refresh = smallButton("Refresh Access");
        refresh.setOnClickListener(v -> {
            String key = keyStore.load();
            if (key == null) showLogin("Saved API key is unavailable.");
            else authenticate(key, true);
        });
        buttons.addView(refresh, new LinearLayout.LayoutParams(0, dp(46), 1f));

        Button logout = smallButton("Forget Key");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        lp.leftMargin = dp(8);
        buttons.addView(logout, lp);
        logout.setOnClickListener(v -> {
            keyStore.clear();
            session = null;
            showLogin(null);
        });

        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        bp.topMargin = dp(12);
        card.addView(buttons, bp);

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = dp(14);
        root.addView(card, cp);
    }

    private boolean canAccess(Tool tool) {
        if (session == null) return false;
        if (session.hasGlobalToolAccess()) return true;
        return session.tier.level >= tool.minimumTier.level;
    }

    private void addToolCard(Tool tool) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(rounded(PANEL, BORDER, 16));
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> openTool(tool));

        card.addView(text(tool.title, 20, TEXT, true));

        TextView desc = text(tool.subtitle, 14, MUTED, false);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descParams.topMargin = dp(6);
        descParams.bottomMargin = dp(8);
        card.addView(desc, descParams);

        TextView required = text("Minimum access: " + tool.minimumTier.label,
                11, tool.minimumTier.displayColor(), false);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.bottomMargin = dp(12);
        card.addView(required, rp);

        Button open = new Button(this);
        open.setText("Open Tool");
        open.setAllCaps(false);
        open.setTextColor(Color.rgb(17, 17, 17));
        open.setTextSize(15);
        open.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        open.setBackground(rounded(ACCENT, ACCENT, 11));
        open.setOnClickListener(v -> openTool(tool));
        card.addView(open, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(14);
        root.addView(card, cardParams);
    }

    private void addAccessControlCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(rounded(PANEL, ACCENT, 16));

        card.addView(text("⚙️ Rank Access Control", 20, TEXT, true));

        TextView desc = text(
                "Leader/Co-leader view: inspect Torn positions, abilities, and detected permission colors. Shared app-by-rank overrides are prepared for the backend layer.",
                14, MUTED, false);
        LinearLayout.LayoutParams d = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        d.topMargin = dp(6);
        d.bottomMargin = dp(14);
        card.addView(desc, d);

        Button open = new Button(this);
        open.setText("Open Access Control");
        open.setAllCaps(false);
        open.setTextColor(Color.rgb(17, 17, 17));
        open.setTextSize(15);
        open.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        open.setBackground(rounded(ACCENT, ACCENT, 11));
        open.setOnClickListener(v -> showAccessControl());
        card.addView(open, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(14);
        root.addView(card, cardParams);
    }

    private void showAccessControl() {
        if (session == null || !session.canManageAccess()) {
            Toast.makeText(this, "Leader or Co-leader access required.", Toast.LENGTH_SHORT).show();
            return;
        }

        screen = Screen.ACCESS;
        ScrollView scroll = pageShell();
        LinearLayout column = contentColumn(scroll);

        Button back = smallButton("← Home");
        back.setOnClickListener(v -> showHome());
        column.addView(back, new LinearLayout.LayoutParams(dp(100), dp(44)));

        TextView title = text("⚙️ Rank Access Control", 25, TEXT, true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.topMargin = dp(16);
        column.addView(title, tp);

        TextView explainer = text(
                "Torn remains the authority for faction membership and rank abilities. Green is member access; Orange is elevated; Red and Black receive global tool access. Leader and Co-leader always receive global access and are the only roles allowed to manage the future shared app-access matrix.",
                14, MUTED, false);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ep.topMargin = dp(7);
        ep.bottomMargin = dp(14);
        column.addView(explainer, ep);

        if (session.positions == null || session.positions.length() == 0) {
            column.addView(text(
                    "Torn did not return faction position details for this key. /faction/positions requires Faction API Access.",
                    14, BAD, false));
        } else {
            for (int i = 0; i < session.positions.length(); i++) {
                JSONObject pos = session.positions.optJSONObject(i);
                if (pos != null) addPositionCard(column, pos);
            }
        }

        TextView backend = text(
                "Shared override layer: backend source is included in this repository. Once deployed, this screen will gain per-rank app toggles that apply to every Duck Force member's phone.",
                13, ACCENT, false);
        LinearLayout.LayoutParams bep = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bep.topMargin = dp(14);
        column.addView(backend, bep);

        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    private void addPositionCard(LinearLayout column, JSONObject pos) {
        String name = pos.optString("name", "Unnamed position");
        JSONArray abilities = pos.optJSONArray("abilities");
        AccessTier tier = AccessPolicy.tierForAbilities(abilities);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(rounded(PANEL, tier.displayColor(), 14));

        card.addView(text(name + (pos.optBoolean("is_default", false) ? " • Default" : ""),
                18, TEXT, true));

        TextView access = text("Detected Torn tier: " + tier.label,
                13, tier.displayColor(), true);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ap.topMargin = dp(4);
        card.addView(access, ap);

        StringBuilder abilityText = new StringBuilder();
        if (abilities != null) {
            for (int i = 0; i < abilities.length(); i++) {
                if (i > 0) abilityText.append(" • ");
                abilityText.append(abilities.optString(i));
            }
        }
        if (abilityText.length() == 0) abilityText.append("No listed abilities");

        TextView abilitiesView = text(abilityText.toString(), 12, MUTED, false);
        LinearLayout.LayoutParams avp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        avp.topMargin = dp(8);
        card.addView(abilitiesView, avp);

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = dp(10);
        column.addView(card, cp);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void openTool(Tool tool) {
        if (!canAccess(tool)) {
            Toast.makeText(this, "Your Duck Force access level does not allow this tool.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        screen = Screen.TOOL;
        currentTool = tool;
        destroyWebView();

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(BG);
        applySystemBarInsets(page);

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
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        page.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

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
        settings.setUserAgentString(settings.getUserAgentString()
                + " DuckForceToolkit/" + APP_VERSION);

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .setDomain(tool.domain)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.setWebViewClient(new ToolWebViewClient(assetLoader, tool));
        webView.setBackgroundColor(BG);

        page.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(page);
        page.requestApplyInsets();
        webView.loadUrl("https://" + tool.domain + "/assets/tools/" + tool.asset);
    }

    private void injectSavedApiKey(WebView view) {
        String key = keyStore.load();
        if (key == null || key.isEmpty()) return;

        String quoted = JSONObject.quote(key);
        String js = "(function(){var e=document.getElementById('key');"
                + "if(e){e.value=" + quoted + ";e.readOnly=true;"
                + "e.placeholder='Using app API key';"
                + "e.title='API key supplied by Duck Force Toolkit';}})();";
        view.evaluateJavascript(js, null);
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
        if (screen == Screen.TOOL) {
            if (webView != null && webView.canGoBack()) webView.goBack();
            else showHome();
        } else if (screen == Screen.ACCESS) {
            showHome();
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
            if ("api.torn.com".equalsIgnoreCase(uri.getHost())
                    && "GET".equalsIgnoreCase(request.getMethod())) {
                WebResourceResponse proxied = proxyTornApi(uri);
                if (proxied != null) return proxied;
            }
            return assetLoader.shouldInterceptRequest(uri);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (url != null && url.contains(tool.domain)) injectSavedApiKey(view);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String host = uri.getHost();
            if (host != null
                    && (host.equalsIgnoreCase(tool.domain)
                    || host.equalsIgnoreCase("api.torn.com"))) {
                return false;
            }

            String scheme = uri.getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Unable to open link.",
                            Toast.LENGTH_SHORT).show();
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
            connection.setRequestProperty("User-Agent",
                    "DuckForceToolkit/" + APP_VERSION + " Android");

            int status = connection.getResponseCode();
            InputStream raw = status >= 400
                    ? connection.getErrorStream()
                    : connection.getInputStream();
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
            for (Map.Entry<String, List<String>> entry
                    : connection.getHeaderFields().entrySet()) {
                if (entry.getKey() != null
                        && entry.getValue() != null
                        && !entry.getValue().isEmpty()) {
                    headers.put(entry.getKey(), entry.getValue().get(0));
                }
            }
            headers.put("Access-Control-Allow-Origin", "*");
            headers.put("Access-Control-Allow-Methods", "GET, OPTIONS");
            headers.put("Cache-Control", "no-store");

            String reason = connection.getResponseMessage();
            if (reason == null || reason.isEmpty()) {
                reason = status >= 400 ? "Error" : "OK";
            }

            return new WebResourceResponse(
                    mime, charset, status, reason, headers, stream);
        } catch (Exception e) {
            if (connection != null) connection.disconnect();
            return null;
        }
    }
}
