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
import android.widget.ImageView;
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
    private static final String APP_VERSION = "0.4.1";
    private static final String DUCK_FORCE_NAME = "Duck Force";

    private static final int BG = Color.rgb(5, 8, 12);
    private static final int BG2 = Color.rgb(9, 13, 18);
    private static final int SURFACE = Color.rgb(13, 18, 24);
    private static final int PANEL = Color.rgb(17, 23, 31);
    private static final int PANEL2 = Color.rgb(22, 29, 38);
    private static final int BORDER = Color.rgb(43, 53, 65);
    private static final int GOLD = Color.rgb(209, 151, 66);
    private static final int GOLD2 = Color.rgb(239, 196, 124);
    private static final int TEXT = Color.rgb(244, 246, 248);
    private static final int MUTED = Color.rgb(154, 164, 176);
    private static final int BLUE = Color.rgb(90, 139, 180);
    private static final int GOOD = Color.rgb(79, 158, 101);
    private static final int BAD = Color.rgb(199, 84, 84);

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

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private GradientDrawable rounded(int color, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke);
        return d;
    }

    private GradientDrawable gradient(int start, int end, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        d.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke);
        return d;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setLineSpacing(0f, 1.08f);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    private TextView overline(String value) {
        TextView t = text(value.toUpperCase(), 11, GOLD2, true);
        t.setLetterSpacing(0.16f);
        return t;
    }

    private Button primary(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.rgb(18, 13, 7));
        b.setTextSize(14);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setBackground(gradient(GOLD2, GOLD, GOLD, 12));
        return b;
    }

    private Button secondary(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(TEXT);
        b.setTextSize(13);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setBackground(rounded(PANEL2, BORDER, 11));
        return b;
    }

    @SuppressWarnings("deprecation")
    private ScrollView shell() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        int l = dp(18), t = dp(14), r = dp(18), b = dp(28);
        scroll.setPadding(l, t, r, b);
        scroll.setClipToPadding(false);
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

    private void spacer(LinearLayout parent, int value) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(value)));
    }

    private ImageView badge(int size) {
        ImageView image = new ImageView(this);
        image.setImageResource(R.mipmap.ic_launcher);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setContentDescription("Duck Force");
        image.setBackground(rounded(BG2, GOLD, size / 2));
        image.setPadding(dp(2), dp(2), dp(2), dp(2));
        image.setClipToOutline(true);
        return image;
    }

    private LinearLayout card(String eyebrow, String title, String body, int accent) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(17), dp(15), dp(17), dp(15));
        c.setBackground(rounded(PANEL, accent == Color.TRANSPARENT ? BORDER : accent, 16));
        if (eyebrow != null && !eyebrow.isEmpty()) c.addView(overline(eyebrow));
        TextView titleView = text(title, 18, TEXT, true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.topMargin = eyebrow == null || eyebrow.isEmpty() ? 0 : dp(5);
        c.addView(titleView, tp);
        if (body != null && !body.isEmpty()) {
            TextView bodyView = text(body, 13, MUTED, false);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bp.topMargin = dp(6);
            c.addView(bodyView, bp);
        }
        return c;
    }

    private void addCard(LinearLayout parent, LinearLayout card) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(10);
        parent.addView(card, p);
    }

    private LinearLayout actionCard(String eyebrow, String title, String body, int accent, String action) {
        LinearLayout c = card(eyebrow, title, body, accent);
        TextView open = text(action, 12, GOLD2, true);
        open.setGravity(Gravity.END);
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        op.topMargin = dp(12);
        c.addView(open, op);
        c.setClickable(true);
        c.setFocusable(true);
        return c;
    }

    private void showLogin(String error) {
        screen = Screen.LOGIN;
        session = null;
        ScrollView scroll = shell();
        LinearLayout c = column(scroll);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(dp(24), dp(30), dp(24), dp(28));
        hero.setBackground(gradient(Color.rgb(26, 31, 38), Color.rgb(10, 14, 20), BORDER, 24));
        hero.addView(badge(102), new LinearLayout.LayoutParams(dp(102), dp(102)));
        spacer(hero, 18);
        TextView brand = overline("Duck Force");
        brand.setGravity(Gravity.CENTER);
        hero.addView(brand);
        TextView title = text("Faction Companion", 30, TEXT, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleP.topMargin = dp(5);
        hero.addView(title, titleP);
        TextView sub = text("Fast access to the faction tools and requests that matter when you are away from Torn.", 14, MUTED, false);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subP.topMargin = dp(9);
        hero.addView(sub, subP);
        c.addView(hero);
        spacer(c, 14);

        LinearLayout login = card("Secure sign-in", "Connect your Torn account",
                "Your key verifies your identity and Duck Force membership, then stays encrypted on this device.", Color.TRANSPARENT);
        TextView label = text("TORN API KEY", 10, MUTED, true);
        label.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams labelP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelP.topMargin = dp(15);
        login.addView(label, labelP);

        EditText key = new EditText(this);
        key.setHint("Enter your API key");
        key.setHintTextColor(Color.rgb(105, 116, 129));
        key.setTextColor(TEXT);
        key.setSingleLine(true);
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setPadding(dp(14), 0, dp(14), 0);
        key.setBackground(rounded(BG2, BORDER, 11));
        LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        kp.topMargin = dp(7);
        login.addView(key, kp);

        Button connect = primary("Connect to Duck Force");
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        cp.topMargin = dp(12);
        login.addView(connect, cp);

        TextView status = text(error == null ? "Encrypted locally  •  Duck Force only  •  v" + APP_VERSION : error,
                11, error == null ? MUTED : BAD, false);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stp.topMargin = dp(11);
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
        LinearLayout load = new LinearLayout(this);
        load.setOrientation(LinearLayout.VERTICAL);
        load.setGravity(Gravity.CENTER_HORIZONTAL);
        load.setPadding(dp(26), dp(40), dp(26), dp(40));
        load.setBackground(gradient(SURFACE, BG2, BORDER, 22));
        load.addView(badge(92), new LinearLayout.LayoutParams(dp(92), dp(92)));
        spacer(load, 18);
        TextView title = text("Duck Force Companion", 24, TEXT, true);
        title.setGravity(Gravity.CENTER);
        load.addView(title);
        TextView body = text("Verifying your saved Torn account…", 13, MUTED, false);
        body.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.topMargin = dp(7);
        load.addView(body, bp);
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
        hero.setBackground(gradient(Color.rgb(27, 33, 41), Color.rgb(12, 17, 23), BORDER, 20));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(badge(64), new LinearLayout.LayoutParams(dp(64), dp(64)));
        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(dp(14), 0, 0, 0);
        names.addView(overline("Duck Force Companion"));
        TextView welcome = text("Welcome back, " + session.playerName, 21, TEXT, true);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wp.topMargin = dp(3);
        names.addView(welcome, wp);
        top.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        hero.addView(top);

        TextView meta = text(session.position + "  •  " + session.accessLabel(), 12, MUTED, false);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mp.topMargin = dp(13);
        hero.addView(meta, mp);

        if (AppRoles.isOwner(session)) {
            TextView owner = text("OWNER / DEVELOPER", 10, GOLD2, true);
            owner.setLetterSpacing(0.10f);
            owner.setPadding(dp(9), dp(5), dp(9), dp(5));
            owner.setBackground(rounded(BG2, GOLD, 10));
            LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            op.topMargin = dp(10);
            hero.addView(owner, op);
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button refresh = secondary("Refresh access");
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

        spacer(c, 18);
        c.addView(overline("Faction companion"));
        spacer(c, 9);

        LinearLayout banking = actionCard("Banking", "Balance & payout requests",
                "Request your faction balance or payout and review your request history.", BLUE, "OPEN BANKING  ›");
        banking.setOnClickListener(v -> showBanking());
        addCard(c, banking);

        if (canUseFactionTool(AccessTier.ORANGE)) {
            addToolCard(c, "Armory", "Xanax Armory Log", "Review faction Xanax armory activity.", "ARMORY", AccessTier.ORANGE);
            addToolCard(c, "Audit", "Faction Xanax Auditor", "Review Xanax usage and contributor snapshots.", "AUDITOR", AccessTier.ORANGE);
        }

        if (session.canManageAccess()) {
            LinearLayout leadership = actionCard("Leadership", "Leadership Controls",
                    "Faction permissions, chat-listener guidance and administrative companion settings.", GOLD, "OPEN CONTROLS  ›");
            leadership.setOnClickListener(v -> showLeadership());
            addCard(c, leadership);
        }

        if (AppRoles.isOwner(session)) {
            spacer(c, 8);
            c.addView(overline("Private tools"));
            spacer(c, 9);
            addToolCard(c, "Private", "Company Train Calculator", "Company-training payment calculator.", "TRAIN", AccessTier.GREEN);
            LinearLayout dev = actionCard("Developer", "Developer Console",
                    "Owner status, private-tool controls and future per-player grants.", GOLD, "OPEN CONSOLE  ›");
            dev.setOnClickListener(v -> showDeveloper());
            addCard(c, dev);
        }

        TextView footer = text("Duck Force Companion  •  v" + APP_VERSION, 11, Color.rgb(103, 113, 125), false);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fp.topMargin = dp(8);
        c.addView(footer, fp);
        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    private boolean canUseFactionTool(AccessTier minimum) {
        return session != null && (session.hasGlobalToolAccess() || session.tier.level >= minimum.level);
    }

    private void addToolCard(LinearLayout parent, String eyebrow, String title, String body, String tool, AccessTier tier) {
        LinearLayout c = actionCard(eyebrow, title, body, tier.displayColor(), "OPEN  ›");
        TextView access = text("Access: " + tier.label, 10, tier.displayColor(), true);
        LinearLayout.LayoutParams accessP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        accessP.topMargin = dp(8);
        c.addView(access, accessP);
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
        addBack(c, "Banking", "Balance & payout companion");

        addCard(c, card("Prototype", "Local banking requests",
                "Requests are still stored on this phone for this test build. The shared Duck Force queue comes in the backend pass.", BLUE));

        TextView amountLabel = text("AMOUNT", 10, MUTED, true);
        amountLabel.setLetterSpacing(0.12f);
        c.addView(amountLabel);
        EditText amount = field("Leave blank for full balance", true);
        c.addView(amount, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        spacer(c, 10);

        TextView noteLabel = text("NOTE", 10, MUTED, true);
        noteLabel.setLetterSpacing(0.12f);
        c.addView(noteLabel);
        EditText note = field("Optional note — e.g. war supplies", false);
        c.addView(note, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        spacer(c, 12);

        Button submit = primary("Submit payout request");
        submit.setOnClickListener(v -> {
            BankingDraftStore.add(this, session, amount.getText().toString(), note.getText().toString());
            Toast.makeText(this, "Request saved on this device.", Toast.LENGTH_SHORT).show();
            showBanking();
        });
        c.addView(submit, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        spacer(c, 14);

        if (session.hasGlobalToolAccess()) {
            addCard(c, card("Access", "Queue visibility enabled",
                    "Your current faction access qualifies you to view the shared payout queue once the backend is connected.", GOOD));
        }

        c.addView(overline("Requests on this phone"));
        spacer(c, 9);
        JSONArray rows = BankingDraftStore.all(this);
        if (rows.length() == 0) {
            addCard(c, card("History", "No requests yet", "Submit a request above to test the companion workflow.", Color.TRANSPARENT));
        } else {
            for (int i = rows.length() - 1; i >= 0; i--) {
                JSONObject row = rows.optJSONObject(i);
                if (row == null) continue;
                String when = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(row.optLong("created", 0)));
                String body = row.optString("amount", "FULL BALANCE") + "  •  " + when;
                String n = row.optString("note", "");
                if (!n.isEmpty()) body += "\n" + n;
                addCard(c, card("Local request", "Pending test request", body, BORDER));
            }
            Button clear = secondary("Clear local test requests");
            clear.setOnClickListener(v -> { BankingDraftStore.clear(this); showBanking(); });
            c.addView(clear, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));
        }

        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    private EditText field(String hint, boolean numeric) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Color.rgb(105, 116, 129));
        e.setTextColor(TEXT);
        e.setSingleLine(true);
        if (numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setPadding(dp(13), 0, dp(13), 0);
        e.setBackground(rounded(BG2, BORDER, 11));
        return e;
    }

    private void showLeadership() {
        if (session == null || !session.canManageAccess()) { showHome(); return; }
        screen = Screen.LEADERSHIP;
        ScrollView scroll = shell();
        LinearLayout c = column(scroll);
        addBack(c, "Leadership Controls", "Duck Force administration");
        addCard(c, card("Chat listener", "Banking request detection",
                "Recognizes banker, balance, balance-check, withdraw, cash-out and amount-specific requests while faction chat is actively viewed.", BLUE));
        addCard(c, card("Catch-up", "Retroactive loaded-message scan",
                "Opening faction chat scans messages Torn has already loaded. Scrolling upward scans older messages as Torn renders them.", GOOD));
        addCard(c, card("Mobile", "TornPDA installation",
                "Settings → Advanced browser settings → Manage scripts. One-tap export will be enabled with the shared banking backend.", GOLD));

        if (session.positions != null && session.positions.length() > 0) {
            spacer(c, 4);
            c.addView(overline("Faction positions"));
            spacer(c, 9);
            for (int i = 0; i < session.positions.length(); i++) {
                JSONObject pos = session.positions.optJSONObject(i);
                if (pos == null) continue;
                AccessTier tier = AccessPolicy.tierForAbilities(pos.optJSONArray("abilities"));
                addCard(c, card("Permission band", pos.optString("name", "Position"), tier.label + " access", tier.displayColor()));
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
        addBack(c, "Developer Console", "Private owner controls");
        addCard(c, card("Identity", "Owner / Developer active",
                session.playerName + " [" + session.playerId + "] is recognized by Torn ID. Your API key is not the developer credential.", GOLD));
        addCard(c, card("Private tools", "Company Train Calculator",
                "Private tools are separated from the faction-facing companion menu and can later be granted per player.", BLUE));
        addCard(c, card("Next", "Player grants",
                "Backend-controlled individual grants, delegated developers and beta features remain the next access-control step.", GOOD));
        addCard(c, card("Release", "Play Store track",
                "v0.4.1 is the professional-polish pass. Target remains Play Store-ready v0.7.0.", Color.TRANSPARENT));
        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    private void addBack(LinearLayout c, String title, String subtitle) {
        Button back = secondary("← Companion");
        back.setOnClickListener(v -> showHome());
        c.addView(back, new LinearLayout.LayoutParams(dp(122), dp(42)));
        TextView t = text(title, 27, TEXT, true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.topMargin = dp(16);
        c.addView(t, tp);
        TextView s = text(subtitle, 13, MUTED, false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = dp(4);
        sp.bottomMargin = dp(15);
        c.addView(s, sp);
    }

    @Override
    public void onBackPressed() {
        if (screen != Screen.HOME && screen != Screen.LOGIN) showHome();
        else super.onBackPressed();
    }
}
