package com.matthayesego.duckforcetoolkit;

import org.json.JSONArray;
import org.json.JSONObject;

public final class WarStatus {
    public final boolean present;
    public final int warId;
    public final long start;
    public final long end;
    public final int target;
    public final int opponentFactionId;
    public final String opponent;
    public final int ourScore;
    public final int opponentScore;

    private WarStatus(boolean present, int warId, long start, long end, int target,
                      int opponentFactionId, String opponent, int ourScore, int opponentScore) {
        this.present = present;
        this.warId = warId;
        this.start = start;
        this.end = end;
        this.target = target;
        this.opponentFactionId = opponentFactionId;
        this.opponent = opponent == null ? "Opponent" : opponent;
        this.ourScore = ourScore;
        this.opponentScore = opponentScore;
    }

    public static WarStatus none() { return new WarStatus(false, 0, 0, 0, 0, 0, "", 0, 0); }

    public static WarStatus from(JSONObject root, int factionId) {
        if (root == null) return none();
        JSONObject wars = root.optJSONObject("wars");
        JSONObject ranked = wars == null ? null : wars.optJSONObject("ranked");
        if (ranked == null) return none();
        int warId = ranked.optInt("war_id", 0);
        long start = ranked.optLong("start", 0);
        long end = ranked.isNull("end") ? 0 : ranked.optLong("end", 0);
        int target = ranked.optInt("target", 0);
        int opponentFactionId = 0;
        String opponent = "Opponent";
        int ourScore = 0, opponentScore = 0;
        JSONArray factions = ranked.optJSONArray("factions");
        if (factions != null) {
            for (int i = 0; i < factions.length(); i++) {
                JSONObject f = factions.optJSONObject(i);
                if (f == null) continue;
                int id = f.optInt("id", 0);
                if (id == factionId) ourScore = f.optInt("score", 0);
                else {
                    opponentFactionId = id;
                    opponent = f.optString("name", opponent);
                    opponentScore = f.optInt("score", 0);
                }
            }
        }
        return new WarStatus(true, warId, start, end, target, opponentFactionId, opponent, ourScore, opponentScore);
    }

    public boolean isUpcoming(long nowSeconds) { return present && start > nowSeconds; }
    public boolean isLive(long nowSeconds) { return present && start > 0 && nowSeconds >= start && (end <= 0 || nowSeconds < end); }

    public String headline(long nowSeconds) {
        if (!present) return "No ranked war currently scheduled";
        if (isUpcoming(nowSeconds)) return "WAR STARTS IN " + duration(start - nowSeconds);
        if (isLive(nowSeconds)) return "WAR LIVE — " + ourScore + " vs " + opponentScore;
        return "Latest ranked war ended";
    }

    public String detail(long nowSeconds) {
        if (!present) return "Tap for war status and leadership notices.";
        String targetText = target > 0 ? " • target " + target : "";
        if (isUpcoming(nowSeconds)) return opponent + targetText;
        if (isLive(nowSeconds)) return opponent + targetText + " • " + duration(Math.max(0, nowSeconds - start)) + " elapsed";
        return opponent + " • final " + ourScore + "–" + opponentScore;
    }

    public static String duration(long seconds) {
        long s = Math.max(0, seconds);
        long days = s / 86400; s %= 86400;
        long hours = s / 3600; s %= 3600;
        long mins = s / 60; s %= 60;
        if (days > 0) return days + "d " + hours + "h " + mins + "m";
        if (hours > 0) return hours + "h " + mins + "m " + s + "s";
        return mins + "m " + s + "s";
    }
}
