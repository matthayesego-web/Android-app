package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

public class CompanionActivity extends Activity {
    private static final String APP_VERSION = "0.4.0";
    private static final String DUCK_FORCE_NAME = "Duck Force";

    private static final int BG = Color.rgb(8, 12, 18);
    private static final int BG2 = Color.rgb(12, 18, 27);
    private static final int PANEL = Color.rgb(20, 27, 38);
    private static final int PANEL2 = Color.rgb(27, 36, 49);
    private static final int BORDER = Color.rgb(49, 63, 81);
    private static final int ACCENT = Color.rgb(243, 184, 52);
    private static final int ACCENT2 = Color.rgb(255, 216, 118);
    private static final int TEXT = Color.rgb(245, 248, 252);
    private static final int MUTED = Color.rgb(151, 163, 179);
    private static final int GOOD = Color.rgb(63, 185, 80);
    private static final int BAD = Color.rgb(248, 81, 73);
    private static final int BLUE = Color.rgb(88, 166, 255);

    private enum Screen { LOGIN, HOME, BANKING, LEADERSHIP, DEVELOPER }
    private Screen screen = Screen.LOGIN;
    private AuthSession session;
    private SecureApiKeyStore keyStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        keyStore = new SecureApiKeyStore(this);
        String saved = keyStore.load();
        if (saved == null || saved.trim().isEmpty()) showLogin(null);
        else authenticate(saved, true);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable rounded(int color, int strokeColor, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        if (strokeColor != Color.TRANSPARENT) d.setStroke(dp(1), strokeColor);
        return d;
    }

    private GradientDrawable gradient(int start, int end, int stroke, int radiusDp) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        d.setCornerRadius(dp(radiusDp));
        if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke);
        return d;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setLineSpacing(0f, 1.08f);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private TextView section(String value) {
        TextView t = text(value.toUpperCase(), 12, MUTED, true);
        t.setLetterSpacing(0.08f);
        return t;
    }

    private Button primary(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.rgb(23, 17, 7));
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(gradient(ACCENT2, ACCENT, ACCENT, 12));
        return b;
    }

    private Button secondary(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(TEXT);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(rounded(PANEL2, BORDER, 11));
        return b;
    }

    @SuppressWarnings("deprecation")
    private ScrollView shell() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        int l = dp(16), t = dp(16), r = dp(16), b = dp(28);
        scroll.setPadding(l, t, r, b);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(l + insets.getSystemWindowInsetLeft(), t + insets.getSystemWindowInsetTop(),
                    r + insets.getSystemWindowInsetRight(), b + insets.getSystemWindowInsetBottom());
            return insets;
        });
        return scroll;
    }

    private LinearLayout column(ScrollView scroll) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(c, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return c;
    }

    private void spacer(LinearLayout c, int dp) {
        View v = new View(this);
        c.addView(v, new LinearLayout.LayoutParams(1, this.dp(dp)));
    }

    private LinearLayout card(String title, String body, int stroke) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(15), dp(16), dp(15));
        c.setBackground(rounded(PANEL, stroke, 17));
        c.addView(text(title, 18, TEXT, true));
        if (body != null && !body.isEmpty()) {
            TextView b = text(body, 13, MUTED, false);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            p.topMargin = dp(5);
            c.addView(b, p);
        }
        return c;
    }

    private void addCard(LinearLayout parent, LinearLayout card) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(10);
        parent.addView(card, p);
    }

    private void showLogin(String error) {
        screen = Screen.LOGIN;
        session = null;
        ScrollView scroll = shell();
        LinearLayout c = column(scroll);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(dp(22), dp(28), dp(22), dp(26));
        hero.setBackground(gradient(Color.rgb(35, 49, 69), Color.rgb(17, 24, 35), ACCENT, 24));
        TextView icon = text("🦆", 52, TEXT, false);
        icon.setGravity(Gravity.CENTER);
        hero.addView(icon);
        TextView brand = text("DUCK FORCE", 13, ACCENT2, true);
        brand.setLetterSpacing(0.22f);
        brand.setGravity(Gravity.CENTER);
        hero.addView(brand);
        TextView title = text("Faction Companion", 30, TEXT, true);
        title.setGravity(Gravity.CENTER);
        hero.addView(title);
        TextView sub = text("Your Duck Force tools, requests and leadership access in one place.", 14, MUTED, false);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = dp(7);
        hero.addView(sub, sp);
        c.addView(hero);
        spacer(c, 14);

        LinearLayout login = card("Connect your Torn account", "Your key verifies your identity and Duck Force membership, then stays encrypted on this device.", BORDER);
        EditText key = new EditText(this);
        key.setHint("16-character Torn API key");
        key.setHintTextColor(MUTED);
        key.setTextColor(TEXT);
        key.setSingleLine(true);
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setPadding(dp(14), 0, dp(14), 0);
        key.setBackground(rounded(BG2, BORDER, 12));
        LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        kp.topMargin = dp(14);
        login.addView(key, kp);

        Button connect = primary("Connect to Duck Force");
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        bp.topMargin = dp(12);
        login.addView(connect, bp);

        TextView status = text(error == null ? "🔐 Encrypted locally  •  Duck Force only  •  v" + APP_VERSION : error,
                12, error == null ? MUTED : BAD, false);
        status.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams stp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stp.topMargin = dp(10);
        login.addView(status, stp);

        connect.setOnClickListener(v -> {
            String value = key.getText().toString().trim();
            if (value.isEmpty()) {
                status.setText("Enter an API key first.");
                status.setTextColor(BAD);
                return;
            }
            connect.setEnabled(false);
            connect.setText("Checking access…");
            authenticate(value, false);
        });
        c.addView(login);
        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    private void showLoading() {
        ScrollView scroll = shell();
        LinearLayout c = column(scroll);
        LinearLayout load = card("🦆 Duck Force Companion", "Verifying your saved Torn account…", ACCENT);
        c.addView(load);
        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    private void authenticate(String key, boolean saved) {
        if (saved) showLoading();
        new Thread(() -> {
            try {
                AuthSession result = TornApiClient.authenticate(key);
                if (result.factionName == null || !DUCK_FORCE_NAME.equalsIgnoreCase(result.factionName.trim())) {
                    throw new IOException("This build is restricted to Duck Force members.");
                }
                keyStore.save(key);
                session = result;
                runOnUiThread(this::showHome);
            } catch (Exception e) {
                if (saved) keyStore.clear();
                String message = e.getMessage() == null ? "Unable to verify Torn account." : e.getMessage();
                runOnUiThread(() -> showLogin(message));
            }
        }).start();
    }

    private void showHome() {
        if (session == null) { showLogin("Connect your Torn account to continue."); return; }
        screen = Screen.HOME;
        ScrollView scroll = shell();
        LinearLayout c = column(scroll);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(gradient(Color.rgb(34, 49, 69), Color.rgb(18, 27, 40), BORDER, 20));
        hero.addView(text("Welcome back, " + session.playerName, 22, TEXT, true));
        TextView meta = text(session.factionName + " • " + session.position + " • " + session.accessLabel(), 13, MUTED, false);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mp.topMargin = dp(5);
        hero.addView(meta, mp);
        if (AppRoles.isOwner(session)) {
            TextView owner = text("OWNER / DEVELOPER", 11, ACCENT2, true);
            owner.setPadding(dp(9), dp(5), dp(9), dp(5));
            owner.setBackground(rounded(BG2, ACCENT, 11));
            LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            op.topMargin = dp(10);
            hero.addView(owner, op);
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button refresh = secondary("↻ Refresh");
        refresh.setOnClickListener(v -> {
            String key = keyStore.load();
            if (key != null) authenticate(key, true);
        });
        actions.addView(refresh, new LinearLayout.LayoutParams(0, dp(44), 1f));
        Button logout = secondary("Forget key");
        logout.setOnClickListener(v -> { keyStore.clear(); session = null; showLogin(null); });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        lp.leftMargin = dp(8);
        actions.addView(logout, lp);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        ap.topMargin = dp(14);
        hero.addView(actions, ap);
        c.addView(hero);

        spacer(c, 16);
        c.addView(section("Faction companion"));
        spacer(c, 8);

        LinearLayout banking = card("💰 Banking", "Request your faction balance/payout and review your request history.", BLUE);
        banking.setOnClickListener(v -> showBanking());
        banking.setClickable(true);
        addCard(c, banking);

        if (canUseFactionTool(AccessTier.ORANGE)) {
            addToolCard(c, "📦 Armory Log", "Faction Xanax armory activity.", "ARMORY", AccessTier.ORANGE);
            addToolCard(c, "💊 Faction Xanax Auditor", "Faction Xanax usage and contributor snapshots.", "AUDITOR", AccessTier.ORANGE);
        }

        if (session.canManageAccess()) {
            LinearLayout leadership = card("⚙️ Leadership Controls", "Faction permissions, listener guidance and administrative companion settings.", ACCENT);
            leadership.setClickable(true);
            leadership.setOnClickListener(v -> showLeadership());
            addCard(c, leadership);
        }

        if (AppRoles.isOwner(session)) {
            spacer(c, 4);
            c.addView(section("My tools"));
            spacer(c, 8);
            addToolCard(c, "🏋️ Company Train Calculator", "Private company-training payment calculator.", "TRAIN", AccessTier.GREEN);

            LinearLayout dev = card("🛠 Developer Console", "Private tools, owner status and future per-player grants.", ACCENT);
            dev.setClickable(true);
            dev.setOnClickListener(v -> showDeveloper());
            addCard(c, dev);
        }

        TextView footer = text("Duck Force Companion v" + APP_VERSION + " • Companion-first foundation", 11, MUTED, false);
        footer.setGravity(Gravity.CENTER_HORIZONTAL);
        c.addView(footer);
        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    private boolean canUseFactionTool(AccessTier minimum) {
        return session != null && (session.hasGlobalToolAccess() || session.tier.level >= minimum.level);
    }

    private void addToolCard(LinearLayout parent, String title, String body, String tool, AccessTier tier) {
        LinearLayout c = card(title, body, tier.displayColor());
        TextView access = text("Requires " + tier.label + " • Tap to open", 11, tier.displayColor(), true);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ap.topMargin = dp(10);
        c.addView(access, ap);
        c.setClickable(true);
        c.setOnClickListener(v -> openTool(tool));
        addCard(parent, c);
    }

    private void openTool(String tool) {
        Intent i = new Intent(this, ToolHostActivity.class);
        i.putExtra(ToolHostActivity.EXTRA_TOOL, tool);
        startActivity(i);
    }

    private void showBanking() {
        screen = Screen.BANKING;
        ScrollView scroll = shell();
        LinearLayout c = column(scroll);
        addBack(c, "Banking");

        addCard(c, card("Banking Companion — v0.4 prototype",
                "This test stores requests on this phone only. The shared Duck Force queue and live balance reconciliation will connect to the backend in a later pass.", BLUE));

        EditText amount = new EditText(this);
        amount.setHint("Amount requested — leave blank for full balance");
        amount.setHintTextColor(MUTED);
        amount.setTextColor(TEXT);
        amount.setSingleLine(true);
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amount.setPadding(dp(12), 0, dp(12), 0);
        amount.setBackground(rounded(BG2, BORDER, 11));
        c.addView(amount, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        spacer(c, 8);

        EditText note = new EditText(this);
        note.setHint("Optional note — e.g. war supplies");
        note.setHintTextColor(MUTED);
        note.setTextColor(TEXT);
        note.setSingleLine(true);
        note.setPadding(dp(12), 0, dp(12), 0);
        note.setBackground(rounded(BG2, BORDER, 11));
        c.addView(note, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        spacer(c, 10);

        Button submit = primary("Submit Test Payout Request");
        submit.setOnClickListener(v -> {
            BankingDraftStore.add(this, session, amount.getText().toString(), note.getText().toString());
            Toast.makeText(this, "Test request saved.", Toast.LENGTH_SHORT).show();
            showBanking();
        });
        c.addView(submit, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        spacer(c, 14);

        if (session.hasGlobalToolAccess()) {
            addCard(c, card("Queue visibility enabled",
                    "Your Red/Black/global access qualifies you to see faction payout requests. The shared queue itself is not connected in this prototype yet.", GOOD));
        }

        addCard(c, card("Retroactive rule",
                "When the chat listener is connected, requests found later with a current faction balance under $1,000,000 will be logged as likely already handled rather than cluttering the normal pending queue.", ACCENT));

        c.addView(section("Requests saved on this phone"));
        spacer(c, 8);
        JSONArray rows = BankingDraftStore.all(this);
        if (rows.length() == 0) {
            addCard(c, card("No test requests yet", "Create one above to test the companion workflow.", BORDER));
        } else {
            for (int i = rows.length() - 1; i >= 0; i--) {
                JSONObject row = rows.optJSONObject(i);
                if (row == null) continue;
                String when = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(new Date(row.optLong("created", 0)));
                String body = row.optString("amount", "FULL BALANCE") + " • " + when;
                String n = row.optString("note", "");
                if (!n.isEmpty()) body += "\n" + n;
                addCard(c, card("LOCAL TEST", body, BORDER));
            }
            Button clear = secondary("Clear Local Test Requests");
            clear.setOnClickListener(v -> { BankingDraftStore.clear(this); showBanking(); });
            c.addView(clear, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));
        }

        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    private void showLeadership() {
        if (session == null || !session.canManageAccess()) { showHome(); return; }
        screen = Screen.LEADERSHIP;
        ScrollView scroll = shell();
        LinearLayout c = column(scroll);
        addBack(c, "Leadership Controls");
        addCard(c, card("💬 Faction Chat Listener",
                "The bundled listener recognizes banker requests, balance requests, balance checks, withdrawals, cash-out wording and amount-specific requests such as “bank 25m”. It scans messages Torn has loaded while faction chat is actively viewed.", BLUE));
        addCard(c, card("Retroactive scanning",
                "Opening chat can catch already-loaded unread requests. Scrolling upward lets the listener inspect older messages as Torn renders them. Duplicate fingerprints prevent repeat processing.", GOOD));
        addCard(c, card("TornPDA install path",
                "Settings → Advanced browser settings → Manage scripts. The listener source remains bundled with the app project; one-tap export will be enabled when the shared banking backend is deployed.", ACCENT));

        if (session.positions != null && session.positions.length() > 0) {
            c.addView(section("Faction positions returned by Torn"));
            spacer(c, 8);
            for (int i = 0; i < session.positions.length(); i++) {
                JSONObject pos = session.positions.optJSONObject(i);
                if (pos == null) continue;
                JSONArray abilities = pos.optJSONArray("abilities");
                AccessTier tier = AccessPolicy.tierForAbilities(abilities);
                addCard(c, card(pos.optString("name", "Position"), tier.label + " permission band", tier.displayColor()));
            }
        }
        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    private void showDeveloper() {
        if (!AppRoles.isOwner(session)) { showHome(); return; }
        screen = Screen.DEVELOPER;
        ScrollView scroll = shell();
        LinearLayout c = column(scroll);
        addBack(c, "Developer Console");
        addCard(c, card("Owner identity active",
                session.playerName + " [" + session.playerId + "] is recognized as the Duck Force Toolkit Owner/Developer. The API key itself is not the developer credential.", ACCENT));
        addCard(c, card("Private tools",
                "Company Train Calculator is now separated from the faction-facing companion menu and appears under My Tools for the Owner.", BLUE));
        addCard(c, card("Player grants — next backend step",
                "The architecture is ready for backend-controlled individual grants, delegated developers, beta features and private tools without shipping separate APKs.", GOOD));
        addCard(c, card("Release foundation",
                "v0.4.0 begins the permanent signing and Play Store preparation track. Target: Play Store-ready v0.7.0.", BORDER));
        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    private void addBack(LinearLayout c, String title) {
        Button back = secondary("← Home");
        back.setOnClickListener(v -> showHome());
        c.addView(back, new LinearLayout.LayoutParams(dp(104), dp(42)));
        TextView t = text(title, 26, TEXT, true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.topMargin = dp(14);
        tp.bottomMargin = dp(14);
        c.addView(t, tp);
    }

    @Override
    public void onBackPressed() {
        if (screen != Screen.HOME && screen != Screen.LOGIN) showHome();
        else super.onBackPressed();
    }
}
