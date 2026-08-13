package com.matthayesego.duckforcetoolkit;

public final class AppRoles {
    private static final int OWNER_ID = 3987363;

    private AppRoles() {}

    public static boolean isOwner(AuthSession session) {
        return session != null && session.playerId == OWNER_ID;
    }

    public static String label(AuthSession session) {
        return isOwner(session) ? "Owner / Developer" : "Faction Member";
    }
}
