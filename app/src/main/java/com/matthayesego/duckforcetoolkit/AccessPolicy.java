package com.matthayesego.duckforcetoolkit;

import org.json.JSONArray;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class AccessPolicy {
    private AccessPolicy() {}

    private static final Set<String> ORANGE = normalizedSet(
            "Organised Crimes", "Organized Crimes", "Item Giving", "Money Giving",
            "Points Giving", "Forum Management", "Application Management"
    );
    private static final Set<String> RED = normalizedSet(
            "Kick Members", "Kick Member", "Balance Adjustment", "War Management", "Upgrade Management"
    );
    private static final Set<String> BLACK = normalizedSet(
            "Newsletter Sending", "Announcement Changes", "Description Changes"
    );

    private static Set<String> normalizedSet(String... values) {
        Set<String> out = new HashSet<>();
        Arrays.stream(values).forEach(v -> out.add(normalize(v)));
        return out;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static AccessTier tierForAbilities(JSONArray abilities) {
        AccessTier result = AccessTier.GREEN;
        if (abilities == null) return result;
        for (int i = 0; i < abilities.length(); i++) {
            String ability = normalize(abilities.optString(i));
            if (BLACK.contains(ability)) return AccessTier.BLACK;
            if (RED.contains(ability)) result = AccessTier.RED;
            else if (ORANGE.contains(ability) && result.level < AccessTier.ORANGE.level) result = AccessTier.ORANGE;
        }
        return result;
    }

    public static boolean isLeaderPosition(String position) {
        if (position == null) return false;
        String p = position.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "").replace(" ", "").trim();
        return "leader".equals(p) || "coleader".equals(p);
    }
}
