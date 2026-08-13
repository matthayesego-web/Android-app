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

    public AuthSession(int playerId, String playerName, int factionId, String factionName, String position,
                       boolean factionApiAccess, AccessTier tier, JSONArray positions) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.factionId = factionId;
        this.factionName = factionName;
        this.position = position;
        this.factionApiAccess = factionApiAccess;
        this.tier = tier;
        this.positions = positions;
    }

    public boolean canManageAccess() { return AccessPolicy.isLeaderPosition(position); }

    public boolean hasGlobalToolAccess() {
        return AccessPolicy.isLeaderPosition(position)
                || tier == AccessTier.RED || tier == AccessTier.BLACK || tier == AccessTier.GLOBAL;
    }

    public String accessLabel() {
        if (AccessPolicy.isLeaderPosition(position)) return AccessTier.GLOBAL.label;
        if (tier == AccessTier.RED || tier == AccessTier.BLACK) return tier.label + " • Global tools";
        return tier.label;
    }
}
