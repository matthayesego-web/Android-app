package com.matthayesego.duckforcetoolkit;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Warms the app's reusable data before Home is shown.
 *
 * Public GET health pings wake every Apps Script deployment without spending Torn API requests.
 * Once the current Torn session is verified, the high-value authenticated datasets are loaded in
 * parallel and stored in StartupWarmCache. The launcher owns the visible timeout; unfinished work
 * can safely continue in the process and populate caches after Home opens.
 */
public final class StartupWarmup {
    public interface Listener {
        void onProgress(String message, int completed, int total);
        void onFinished(Result result);
    }

    public static final class Result {
        public final AuthSession session;
        public final int completed;
        public final int total;
        public final String warning;

        Result(AuthSession session, int completed, int total, String warning) {
            this.session = session;
            this.completed = completed;
            this.total = total;
            this.warning = warning == null ? "" : warning;
        }
    }

    private static final int CORE_TASKS = 4;

    private StartupWarmup() {}

    public static void start(Context context, Listener listener) {
        if (context == null) return;
        Context app = context.getApplicationContext();
        new Thread(() -> run(app, listener), "TornFCA-StartupWarmup").start();
    }

    private static void run(Context app, Listener listener) {
        prewarmBackendHealth();
        progress(listener, "Verifying your Torn session…", 0, CORE_TASKS + 1);
        SecureApiKeyStore keyStore = new SecureApiKeyStore(app);
        String key = keyStore.load();
        if (key == null || key.isBlank()) {
            finished(listener, new Result(null, 0, CORE_TASKS + 1, "Connect a Torn API key to warm faction data."));
            return;
        }

        AuthSession session;
        try {
            session = TornApiClient.cachedSession(key);
            if (session == null) session = TornApiClient.authenticate(key);
            FactionScopeCache.save(app, key, session);
            StartupWarmCache.putSession(session);
            progress(listener, "Torn session ready. Loading faction data…", 1, CORE_TASKS + 1);
        } catch (Exception e) {
            finished(listener, new Result(null, 0, CORE_TASKS + 1,
                    e.getMessage() == null ? "Torn session verification failed." : e.getMessage()));
            return;
        }

        final AuthSession verified = session;
        CountDownLatch latch = new CountDownLatch(CORE_TASKS);
        AtomicInteger done = new AtomicInteger(1);

        launch("TornFCA-Warm-Notices", latch, () -> {
            JSONArray rows = CompanionBackendClient.getNotices(key);
            StartupWarmCache.putNotices(app, verified.factionId, rows);
        }, listener, done, "Faction notices ready.");

        launch("TornFCA-Warm-Banking", latch, () -> {
            JSONObject response = CompanionBackendClient.getBankingRequests(key, false);
            StartupWarmCache.putBanking(verified.factionId, verified.playerId, response);
        }, listener, done, "Banking queue ready.");

        launch("TornFCA-Warm-War", latch, () -> {
            JSONObject response = TornApiClient.getJson("/faction/wars", key);
            StartupWarmCache.putWar(verified.factionId, response);
        }, listener, done, "War status ready.");

        launch("TornFCA-Warm-Community", latch, () -> CommunityBackendClient.config(key),
                listener, done, "Community services ready.");

        try { latch.await(10L, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Secondary work should never hold the splash screen open.
        try { PremiumBackendClient.refreshAsync(app, verified.playerId); } catch (Exception ignored) {}
        try { PushNotifications.syncIfReady(app); } catch (Exception ignored) {}

        int completed = Math.min(CORE_TASKS + 1, done.get());
        String warning = completed < CORE_TASKS + 1
                ? "Some services are still warming in the background."
                : "";
        finished(listener, new Result(verified, completed, CORE_TASKS + 1, warning));
    }

    private interface WarmTask { void run() throws Exception; }

    private static void launch(String name, CountDownLatch latch, WarmTask task, Listener listener,
                               AtomicInteger done, String successMessage) {
        new Thread(() -> {
            try {
                task.run();
                int completed = done.incrementAndGet();
                progress(listener, successMessage, completed, CORE_TASKS + 1);
            } catch (Exception ignored) {
                // One slow/unavailable service must not block the entire app. Its screen can retry.
            } finally {
                latch.countDown();
            }
        }, name).start();
    }

    private static void progress(Listener listener, String message, int completed, int total) {
        if (listener != null) listener.onProgress(message, completed, total);
    }

    private static void finished(Listener listener, Result result) {
        if (listener != null) listener.onFinished(result);
    }

    private static void prewarmBackendHealth() {
        String[] urls = {
                BuildConfig.FACTION_BACKEND_URL,
                BuildConfig.COMMUNITY_BACKEND_URL,
                BuildConfig.PREMIUM_BACKEND_URL,
                BuildConfig.DEVELOPER_BACKEND_URL,
                BuildConfig.WARPAY_BACKEND_URL,
                BuildConfig.FEEDBACK_BACKEND_URL
        };
        for (int i = 0; i < urls.length; i++) {
            final String value = urls[i] == null ? "" : urls[i].trim();
            if (!value.startsWith("https://") || value.contains("###")) continue;
            new Thread(() -> ping(value), "TornFCA-Backend-Wake").start();
        }
    }

    private static void ping(String value) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(value).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(4500);
            c.setReadTimeout(5500);
            c.setUseCaches(false);
            c.setRequestProperty("Accept", "application/json");
            c.setRequestProperty("User-Agent", "TornFCA/" + TornFcaBrand.VERSION + " StartupWarmup");
            int code = c.getResponseCode();
            InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
            if (in != null) try (InputStream ignored = in) { while (ignored.read() != -1) { /* drain */ } }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.disconnect();
        }
    }
}
