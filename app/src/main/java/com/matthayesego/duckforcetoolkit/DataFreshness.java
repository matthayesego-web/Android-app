package com.matthayesego.duckforcetoolkit;

/** Small UI helper for describing cached/live data without exposing implementation details. */
public final class DataFreshness {
    private DataFreshness() {}

    public static String label(long ageMs, boolean refreshing) {
        String age = ageText(ageMs);
        if (refreshing) return ageMs >= 0L ? age + " • refreshing…" : "Refreshing…";
        return ageMs >= 0L ? age : "Waiting for first refresh";
    }

    public static String ageText(long ageMs) {
        if (ageMs < 0L) return "Not updated yet";
        long seconds = Math.max(0L, ageMs / 1000L);
        if (seconds < 10L) return "Updated just now";
        if (seconds < 60L) return "Updated " + seconds + " sec ago";
        long minutes = seconds / 60L;
        if (minutes < 60L) return "Updated " + minutes + " min ago";
        long hours = minutes / 60L;
        if (hours < 24L) return "Updated " + hours + " hr" + (hours == 1L ? "" : "s") + " ago";
        long days = hours / 24L;
        return "Updated " + days + " day" + (days == 1L ? "" : "s") + " ago";
    }
}
