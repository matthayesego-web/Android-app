package com.matthayesego.duckforcetoolkit;

import android.content.Context;

/**
 * Presentation-only access reducer used by Standard Member Preview.
 *
 * This never grants authority and never changes the real Torn identity sent to a backend.
 * It only removes leadership presentation/capabilities while a developer is testing the normal
 * member experience. Server-side Torn verification remains authoritative for real writes.
 */
public final class MemberPresentationPolicy {
    private MemberPresentationPolicy() {}

    public static boolean memberPreview(Context context) {
        return context != null && DeveloperPreviewStore.isMemberPreview(context);
    }

    public static boolean leadershipVisible(Context context, String actualPosition) {
        return !memberPreview(context) && AccessPolicy.isLeaderPosition(actualPosition);
    }

    public static String effectivePosition(Context context, String actualPosition) {
        if (memberPreview(context)) return "Member";
        return actualPosition == null || actualPosition.trim().isEmpty() ? "Member" : actualPosition;
    }
}
