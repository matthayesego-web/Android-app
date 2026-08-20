package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** A brief, tappable faction-announcement banner that follows authenticated TornFCA screens. */
public final class FactionAnnouncementOverlay {
    private static final String TAG = "tornfca-faction-notice-overlay";
    private static final long AUTO_DISMISS_MS = 3200L;
    private static final Map<Activity, Boolean> ACTIVE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Activity, View> VISIBLE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Activity, String> VISIBLE_IDS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Activity, Boolean> OBSERVED = Collections.synchronizedMap(new WeakHashMap<>());

    private FactionAnnouncementOverlay() {}

    public static void attach(Activity activity) {
        if (activity == null || activity.isFinishing() || excluded(activity)) return;
        ACTIVE.put(activity, Boolean.TRUE);
        FrameLayout content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        observe(activity, content);
        ensure(activity, content);
    }

    private static void observe(Activity activity, FrameLayout content) {
        if (OBSERVED.put(activity, Boolean.TRUE) != null) return;
        content.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (activity.isFinishing() || !ACTIVE.containsKey(activity)) return;
            FrameLayout latest = activity.findViewById(android.R.id.content);
            if (latest != null) ensure(activity, latest);
        });
    }

    private static void ensure(Activity activity, FrameLayout content) {
        SecureApiKeyStore keyStore = new SecureApiKeyStore(activity);
        String key = keyStore.load();
        if (key == null || key.isBlank()) { removeVisible(activity, content); return; }
        AuthSession hot = TornApiClient.cachedSession(key);
        FactionScopeCache.Scope scope = hot == null ? FactionScopeCache.load(activity, key) : null;
        int factionId = hot != null ? hot.factionId : scope == null ? 0 : scope.factionId;
        String factionName = hot != null ? hot.factionName : scope == null ? "Faction" : scope.factionName;
        String position = hot != null ? hot.position : scope == null ? "" : scope.position;
        if (factionId <= 0) { removeVisible(activity, content); return; }

        JSONObject notice = StartupWarmCache.latestVisibleNotice(activity, factionId);
        if (notice == null) { removeVisible(activity, content); return; }
        String id = notice.optString("id", "");
        View existing = VISIBLE.get(activity);
        if (existing != null && existing.getParent() == content && id.equals(VISIBLE_IDS.get(activity))) return;
        removeVisible(activity, content);

        String title = notice.optString("title", "Faction announcement").trim();
        String body = notice.optString("message", "").trim();
        LinearLayout row = new LinearLayout(activity);
        row.setTag(TAG);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = dp(activity, 12);
        row.setPadding(pad, dp(activity, 9), dp(activity, 7), dp(activity, 9));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(34, 25, 40), Color.rgb(12, 19, 28)});
        bg.setCornerRadius(dp(activity, 14));
        bg.setStroke(dp(activity, 1), Color.rgb(243, 184, 52));
        row.setBackground(bg);
        row.setElevation(dp(activity, 12));

        LinearLayout copy = new LinearLayout(activity);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView headline = new TextView(activity);
        headline.setText("NEW ANNOUNCEMENT  •  " + title);
        headline.setTextColor(Color.rgb(248, 210, 124));
        headline.setTextSize(12.5f);
        headline.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        headline.setMaxLines(1);
        headline.setEllipsize(android.text.TextUtils.TruncateAt.END);
        copy.addView(headline, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView preview = new TextView(activity);
        preview.setText(body.isEmpty() ? "Tap to open announcements" : body + "  •  Tap to read");
        preview.setTextColor(Color.rgb(240, 243, 248));
        preview.setTextSize(12f);
        preview.setMaxLines(1);
        preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mp.topMargin = dp(activity, 2);
        copy.addView(preview, mp);
        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView close = new TextView(activity);
        close.setText("×");
        close.setGravity(Gravity.CENTER);
        close.setTextColor(Color.rgb(190, 197, 208));
        close.setTextSize(22f);
        close.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        close.setContentDescription("Dismiss announcement banner");
        row.addView(close, new LinearLayout.LayoutParams(dp(activity, 40), dp(activity, 42)));
        close.setOnClickListener(v -> StartupWarmCache.dismissNotice(activity, id));

        String finalFactionName = factionName;
        String finalPosition = position;
        row.setOnClickListener(v -> {
            StartupWarmCache.dismissNotice(activity, id);
            Intent i = new Intent(activity, WarNoticeActivity.class);
            i.putExtra(WarNoticeActivity.EXTRA_FACTION_ID, factionId);
            i.putExtra(WarNoticeActivity.EXTRA_FACTION_NAME, finalFactionName);
            i.putExtra(WarNoticeActivity.EXTRA_CAN_PUBLISH, AccessPolicy.isLeaderPosition(finalPosition));
            activity.startActivity(i);
        });

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP);
        lp.leftMargin = dp(activity, 12);
        lp.rightMargin = dp(activity, 12);
        lp.topMargin = statusBarHeight(activity) + dp(activity, 8);
        content.addView(row, lp);
        VISIBLE.put(activity, row);
        VISIBLE_IDS.put(activity, id);

        row.postDelayed(() -> {
            if (activity.isFinishing()) return;
            View current = VISIBLE.get(activity);
            if (current == row && id.equals(VISIBLE_IDS.get(activity))) StartupWarmCache.dismissNotice(activity, id);
        }, AUTO_DISMISS_MS);
    }

    public static void detach(Activity activity) {
        if (activity == null) return;
        ACTIVE.remove(activity);
        FrameLayout content = activity.findViewById(android.R.id.content);
        if (content != null) removeVisible(activity, content);
        VISIBLE.remove(activity);
        VISIBLE_IDS.remove(activity);
    }

    public static void refreshVisible() {
        Activity[] activities;
        synchronized (ACTIVE) { activities = ACTIVE.keySet().toArray(new Activity[0]); }
        for (Activity activity : activities) {
            if (activity == null || activity.isFinishing()) continue;
            activity.runOnUiThread(() -> {
                FrameLayout content = activity.findViewById(android.R.id.content);
                if (content != null) ensure(activity, content);
            });
        }
    }

    private static void removeVisible(Activity activity, FrameLayout content) {
        View existing = VISIBLE.remove(activity);
        VISIBLE_IDS.remove(activity);
        if (existing != null) existing.removeCallbacks(null);
        if (existing != null && existing.getParent() == content) content.removeView(existing);
        for (int i = content.getChildCount() - 1; i >= 0; i--) {
            View child = content.getChildAt(i);
            if (TAG.equals(String.valueOf(child.getTag()))) content.removeViewAt(i);
        }
    }

    private static boolean excluded(Activity activity) {
        return activity instanceof AccessGateActivity
                || activity instanceof LegalActivity
                || activity instanceof LegalDocumentActivity
                || activity instanceof WarNoticeActivity;
    }

    private static int statusBarHeight(Activity activity) {
        int id = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? activity.getResources().getDimensionPixelSize(id) : 0;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
