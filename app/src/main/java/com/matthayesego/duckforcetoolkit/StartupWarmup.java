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
 * Warms TornFCA once per Android process.
 *
 * Putting the app in the background does not trigger another warm cycle. If Android kills the
 * process or the user fully closes it, these process flags disappear and the next cold launch warms
 * the services again. Individual screens may still refresh their own time-sensitive data.
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

    private static final int CORE_TASKS = 5;
    private static boolean startedThisProcess=false;
    private static boolean finishedThisProcess=false;
    private static Result lastResult;

    private StartupWarmup() {}

    public static synchronized boolean hasStartedThisProcess(){return startedThisProcess;}
    public static synchronized boolean hasFinishedThisProcess(){return finishedThisProcess;}
    public static synchronized Result lastResult(){return lastResult;}

    public static void start(Context context, Listener listener) {
        if (context == null) return;
        synchronized (StartupWarmup.class) {
            if (startedThisProcess) {
                Result cached=lastResult;
                if(listener!=null)listener.onFinished(cached==null?new Result(null,0,CORE_TASKS+1,"Startup warmup is already running in this app session."):cached);
                return;
            }
            startedThisProcess=true;
        }
        Context app = context.getApplicationContext();
        new Thread(() -> run(app, listener), "TornFCA-StartupWarmup").start();
    }

    private static void run(Context app, Listener listener) {
        progress(listener, "Verifying your Torn session…", 0, CORE_TASKS + 1);
        SecureApiKeyStore keyStore = new SecureApiKeyStore(app);
        String key = keyStore.load();
        if (key == null || key.isBlank()) {
            complete(listener, new Result(null, 0, CORE_TASKS + 1, "Connect a Torn API key to warm faction data."));
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
            complete(listener, new Result(null, 0, CORE_TASKS + 1,
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

        launch("TornFCA-Warm-Roster", latch, () -> {
            JSONObject response = TornApiClient.getJson("/faction/members", key);
            JSONArray members=response.optJSONArray("members");
            if(members!=null)FactionMemberCache.save(verified.factionId,members);
        }, listener, done, "Faction roster ready.");

        launch("TornFCA-Warm-Community", latch, () -> CommunityBackendClient.config(key),
                listener, done, "Community services ready.");

        // Faction/Community are covered by real warm data, Premium refreshes below, and Developer
        // policy has its own TTL-aware refresh. Only otherwise-idle services get passive wakeups.
        prewarmSecondaryBackends();

        try { latch.await(10L, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        try { PremiumBackendClient.refreshAsync(app, verified.playerId); } catch (Exception ignored) {}
        try { PushNotifications.syncIfReady(app); } catch (Exception ignored) {}

        int completed = Math.min(CORE_TASKS + 1, done.get());
        String warning = completed < CORE_TASKS + 1 ? "Some services are still warming in the background." : "";
        complete(listener, new Result(verified, completed, CORE_TASKS + 1, warning));
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
            } finally {
                latch.countDown();
            }
        }, name).start();
    }

    private static void progress(Listener listener, String message, int completed, int total) {
        if (listener != null) listener.onProgress(message, completed, total);
    }

    private static void complete(Listener listener, Result result) {
        synchronized (StartupWarmup.class) {
            lastResult=result;
            finishedThisProcess=true;
        }
        if (listener != null) listener.onFinished(result);
    }

    private static void prewarmSecondaryBackends() {
        String[] urls = {
                BuildConfig.WARPAY_BACKEND_URL,
                BuildConfig.FEEDBACK_BACKEND_URL
        };
        for (String raw : urls) {
            final String value = raw == null ? "" : raw.trim();
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
            if (in != null) try (InputStream ignored = in) { while (ignored.read() != -1) {} }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.disconnect();
        }
    }
}
