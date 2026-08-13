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

public class ToolkitActivity extends Activity {
    private static final String APP_VERSION = "0.3.0";
    private static final String LISTENER_VERSION = "0.3.0";
    private static final String DUCK_FORCE_NAME = "Duck Force";
    private static final int DUCK_FORCE_FACTION_ID = 0;

    private static final int BG = Color.rgb(8, 12, 18);
    private static final int BG_2 = Color.rgb(12, 18, 27);
    private static final int PANEL = Color.rgb(20, 27, 38);
    private static final int PANEL_2 = Color.rgb(27, 36, 49);
    private static final int BORDER = Color.rgb(49, 63, 81);
    private static final int ACCENT = Color.rgb(243, 184, 52);
    private static final int ACCENT_2 = Color.rgb(255, 216, 118);
    private static final int TEXT = Color.rgb(245, 248, 252);
    private static final int MUTED = Color.rgb(151, 163, 179);
    private static final int GOOD = Color.rgb(63, 185, 80);
    private static final int BAD = Color.rgb(248, 81, 73);
    private static final int BLUE = Color.rgb(88, 166, 255);

    private enum Screen { LOGIN, HOME, ACCESS, LISTENER, TOOL }
    private Screen screen = Screen.LOGIN;

    private LinearLayout root;
    private WebView webView;
    private Tool currentTool;
    private AuthSession session;
    private SecureApiKeyStore keyStore;

    private enum Tool {
        AUDITOR(
                "💊 Faction Xanax Auditor",
                "Review faction Xanax activity, member activity and contributor snapshots.",
                "auditor.duckforce.app",
                "xanax_auditor.html",
                AccessTier.ORANGE
        ),
        ARMORY(
                "📦 Xanax Armory Log",
                "Review Xanax use since the most recent major armory restock.",
                "armory.duckforce.app",
                "armory_log.html",
                AccessTier.ORANGE
        ),
        TRAIN(
                "🏋️ Company Train Calculator",
                "Calculate cash and Xanax payment combinations for company training sessions.",
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
        if (savedKey == null || savedKey.trim().isEmpty()) showLogin(null);
        else authenticate(savedKey, true);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable rounded(int color, int strokeColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeColor != Color.TRANSPARENT) drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private GradientDrawable gradient(int start, int end, int strokeColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{start, end}
        );
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeColor != Color.TRANSPARENT) drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private TextView text(String value, float sizeSp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setLineSpacing(0f, 1.08f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView sectionHeading(String value) {
        TextView heading = text(value, 13, MUTED, true);
        heading.setAllCaps(true);
        heading.setLetterSpacing(0.08f);
        return heading;
    }

    private Button primaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(Color.rgb(22, 17, 8));
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(gradient(ACCENT_2, ACCENT, ACCENT, 12));
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(TEXT);
        button.setTextSize(13);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(rounded(PANEL_2, BORDER, 11));
        return button;
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
        column.setPadding(dp(16), dp(16), dp(16), dp(28));
        column.setBackgroundColor(BG);
        scroll.addView(column, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return column;
    }

    private void spacer(LinearLayout parent, int heightDp) {
        View view = new View(this);
        parent.addView(view, new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private LinearLayout infoCard(String titleValue, String body, int accentColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(14), dp(15), dp(14));
        card.setBackground(rounded(PANEL, accentColor, 15));
        card.addView(text(titleValue, 17, TEXT, true));
        TextView desc = text(body, 13, MUTED, false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(5);
        card.addView(desc, p);
        return card;
    }

    private void showLogin(String errorMessage) {
        screen = Screen.LOGIN;
        session = null;
        currentTool = null;
        destroyWebView();

        ScrollView scroll = pageShell();
        LinearLayout column = contentColumn(scroll);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(dp(22), dp(28), dp(22), dp(26));
        hero.setBackground(gradient(Color.rgb(36, 50, 70), Color.rgb(18, 25, 37), BORDER, 24));

        TextView duck = text("🦆", 50, TEXT, false);
        duck.setGravity(Gravity.CENTER);
        duck.setBackground(gradient(Color.rgb(55, 68, 87), Color.rgb(28, 37, 52), ACCENT, 43));
        hero.addView(duck, new LinearLayout.LayoutParams(dp(86), dp(86)));

        TextView brand = text("DUCK FORCE", 13, ACCENT_2, true);
        brand.setLetterSpacing(0.22f);
        brand.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams brandParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        brandParams.topMargin = dp(16);
        hero.addView(brand, brandParams);

        TextView title = text("Torn Toolkit", 31, TEXT, true);
        title.setGravity(Gravity.CENTER);
        hero.addView(title);

        TextView tagline = text("One secure connection. The right tools for your Duck Force role.", 14, MUTED, false);
        tagline.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams taglineParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        taglineParams.topMargin = dp(7);
        hero.addView(tagline, taglineParams);

        column.addView(hero);
        spacer(column, 14);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(rounded(PANEL, BORDER, 18));
        card.addView(text("Connect your Torn account", 21, TEXT, true));

        TextView info = text(
                "Enter your own Torn API key once. The app verifies Duck Force membership and your faction position, then encrypts the key locally on this phone.",
                14, MUTED, false);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ip.topMargin = dp(7);
        ip.bottomMargin = dp(14);
        card.addView(info, ip);

        EditText input = new EditText(this);
        input.setHint("16-character Torn API key");
        input.setHintTextColor(MUTED);
        input.setTextColor(TEXT);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setBackground(rounded(BG_2, BORDER, 12));
        card.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        Button connect = primaryButton("Connect to Duck Force");
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        cp.topMargin = dp(12);
        card.addView(connect, cp);

        TextView status = text(
                errorMessage == null
                        ? "🔐 Encrypted locally  •  🛡️ Duck Force only  •  ⚡ One-key setup"
                        : errorMessage,
                12,
                errorMessage == null ? MUTED : BAD,
                false);
        status.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = dp(12);
        card.addView(status, sp);

        connect.setOnClickListener(v -> {
            String key = input.getText().toString().trim();
            if (key.isEmpty()) {
                status.setText("Enter an API key first.");
                status.setTextColor(BAD);
                return;
            }
            connect.setEnabled(false);
            connect.setText("Checking Duck Force access…");
            status.setText("Verifying Torn account and faction…");
            status.setTextColor(MUTED);
            authenticate(key, false);
        });

        column.addView(card);

        TextView version = text("Duck Force Toolkit v" + APP_VERSION, 11, MUTED, false);
        version.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vp.topMargin = dp(14);
        column.addView(version, vp);

        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    private void showLoading(String message) {
        screen = Screen.LOGIN;
        ScrollView scroll = pageShell();
        LinearLayout column = contentColumn(scroll);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(24), dp(34), dp(24), dp(34));
        card.setBackground(gradient(Color.rgb(32, 44, 62), Color.rgb(17, 24, 35), BORDER, 22));
        card.addView(text("🦆", 48, TEXT, false));

        TextView title = text("Duck Force Toolkit", 25, TEXT, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.topMargin = dp(10);
        card.addView(title, tp);

        TextView status = text(message, 14, MUTED, false);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stp.topMargin = dp(8);
        card.addView(status, stp);

        column.addView(card);
        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    private void authenticate(String apiKey, boolean fromSavedKey) {
        if (fromSavedKey) showLoading("Verifying saved Torn account…");

        new Thread(() -> {
            try {
                AuthSession result = TornApiClient.authenticate(apiKey);
                if (!isDuckForce(result)) throw new IOException("This build is restricted to Duck Force members.");
                keyStore.save(apiKey);
                session = result;
                runOnUiThread(this::showHome);
            } catch (Exception e) {
                if (fromSavedKey) keyStore.clear();
                String message = e.getMessage() == null ? "Unable to verify Torn account." : e.getMessage();
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

        addHomeHero();
        spacer(root, 16);
        root.addView(sectionHeading("Your tools"));
        spacer(root, 9);

        for (Tool tool : Tool.values()) {
            if (canAccess(tool)) addToolCard(tool);
        }

        if (session.canManageAccess()) {
            spacer(root, 4);
            root.addView(sectionHeading("Duck Force administration"));
            spacer(root, 9);
            addAccessControlCard();
        }

        TextView footer = text("v" + APP_VERSION + "  •  Duck Force role-aware toolkit", 11, MUTED, false);
        footer.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fp.topMargin = dp(10);
        root.addView(footer, fp);

        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    private void addHomeHero() {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(gradient(Color.rgb(35, 50, 70), Color.rgb(20, 28, 41), BORDER, 20));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView duck = text("🦆", 30, TEXT, false);
        duck.setGravity(Gravity.CENTER);
        duck.setBackground(rounded(Color.rgb(44, 56, 73), ACCENT, 22));
        top.addView(duck, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(dp(12), 0, 0, 0);
        names.addView(text("Welcome, " + session.playerName, 21, TEXT, true));
        names.addView(text(session.factionName + " • " + session.position, 13, MUTED, false));
        top.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        hero.addView(top);

        TextView access = text(session.accessLabel(), 12, session.tier.displayColor(), true);
        access.setGravity(Gravity.CENTER);
        access.setPadding(dp(10), dp(6), dp(10), dp(6));
        access.setBackground(rounded(BG_2, session.tier.displayColor(), 12));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ap.topMargin = dp(13);
        hero.addView(access, ap);

        String apiStatus = session.factionApiAccess
                ? "Faction permissions synced from Torn."
                : "Member-safe fallback active; refresh after faction permissions change.";
        TextView status = text(apiStatus, 12, MUTED, false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = dp(8);
        hero.addView(status, sp);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button refresh = secondaryButton("↻ Refresh access");
        refresh.setOnClickListener(v -> {
            String key = keyStore.load();
            if (key == null) showLogin("Saved API key is unavailable.");
            else authenticate(key, true);
        });
        actions.addView(refresh, new LinearLayout.LayoutParams(0, dp(44), 1f));

        Button logout = secondaryButton("Forget key");
        logout.setOnClickListener(v -> {
            keyStore.clear();
            session = null;
            showLogin(null);
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        lp.leftMargin = dp(8);
        actions.addView(logout, lp);

        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        actionsParams.topMargin = dp(14);
        hero.addView(actions, actionsParams);

        root.addView(hero);
    }

    private boolean canAccess(Tool tool) {
        if (session == null) return false;
        if (session.hasGlobalToolAccess()) return true;
        return session.tier.level >= tool.minimumTier.level;
    }

    private void addToolCard(Tool tool) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(15), dp(16), dp(15));
        card.setBackground(rounded(PANEL, BORDER, 17));
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> openTool(tool));

        card.addView(text(tool.title, 19, TEXT, true));

        TextView desc = text(tool.subtitle, 13, MUTED, false);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descParams.topMargin = dp(5);
        card.addView(desc, descParams);

        LinearLayout lower = new LinearLayout(this);
        lower.setOrientation(LinearLayout.HORIZONTAL);
        lower.setGravity(Gravity.CENTER_VERTICAL);

        TextView access = text(tool.minimumTier.label, 10, tool.minimumTier.displayColor(), true);
        access.setPadding(dp(8), dp(5), dp(8), dp(5));
        access.setBackground(rounded(BG_2, tool.minimumTier.displayColor(), 10));
        lower.addView(access);

        TextView open = text("Open  ›", 13, ACCENT_2, true);
        open.setGravity(Gravity.END);
        lower.addView(open, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout.LayoutParams lowerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lowerParams.topMargin = dp(12);
        card.addView(lower, lowerParams);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(10);
        root.addView(card, cardParams);
    }

    private void addAccessControlCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(gradient(Color.rgb(42, 37, 25), Color.rgb(26, 29, 33), ACCENT, 18));

        card.addView(text("⚙️ Leadership Control Center", 19, TEXT, true));
        TextView desc = text(
                "Rank access, Torn permission inspection and the Duck Force faction-chat listener live here.",
                13, MUTED, false);
        LinearLayout.LayoutParams d = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        d.topMargin = dp(6);
        d.bottomMargin = dp(12);
        card.addView(desc, d);

        Button open = primaryButton("Open Leadership Controls");
        open.setOnClickListener(v -> showAccessControl());
        card.addView(open, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = dp(12);
        root.addView(card, cp);
    }

    private void showAccessControl() {
        if (session == null || !session.canManageAccess()) {
            Toast.makeText(this, "Leader or Co-leader access required.", Toast.LENGTH_SHORT).show();
            return;
        }

        screen = Screen.ACCESS;
        ScrollView scroll = pageShell();
        LinearLayout column = contentColumn(scroll);

        Button back = secondaryButton("← Home");
        back.setOnClickListener(v -> showHome());
        column.addView(back, new LinearLayout.LayoutParams(dp(104), dp(42)));

        TextView title = text("Leadership Controls", 26, TEXT, true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.topMargin = dp(14);
        column.addView(title, tp);
        column.addView(text("Duck Force administration", 13, MUTED, false));
        spacer(column, 14);

        LinearLayout listener = new LinearLayout(this);
        listener.setOrientation(LinearLayout.VERTICAL);
        listener.setPadding(dp(16), dp(16), dp(16), dp(16));
        listener.setBackground(rounded(PANEL, BLUE, 17));
        listener.addView(text("💬 Faction Chat Listener", 19, TEXT, true));

        TextView ldesc = text(
                "Retroactively scans faction-chat messages Torn has loaded and recognizes balance/banker requests before they enter the banking workflow.",
                13, MUTED, false);
        LinearLayout.LayoutParams ldp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ldp.topMargin = dp(6);
        ldp.bottomMargin = dp(11);
        listener.addView(ldesc, ldp);

        Button listenerGuide = primaryButton("Listener Details & Install Guide");
        listenerGuide.setOnClickListener(v -> showListenerGuide());
        listener.addView(listenerGuide, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        column.addView(listener);

        spacer(column, 14);
        column.addView(sectionHeading("Torn faction positions"));
        spacer(column, 9);

        if (session.positions == null || session.positions.length() == 0) {
            column.addView(infoCard(
                    "Faction position details unavailable",
                    "The current key did not return /faction/positions. That selection requires Faction API Access.",
                    BAD
            ));
        } else {
            for (int i = 0; i < session.positions.length(); i++) {
                JSONObject pos = session.positions.optJSONObject(i);
                if (pos != null) addPositionCard(column, pos);
            }
        }

        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    private void addPositionCard(LinearLayout column, JSONObject pos) {
        String name = pos.optString("name", "Unnamed position");
        JSONArray abilities = pos.optJSONArray("abilities");
        AccessTier tier = AccessPolicy.tierForAbilities(abilities);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(13), dp(14), dp(13));
        card.setBackground(rounded(PANEL, tier.displayColor(), 14));

        card.addView(text(name + (pos.optBoolean("is_default", false) ? " • Default" : ""), 17, TEXT, true));

        TextView access = text(tier.label, 12, tier.displayColor(), true);
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

        TextView abilitiesView = text(abilityText.toString(), 11, MUTED, false);
        LinearLayout.LayoutParams avp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        avp.topMargin = dp(7);
        card.addView(abilitiesView, avp);

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = dp(9);
        column.addView(card, cp);
    }

    private void showListenerGuide() {
        if (session == null || !session.canManageAccess()) {
            Toast.makeText(this, "Leader or Co-leader access required.", Toast.LENGTH_SHORT).show();
            return;
        }

        screen = Screen.LISTENER;
        ScrollView scroll = pageShell();
        LinearLayout column = contentColumn(scroll);

        Button back = secondaryButton("← Admin");
        back.setOnClickListener(v -> showAccessControl());
        column.addView(back, new LinearLayout.LayoutParams(dp(104), dp(42)));

        TextView title = text("💬 Duck Force Chat Listener", 25, TEXT, true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.topMargin = dp(14);
        column.addView(title, tp);
        column.addView(text("Bundled listener source • v" + LISTENER_VERSION, 13, MUTED, false));
        spacer(column, 14);

        column.addView(infoCard(
                "Recognized payout phrases",
                "The listener includes patterns for “banker”, “bank please”, “can I get my balance please?”, “balance check please”, “what's my balance?”, “withdraw”, “cash out”, and amount-specific requests such as “bank 25m”.",
                BLUE
        ));
        spacer(column, 10);

        column.addView(infoCard(
                "Retroactive scanning",
                "When faction chat is actively open, the listener scans messages Torn has already loaded, then watches newly loaded and newly arriving messages. If you scroll upward and Torn loads older history, those messages are scanned too. Local fingerprints prevent the same chat message from being processed repeatedly.",
                GOOD
        ));
        spacer(column, 10);

        column.addView(infoCard(
                "Low-balance reconciliation",
                "The banking design uses $1,000,000 as the default likely-already-paid threshold. A retroactive request whose current faction balance is below that amount is kept in the audit history but should not clutter the normal pending-payout queue. The threshold will be adjustable by leadership.",
                ACCENT
        ));
        spacer(column, 12);

        column.addView(sectionHeading("TornPDA install flow"));
        spacer(column, 8);
        column.addView(infoCard(
                "Mobile-first installation",
                "1. Open TornPDA.\n\n2. Go to Settings → Advanced browser settings → Manage scripts.\n\n3. Add/import the Duck Force Banking Chat Listener and enable it.\n\n4. Use Torn normally. The listener only operates on faction-chat content Torn has loaded while the chat is actively being viewed.\n\nThe listener source is bundled inside this app project at assets/listener/duckforce_chat_listener.user.js. The final one-tap export button will be enabled when the shared banking queue endpoint is deployed.",
                BLUE
        ));

        TextView privacy = text(
                "Privacy design: only messages matching a banking-request pattern are eligible for the queue; the listener is not designed to upload general faction chat.",
                11, MUTED, false);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pp.topMargin = dp(10);
        column.addView(privacy, pp);

        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void openTool(Tool tool) {
        if (!canAccess(tool)) {
            Toast.makeText(this, "Your Duck Force access level does not allow this tool.", Toast.LENGTH_SHORT).show();
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

        Button home = secondaryButton("← Home");
        home.setOnClickListener(v -> showHome());
        toolbar.addView(home, new LinearLayout.LayoutParams(dp(92), dp(44)));

        TextView barTitle = text(tool.title.replaceFirst("^[^ ]+\\s", ""), 15, TEXT, true);
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
        settings.setUserAgentString(settings.getUserAgentString() + " DuckForceToolkit/" + APP_VERSION);

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
                + "e.placeholder='Using Duck Force Toolkit API key';"
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
        } else if (screen == Screen.LISTENER) {
            showAccessControl();
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
            if (host != null && (host.equalsIgnoreCase(tool.domain) || host.equalsIgnoreCase("api.torn.com"))) {
                return false;
            }
            String scheme = uri.getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception e) {
                    Toast.makeText(ToolkitActivity.this, "Unable to open link.", Toast.LENGTH_SHORT).show();
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
            connection.setRequestProperty("User-Agent", "DuckForceToolkit/" + APP_VERSION + " Android");

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
                    if (trimmed.toLowerCase().startsWith("charset=")) charset = trimmed.substring(8).trim();
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
