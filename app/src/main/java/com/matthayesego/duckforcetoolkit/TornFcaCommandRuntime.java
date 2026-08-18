package com.matthayesego.duckforcetoolkit;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

/**
 * Canonical command-shell runtime for the v0.10.1 -> v1.0 line.
 *
 * The side-by-side Beta keeps Beta branding/package identity, while the production candidate uses
 * the same proven command-center shell with normal TornFCA branding. Keeping the target in one
 * place prevents legal/war/return flows from drifting back to the legacy shell.
 */
public final class TornFcaCommandRuntime {
    private TornFcaCommandRuntime() {}

    /** v0.10.1 makes the command-center the canonical visible shell for both build variants. */
    public static boolean enabled() { return true; }

    public static boolean isBetaBuild() {
        return BuildConfig.APPLICATION_ID != null && BuildConfig.APPLICATION_ID.endsWith(".beta");
    }

    public static void install(Application app) {
        if (app == null || !enabled()) return;
        BetaSurfacePolish.install(app);
        BetaGaugeLiveData.install(app);
        BetaUxRepair.install(app);
    }

    public static Intent homeIntent(Context context, String section) {
        Intent i = new Intent(context, BetaCommandActivity.class);
        if (section != null && !section.trim().isEmpty()) {
            i.putExtra(BetaCommandActivity.EXTRA_SECTION, section.trim());
        }
        return i;
    }

    public static String topBrand() {
        return isBetaBuild() ? "TORN FCA BETA" : "TORN FCA";
    }

    public static String versionBadge() {
        return isBetaBuild() ? "BETA   •   v" + TornFcaBrand.VERSION : "v" + TornFcaBrand.VERSION;
    }

    public static String footerPrefix() {
        return isBetaBuild() ? "Torn FCA Beta v" : "Torn FCA v";
    }

    public static String footer(String factionName) {
        String faction = factionName == null || factionName.trim().isEmpty() ? "Faction" : factionName.trim();
        return footerPrefix() + TornFcaBrand.VERSION + "  •  " + faction;
    }
}
