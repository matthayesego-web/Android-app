package com.matthayesego.duckforcetoolkit;

import org.json.JSONArray;

public final class AuthSession {
    public final int playerId;
    public final String playerName;
    public final int factionId;
    public final String factionName;
    public final String position;
    public final boolean factionApiAccess;
    public final AccessTier tier;
    public final JSONArray positions;
    public final JSONArray abilities;
    public final boolean permissionsResolved;

    public AuthSession(int playerId, String playerName, int factionId, String factionName, String position,
                       boolean factionApiAccess, AccessTier tier, JSONArray positions,
                       JSONArray abilities, boolean permissionsResolved) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.factionId = factionId;
        this.factionName = factionName;
        this.position = position;
        this.factionApiAccess = factionApiAccess;
        this.tier = tier;
        this.positions = positions == null ? new JSONArray() : positions;
        this.abilities = abilities == null ? new JSONArray() : abilities;
        this.permissionsResolved = permissionsResolved;
    }

    public AuthSession withPermissions(JSONArray resolvedAbilities) {
        JSONArray safe = resolvedAbilities == null ? new JSONArray() : resolvedAbilities;
        return new AuthSession(playerId, playerName, factionId, factionName, position,
                factionApiAccess, AccessPolicy.tierForAbilities(safe), positions, safe, true);
    }

    public boolean hasPermission(String permission) {
        if (AccessPolicy.isLeaderPosition(position)) return true;
        if (permission == null || permission.trim().isEmpty()) return false;
        for (int i = 0; i < abilities.length(); i++) {
            if (permission.equalsIgnoreCase(abilities.optString(i, "").trim())) return true;
        }
        return false;
    }

    public boolean canManageAccess() { return AccessPolicy.isLeaderPosition(position); }

    public boolean canPublishNotices() {
        return AccessPolicy.isLeaderPosition(position) || hasPermission("Announcement Changes");
    }

    public boolean canManageBankingQueue() {
        return AccessPolicy.isLeaderPosition(position)
                || hasPermission("Money Giving")
                || hasPermission("Balance Adjustment");
    }

    public boolean hasGlobalToolAccess() { return AccessPolicy.isLeaderPosition(position); }

    public String accessLabel() {
        if (AccessPolicy.isLeaderPosition(position)) return "Leadership permissions";
        if (permissionsResolved) return "Torn permissions verified";
        return "Torn permissions not cached";
    }
}
