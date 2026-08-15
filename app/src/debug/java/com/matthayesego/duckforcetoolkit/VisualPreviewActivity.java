package com.matthayesego.duckforcetoolkit;

import android.os.Bundle;

import org.json.JSONArray;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Internal-debug-only visual harness. Never packaged in release builds. */
public class VisualPreviewActivity extends PolishedCompanionActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Field sessionField = CompanionActivity.class.getDeclaredField("session");
            sessionField.setAccessible(true);
            AuthSession preview = new AuthSession(
                    3987363,
                    "MattWithADuck",
                    0,
                    "Duck Force",
                    "Leader",
                    true,
                    AccessTier.GLOBAL,
                    new JSONArray(),
                    new JSONArray(),
                    true
            );
            sessionField.set(this, preview);

            Method showHome = CompanionActivity.class.getDeclaredMethod("showHome");
            showHome.setAccessible(true);
            showHome.invoke(this);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to render internal visual preview", e);
        }
    }
}
